/*******************************************************************************
 * Copyright 2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package emlab.gen.role.pricewarranty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;


import emlab.gen.domain.agent.EMLabModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.renewablesupport.BaseCostFip;
import emlab.gen.domain.policy.renewablesupport.BiasFactor;
import emlab.gen.domain.policy.renewablesupport.ForecastingInformationReport;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.engine.Schedule;
import emlab.gen.repository.Reps;

import emlab.gen.util.GeometricTrendRegression;
import emlab.gen.util.MapValueComparator;

/**
 * @author Kaveri3012 This role loops through eligible technologies, eligible
 *         nodes,
 * 
 *         computes LCOE per technology per node and creates an object,
 *         BaseCost, to store it.
 * 
 *         In technology neutral mode, after computing LCOE per technology, it
 *         should store LCOE per technology and create a merit order upto which
 *         a cetrain target is filled.
 * 
 */

public class ComputePremiumRoleExAnte extends AbstractComputePremiumRole{

    Reps reps;

    public ComputePremiumRoleExAnte(Schedule schedule) {
        super(schedule);
    }

    private class Key2D {
        private final PowerGeneratingTechnology techKey;
        private final PowerGridNode nodeKey;

        private Key2D(PowerGeneratingTechnology key1, PowerGridNode key2) {
            this.techKey = key1;
            this.nodeKey = key2;
        }

        // include default implementations for
        // Object.equals(Object) and Object.hashCode()
        // Tip: If you're using Eclipse it can generate
        // them for you.
    }


    public void act(RenewableSupportFipScheme scheme) {

        Regulator regulator = scheme.getRegulator();

        // should be close to the investor's future time point.
        long futureTimePoint = scheme.getFutureSchemeStartTime() + getCurrentTick();

        ElectricitySpotMarket market = reps.findElectricitySpotMarketForZone(regulator.getZone());

        Iterable<PowerGeneratingTechnology> eligibleTechnologies = scheme.getPowerGeneratingTechnologiesEligible();

        Map<Key2D, Double> baseCostMap = new HashMap<Key2D, Double>();

        ForecastingInformationReport fReport;

        for (PowerGeneratingTechnology technology : eligibleTechnologies) {

            EMLabModel model = getReps().emlabModel;
            if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                continue;

            for (PowerGridNode node : reps.findAllPowerGridNodesByZone(regulator.getZone())) {

                // or create a new power plant if above statement returns null,
                // and assign it to a random energy producer.
                
                // TODO MM Check how is that random?
                EnergyProducer producer =  reps.energyProducers.iterator().next();
                
                PowerPlant plant = reps.createAndSpecifyTemporaryPowerPlant(getCurrentTick(), producer, node, technology);

                // logger.warn("creating a new power plant for " +
                // producer.getName() + ", of technology "
                // + plant.getTechnology().getName() + ", with node" +
                // node.getName() + "for time "
                // + futureTimePoint);

                // ==== Expectations ===

                Map<Substance, Double> expectedFuelPrices = predictFuelPrices(producer, futureTimePoint);
                // logger.warn("expected fuel prices" + expectedFuelPrices);

                // CO2
                Map<ElectricitySpotMarket, Double> expectedCO2Price = determineExpectedCO2PriceInclTaxAndFundamentalForecast(
                        futureTimePoint, producer.getNumberOfYearsBacklookingForForecasting(), 0, getCurrentTick());
                // logger.warn("expected CO2 price" + expectedCO2Price);

                double annualMarginalCost = 0d;
                double totalGenerationinMWh = 0d;
                double fiPremium = 0d;

                Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
                for (Substance fuel : technology.getFuels()) {
                    myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
                }
                Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices,
                        expectedCO2Price.get(market));
                plant.setFuelMix(fuelMix);

                double expectedMarginalCost = determineExpectedMarginalCost(plant, expectedFuelPrices,
                        expectedCO2Price.get(market));
                // logger.warn("expected marginal cost in fip role for plant " +
                // plant + "is " + expectedMarginalCost);

                totalGenerationinMWh = plant.getAnnualFullLoadHours() * plant.getActualNominalCapacity();
                annualMarginalCost = totalGenerationinMWh * expectedMarginalCost;
                double runningHours = 0d;
                double expectedGrossProfit = 0d;
                long numberOfSegments = getReps().segments.size();
                double totalAnnualExpectedGenerationOfPlant = 0d;

                Map<ElectricitySpotMarket, Double> expectedDemand = new HashMap<ElectricitySpotMarket, Double>();
                for (ElectricitySpotMarket elm : getReps().electricitySpotMarkets) {
                    GeometricTrendRegression gtr = new GeometricTrendRegression();
                    for (long time = getCurrentTick(); time > getCurrentTick()
                            - producer.getNumberOfYearsBacklookingForForecasting() && time >= 0; time = time - 1) {
                        gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
                    }
                    expectedDemand.put(elm, gtr.predict(futureTimePoint));
                }

                MarketInformation marketInformation = new MarketInformation(market, expectedDemand, expectedFuelPrices,
                        expectedCO2Price.get(market).doubleValue(), futureTimePoint);

                double loadFactor = 0d;
                double expectedAnnualVariableCost = 0d;
                double expectedAnnualVariableRevenue = 0d;

                for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
                    double expectedElectricityPrice = marketInformation.expectedElectricityPricesPerSegment
                            .get(segmentLoad.getSegment());
                    double hours = segmentLoad.getSegment().getLengthInHours();

                    runningHours = runningHours + hours;
                    if (technology.isIntermittent()) {

                        loadFactor = reps
                                .findIntermittentTechnologyNodeLoadFactorForNodeAndTechnology(node, technology)
                                .getLoadFactorForSegment(segmentLoad.getSegment());

                        expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost) * hours
                                * plant.getActualNominalCapacity() * loadFactor;
                        expectedAnnualVariableCost += expectedMarginalCost * hours * plant.getActualNominalCapacity()
                                * loadFactor;
                        expectedAnnualVariableRevenue += expectedElectricityPrice * hours
                                * plant.getActualNominalCapacity() * loadFactor;

                        totalAnnualExpectedGenerationOfPlant += hours * plant.getActualNominalCapacity() * loadFactor;

                    } else {
                        expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost) * hours * plant
                                .getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(), numberOfSegments);

                        expectedAnnualVariableCost += expectedMarginalCost * hours * plant
                                .getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(), numberOfSegments);
                        expectedAnnualVariableRevenue += expectedElectricityPrice * hours * plant
                                .getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(), numberOfSegments);

                        totalAnnualExpectedGenerationOfPlant += hours * plant.getAvailableCapacity(futureTimePoint,
                                segmentLoad.getSegment(), numberOfSegments);

                    }

                }

                // logger.warn("for technology " +
                // plant.getTechnology().getName() + " total generation is "
                // + totalGenerationinMWh + " and running hours is " +
                // fullLoadHours);

                double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());

                double operatingProfit = expectedGrossProfit - fixedOMCost;

                // VERIFICATION:
                double operatingCost = expectedAnnualVariableCost + fixedOMCost;
                double operatingRevenue = expectedAnnualVariableRevenue;

                // logger.warn("Compute FIP: Technology" + technology.getName()
                // + ": Operating Cost" + operatingCost
                // + "operating revenue" + operatingRevenue);

                // End VERIFICATION

                long durationOfSupportScheme = scheme.getSupportSchemeDuration();

                double wacc = (1 - regulator.getDebtRatioOfInvestments()) * regulator.getEquityInterestRate()
                        + regulator.getDebtRatioOfInvestments() * regulator.getLoanInterestRate();

                double waccAdjusted = (1 - regulator.getDebtRatioOfInvestments())
                        * (regulator.getEquityInterestRate() + regulator.getEquityRatePriceRiskComponent())
                        + regulator.getDebtRatioOfInvestments() * regulator.getLoanInterestRate();

                TreeMap<Integer, Double> discountedProjectCapitalOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                        technology.getDepreciationTime(), (int) plant.getActualLeadtime(),
                        plant.getActualInvestedCapital(), 0); // returns
                                                              // negative value,
                                                              // cause
                                                              // investment cost
                                                              // is -ve in
                                                              // function

                // returns +ve cause 'operating profit' is positive
                TreeMap<Integer, Double> discountedProjectOperatingRevenue = calculateSimplePowerPlantInvestmentCashFlow(
                        technology.getDepreciationTime(), (int) plant.getActualLeadtime(), 0, operatingRevenue);

                TreeMap<Integer, Double> discountedProjectOperatingCost = calculateSimplePowerPlantInvestmentCashFlow(
                        technology.getDepreciationTime(), (int) plant.getActualLeadtime(), 0, -operatingCost);

                TreeMap<Integer, Double> factorDiscountedGenerationSeries = calculateSimplePowerPlantInvestmentCashFlow(
                        (int) durationOfSupportScheme, (int) plant.getActualLeadtime(), 0, 1);

                double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);// are
                double discountedOpRevenue = npv(discountedProjectOperatingRevenue, waccAdjusted);
                double discountedOpCost = npv(discountedProjectOperatingCost, wacc);

                // Calculation of weighted average cost of capital,
                // based on regulator's assumption of companies debt-ratio

                double factorDiscountedGeneration = npv(factorDiscountedGenerationSeries, wacc);
                BiasFactor biasFactor = reps
                        .findBiasFactorGivenTechnologyNodeAndScheme(technology.getName(), node.getName(), scheme);

                if (scheme.isCostContainmentMechanismEnabled() && scheme.isTechnologySpecificityEnabled() == false) {
                    computeDegressionAndResetBiasFactor(scheme, biasFactor, null);
                } else if (scheme.isCostContainmentMechanismEnabled()
                        && scheme.isTechnologySpecificityEnabled() == true) {
                    computeDegressionAndResetBiasFactor(scheme, biasFactor, technology);
                }

                // FOR VERIFICATION
                double projectValue = discountedCapitalCosts + discountedOpCost + discountedOpRevenue;
                double biasFactorValue = biasFactor.getFeedInPremiumBiasFactor();

                // logger.warn("Compute FIP:discountedCapitalCosts " +
                // discountedCapitalCosts + "discountedOpCost"
                // + discountedOpCost + "discountedOpRevenue" +
                // discountedOpRevenue);
                // logger.warn("Compute FIP:totalGenerationinMWh " +
                // totalGenerationinMWh);
                // logger.warn("Compute FIP: factorDiscountedGeneration " +
                // factorDiscountedGeneration);
                // logger.warn("Compute FIP: biasFactorValue " +
                // biasFactorValue);
                // logger.warn("Compute FIP:Project Value " + projectValue);

                if (projectValue < 0) {
                    fiPremium = -projectValue * biasFactorValue / (totalGenerationinMWh * factorDiscountedGeneration);
                } else {
                    fiPremium = 0d;
                }

                fReport = new ForecastingInformationReport();
                fReport.setTick(getCurrentTick());
                fReport.setForecastingForTick(getCurrentTick() + scheme.getFutureSchemeStartTime());
                fReport.setAgent(regulator.getName());
                fReport.setProjectValuePerMwWithoutSubsidy(projectValue / plant.getActualNominalCapacity());
                fReport.setExpectedOpRevenueElectricityMarketWithoutSubsidy(operatingRevenue);
                fReport.setTechnologyName(technology.getName());
                fReport.setNodeName(node.getName());
                fReport.setExpectedAnnualGeneration(totalAnnualExpectedGenerationOfPlant);
                fReport.setProjectValuePerMwWithSubsidy(0);
                fReport.setExpectedOpRevenueElectricityMarketWithSubsidy(0);

                // logger.warn("expectedBaseCost in fipRole for plant" + plant +
                // "in tick" + futureTimePoint + "is "
                // + fiPremium);
                if (scheme.isTechnologySpecificityEnabled() == true) {
                    BaseCostFip baseCostFip = new BaseCostFip();

                    baseCostFip.setCostPerMWh(fiPremium);
                    baseCostFip.setStartTime(futureTimePoint);
                    baseCostFip.setNode(node);
                    baseCostFip.setTechnology(technology);
                    baseCostFip.setEndTime(futureTimePoint + scheme.getSupportSchemeDuration());

                    fReport.setExpectedOpRevenueElectricityMarketWithSubsidy(
                            fiPremium * totalAnnualExpectedGenerationOfPlant);

                } else {

                    baseCostMap.put(new Key2D(technology, node), fiPremium);

                    // logger.warn("Creating base cost map: technology " +
                    // technology.getName() + "premium " + fiPremium);

                }

            }
        }

        // aforementioned code section.
        if (scheme.isTechnologySpecificityEnabled() == false) {

            MapValueComparator comp = new MapValueComparator(baseCostMap);
            TreeMap<Key2D, Double> meritOrderBaseCost = new TreeMap<Key2D, Double>(comp);
            meritOrderBaseCost.putAll(baseCostMap);
            // logger.warn("Technology Cost Map" + baseCostMap);

            double renewableGenerationAccepted = 0d;
            double baseCostFipTechNeutral = 0d;
            // double sumOfPotentialsAccepted = 0d;

            double renewableTargetInMwh = computeRenewableGenerationTarget(scheme, null);
            // logger.warn("Theoretical Target for tick " + futureTimePoint +
            // "in MWh is " + renewableTargetInMwh);
            double generationFromRenewables = totalExpectedGenerationFromRenewables(scheme, null);
            // logger.warn("Renewable Target Total for tick " + futureTimePoint
            // + "in MWh is " + renewableTargetInMwh);

            // change to prediction
            double totalExpectedConsumption = 0d;

            GeometricTrendRegression gtr = new GeometricTrendRegression();
            for (long time = getCurrentTick(); time > getCurrentTick()
                    - scheme.getRegulator().getNumberOfYearsLookingBackToForecastDemand()
                    && time >= 0; time = time - 1) {
                gtr.addData(time, market.getDemandGrowthTrend().getValue(time));
            }

            double demandFactor = gtr.predict(futureTimePoint);
            for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
                // logger.warn("segmentLoad: " + segmentLoad);
                totalExpectedConsumption += segmentLoad.getBaseLoad() * demandFactor
                        * segmentLoad.getSegment().getLengthInHours();
            }

            // if (renewableTargetInMwh - generationFromRenewables > 0)
            // renewableTargetInMwh = renewableTargetInMwh -
            // generationFromRenewables;
            // else
            // renewableTargetInMwh = 0;

            // logger.warn("Actual Target for tick " + futureTimePoint + "in MWh
            // is " + renewableTargetInMwh);

            for (Entry<Key2D, Double> technologyCost : meritOrderBaseCost.entrySet()) {
                Key2D baseCostKey = technologyCost.getKey();
                PowerGeneratingTechnology technology = baseCostKey.techKey;
                PowerGridNode node = baseCostKey.nodeKey;
                double technologyPotential = 0d;
                ArrayList<PowerGridNode> nodeList = new ArrayList<PowerGridNode>();
                nodeList.add(node);

                boolean nodeUnique = (Collections.frequency(nodeList, node) == 1) ? true : false;
                // Determine available capacity in the future in this
                // segment

                // logger.warn("Regulator Name " + regulator.getName() +
                // "Technology " + technology.getName()
                // + " Node " + node.getName());
                technologyPotential = reps
                        .findTechnologySpecificRenewablePotentialLimitTimeSeriesByRegulator(scheme.getRegulator(),
                                technology.getName())
                        .getValue(futureTimePoint);

                // logger.warn("for Technology" + technology.getName() + "the
                // potential in MWh is " + technologyPotential);

                if ((renewableTargetInMwh - (renewableGenerationAccepted + technologyPotential) > 0)) {
                    if ((!technology.isIntermittent() && nodeUnique) || technology.isIntermittent()) {
                        renewableGenerationAccepted += technologyPotential;
                        baseCostFipTechNeutral = technologyCost.getValue();
                    }

                    // logger.warn("If condition 1");
                } else if (renewableTargetInMwh - (renewableGenerationAccepted + technologyPotential) <= 0) {
                    if ((!technology.isIntermittent() && nodeUnique) || technology.isIntermittent()) {
                        renewableGenerationAccepted += (renewableTargetInMwh - renewableGenerationAccepted);
                        baseCostFipTechNeutral = technologyCost.getValue();
                        // logger.warn("If condition 2");
                        break;
                    }
                }
            }

            // logger.warn("renewable generation accepted is" +
            // renewableGenerationAccepted + "fip set is "
            // + baseCostFipTechNeutral);
            BaseCostFip baseCostFip = new BaseCostFip();

            baseCostFip.setCostPerMWh(baseCostFipTechNeutral);
            baseCostFip.setStartTime(futureTimePoint);
            // baseCostFip.setNode(node);
            // baseCostFip.setTechnology(technology);
            baseCostFip.setEndTime(futureTimePoint + scheme.getSupportSchemeDuration());
        }

    }



}