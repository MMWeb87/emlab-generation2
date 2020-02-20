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
import java.util.logging.Level;

import org.apache.commons.math.stat.regression.SimpleRegression;

import emlab.gen.domain.agent.EMLabModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.CO2Auction;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.renewablesupport.BaseCostFip;
import emlab.gen.domain.policy.renewablesupport.BiasFactor;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.policy.renewablesupport.RenewableTarget;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.engine.Schedule;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;
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

public class ComputePremiumRoleExPost extends AbstractComputePremiumRole{

    Reps reps;

	public ComputePremiumRoleExPost(Schedule schedule) {
	    super(schedule);
	}

    public class Key2D {
        private final PowerGeneratingTechnology techKey;
        private final PowerGridNode nodeKey;

        public Key2D(PowerGeneratingTechnology key1, PowerGridNode key2) {
            this.techKey = key1;
            this.nodeKey = key2;
        }
    }


    public void act(RenewableSupportFipScheme scheme) {

        Regulator regulator = scheme.getRegulator();
        // should be close to the investor's future time point.
        long futureTimePoint = scheme.getFutureSchemeStartTime() + getCurrentTick();

        ElectricitySpotMarket market = reps.findElectricitySpotMarketForZone(regulator.getZone());

        Iterable<PowerGeneratingTechnology> eligibleTechnologies = scheme.getPowerGeneratingTechnologiesEligible();

        Map<Key2D, Double> baseCostMap = new HashMap<Key2D, Double>();

        for (PowerGeneratingTechnology technology : eligibleTechnologies) {

            // logger.warn("technology loop");

            EMLabModel model = getReps().emlabModel;
            if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                continue;

            for (PowerGridNode node : reps.findAllPowerGridNodesByZone(regulator.getZone())) {

                // or create a new power plant if above statement returns
                // null,
                // and assign it to a random energy producer.

                EnergyProducer producer = getReps().energyProducers.iterator().next();

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
                double lcoe = 0d;

                Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
                for (Substance fuel : technology.getFuels()) {
                    myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
                }
                Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices,
                        expectedCO2Price.get(market));
                plant.setFuelMix(fuelMix);

                double expectedMarginalCost = determineExpectedMarginalCost(plant, expectedFuelPrices,
                        expectedCO2Price.get(market));
                // logger.warn("expected marginal cost in fip role for plant
                // " +
                // plant + "is " + expectedMarginalCost);

                totalGenerationinMWh = plant.getAnnualFullLoadHours() * plant.getActualNominalCapacity();
                annualMarginalCost = totalGenerationinMWh * expectedMarginalCost;

                // logger.warn("for technology " +
                // plant.getTechnology().getName() + " total generation is "
                // + totalGenerationinMWh + " and running hours is " +
                // fullLoadHours);

                double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());
                double operatingCost = fixedOMCost + annualMarginalCost;

                long durationOfSupportScheme = scheme.getSupportSchemeDuration();
                long finishedConstruction = plant.calculateActualPermittime() + plant.calculateActualLeadtime();

                // logger.warn("Fixed OM cost for technology " +
                // plant.getTechnology().getName() + " is " + fixedOMCost
                // + " and operatingCost is " + operatingCost);

                TreeMap<Integer, Double> discountedProjectCapitalOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                        (int) technology.getDepreciationTime(), (int) plant.getActualLeadtime(),
                        plant.getActualInvestedCapital(), 0);

                // Creation of in cashflow during operation
                TreeMap<Integer, Double> discountedProjectCashOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                        (int) technology.getDepreciationTime(), (int) plant.getActualLeadtime(), 0, operatingCost);

                TreeMap<Integer, Double> factorDiscountedGenerationSeries = calculateSimplePowerPlantInvestmentCashFlow(
                        (int) durationOfSupportScheme, (int) plant.getActualLeadtime(), 0, 1);

                // Calculation of weighted average cost of capital,
                // based on regulator's assumption of companies debt-ratio, also
                // taking into account lower price risk associated with ex-post
                // scenarios

                double wacc = (1 - regulator.getDebtRatioOfInvestments()) * (regulator.getEquityInterestRate())
                        + regulator.getDebtRatioOfInvestments() * regulator.getLoanInterestRate();

                double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);
                // logger.warn("discountedCapitalCosts " +
                // discountedCapitalCosts);
                double discountedOpCost = npv(discountedProjectCashOutflow, wacc);
                double factorDiscountedGeneration = npv(factorDiscountedGenerationSeries, wacc);
                // logger.warn("discountedOpCost " + discountedOpCost);
                BiasFactor biasFactor = reps
                        .findBiasFactorGivenTechnologyNodeAndScheme(technology.getName(), node.getName(), scheme);

                if (scheme.isCostContainmentMechanismEnabled() && scheme.isTechnologySpecificityEnabled() == false) {
                    computeDegressionAndResetBiasFactor(scheme, biasFactor, null);
                } else if (scheme.isCostContainmentMechanismEnabled()
                        && scheme.isTechnologySpecificityEnabled() == true) {
                    computeDegressionAndResetBiasFactor(scheme, biasFactor, technology);
                }

                // FOR VERIFICATION
                double projectCost = discountedCapitalCosts + discountedOpCost;
                // logger.warn("discountedOpCost in FipRole is" +
                // discountedOpCost + "total Generation is"
                // + totalGenerationinMWh + "flh is" +
                // plant.getAnnualFullLoadHours());
                // logger.warn("discountedCapCost in FipRole is " +
                // discountedCapitalCosts);

                double biasFactorValue = biasFactor.getFeedInPremiumBiasFactor();
                lcoe = (discountedCapitalCosts + discountedOpCost) * biasFactorValue
                        / (totalGenerationinMWh * factorDiscountedGeneration);
                // logger.warn(
                // "expectedBaseCost in fipRole for plant" + plant + "in
                // tick" +
                // futureTimePoint + "is " + lcoe);

                if (scheme.isTechnologySpecificityEnabled() == true) {
                    BaseCostFip baseCostFip = new BaseCostFip();

                    baseCostFip.setCostPerMWh(lcoe);
                    baseCostFip.setStartTime(futureTimePoint);
                    baseCostFip.setNode(node);
                    baseCostFip.setTechnology(technology);
                    baseCostFip.setEndTime(futureTimePoint + scheme.getSupportSchemeDuration());

                    // logger.warn("Creating BaseCost object: technology " +
                    // technology.getName() + "premium " + lcoe);
                } else {

                    logger.log(Level.INFO, "Creating base cost map: technology " + technology.getName() + "premium " + lcoe);
                    baseCostMap.put(new Key2D(technology, node), lcoe);
                    // Use that as a key, depending on the mode -
                    // technology,
                    // location specificity, to create your merit order in
                    // the
                    // code section below.
                }

            }
        }

        if (scheme.isTechnologySpecificityEnabled() == false) {

            MapValueComparator comp = new MapValueComparator(baseCostMap);
            TreeMap<Key2D, Double> meritOrderBaseCost = new TreeMap<Key2D, Double>(comp);
            meritOrderBaseCost.putAll(baseCostMap);
            // logger.warn("Technology Cost Map" + baseCostMap);

            double renewableGenerationAccepted = 0d;
            double baseCostFipTechNeutral = 0d;
            // double sumOfPotentialsAccepted = 0d;

            double renewableTargetInMwh = computeRenewableGenerationTarget(scheme, null);
            double generationFromRenewables = totalExpectedGenerationFromRenewables(scheme, null);
            // logger.warn("Renewable Target Total for tick " + futureTimePoint
            // + "in MWh is " + renewableTargetInMwh);

            // change to prediction
            double totalExpectedConsumption = 0d;
            double demandFactor = market.getDemandGrowthTrend()
                    .getValue(getCurrentTick() + scheme.getFutureSchemeStartTime());

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

            logger.log(Level.INFO, "Actual Target for tick " + futureTimePoint + "in MWh is " + renewableTargetInMwh);

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
                // logger.warn(
                // "technology name" + technology.getName() + "regulator name" +
                // scheme.getRegulator().getName());
                technologyPotential = reps
                        .findTechnologySpecificRenewablePotentialLimitTimeSeriesByRegulator(scheme.getRegulator(),
                                technology.getName())
                        .getValue(futureTimePoint);

                // logger.warn("for Technology" + technology.getName() + "the
                // potential in MWh is " + technologyPotential);
                // plantCapacity/plant.getActualNominalCapacity());
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
