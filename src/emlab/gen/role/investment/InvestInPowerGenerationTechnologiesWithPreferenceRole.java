/** *****************************************************************************
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
 ***************************************************************************** */
package emlab.gen.role.investment;

import emlab.gen.domain.agent.BigBank;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.PowerPlantManufacturer;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.PowerGeneratingTechnologyTarget;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGeneratingTechnologyNodeLimit;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.engine.Schedule;
import emlab.gen.util.GeometricTrendRegression;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.concurrent.ThreadLocalRandom;


/**
 * {@link EnergyProducer}s decide to invest in new {@link PowerPlant}
 *
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 * <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas Chmieliauskas</a>
 * @author JCRichstein
 * @author Marc Melliger
 */
public class InvestInPowerGenerationTechnologiesWithPreferenceRole<T extends EnergyProducer> extends InvestInPowerGenerationTechnologiesRole<T> {

    Map<ElectricitySpotMarket, MarketInformation> marketInfoMap = new HashMap<ElectricitySpotMarket, MarketInformation>();
    
    private double randomUtilityBound = 0;

    public InvestInPowerGenerationTechnologiesWithPreferenceRole(Schedule schedule) {
        super(schedule);
    }

    @Override
    public void act(T agent) {
        logger.info(agent + " in considering investment with horizon " + agent.getInvestmentFutureTimeHorizon());
        long futureTimePoint = getCurrentTick() + agent.getInvestmentFutureTimeHorizon();

        // ==== Expectations ===
        Map<Substance, Double> expectedFuelPrices = predictFuelPrices(agent, futureTimePoint);

        // CO2
        Map<ElectricitySpotMarket, Double> expectedCO2Price = determineExpectedCO2PriceInclTax(futureTimePoint,
                agent.getNumberOfYearsBacklookingForForecasting(), getCurrentTick());

        // logger.warn(expectedCO2Price.toString());
        //Demand
        Map<ElectricitySpotMarket, Double> expectedDemand = new HashMap<ElectricitySpotMarket, Double>();
        for (ElectricitySpotMarket elm : getReps().electricitySpotMarkets) {
            GeometricTrendRegression gtr = new GeometricTrendRegression();
            for (long time = getCurrentTick(); time > getCurrentTick() - agent.getNumberOfYearsBacklookingForForecasting() && time >= 0; time = time - 1) {
                gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
            }
            expectedDemand.put(elm, gtr.predict(futureTimePoint));
        }

        
        PowerPlant bestPlant = null;
        ElectricitySpotMarket bestPlantMarket = null;
        double highestValue = Double.MIN_VALUE;
        
        for(ElectricitySpotMarket market : agent.getPotentialInvestorMarkets()) {
	        
	        MarketInformation marketInformation = new MarketInformation(market, expectedDemand, expectedFuelPrices, expectedCO2Price.get(market)
	                .doubleValue(), futureTimePoint);
		        
	        for (PowerGeneratingTechnology technology : getReps().powerGeneratingTechnologies) {
	        	
	        	// This investor role only considers investing into technologies for which it has preferences
	        	if(agent.getUtilityTechnology().containsKey(technology.getName())) {	        	
		
		            PowerPlant plant = getReps().createAndSpecifyTemporaryPowerPlant(getCurrentTick(), agent, getNodeForZone(market.getZone()), technology);
		//            plant.specifyNotPersist(getCurrentTick(), agent, getNodeForZone(market.getZone()), technology);
		            // if too much capacity of this technology in the pipeline (not
		            // limited to the 5 years)
		            double expectedInstalledCapacityOfTechnology = getReps().calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market, technology, futureTimePoint);
		            PowerGeneratingTechnologyTarget technologyTarget = getReps().findPowerGeneratingTechnologyTargetByTechnologyAndMarket(technology, market);
		            if (technologyTarget != null) {
		                double technologyTargetCapacity = technologyTarget.getTrend().getValue(futureTimePoint);
		                expectedInstalledCapacityOfTechnology = (technologyTargetCapacity > expectedInstalledCapacityOfTechnology) ? technologyTargetCapacity : expectedInstalledCapacityOfTechnology;
		            }
		            double pgtNodeLimit = Double.MAX_VALUE;
		            PowerGeneratingTechnologyNodeLimit pgtLimit = getReps()
		                    .findOneByTechnologyAndNode(technology, plant.getLocation());
		            if (pgtLimit != null) {
		                pgtNodeLimit = pgtLimit.getUpperCapacityLimit(futureTimePoint);
		            }
		            double expectedInstalledCapacityOfTechnologyInNode = getReps().calculateCapacityOfExpectedOperationalPowerPlantsByNodeAndTechnology(plant.getLocation(),
		                    technology, futureTimePoint);
		            double expectedOwnedTotalCapacityInMarket = getReps().calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwner(market, futureTimePoint, agent);
		            double expectedOwnedCapacityInMarketOfThisTechnology = getReps()
		                    .calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwnerAndTechnology(market, technology, futureTimePoint,
		                            agent);
		            double capacityOfTechnologyInPipeline = getReps().calculateCapacityOfPowerPlantsByTechnologyInPipeline(
		                    technology, getCurrentTick());
		            double operationalCapacityOfTechnology = getReps().calculateCapacityOfOperationalPowerPlantsByTechnology(
		                    technology, getCurrentTick());
		            double capacityInPipelineInMarket = getReps()
		                    .calculateCapacityOfPowerPlantsByMarketInPipeline(market, getCurrentTick());
		
		            if ((expectedInstalledCapacityOfTechnology + plant.getActualNominalCapacity())
		                    / (marketInformation.maxExpectedLoad + plant.getActualNominalCapacity()) > technology
		                    .getMaximumInstalledCapacityFractionInCountry()) {
		                // logger.warn(agent +
		                // " will not invest in {} technology because there's too much of this type in the market",
		                // technology);
		            } else if ((expectedInstalledCapacityOfTechnologyInNode + plant.getActualNominalCapacity()) > pgtNodeLimit) {
		
		            } else if (expectedOwnedCapacityInMarketOfThisTechnology > expectedOwnedTotalCapacityInMarket
		                    * technology.getMaximumInstalledCapacityFractionPerAgent()) {
		                // logger.warn(agent +
		                // " will not invest in {} technology because there's too much capacity planned by him",
		                // technology);
		            } else if (capacityInPipelineInMarket > 0.2 * marketInformation.maxExpectedLoad) {
		                // logger.warn("Not investing because more than 20% of demand in pipeline.");
		
		            } else if ((capacityOfTechnologyInPipeline > 2.0 * operationalCapacityOfTechnology)
		                    && capacityOfTechnologyInPipeline > 9000) { // TODO:
		                // reflects that you cannot expand a technology out of zero.
		                // logger.warn(agent +
		                // " will not invest in {} technology because there's too much capacity in the pipeline",
		                // technology);
		            } else if (plant.getActualInvestedCapital() * (1 - agent.getDebtRatioOfInvestments()) > agent
		                    .getDownpaymentFractionOfCash() * agent.getCash()) {
		                // logger.warn(agent +
		                // " will not invest in {} technology as he does not have enough money for downpayment",
		                // technology);
		            } else {
		
		                // Passes all hard limits. Financial consideration.
		                Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
		                for (Substance fuel : technology.getFuels()) {
		                    myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
		                }
		                //TODO: all investment: change to an empty fuel mix default.
		                Set<SubstanceShareInFuelMix> fuelMix = new HashSet<SubstanceShareInFuelMix>();
		                if (myFuelPrices.size() > 0) {
		                    fuelMix = calculateFuelMix(plant, myFuelPrices, expectedCO2Price.get(market));
		                }
		                plant.setFuelMix(fuelMix);
		
		                double expectedMarginalCost = determineExpectedMarginalCost(plant, expectedFuelPrices, expectedCO2Price.get(market));
		                double runningHours = 0d;
		                double expectedGrossProfit = 0d;
		
		                long numberOfSegments = getReps().segments.size();
		
		                // TODO somehow the prices of long-term contracts could also
		                // be used here to determine the expected profit. Maybe not
		                // though...
		                for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
		                    double expectedElectricityPrice = marketInformation.expectedElectricityPricesPerSegment.get(segmentLoad
		                            .getSegment());
		                    double hours = segmentLoad.getSegment().getLengthInHours();
		                    if (expectedMarginalCost <= expectedElectricityPrice) {
		                        runningHours += hours;
		                        expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost) * hours
		                                * plant.getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(), numberOfSegments);
		                    }
		                }
		
		                //logger.warning(agent + "expects technology " + technology + " to have " + runningHours + " hours running");
		                //expect to meet minimum running hours?
		                if (runningHours < plant.getTechnology().getMinimumRunningHours()) {
		                    logger.info(agent + " will not invest in " + technology + " technology as he expect to have " + runningHours + " running hours, which is lower then required");
		                } else {
		
		                    double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());// /
		
		                    double operatingProfit = expectedGrossProfit - fixedOMCost;
		
		                    // TODO Alter discount rate on the basis of the amount
		                    // in long-term contracts?
		                    // TODO Alter discount rate on the basis of other stuff,
		                    // such as amount of money, market share, portfolio
		                    // size.
		                    // Calculation of weighted average cost of capital,
		                    // based on the companies debt-ratio
		                    double wacc = (1 - agent.getDebtRatioOfInvestments()) * agent.getEquityInterestRate()
		                            + agent.getDebtRatioOfInvestments() * agent.getLoanInterestRate();
		
		                    // Creation of out cash-flow during power plant building
		                    // phase (note that the cash-flow is negative!)
		                    TreeMap<Integer, Double> discountedProjectCapitalOutflow = calculateSimplePowerPlantInvestmentCashFlow(
		                            technology.getDepreciationTime(), (int) plant.getActualLeadtime(),
		                            plant.getActualInvestedCapital(), 0);
		                    logger.log(Level.FINE,"Discounted capital outflow: during building" + discountedProjectCapitalOutflow.toString());

		                    // Creation of in cashflow during operation
		                    TreeMap<Integer, Double> discountedProjectCashInflow = calculateSimplePowerPlantInvestmentCashFlow(
		                            technology.getDepreciationTime(), (int) plant.getActualLeadtime(), 0, operatingProfit);
		                    logger.log(Level.FINE,"Discounted cash inflow during operation:" + discountedProjectCashInflow.toString());
		                    
		                    double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);// are
		                    logger.log(Level.FINE,"Discounted Capital Costs:" + discountedCapitalCosts);
		                    
		                    // defined
		                    // negative!!
		                    // plant.getActualNominalCapacity();
		
		                    // logger.warn("Agent {}  found that the discounted capital for technology {} to be "
		                    // + discountedCapitalCosts, agent,
		                    // technology);
		                    double discountedOpProfit = npv(discountedProjectCashInflow, wacc);
		                    logger.log(Level.FINE,"Discounted Profit:" + discountedOpProfit);
		
		                    //logger.warning(agent + " found that the projected discounted inflows for technology " + technology + " to be " + discountedOpProfit);
		                    double projectValue = discountedOpProfit + discountedCapitalCosts;
		
		                    logger.info(agent + " found the project value for technology " + technology + " to be " + Math.round(projectValue / (plant.getActualNominalCapacity() * 1e3)) / 1e3 + " million EUR/kW (running hours: " + runningHours + ")");
		                    // double projectTotalValue = projectValuePerMW *
		                    // plant.getActualNominalCapacity();
		                    // double projectReturnOnInvestment = discountedOpProfit
		                    // / (-discountedCapitalCosts);
		                                        
		                    
		                    double projectReturnOnInvestment = discountedOpProfit / (-discountedCapitalCosts);
		                    double projectReturnOnEquity = projectReturnOnInvestment / (1 - agent.getDebtRatioOfInvestments());
		
		                    double partWorthUtilityReturn = determineUtilityReturn(projectReturnOnEquity, agent);
		                    double partWorthUtilityTechnology = determineUtilityTechnology(technology, agent);
		                    double partWorthUtilityCountry = determineUtilityCountry(market, agent);
		                    
		                    // TODO MM
		                    double partWorthUtilityPolicy = 0; 
		                    
		                    double totalUtility = partWorthUtilityReturn + partWorthUtilityTechnology + partWorthUtilityPolicy + partWorthUtilityCountry; 
		                    double totalRandomUtility = totalUtility * (1 + ThreadLocalRandom.current().nextDouble(-1 * getRandomUtilityBound(), getRandomUtilityBound()));
		
		                    logger.log(Level.INFO, 
		                    		"Agent " + agent + " found in market " + market.getName() + "\n"
		                    				+ " for technology " + technology + "\n "
		                            		+ " the utility for technology " + technology + " to be " + partWorthUtilityTechnology + "\n"
		                            		+ " the ROI to be " + projectReturnOnInvestment + "\n" 
		                            		+ " the ROE to be " + projectReturnOnEquity + "\n" 
		                            		+ " the utility for return " + technology + " to be " + partWorthUtilityReturn + "\n"
		                            		+ " the utility for market " + market + " to be " + partWorthUtilityCountry + "\n"
		                            		+ " the total utility to be " + totalUtility + "\n"
		                            		+ " the total random utility to be " + totalRandomUtility + "\n");
		                    	                    	                    
		                    if(totalRandomUtility > highestValue) {
		                    	highestValue = totalRandomUtility;
		                    	bestPlant = plant;
		                    	bestPlantMarket = market;
		                    }
			                    
		                }
		
		            }
		        
		        }
	        }
        }

        if (bestPlant != null) {
            logger.log(Level.INFO, "{0} invested in technology {1} in market {2} at tick {3}", new Object[]{agent, bestPlant.getTechnology(), bestPlantMarket.getName(), getCurrentTick()});
//            PowerPlant plant = getReps().createAndSpecifyTemporaryPowerPlant(getCurrentTick(), agent, getNodeForZone(market.getZone()), bestTechnology);
            getReps().createPowerPlantFromPlant(bestPlant);
            //TODO recalculate fuelmix in other investment roles!
            Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
            for (Substance fuel : bestPlant.getTechnology().getFuels()) {
                myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
            }
            
            // TODO need to change market variable here, since loop ends above, e.g. introduce bestPlantMarket
            bestPlant.setFuelMix(calculateFuelMix(bestPlant, myFuelPrices, expectedCO2Price.get(bestPlantMarket)));

            PowerPlantManufacturer manufacturer = getReps().powerPlantManufacturer;
            BigBank bigbank = getReps().bigBank;

            double investmentCostPayedByEquity = bestPlant.getActualInvestedCapital() * (1 - agent.getDebtRatioOfInvestments());
            double investmentCostPayedByDebt = bestPlant.getActualInvestedCapital() * agent.getDebtRatioOfInvestments();
            double downPayment = investmentCostPayedByEquity;
            createSpreadOutDownPayments(agent, manufacturer, downPayment, bestPlant);

            double amount = determineLoanAnnuities(investmentCostPayedByDebt, bestPlant.getTechnology().getDepreciationTime(),
                    agent.getLoanInterestRate());
            // logger.warn("Loan amount is: " + amount);
            Loan loan = getReps().createLoan(agent, bigbank, amount, bestPlant.getTechnology().getDepreciationTime(),
                    getCurrentTick(), bestPlant);
            // Create the loan
            bestPlant.createOrUpdateLoan(loan);

        } else {
            // logger.warn("{} found no suitable technology anymore to invest in at tick "
            // + getCurrentTick(), agent);
            // agent will not participate in the next round of investment if
            // he does not invest now
            setNotWillingToInvest(agent);
        }
        logger.info("Investment done for " + agent);
    }
    
     /**
     * Determines part-worth utility from return on equity number and EnergyProducers return sensitivity. 
     * A linear extrapolation is used based on the difference between two return values. This
     * difference is assumed to show the sensitivity of investors to changes in returns.
     * 
     * Example:
     * 	assume return 6% has part-worth utility of 0 and 7% of 40, i.e. 40u per 1%
     *  then with 0.1 ROE -> (0.1-0.06) * 40/0.01 = 160
     *
     * @param projectReturnOnEquity 	ROE of the project
     * @param agent						EnergyProducer
     * @return double utility value
     *
     */
    private double determineUtilityReturn(double projectReturnOnEquity, EnergyProducer agent) {

    	// TODO MM all based on 6% -> could be more generic hence.
    	// TODO MM andwhat if 5-6% is different from 6-7% (which it will be probably) ?

    	double investorReturnUtilitySensitivity = agent.getUtilityReturn().get("7%") - agent.getUtilityReturn().get("6%");  
    	double utility = (projectReturnOnEquity - 0.06) * investorReturnUtilitySensitivity / 0.01;
        
        // TODO MM idea 2: weight 
        
        
        return utility;
    }
     
    /**
     * Determines part-worth utility from technology
     * @param agent 
     *
     * @param projectReturnOnEquity
     * @return double utility value
     */
    private double determineUtilityTechnology(PowerGeneratingTechnology technology, EnergyProducer agent) {
        
        double utility = agent.getUtilityTechnology().get(technology.getName());
    	
        return utility;
    }
    
    /**
     * Determines part-worth utility from market. The own country/market is equal to investorMarket
     * @param agent 
     *
     * @param projectReturnOnEquity
     * @return double utility value
     */
    private double determineUtilityCountry(ElectricitySpotMarket market, EnergyProducer agent) {
        
    	double utility;
    	
    	if(agent.getInvestorMarket() == market) {
    		// TODO generalize
    		utility = agent.getUtilityCountry().get("Own country");
    		
    	} else {
    		utility = agent.getUtilityCountry().get("Known country");
    		
    	}
    	// TODO unkonwn country? Not implemetable at all?
    	
    	
        return utility;
    }


	public double getRandomUtilityBound() {
		return randomUtilityBound;
	}

	public void setRandomUtilityBound(double randomUtilityBound) {
		this.randomUtilityBound = randomUtilityBound;
	}

}
