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

import org.apache.commons.math.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.agent.StochasticTargetInvestor;
import emlab.gen.domain.agent.StrategicReserveOperator;
import emlab.gen.domain.agent.TargetInvestor;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.CO2Auction;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.PowerGeneratingTechnologyTarget;
import emlab.gen.domain.policy.renewablesupport.BaseCostFip;
import emlab.gen.domain.policy.renewablesupport.BiasFactor;
import emlab.gen.domain.policy.renewablesupport.ForecastingInformationReport;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.policy.renewablesupport.RenewableTarget;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;
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

@RoleComponent
public class ComputePremiumRoleExAnte extends AbstractEnergyProducerRole<EnergyProducer>
        implements Role<EnergyProducer> {

    @Transient
    @Autowired
    Reps reps;

    @Transient
    @Autowired
    Neo4jTemplate template;

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

    @SuppressWarnings("unchecked")
    @Transactional
    public void act(RenewableSupportFipScheme scheme) {

        Regulator regulator = scheme.getRegulator();

        // should be close to the investor's future time point.
        long futureTimePoint = scheme.getFutureSchemeStartTime() + getCurrentTick();

        ElectricitySpotMarket market = reps.marketRepository.findElectricitySpotMarketForZone(regulator.getZone());

        Iterable<PowerGeneratingTechnology> eligibleTechnologies = scheme.getPowerGeneratingTechnologiesEligible();

        Map<Key2D, Double> baseCostMap = new HashMap<Key2D, Double>();

        ForecastingInformationReport fReport;

        for (PowerGeneratingTechnology technology : eligibleTechnologies) {

            DecarbonizationModel model = reps.genericRepository.findAll(DecarbonizationModel.class).iterator().next();
            if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                continue;

            for (PowerGridNode node : reps.powerGridNodeRepository.findAllPowerGridNodesByZone(regulator.getZone())) {

                // or create a new power plant if above statement returns null,
                // and assign it to a random energy producer.
                PowerPlant plant = new PowerPlant();

                EnergyProducer producer = reps.energyProducerRepository.findAll().iterator().next();

                plant.specifyNotPersist(getCurrentTick(), producer, node, technology);
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
                long numberOfSegments = reps.segmentRepository.count();
                double totalAnnualExpectedGenerationOfPlant = 0d;

                Map<ElectricitySpotMarket, Double> expectedDemand = new HashMap<ElectricitySpotMarket, Double>();
                for (ElectricitySpotMarket elm : reps.template.findAll(ElectricitySpotMarket.class)) {
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

                        loadFactor = reps.intermittentTechnologyNodeLoadFactorRepository
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
                        technology.getDepreciationTime(), (int) plant.getActualLeadTime(),
                        plant.getActualInvestedCapital(), 0); // returns
                                                              // negative value,
                                                              // cause
                                                              // investment cost
                                                              // is -ve in
                                                              // function

                // returns +ve cause 'operating profit' is positive
                TreeMap<Integer, Double> discountedProjectOperatingRevenue = calculateSimplePowerPlantInvestmentCashFlow(
                        technology.getDepreciationTime(), (int) plant.getActualLeadTime(), 0, operatingRevenue);

                TreeMap<Integer, Double> discountedProjectOperatingCost = calculateSimplePowerPlantInvestmentCashFlow(
                        technology.getDepreciationTime(), (int) plant.getActualLeadTime(), 0, -operatingCost);

                TreeMap<Integer, Double> factorDiscountedGenerationSeries = calculateSimplePowerPlantInvestmentCashFlow(
                        (int) durationOfSupportScheme, (int) plant.getActualLeadTime(), 0, 1);

                double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);// are
                double discountedOpRevenue = npv(discountedProjectOperatingRevenue, waccAdjusted);
                double discountedOpCost = npv(discountedProjectOperatingCost, wacc);

                // Calculation of weighted average cost of capital,
                // based on regulator's assumption of companies debt-ratio

                double factorDiscountedGeneration = npv(factorDiscountedGenerationSeries, wacc);
                BiasFactor biasFactor = reps.renewableSupportSchemeRepository
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
                fReport.PersistReport();

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
                    baseCostFip.persist();

                    fReport.setExpectedOpRevenueElectricityMarketWithSubsidy(
                            fiPremium * totalAnnualExpectedGenerationOfPlant);
                    fReport.persist();

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
                technologyPotential = reps.renewableTargetForTenderRepository
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
            baseCostFip.persist();
        }

    }

    private void computeDegressionAndResetBiasFactor(RenewableSupportFipScheme scheme, BiasFactor biasFactor,
            PowerGeneratingTechnology technology) {

        // get target value of renewable generation
        double renewableTargetInMwh = 0d; // computeRenewableGenerationTarget(scheme);
        double generationFromRenewables = 0d; // totalExpectedGenerationFromRenewables(scheme);

        double degressionFactor = biasFactor.getDegressionFactor();
        double newBiasFactor = 0d;

        if (scheme.isTechnologySpecificityEnabled()) {
            renewableTargetInMwh = computeRenewableGenerationTarget(scheme, technology);
            generationFromRenewables = totalExpectedGenerationFromRenewables(scheme, technology);
        }

        if (generationFromRenewables > renewableTargetInMwh) {
            newBiasFactor = biasFactor.getFeedInPremiumBiasFactor() * (1 - degressionFactor);
            biasFactor.setFeedInPremiumBiasFactor(newBiasFactor);
            // logger.warn("DEGRESSING!!!");
        } else if (generationFromRenewables < renewableTargetInMwh) {
            newBiasFactor = biasFactor.getFeedInPremiumBiasFactor() * (1 + degressionFactor);
            biasFactor.setFeedInPremiumBiasFactor(newBiasFactor);
        }
        // if expected generation exceeds target, degress by a certain
        // percentage.
        // else if expected generation is lower than a certain margin, increase
        // bias factor. - will have to create targetLowerMargin and
        // targetUpperMargin for it, best created in target object.

    }

    private double computeRenewableGenerationTarget(RenewableSupportFipScheme scheme,
            PowerGeneratingTechnology technology) {
        double demandFactor;
        double targetFactor;
        Zone zone = scheme.getRegulator().getZone();

        // logger.warn("Calculate Renewable Target Role started of zone: " +
        // zone);

        ElectricitySpotMarket market = reps.marketRepository.findElectricitySpotMarketForZone(zone);

        // Assuming perfect knowledge of demand

        GeometricTrendRegression gtr = new GeometricTrendRegression();
        for (long time = getCurrentTick(); time > getCurrentTick()
                - scheme.getRegulator().getNumberOfYearsLookingBackToForecastDemand() && time >= 0; time = time - 1) {
            gtr.addData(time, market.getDemandGrowthTrend().getValue(time));
        }
        demandFactor = gtr.predict(getCurrentTick() + scheme.getFutureSchemeStartTime());

        /*
         * it aggregates segments from both countries, so the boolean should
         * actually be true here and the code adjusted to FALSE case. Or a query
         * should be adjusted what probably will take less time.
         */

        RenewableTarget target = new RenewableTarget();
        // get renewable energy target in factor (percent)
        if (scheme.isTechnologySpecificityEnabled() == false) {
            target = reps.renewableTargetForTenderRepository
                    .findTechnologyNeutralRenewableTargetForTenderByRegulator(scheme.getRegulator());
        } else {
            target = reps.renewableTargetForTenderRepository.findTechnologySpecificRenewableTargetForTenderByRegulator(
                    scheme.getRegulator(), technology.getName());
        }

        // logger.warn("Renewable Target is " + target);

        targetFactor = target.getYearlyRenewableTargetTimeSeries()
                .getValue(getCurrentTick() + scheme.getFutureSchemeStartTime());
                // logger.warn("targetFactor is " + targetFactor);

        // get totalLoad in MWh
        double totalExpectedConsumption = 0d;

        for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
            // logger.warn("segmentLoad: " + segmentLoad);
            totalExpectedConsumption += segmentLoad.getBaseLoad() * demandFactor
                    * segmentLoad.getSegment().getLengthInHours();

            // logger.warn("demand factor is: " + demandFactor);

        }

        double renewableTargetInMwh = targetFactor * totalExpectedConsumption;

        return renewableTargetInMwh;
    }

    public double determineExpectedMarginalCost(PowerPlant plant, Map<Substance, Double> expectedFuelPrices,
            double expectedCO2Price) {
        double mc = determineExpectedMarginalFuelCost(plant, expectedFuelPrices);
        double co2Intensity = plant.calculateEmissionIntensity();
        mc += co2Intensity * expectedCO2Price;
        return mc;
    }

    public double determineExpectedMarginalFuelCost(PowerPlant powerPlant, Map<Substance, Double> expectedFuelPrices) {
        double fc = 0d;
        for (SubstanceShareInFuelMix mix : powerPlant.getFuelMix()) {
            double amount = mix.getShare();
            double fuelPrice = expectedFuelPrices.get(mix.getSubstance());
            fc += amount * fuelPrice;
        }
        return fc;
    }

    private double totalExpectedGenerationFromRenewables(RenewableSupportFipScheme scheme,
            PowerGeneratingTechnology technologySpecified) {

        double totalExpectedGeneration = 0d;
        double expectedGenerationPerTechnology = 0d;
        double expectedGenerationPerPlant = 0d;
        long numberOfSegments = reps.segmentRepository.count();
        // logger.warn("numberOfsegments: " + numberOfSegments);
        ElectricitySpotMarket market = reps.marketRepository
                .findElectricitySpotMarketForZone(scheme.getRegulator().getZone());

        if (scheme.isTechnologySpecificityEnabled() == false) {
            for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {
                expectedGenerationPerTechnology = 0d;
                for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByMarketAndTechnology(
                        market, technology, getCurrentTick() + scheme.getFutureSchemeStartTime())) {
                    expectedGenerationPerPlant = 0d;
                    for (Segment segment : reps.segmentRepository.findAll()) {
                        double availablePlantCapacity = plant.getAvailableCapacity(
                                getCurrentTick() + scheme.getFutureSchemeStartTime(), segment, numberOfSegments);
                        double lengthOfSegmentInHours = segment.getLengthInHours();
                        expectedGenerationPerPlant += availablePlantCapacity * lengthOfSegmentInHours;
                    }
                    expectedGenerationPerTechnology += expectedGenerationPerPlant;
                }
                // logger.warn("Total expected generation for technology " +
                // technology.getName() + "is "
                // + expectedGenerationPerTechnology);

                totalExpectedGeneration += expectedGenerationPerTechnology;

            }
        } else {
            for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByMarketAndTechnology(market,
                    technologySpecified, getCurrentTick() + scheme.getFutureSchemeStartTime())) {
                expectedGenerationPerPlant = 0d;
                for (Segment segment : reps.segmentRepository.findAll()) {
                    double availablePlantCapacity = plant.getAvailableCapacity(
                            getCurrentTick() + scheme.getFutureSchemeStartTime(), segment, numberOfSegments);
                    double lengthOfSegmentInHours = segment.getLengthInHours();
                    expectedGenerationPerPlant += availablePlantCapacity * lengthOfSegmentInHours;
                }
                expectedGenerationPerTechnology += expectedGenerationPerPlant;
            }
            // logger.warn("Total expected generation for technology " +
            // technologySpecified.getName() + "is "
            // + expectedGenerationPerTechnology);

            totalExpectedGeneration += expectedGenerationPerTechnology;
        }
        return totalExpectedGeneration;
    }

    private TreeMap<Integer, Double> calculateSimplePowerPlantInvestmentCashFlow(int depreciationTime, int buildingTime,
            double totalInvestment, double operatingProfit) {
        TreeMap<Integer, Double> investmentCashFlow = new TreeMap<Integer, Double>();
        double equalTotalDownPaymentInstallement = totalInvestment / buildingTime;
        for (int i = 0; i < buildingTime; i++) {
            investmentCashFlow.put(new Integer(i), -equalTotalDownPaymentInstallement);
        }
        for (int i = buildingTime; i < depreciationTime + buildingTime; i++) {
            investmentCashFlow.put(new Integer(i), operatingProfit);
        }
        return investmentCashFlow;
    }

    private double npv(TreeMap<Integer, Double> netCashFlow, double wacc) {
        double npv = 0;
        for (Integer iterator : netCashFlow.keySet()) {
            npv += netCashFlow.get(iterator).doubleValue() / Math.pow(1 + wacc, iterator.intValue());
        }
        return npv;
    }

    private Map<ElectricitySpotMarket, Double> demandForecast(ElectricitySpotMarket market, long futureTimePoint,
            EnergyProducer producer) {
        Map<ElectricitySpotMarket, Double> expectedDemand = new HashMap<ElectricitySpotMarket, Double>();
        for (ElectricitySpotMarket elm : reps.template.findAll(ElectricitySpotMarket.class)) {
            GeometricTrendRegression gtr = new GeometricTrendRegression();
            for (long time = getCurrentTick(); time > getCurrentTick()
                    - producer.getNumberOfYearsBacklookingForForecasting() && time >= 0; time = time - 1) {
                gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
            }
            expectedDemand.put(elm, gtr.predict(futureTimePoint));
        }

        return expectedDemand;
    }

    /**
     * Calculates expected CO2 price based on a geometric trend estimation, of
     * the past years. The adjustmentForDetermineFuelMix needs to be set to 1,
     * if this is used in the determine fuel mix role.
     *
     * @param futureTimePoint
     *            Year the prediction is made for
     * @param yearsLookingBackForRegression
     *            How many years are used as input for the regression, incl. the
     *            current tick.
     * @return
     */
    protected HashMap<ElectricitySpotMarket, Double> determineExpectedCO2PriceInclTaxAndFundamentalForecast(
            long futureTimePoint, long yearsLookingBackForRegression, int adjustmentForDetermineFuelMix,
            long clearingTick) {
        HashMap<ElectricitySpotMarket, Double> co2Prices = new HashMap<ElectricitySpotMarket, Double>();
        CO2Auction co2Auction = reps.genericRepository.findFirst(CO2Auction.class);
        Iterable<ClearingPoint> cps = reps.clearingPointRepository.findAllClearingPointsForMarketAndTimeRange(
                co2Auction, clearingTick - yearsLookingBackForRegression + 1 - adjustmentForDetermineFuelMix,
                clearingTick - adjustmentForDetermineFuelMix, false);
        // Create regression object and calculate average
        SimpleRegression sr = new SimpleRegression();
        Government government = reps.template.findAll(Government.class).iterator().next();
        double lastPrice = 0;
        double averagePrice = 0;
        int i = 0;
        for (ClearingPoint clearingPoint : cps) {
            sr.addData(clearingPoint.getTime(), clearingPoint.getPrice());
            lastPrice = clearingPoint.getPrice();
            averagePrice += lastPrice;
            i++;
        }
        averagePrice = averagePrice / i;
        double expectedCO2Price;
        double expectedRegressionCO2Price;
        if (i > 1) {
            expectedRegressionCO2Price = sr.predict(futureTimePoint);
            expectedRegressionCO2Price = Math.max(0, expectedRegressionCO2Price);
            expectedRegressionCO2Price = Math.min(expectedRegressionCO2Price,
                    government.getCo2Penalty(futureTimePoint));
        } else {
            expectedRegressionCO2Price = lastPrice;
        }
        ClearingPoint expectedCO2ClearingPoint = reps.clearingPointRepository.findClearingPointForMarketAndTime(
                co2Auction,
                getCurrentTick()
                        + reps.genericRepository.findFirst(DecarbonizationModel.class).getCentralForecastingYear(),
                true);
        expectedCO2Price = (expectedCO2ClearingPoint == null) ? 0 : expectedCO2ClearingPoint.getPrice();
        expectedCO2Price = (expectedCO2Price + expectedRegressionCO2Price) / 2;
        for (ElectricitySpotMarket esm : reps.marketRepository.findAllElectricitySpotMarkets()) {
            double nationalCo2MinPriceinFutureTick = reps.nationalGovernmentRepository
                    .findNationalGovernmentByElectricitySpotMarket(esm).getMinNationalCo2PriceTrend()
                    .getValue(futureTimePoint);
            double co2PriceInCountry = 0d;
            if (expectedCO2Price > nationalCo2MinPriceinFutureTick) {
                co2PriceInCountry = expectedCO2Price;
            } else {
                co2PriceInCountry = nationalCo2MinPriceinFutureTick;
            }
            co2PriceInCountry += reps.genericRepository.findFirst(Government.class).getCO2Tax(futureTimePoint);
            co2Prices.put(esm, Double.valueOf(co2PriceInCountry));
        }
        return co2Prices;
    }

    public Map<Substance, Double> predictFuelPrices(EnergyProducer agent, long futureTimePoint) {
        // Fuel Prices
        Map<Substance, Double> expectedFuelPrices = new HashMap<Substance, Double>();
        for (Substance substance : reps.substanceRepository.findAllSubstancesTradedOnCommodityMarkets()) {
            // Find Clearing Points for the last 5 years (counting current year
            // as one of the last 5 years).
            Iterable<ClearingPoint> cps = reps.clearingPointRepository
                    .findAllClearingPointsForSubstanceTradedOnCommodityMarkesAndTimeRange(substance,
                            getCurrentTick() - (agent.getNumberOfYearsBacklookingForForecasting() - 1),
                            getCurrentTick(), false);
            // logger.warn("{}, {}",
            // getCurrentTick()-(agent.getNumberOfYearsBacklookingForForecasting()-1),
            // getCurrentTick());
            // Create regression object
            SimpleRegression gtr = new SimpleRegression();
            for (ClearingPoint clearingPoint : cps) {
                // logger.warn("CP {}: {} , in" + clearingPoint.getTime(),
                // substance.getName(), clearingPoint.getPrice());
                gtr.addData(clearingPoint.getTime(), clearingPoint.getPrice());
            }
            gtr.addData(getCurrentTick(), findLastKnownPriceForSubstance(substance, getCurrentTick()));
            expectedFuelPrices.put(substance, gtr.predict(futureTimePoint));
            // logger.warn("Forecast {}: {}, in Step " + futureTimePoint,
            // substance, expectedFuelPrices.get(substance));
        }
        return expectedFuelPrices;
    }

    private class MarketInformation {

        Map<Segment, Double> expectedElectricityPricesPerSegment;
        double maxExpectedLoad = 0d;
        Map<PowerPlant, Double> meritOrder;
        double capacitySum;

        MarketInformation(ElectricitySpotMarket market, Map<ElectricitySpotMarket, Double> expectedDemand,
                Map<Substance, Double> fuelPrices, double co2price, long time) {
            // determine expected power prices
            expectedElectricityPricesPerSegment = new HashMap<Segment, Double>();
            Map<PowerPlant, Double> marginalCostMap = new HashMap<PowerPlant, Double>();
            capacitySum = 0d;

            // get merit order for this market
            for (PowerPlant plant : reps.powerPlantRepository.findExpectedOperationalPowerPlantsInMarket(market,
                    time)) {

                double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                marginalCostMap.put(plant, plantMarginalCost);
                capacitySum += plant.getActualNominalCapacity();
            }

            // get difference between technology target and expected operational
            // capacity
            for (TargetInvestor targetInvestor : reps.targetInvestorRepository.findAllByMarket(market)) {
                if (!(targetInvestor instanceof StochasticTargetInvestor)) {
                    for (PowerGeneratingTechnologyTarget pggt : targetInvestor.getPowerGenerationTechnologyTargets()) {
                        double expectedTechnologyCapacity = reps.powerPlantRepository
                                .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market,
                                        pggt.getPowerGeneratingTechnology(), time);
                        double targetDifference = pggt.getTrend().getValue(time) - expectedTechnologyCapacity;
                        if (targetDifference > 0) {

                            PowerPlant plant = new PowerPlant();

                            plant.specifyNotPersist(getCurrentTick(), new EnergyProducer(),
                                    reps.powerGridNodeRepository.findFirstPowerGridNodeByElectricitySpotMarket(market),
                                    pggt.getPowerGeneratingTechnology());
                            plant.setActualNominalCapacity(targetDifference);
                            double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                            marginalCostMap.put(plant, plantMarginalCost);
                            capacitySum += targetDifference;

                            // logger.warn("SubmitBid 618 - Agent " +
                            // targetInvestor +
                            // " invested in technology at tick "
                            // + getCurrentTick() + " in tech " +
                            // pggt.getPowerGeneratingTechnology());

                        }
                    }

                } else {
                    for (PowerGeneratingTechnologyTarget pggt : targetInvestor.getPowerGenerationTechnologyTargets()) {
                        double expectedTechnologyCapacity = reps.powerPlantRepository
                                .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market,
                                        pggt.getPowerGeneratingTechnology(), time);
                        double expectedTechnologyAddition = 0;
                        long contructionTime = getCurrentTick()
                                + pggt.getPowerGeneratingTechnology().getExpectedLeadtime()
                                + pggt.getPowerGeneratingTechnology().getExpectedPermittime();
                        for (long investmentTimeStep = contructionTime
                                + 1; investmentTimeStep <= time; investmentTimeStep = investmentTimeStep + 1) {
                            expectedTechnologyAddition += (pggt.getTrend().getValue(investmentTimeStep)
                                    - pggt.getTrend().getValue(investmentTimeStep - 1));
                        }
                        if (expectedTechnologyAddition > 0) {
                            PowerPlant plant = new PowerPlant();
                            plant.specifyNotPersist(getCurrentTick(), new EnergyProducer(),
                                    reps.powerGridNodeRepository.findFirstPowerGridNodeByElectricitySpotMarket(market),
                                    pggt.getPowerGeneratingTechnology());
                            plant.setActualNominalCapacity(expectedTechnologyAddition);
                            double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                            marginalCostMap.put(plant, plantMarginalCost);
                            capacitySum += expectedTechnologyAddition;

                            // logger.warn("SubmitBid 648 - Agent " +
                            // targetInvestor +
                            // " invested in technology at tick "
                            // + getCurrentTick() + " in tech " +
                            // pggt.getPowerGeneratingTechnology());

                        }
                    }
                }

            }

            MapValueComparator comp = new MapValueComparator(marginalCostMap);
            meritOrder = new TreeMap<PowerPlant, Double>(comp);
            meritOrder.putAll(marginalCostMap);

            long numberOfSegments = reps.segmentRepository.count();

            double demandFactor = expectedDemand.get(market).doubleValue();

            // find expected prices per segment given merit order
            for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {

                double expectedSegmentLoad = segmentLoad.getBaseLoad() * demandFactor;

                if (expectedSegmentLoad > maxExpectedLoad) {
                    maxExpectedLoad = expectedSegmentLoad;
                }

                double segmentSupply = 0d;
                double segmentPrice = 0d;
                double totalCapacityAvailable = 0d;

                for (Entry<PowerPlant, Double> plantCost : meritOrder.entrySet()) {
                    PowerPlant plant = plantCost.getKey();
                    double plantCapacity = 0d;
                    // Determine available capacity in the future in this
                    // segment
                    plantCapacity = plant.getExpectedAvailableCapacity(time, segmentLoad.getSegment(),
                            numberOfSegments);
                    totalCapacityAvailable += plantCapacity;
                    // logger.warn("Capacity of plant " + plant.toString() +
                    // " is " +
                    // plantCapacity/plant.getActualNominalCapacity());
                    if (segmentSupply < expectedSegmentLoad) {
                        segmentSupply += plantCapacity;
                        segmentPrice = plantCost.getValue();
                    }

                }

                // logger.warn("Segment " +
                // segmentLoad.getSegment().getSegmentID() + " supply equals " +
                // segmentSupply + " and segment demand equals " +
                // expectedSegmentLoad);

                // Find strategic reserve operator for the market.
                double reservePrice = 0;
                double reserveVolume = 0;
                for (StrategicReserveOperator operator : reps.strategicReserveOperatorRepository.findAll()) {
                    ElectricitySpotMarket market1 = reps.marketRepository
                            .findElectricitySpotMarketForZone(operator.getZone());
                    if (market.getNodeId().intValue() == market1.getNodeId().intValue()) {
                        reservePrice = operator.getReservePriceSR();
                        reserveVolume = operator.getReserveVolume();
                    }
                }

                if (segmentSupply >= expectedSegmentLoad
                        && ((totalCapacityAvailable - expectedSegmentLoad) <= (reserveVolume))) {
                    expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), reservePrice);
                    // logger.warn("Price: "+
                    // expectedElectricityPricesPerSegment);
                } else if (segmentSupply >= expectedSegmentLoad
                        && ((totalCapacityAvailable - expectedSegmentLoad) > (reserveVolume))) {
                    expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), segmentPrice);
                    // logger.warn("Price: "+
                    // expectedElectricityPricesPerSegment);
                } else {
                    expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), market.getValueOfLostLoad());
                }

            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see agentspring.role.Role#act(agentspring.agent.Agent)
     */
    @Override
    public void act(EnergyProducer arg0) {
        // TODO Auto-generated method stub

    }

}