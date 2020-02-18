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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.linear.LinearConstraint;
import org.apache.commons.math.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.math.optimization.linear.Relationship;
import org.apache.commons.math.optimization.linear.SimplexSolver;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import agentspring.trend.GeometricTrend;
import emlab.gen.domain.agent.CommoditySupplier;
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.agent.StochasticTargetInvestor;
import emlab.gen.domain.agent.StrategicReserveOperator;
import emlab.gen.domain.agent.TargetInvestor;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.CO2Auction;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.DecarbonizationMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.PowerGeneratingTechnologyTarget;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.TenderBid;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGeneratingTechnologyNodeLimit;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.repository.Reps;
import emlab.gen.repository.StrategicReserveOperatorRepository;
import emlab.gen.util.GeometricTrendRegression;
import emlab.gen.util.MapValueComparator;

/**
 * @author kaveri, rjjdejeu
 *
 */

@RoleComponent
public class SubmitTenderBidRoleExpostRevenuePayment extends AbstractRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {

    @Transient
    @Autowired
    Reps reps;

    @Transient
    @Autowired
    Neo4jTemplate template;

    @Transient
    @Autowired
    StrategicReserveOperatorRepository strategicReserveOperatorRepository;

    // market expectations
    @Transient
    Map<ElectricitySpotMarket, MarketInformation> marketInfoMap = new HashMap<ElectricitySpotMarket, MarketInformation>();

    @Transactional
    @Override
    public void act(RenewableSupportSchemeTender scheme) {

        double targetFactorOverall = reps.renewableTargetForTenderRepository
                .findTechnologyNeutralRenewableTargetForTenderByRegulator(scheme.getRegulator())
                .getYearlyRenewableTargetTimeSeries()
                .getValue(getCurrentTick() + scheme.getFutureTenderOperationStartTime());

        Regulator regulator = scheme.getRegulator();
        ElectricitySpotMarket market = reps.marketRepository.findElectricitySpotMarketForZone(regulator.getZone());

        double tenderTarget = scheme.getAnnualRenewableTargetInMwh();
        if (tenderTarget > 0) {
            int noOfPlantsBid = 0;

            for (EnergyProducer agent : reps.energyProducerRepository.findEnergyProducersByMarketAtRandom(market)) {
                double agentsDownpaymentFractionOfCash = agent.getDownpaymentFractionOfCash();
                double agentsCurrentcash = agent.getCash();

                // logger.warn("Submit Tender Bid Role started for: " + agent);

                long futureTimePoint = getCurrentTick() + agent.getInvestmentFutureTimeHorizon();

                // ==== Expectations ===
                Map<Substance, Double> expectedFuelPrices = predictFuelPrices(agent, futureTimePoint);
                // CO2
                Map<ElectricitySpotMarket, Double> expectedCO2Price = determineExpectedCO2PriceInclTaxAndFundamentalForecast(
                        futureTimePoint, agent.getNumberOfYearsBacklookingForForecasting(), 0, getCurrentTick());
                        // logger.warn("{} expects CO2 prices {}",
                        // agent.getName(),
                        // expectedCO2Price);
                        // Map<ElectricitySpotMarket, Double>
                        // expectedCO2PriceOld =
                        // determineExpectedCO2PriceInclTax(
                        // futureTimePoint,
                        // agent.getNumberOfYearsBacklookingForForecasting(),
                        // getCurrentTick());

                // logger.warn("{} used to expect CO2 prices {}",
                // agent.getName(),
                // expectedCO2PriceOld);

                // logger.warn(expectedCO2Price.toString());

                // Demand
                Map<ElectricitySpotMarket, Double> expectedDemand = new HashMap<ElectricitySpotMarket, Double>();
                for (ElectricitySpotMarket elm : reps.template.findAll(ElectricitySpotMarket.class)) {
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
                // RenewableSupportSchemeTender scheme =
                // reps.renewableSupportSchemeTenderRepository
                // .determineSupportSchemeForZone(zone);

                // Iterable<RenewableSupportSchemeTender> schemes = null;
                // schemes =
                // reps.renewableSupportSchemeTenderRepository.determineSpecificSupportSchemeForEnergyProducer(agent);
                //
                // for (RenewableSupportSchemeTender scheme : schemes) {
                //
                // logger.warn(" " + scheme);

                int noOfPlantsConsider = 0;

                for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {
                    double targetFactorTechSpec = reps.renewableTargetForTenderRepository
                            .findTechnologySpecificRenewableTargetTimeSeriesForTenderByScheme(scheme,
                                    technology.getName())
                            .getValue(getCurrentTick() + scheme.getFutureTenderOperationStartTime());

                    // if (scheme.isTechnologySpecificityEnabled() == true) {
                    // PowerGeneratingTechnology technology =
                    // scheme.getCurrentTechnologyUnderConsideration();
                    // }

                    // logger.warn("eligible are: " + technology);

                    DecarbonizationModel model = reps.genericRepository.findAll(DecarbonizationModel.class).iterator()
                            .next();

                    if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                        continue;

                    Iterable<PowerGridNode> possibleInstallationNodes;

                    /*
                     * For dispatchable technologies just choose a random node.
                     * For intermittent evaluate all possibilities.
                     */
                    if (technology.isIntermittent())
                        possibleInstallationNodes = reps.powerGridNodeRepository
                                .findAllPowerGridNodesByZone(market.getZone());
                    else {
                        possibleInstallationNodes = new LinkedList<PowerGridNode>();
                        ((LinkedList<PowerGridNode>) possibleInstallationNodes).add(reps.powerGridNodeRepository
                                .findAllPowerGridNodesByZone(market.getZone()).iterator().next());
                    }

                    // logger.warn("technology is " + technology);

                    // "technology is intermittent? " +
                    // technology.isIntermittent());
                    // logger.warn("possibleInstallationNodes is: " +
                    // possibleInstallationNodes);

                    // logger.warn("Calculating for " + technology.getName() +
                    // ", for Nodes: "
                    // + possibleInstallationNodes.toString());

                    for (PowerGridNode node : possibleInstallationNodes) {

                        PowerPlant plant = new PowerPlant();
                        plant.specifyNotPersist(getCurrentTick(), agent, node, technology);
                        plant.setRenewableTenderDummyPowerPlant(true);
                        double cashAvailableForPlantDownpayments = 0d;

                        // CALCULATING NODE LIMIT
                        double pgtNodeLimit = Double.MAX_VALUE;

                        // logger.warn("pgtNodeLimit 1 is: " + pgtNodeLimit);

                        PowerGeneratingTechnologyNodeLimit pgtLimit = reps.powerGeneratingTechnologyNodeLimitRepository
                                .findOneByTechnologyAndNode(technology, plant.getLocation());
                        double expectedInstalledCapacityOfTechnologyInNode = reps.powerPlantRepository
                                .calculateCapacityOfExpectedOperationalPowerPlantsByNodeAndTechnology(
                                        plant.getLocation(), technology, futureTimePoint);

                        if (pgtLimit != null) {
                            pgtNodeLimit = pgtLimit.getUpperCapacityLimit(futureTimePoint)
                                    - (expectedInstalledCapacityOfTechnologyInNode);
                        }

                        // Calculate bid quantity. Number of plants to be bid -
                        // as
                        // many
                        // as
                        // the node permits

                        double ratioByNodeCapacity = pgtNodeLimit / plant.getActualNominalCapacity();

                        // capacityTesting
                        double numberOfPlantsByNodeLimit = (long) ratioByNodeCapacity; // truncates
                        // towards
                        // lower
                        // integer

                        // strategy for diversification of bids: cash available
                        // for each technology is equal to the proportion of
                        // that
                        // technology's contribution to the overall renewable
                        // generation target

                        double proportionOfCashAvailableByTechnology = targetFactorTechSpec / targetFactorOverall;

                        cashAvailableForPlantDownpayments = agentsDownpaymentFractionOfCash * agentsCurrentcash
                                * proportionOfCashAvailableByTechnology;

                        double cashNeededPerPlant = plant.getActualInvestedCapital()
                                * (1 - agent.getDebtRatioOfInvestments()) / plant.getActualLeadTime();

                        double noOfPlantsByTechnologyTargetBasedCashAvailability = (long) cashAvailableForPlantDownpayments
                                / cashNeededPerPlant;

                        double noOfPlantsByTarget = scheme.getAnnualRenewableTargetInMwh()
                                / (plant.getAnnualFullLoadHours() * plant.getActualNominalCapacity());

                        long noOfPlants = (long) Math.min(numberOfPlantsByNodeLimit, noOfPlantsByTarget);

                        Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
                        for (Substance fuel : technology.getFuels()) {
                            myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
                        }

                        Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices,
                                expectedCO2Price.get(market));
                        plant.setFuelMix(fuelMix);

                        double expectedMarginalCost = determineExpectedMarginalCost(plant, expectedFuelPrices,
                                expectedCO2Price.get(market));
                        // logger.warn("expected marginal cost in fip role for
                        // plant " +
                        // plant + "is " + expectedMarginalCost);
                        // insert here

                        double annualMarginalCost = 0d;
                        double totalGenerationinMWh = 0d;
                        long tenderSchemeDuration = scheme.getSupportSchemeDuration();

                        totalGenerationinMWh = plant.getAnnualFullLoadHours() * plant.getActualNominalCapacity();
                        annualMarginalCost = totalGenerationinMWh * expectedMarginalCost;

                        // logger.warn("for technology " +
                        // plant.getTechnology().getName() + " total generation
                        // is "
                        // + totalGenerationinMWh + " and running hours is " +
                        // fullLoadHours);

                        double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());
                        double operatingCost = fixedOMCost + annualMarginalCost;

                        // end insert here
                        double wacc = (1 - agent.getDebtRatioOfInvestments()) * agent.getEquityInterestRate()
                                + agent.getDebtRatioOfInvestments() * agent.getLoanInterestRate();

                        TreeMap<Integer, Double> discountedProjectCapitalOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                                technology.getDepreciationTime(), (int) (plant.calculateActualLeadtime()),
                                plant.getActualInvestedCapital(), 0);

                        // Creation of in cashflow during operation
                        TreeMap<Integer, Double> discountedProjectCashOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                                technology.getDepreciationTime(), (int) (plant.calculateActualLeadtime()), 1,
                                -operatingCost);

                        double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);
                        double discountedOpCost = npv(discountedProjectCashOutflow, wacc);
                        double projectCost = discountedOpCost + discountedCapitalCosts;

                        double bidPricePerMWh = 0d;

                        // calculate discounted tender return factor term
                        TreeMap<Integer, Double> discountedTenderReturnFactorSummingTerm = calculateSimplePowerPlantInvestmentCashFlow(
                                (int) tenderSchemeDuration, (int) plant.calculateActualLeadtime(), 0, 1);
                        double discountedTenderReturnFactor = npv(discountedTenderReturnFactorSummingTerm, wacc);

                        if (discountedTenderReturnFactor == 0) {
                            bidPricePerMWh = 0d;

                        } else {

                            // calculate generation in MWh per year
                            bidPricePerMWh = -projectCost / (discountedTenderReturnFactor * totalGenerationinMWh);

                            // logger.warn("for scheme" + scheme.getName() +
                            // "bidding for " + noOfPlants + "at price"
                            // + bidPricePerMWh);

                            for (long i = 1; i <= noOfPlants; i++) {

                                noOfPlantsBid++;
                                // logger.warn("FOR pp - no of plants Bid; " +
                                // noOfPlantsBid);

                                long start = getCurrentTick() + plant.calculateActualLeadtime()
                                        + plant.calculateActualPermittime();
                                long finish = getCurrentTick() + plant.calculateActualLeadtime()
                                        + plant.calculateActualPermittime() + tenderSchemeDuration;

                                TenderBid bid = new TenderBid();
                                bid.specifyNotPersist(totalGenerationinMWh, plant, agent, zone, node, start, finish,
                                        bidPricePerMWh, technology, getCurrentTick(), Bid.SUBMITTED, scheme,
                                        cashNeededPerPlant, agent.getName());
                                persistTenderBid(bid);

                            } // end for loop for tender bids

                        } // end else calculate generation in MWh per year

                        deleteTempPowerPlants(plant);
                    } // end else calculate discounted tender return factor
                      // term

                } // end for loop possible installation nodes

            } // end for (PowerGeneratingTechnology technology :
              // reps.genericRepository.findAll(PowerGeneratingTechnology.class))
            logger.warn("Number of tender bids for all energy producers " + noOfPlantsBid);
        } // end For schemes
    }

    @Transactional
    private void deleteTempPowerPlants(PowerPlant plant) {
        plant.setDismantleTime(getCurrentTick());
        plant.persist();
    }

    @Transactional
    private void persistTenderBid(TenderBid bid) {
        bid.persist();
        // logger.warn("bid has been persisted");
    }
    // }

    // Creates n downpayments of equal size in each of the n building years of a
    // power plant
    // @Transactional
    // private void createSpreadOutDownPayments(EnergyProducer agent,
    // PowerPlantManufacturer manufacturer,
    // double totalDownPayment, PowerPlant plant) {
    // int buildingTime = (int) (plant.calculateActualLeadtime() +
    // plant.calculateActualPermittime());
    // reps.nonTransactionalCreateRepository.createCashFlow(agent, manufacturer,
    // totalDownPayment / buildingTime,
    // CashFlow.DOWNPAYMENT, getCurrentTick(), plant);
    // Loan downpayment = reps.loanRepository.createLoan(agent, manufacturer,
    // totalDownPayment / buildingTime,
    // buildingTime - 1, getCurrentTick(), plant);
    // plant.createOrUpdateDownPayment(downpayment);
    // }

    /**
     * Predicts fuel prices for {@link futureTimePoint} using a geometric trend
     * regression forecast. Only predicts fuels that are traded on a commodity
     * market.
     * 
     * @param agent
     * @param futureTimePoint
     * @return Map<Substance, Double> of predicted prices.
     */
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

    public double findLastKnownPriceForSubstance(Substance substance, long clearingTick) {

        DecarbonizationMarket market = reps.marketRepository.findFirstMarketBySubstance(substance);
        if (market == null) {
            logger.warn("No market found for {} so no price can be found", substance.getName());
            return 0d;
        } else {
            return findLastKnownPriceOnMarket(market, clearingTick);
        }
    }

    public double findLastKnownPriceOnMarket(DecarbonizationMarket market, long clearingTick) {
        Double average = calculateAverageMarketPriceBasedOnClearingPoints(
                reps.clearingPointRepositoryOld.findClearingPointsForMarketAndTime(market, clearingTick, false));
        Substance substance = market.getSubstance();

        if (average != null) {
            logger.info("Average price found on market for this tick for {}", substance.getName());
            return average;
        }

        average = calculateAverageMarketPriceBasedOnClearingPoints(
                reps.clearingPointRepositoryOld.findClearingPointsForMarketAndTime(market, clearingTick - 1, false));
        if (average != null) {
            logger.info("Average price found on market for previous tick for {}", substance.getName());
            return average;
        }

        if (market.getReferencePrice() > 0) {
            logger.info("Found a reference price found for market for {}", substance.getName());
            return market.getReferencePrice();
        }

        for (CommoditySupplier supplier : reps.genericRepository.findAll(CommoditySupplier.class)) {
            if (supplier.getSubstance().equals(substance)) {

                return supplier.getPriceOfCommodity().getValue(clearingTick);
            }
        }

        logger.info("No price has been found for {}", substance.getName());
        return 0d;
    }

    private Double calculateAverageMarketPriceBasedOnClearingPoints(Iterable<ClearingPoint> clearingPoints) {
        double priceTimesVolume = 0d;
        double volume = 0d;

        for (ClearingPoint point : clearingPoints) {
            priceTimesVolume += point.getPrice() * point.getVolume();
            volume += point.getVolume();
        }
        if (volume > 0) {
            return priceTimesVolume / volume;
        }
        return null;
    }

    public Set<SubstanceShareInFuelMix> calculateFuelMix(PowerPlant plant, Map<Substance, Double> substancePriceMap,
            double co2Price) {

        double efficiency = plant.getActualEfficiency();

        Set<SubstanceShareInFuelMix> fuelMix = (plant.getFuelMix() == null) ? new HashSet<SubstanceShareInFuelMix>()
                : plant.getFuelMix();

        int numberOfFuels = substancePriceMap.size();
        if (numberOfFuels == 0) {
            logger.info("No fuels, so no operation mode is set. Empty fuel mix is returned");
            return new HashSet<SubstanceShareInFuelMix>();
        } else if (numberOfFuels == 1) {
            SubstanceShareInFuelMix ssifm = null;
            if (!fuelMix.isEmpty()) {
                ssifm = fuelMix.iterator().next();
            } else {
                ssifm = new SubstanceShareInFuelMix().persist();
                fuelMix.add(ssifm);
            }

            Substance substance = substancePriceMap.keySet().iterator().next();

            ssifm.setShare(calculateFuelConsumptionWhenOnlyOneFuelIsUsed(substance, efficiency));
            ssifm.setSubstance(substance);
            logger.info("Setting fuel consumption for {} to {}", ssifm.getSubstance().getName(), ssifm.getShare());

            return fuelMix;
        } else {

            double minimumFuelMixQuality = plant.getTechnology().getMinimumFuelQuality();

            double[] fuelAndCO2Costs = new double[numberOfFuels];
            double[] fuelDensities = new double[numberOfFuels];
            double[] fuelQuality = new double[numberOfFuels];

            int i = 0;
            for (Substance substance : substancePriceMap.keySet()) {
                fuelAndCO2Costs[i] = substancePriceMap.get(substance) + substance.getCo2Density() * (co2Price);
                fuelDensities[i] = substance.getEnergyDensity();
                fuelQuality[i] = (substance.getQuality() - minimumFuelMixQuality) * fuelDensities[i];
                i++;
            }

            logger.info("Fuel prices: {}", fuelAndCO2Costs);
            logger.info("Fuel densities: {}", fuelDensities);
            logger.info("Fuel purities: {}", fuelQuality);

            // Objective function = minimize fuel cost (fuel
            // consumption*fuelprices
            // + CO2 intensity*co2 price/tax)
            LinearObjectiveFunction function = new LinearObjectiveFunction(fuelAndCO2Costs, 0d);

            List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

            // Constraint 1: total fuel density * fuel consumption should match
            // required energy input
            constraints.add(new LinearConstraint(fuelDensities, Relationship.EQ, (1 / efficiency)));

            // Constraint 2&3: minimum fuel quality (times fuel consumption)
            // required
            // The equation is derived from (example for 2 fuels): q1 * x1 /
            // (x1+x2) + q2 * x2 / (x1+x2) >= qmin
            // so that the fuelquality weighted by the mass percentages is
            // greater than the minimum fuel quality.
            constraints.add(new LinearConstraint(fuelQuality, Relationship.GEQ, 0));

            try {
                SimplexSolver solver = new SimplexSolver();
                RealPointValuePair solution = solver.optimize(function, constraints, GoalType.MINIMIZE, true);

                logger.info("Succesfully solved a linear optimization for fuel mix");

                int f = 0;
                Iterator<SubstanceShareInFuelMix> iterator = plant.getFuelMix().iterator();
                for (Substance substance : substancePriceMap.keySet()) {
                    double share = solution.getPoint()[f];

                    SubstanceShareInFuelMix ssifm;
                    if (iterator.hasNext()) {
                        ssifm = iterator.next();
                    } else {
                        ssifm = new SubstanceShareInFuelMix().persist();
                        fuelMix.add(ssifm);
                    }

                    double fuelConsumptionPerMWhElectricityProduced = convertFuelShareToMassVolume(share);
                    logger.info("Setting fuel consumption for {} to {}", substance.getName(),
                            fuelConsumptionPerMWhElectricityProduced);
                    ssifm.setShare(fuelConsumptionPerMWhElectricityProduced);
                    ssifm.setSubstance(substance);
                    f++;
                }

                logger.info("If single fired, it would have been: {}", calculateFuelConsumptionWhenOnlyOneFuelIsUsed(
                        substancePriceMap.keySet().iterator().next(), efficiency));
                return fuelMix;
            } catch (OptimizationException e) {
                logger.warn(
                        "Failed to determine the correct fuel mix. Adding only fuel number 1 in fuel mix out of {} substances and minimum quality of {}",
                        substancePriceMap.size(), minimumFuelMixQuality);
                logger.info("The fuel added is: {}", substancePriceMap.keySet().iterator().next().getName());

                // Override the old one
                fuelMix = new HashSet<SubstanceShareInFuelMix>();
                SubstanceShareInFuelMix ssifm = new SubstanceShareInFuelMix().persist();
                Substance substance = substancePriceMap.keySet().iterator().next();

                ssifm.setShare(calculateFuelConsumptionWhenOnlyOneFuelIsUsed(substance, efficiency));
                ssifm.setSubstance(substance);
                logger.info("Setting fuel consumption for {} to {}", ssifm.getSubstance().getName(), ssifm.getShare());
                fuelMix.add(ssifm);
                return fuelMix;
            }
        }
    }

    public double convertFuelShareToMassVolume(double share) {
        return share * 3600;
    }

    public double calculateFuelConsumptionWhenOnlyOneFuelIsUsed(Substance substance, double efficiency) {

        double fuelConsumptionPerMWhElectricityProduced = convertFuelShareToMassVolume(
                1 / (efficiency * substance.getEnergyDensity()));

        return fuelConsumptionPerMWhElectricityProduced;

    }

    // Create a powerplant investment and operation cash-flow in the form of a
    // map. If only investment, or operation costs should be considered set
    // totalInvestment or operatingProfit to 0
    private TreeMap<Integer, Double> calculateSimplePowerPlantInvestmentCashFlow(int depriacationTime, int buildingTime,
            double totalInvestment, double operatingProfit) {
        TreeMap<Integer, Double> investmentCashFlow = new TreeMap<Integer, Double>();
        double equalTotalDownPaymentInstallement = totalInvestment / buildingTime;
        for (int i = 0; i < buildingTime; i++) {
            investmentCashFlow.put(new Integer(i), -equalTotalDownPaymentInstallement);
        }
        for (int i = buildingTime; i < depriacationTime + buildingTime; i++) {
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

    private PowerGridNode getNodeForZone(Zone zone) {
        for (PowerGridNode node : reps.genericRepository.findAll(PowerGridNode.class)) {
            if (node.getZone().equals(zone)) {
                return node;
            }
        }
        return null;
    }

    public double calculateFixedOperatingCost(PowerPlant powerPlant, long clearingTick) {

        double norm = powerPlant.getActualFixedOperatingCost();
        long timeConstructed = powerPlant.getConstructionStartTime() + powerPlant.calculateActualLeadtime();
        double mod = powerPlant.getTechnology().getFixedOperatingCostModifierAfterLifetime();
        long lifetime = powerPlant.calculateActualLifetime();

        GeometricTrend trend = new GeometricTrend();
        trend.setGrowthRate(mod);
        trend.setStart(norm);

        double currentCost = trend.getValue(clearingTick - (timeConstructed + lifetime));
        return currentCost;
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
                for (StrategicReserveOperator operator : strategicReserveOperatorRepository.findAll()) {
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

    /**
     * Calculates expected CO2 price based on a geometric trend estimation, of
     * the past years. The adjustmentForDetermineFuelMix needs to be set to 1,
     * if this is used in the determine fuel mix role.
     *
     * @param futureTimePoint
     *            Year the prediction is made for
     * @param yearsLookingBackForRegression
     *            How many years are used as input for the regression, incl the
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

}