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
package emlab.gen.role.tender;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import emlab.gen.domain.agent.EMLabModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.TenderBid;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.engine.Role;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractRoleWithFunctionsRole;
import emlab.gen.util.GeometricTrendRegression;

/**
 * @author kaveri, rjjdejeu, marcmel
 */

public class SubmitTenderBidRole extends AbstractRoleWithFunctionsRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {

    Reps reps;


    // market expectations
    Map<ElectricitySpotMarket, MarketInformation> marketInfoMap = new HashMap<ElectricitySpotMarket, MarketInformation>();

    @Override
    public void act(RenewableSupportSchemeTender scheme) {

        Regulator regulator = scheme.getRegulator();

        ElectricitySpotMarket market = reps.findElectricitySpotMarketForZone(regulator.getZone());
//        double targetFactorOverall = reps.findTechnologyNeutralRenewableTargetForTenderByRegulator(scheme.getRegulator())
//                .getYearlyRenewableTargetTimeSeries()
//                .getValue(getCurrentTick() + scheme.getFutureTenderOperationStartTime());

        double tenderTarget = scheme.getAnnualRenewableTargetInMwh();
        if (tenderTarget > 0) {

            for (EnergyProducer agent : reps.findEnergyProducersByMarketAtRandom(market)) {

                long futureTimePoint = getCurrentTick() + agent.getInvestmentFutureTimeHorizon();

                // ==== Expectations ===
                Map<Substance, Double> expectedFuelPrices = predictFuelPrices(agent, futureTimePoint);
                // CO2

                Map<ElectricitySpotMarket, Double> expectedCO2Price = determineExpectedCO2PriceInclTax(futureTimePoint,
                        agent.getNumberOfYearsBacklookingForForecasting(), getCurrentTick()); //copied from ...investment role


                // Demand
                Map<ElectricitySpotMarket, Double> expectedDemand = new HashMap<ElectricitySpotMarket, Double>();
                for (ElectricitySpotMarket elm : getReps().electricitySpotMarkets) {
                    GeometricTrendRegression gtr = new GeometricTrendRegression();
                    for (long time = getCurrentTick(); time > getCurrentTick()
                            - agent.getNumberOfYearsBacklookingForForecasting() && time >= 0; time = time - 1) {
                        gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
                    }
                    expectedDemand.put(elm, gtr.predict(futureTimePoint));
                }

                // ElectricitySpotMarket market = agent.getInvestorMarket();

                // logger.warn("market is: " + market);

                MarketInformation marketInformation = new MarketInformation(market, expectedDemand, expectedFuelPrices,
                        expectedCO2Price.get(market).doubleValue(), futureTimePoint);

                Zone zone = agent.getInvestorMarket().getZone();

                for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {

                    EMLabModel model = getReps().emlabModel;
                    
                    if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                        continue;

                    Iterable<PowerGridNode> possibleInstallationNodes;

                    /*
                     * For dispatchable technologies just choose a random node.
                     * For intermittent evaluate all possibilities.
                     */
                    // if (technology.isIntermittent())
                    possibleInstallationNodes = reps.findAllPowerGridNodesByZone(market.getZone());
                    // else {
                    // possibleInstallationNodes = new
                    // LinkedList<PowerGridNode>();
                    // ((LinkedList<PowerGridNode>)
                    // possibleInstallationNodes).add(reps.powerGridNodeRepository
                    // .findAllPowerGridNodesByZone(market.getZone()).iterator().next());
                    // }

                    for (PowerGridNode node : possibleInstallationNodes) {

                        // logger.warn("node: " + node);

//                        PowerPlant plant = new PowerPlant();
//                        plant.specifyNotPersist(getCurrentTick(), agent, node, technology);

                        PowerPlant plant = getReps().createAndSpecifyTemporaryPowerPlant(getCurrentTick(), agent, node, technology);

                        
                        // plant.setRenewableTenderDummyPowerPlant(true);

                        // CALCULATING NODE LIMIT

                        // logger.warn("pgtNodeLimit 1 is: " + pgtNodeLimit);

                        // Calculate bid quantity. Number of plants to be bid -
                        // as
                        // many
                        // as
                        // the node permits

                        // logger.warn("submit bid: annual full load hours" +
                        // (plant.getAnnualFullLoadHours()));

                        // logger.warn("submit bid role: pgt node limit in Mwh"
                        // + pgtNodeLimitInMwh);
                        // logger.warn("number of plants by node capacity" +
                        // ratioByNodeCapacity);// capacityTesting

                        double cashNeededPerPlant = plant.getActualInvestedCapital()
                                * (1 - agent.getDebtRatioOfInvestments()) / plant.getActualLeadtime();

                        double noOfPlantsByTarget = scheme.getAnnualRenewableTargetInMwh()
                                / (plant.getAnnualFullLoadHours() * plant.getActualNominalCapacity());

                        // logger.warn(
                        // "submit bid role: actual target for scheme " +
                        // scheme.getAnnualRenewableTargetInMwh());
                        // logger.warn("number of plants by target" +
                        // noOfPlantsByTarget + "generation expected per plant"
                        // + (plant.getAnnualFullLoadHours() *
                        // plant.getActualNominalCapacity()));

                        // long noOfPlants = (long)
                        // Math.ceil(Math.min(numberOfPlantsByNodeLimit,
                        // noOfPlantsByTarget));

                        // Target should equal node potential.
                        long noOfPlants = (long) Math.ceil(noOfPlantsByTarget);

                        // logger.warn("NUMBER OF PLANTS TO BE BID FOR" +
                        // numberOfPlants);
                        // computing tender bid price

                        Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
                        for (Substance fuel : technology.getFuels()) {
                            myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
                        }

                        Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices,
                                expectedCO2Price.get(market));
                        plant.setFuelMix(fuelMix);

                        double expectedMarginalCost = determineExpectedMarginalCost(plant, expectedFuelPrices,
                                expectedCO2Price.get(market));
                        double runningHours = 0d;
                        //double expectedGrossProfit = 0d;

                        long numberOfSegments = getReps().segments.size();
                        double totalAnnualExpectedGenerationOfPlant = 0d;

                        double annualMarginalCost = 0d;
                        double totalGenerationinMWh = 0d;
                        double loadFactor = 0d;
                        double expectedAnnualRevenue = 0d;
                        //double expectedAnnualVariableCost = 0d;
                        long tenderSchemeDuration = scheme.getSupportSchemeDuration();

                        totalGenerationinMWh = plant.getAnnualFullLoadHours() * plant.getActualNominalCapacity();
                        annualMarginalCost = totalGenerationinMWh * expectedMarginalCost;

                        // logger.warn("support scheme duration " +
                        // tenderSchemeDuration);

                        // should be
                        // modified when
                        // location
                        // specific

                        for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
                            double expectedElectricityPrice = marketInformation.expectedElectricityPricesPerSegment
                                    .get(segmentLoad.getSegment());

                            double hours = segmentLoad.getSegment().getLengthInHours();

                            // logger.warn("expectedMarginalCost; " +
                            // expectedMarginalCost);
                            // logger.warn("expectedElectricityPrice; " +
                            // expectedElectricityPrice);

                            runningHours = runningHours + hours;
                            if (technology.isIntermittent()) {
                                loadFactor = reps
                                        .findIntermittentTechnologyNodeLoadFactorForNodeAndTechnology(node, technology)
                                        .getLoadFactorForSegment(segmentLoad.getSegment());
                                //expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost) * hours
                                //        * plant.getActualNominalCapacity() * loadFactor;
                                expectedAnnualRevenue += expectedElectricityPrice * hours
                                        * plant.getActualNominalCapacity() * loadFactor;
                                //expectedAnnualVariableCost += expectedMarginalCost * hours
                                //        * plant.getActualNominalCapacity() * loadFactor;

                                totalAnnualExpectedGenerationOfPlant += hours * plant.getActualNominalCapacity()
                                        * loadFactor;

                            } else {
                                //expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost) * hours
                                //       * plant.getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(),
                                //                numberOfSegments);
                                expectedAnnualRevenue += expectedElectricityPrice * hours * plant.getAvailableCapacity(
                                        futureTimePoint, segmentLoad.getSegment(), numberOfSegments);
                                //expectedAnnualVariableCost += expectedMarginalCost * hours * plant.getAvailableCapacity(
                                //        futureTimePoint, segmentLoad.getSegment(), numberOfSegments);
                                totalAnnualExpectedGenerationOfPlant += hours * plant.getAvailableCapacity(
                                        futureTimePoint, segmentLoad.getSegment(), numberOfSegments);

                            }
                        }

                        // logger.warn("expectedGrossProfit; " +
                        // expectedGrossProfit);
                        // logger.warn("totalAnnualExpectedGenerationOfPlant; "
                        // +
                        // totalAnnualExpectedGenerationOfPlant);

                        double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());
                        double operatingCost = fixedOMCost + annualMarginalCost;
                        // logger.warn("fixedOMCost; " + fixedOMCost);

                        //double operatingProfit = expectedGrossProfit - fixedOMCost;
                        double operatingRevenue = expectedAnnualRevenue;

                        // logger.warn("operatingProfit; " + operatingProfit);

                        double wacc = (1 - agent.getDebtRatioOfInvestments()) * agent.getEquityInterestRate()
                                + agent.getDebtRatioOfInvestments() * agent.getLoanInterestRate();

                        double waccAdjusted = (1 - agent.getDebtRatioOfInvestments())
                                * (agent.getEquityInterestRate() + agent.getEquityRatePriceRiskComponent())
                                + agent.getDebtRatioOfInvestments() * agent.getLoanInterestRate();

                        // logger.warn("wacc; " + wacc);

                        TreeMap<Integer, Double> discountedProjectCapitalOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                                technology.getDepreciationTime(),
                                (int) (plant.calculateActualLeadtime() + plant.calculateActualPermittime()),
                                plant.getActualInvestedCapital(), 0);

                        // logger.warn("discountedProjectCapitalOutflow; " +
                        // discountedProjectCapitalOutflow);

                        // Creation of in cashflow during operation
//                        TreeMap<Integer, Double> discountedProjectCashInflow = calculateSimplePowerPlantInvestmentCashFlow(
//                                technology.getDepreciationTime(),
//                                (int) (plant.calculateActualLeadtime() + plant.calculateActualPermittime()), 0,
//                                operatingProfit);

                        TreeMap<Integer, Double> discountedProjectCashflowRevenue = calculateSimplePowerPlantInvestmentCashFlow(
                                technology.getDepreciationTime(),
                                (int) (plant.calculateActualLeadtime() + plant.calculateActualPermittime()), 0,
                                operatingRevenue);

                        TreeMap<Integer, Double> discountedProjectCashOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                                technology.getDepreciationTime(), (int) (plant.calculateActualLeadtime()), 1,
                                -operatingCost);

                        // logger.warn("discountedProjectCashInflow; " +
                        // discountedProjectCashInflow);

                        double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);
                        double discountedOpRevenue = npv(discountedProjectCashflowRevenue, waccAdjusted);
                        double discountedOpCost = npv(discountedProjectCashOutflow, wacc);
                        //double discountedOpProfit = npv(discountedProjectCashInflow, wacc);
                        double projectValue = discountedOpCost + discountedCapitalCosts + discountedOpRevenue;

                        // Ex Post Calculations.

                        double projectCost = discountedOpCost + discountedCapitalCosts;

                        double projectValueFinal = (scheme.isExpostRevenueCalculation() == true) ? projectCost
                                : projectValue;
                        // logger.warn("discountedCapitalCosts; " +
                        // discountedCapitalCosts);
                        // logger.warn("discountedOpProfit; " +
                        // discountedOpProfit);
                        // logger.warn("projectValue; " + projectValue);

                        double bidPricePerMWh = 0d;

                        // if (projectValueFinal >= 0 ||
                        // totalAnnualExpectedGenerationOfPlant == 0) {
                        // bidPricePerMWh = 0d;

                        // } else {

                        // calculate discounted tender return factor term
                        TreeMap<Integer, Double> discountedTenderReturnFactorSummingTerm = calculateSimplePowerPlantInvestmentCashFlow(
                                (int) tenderSchemeDuration, (int) (plant.calculateActualLeadtime()), 0, 1);
                        double discountedTenderReturnFactor = npv(discountedTenderReturnFactorSummingTerm, wacc);

                        // logger.warn("discountedTenderReturnFactor; " +
                        // discountedTenderReturnFactor);

                        if (discountedTenderReturnFactor == 0) {
                            bidPricePerMWh = 0d;

                        } else {

                            // calculate generation in MWh per year
                            bidPricePerMWh = -projectValueFinal
                                    / (discountedTenderReturnFactor * totalAnnualExpectedGenerationOfPlant);

                            if (bidPricePerMWh < 0)
                                bidPricePerMWh = 0;

                            // logger.warn("for scheme" + scheme.getName() +
                            // "bidding for " + noOfPlants + "at price"
                            // + bidPricePerMWh);
                            for (long i = 1; i <= noOfPlants; i++) {

                                long start = getCurrentTick() + plant.calculateActualLeadtime()
                                        + plant.calculateActualPermittime();
                                long finish = getCurrentTick() + plant.calculateActualLeadtime()
                                        + plant.calculateActualPermittime() + tenderSchemeDuration;

                                String investor = agent.getName();

                                // logger.warn("investor is; " + investor);

                                TenderBid bid = new TenderBid();
                                
                                bid.specify(totalAnnualExpectedGenerationOfPlant, null, agent, zone, node,
                                        start, finish, bidPricePerMWh, technology, getCurrentTick(), Bid.SUBMITTED,
                                        scheme, cashNeededPerPlant, investor);
                                
                                // logger.warn("SubmitBid 454 - Agent " +
                                // agent + " ,generation "
                                // + totalAnnualExpectedGenerationOfPlant +
                                // " ,plant " + plant + " ,zone "
                                // + zone + " ,node " + node + " ,start " +
                                // start + " ,finish " + finish
                                // + " ,bid price " + bidPricePerMWh +
                                // " ,tech " + technology
                                // + " ,current tick " + getCurrentTick() +
                                // " ,status " + Bid.SUBMITTED
                                // + " ,scheme " + scheme +
                                // ", cash downpayment; "
                                // + cashNeededForPlantDownpayments,
                                // " ,investor " + investor);

                            } // end for loop for tender bids

                        } // end else calculate generation in MWh per year

                        // } // end else calculate discounted tender return
                        // factor
                        // term
                        plant = null;

                    } // end for loop possible installation nodes

                } // end for (PowerGeneratingTechnology technology :
                  // reps.genericRepository.findAll(PowerGeneratingTechnology.class))
                  // logger.warn("Number of tender bids made" + noOfPlantsBid +
                  // "by producer" + agent.getName()
                  // + "for scheme " + scheme.getName());
            } // end For schemes
        }
    }

 

}
