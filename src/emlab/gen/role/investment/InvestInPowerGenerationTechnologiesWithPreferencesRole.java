/** *****************************************************************************
 * Copyright 2014 the original author or authors.
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

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import emlab.gen.domain.agent.EMLabModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;

/**
 * {@link EnergyProducer}s decide to invest in new {@link PowerPlant}
 *
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 * <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas Chmieliauskas</a>
 * @author JCRichstein
 */
public class InvestInPowerGenerationTechnologiesWithPreferencesRole<T extends EnergyProducer> extends AbstractInvestInPowerGenerationTechnologiesRole<T> implements Role<T> {

    public InvestInPowerGenerationTechnologiesWithPreferencesRole(Schedule schedule) {
        super(schedule);
    }
    
    /**
     * Randomness
     */
    private double randomUtilityBound = 0;


    @Override
    public void act(T agent) {
    	
    	setUseFundamentalCO2Forecast(true);
    	initEvaluationForEnergyProducer(agent, agent.getInvestorMarket());

        double highestValue = Double.MIN_VALUE;
        PowerPlant bestPlant = null;

        for (PowerGeneratingTechnology technology : getReps().powerGeneratingTechnologies) {
        	
        	// Agent only considers investing in technologies for which there are preferences
        	if(agent.getUtilityTechnology().containsKey(technology.getName())) {
	
	            EMLabModel model = getReps().emlabModel;
	
	            if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment()) {
	                continue;
	            }
	
	            for (PowerGridNode node : findPossibleInstallationNodes(technology)) {
	
	                PowerPlant plant = createPowerPlant(technology, node);
	                FutureCapacityExpectation futureCapacityExpectation = new FutureCapacityExpectation(technology, plant, node);
	
	                if(!futureCapacityExpectation.isViableInvestment()) {
	
	                	setFuelMixForPlant(technology, plant);
	                	
	                	FutureFinancialExpectation financialExpectation = new FutureFinancialExpectation(plant);                    
	                    
	                    if (financialExpectation.plantHasRequiredRunningHours()) {
	                    	
	                    	financialExpectation.calculateDiscountedValues();
	                    	
	                    	double projectValue = financialExpectation.calculateProjectValue();
	
	                        /*
	                         * Divide by capacity, in order not to favour large power plants (which have the single largest NPV
	                         */
	                    	
	                        // Assuming investor would not want to make a loss
		                    if (projectValue > 0) {
		                    	
		                    	// TODO implement
		                    	logger.log(Level.INFO, "Because the project value is positive (gain), " + agent + " considers investment options and evaluates other investment attributes.");
		                    
		                    	// TODO in auction module there is a revenue component
			                    double projectReturnOnInvestment = (financialExpectation.getDiscountedOperatingProfit() + financialExpectation.getDiscountedCapitalCosts()) 
			                    		/ (-financialExpectation.getDiscountedCapitalCosts());
			                    double projectReturnOnEquity = projectReturnOnInvestment / (1 - agent.getDebtRatioOfInvestments());
			
			                    double partWorthUtilityReturn = determineUtilityReturn(projectReturnOnEquity, agent);
			                    double partWorthUtilityTechnology = determineUtilityTechnology(technology, agent);
			                    double partWorthUtilityCountry = determineUtilityCountry(this.getMarket(), agent);
			                    
			                    // TODO MM
			                    double partWorthUtilityPolicy = 0; 
			                    
			                    double totalUtility = partWorthUtilityReturn + partWorthUtilityTechnology + partWorthUtilityPolicy + partWorthUtilityCountry; 
			                    double totalRandomUtility = totalUtility * (1 + ThreadLocalRandom.current().nextDouble(-1 * getRandomUtilityBound(), getRandomUtilityBound()));
			
			                    logger.log(Level.INFO, 
			                    		"Agent " + agent + " considers in market " + this.getMarket().getName() + " for technology " + technology + "\n "
			                            		+ " the part-worth utility for technology " + technology + " to be " + partWorthUtilityTechnology + "\n"
			                            		+ " the part-worth utility for return " + technology + " to be " + partWorthUtilityReturn + "\n"
			                            		+ " the part-worth utility for market " + this.getMarket() + " to be " + partWorthUtilityCountry + "\n"
			                            		+ " the part-worth utility for policy " + this.getMarket() + " to be " + partWorthUtilityPolicy + "\n"
	
			                            		+ " the ROI to be " + projectReturnOnInvestment + "\n" 
			                            		+ " the ROE to be " + projectReturnOnEquity + "\n" 
	
			                            		+ " the total utility to be " + totalUtility + "\n"
			                            		+ " the total random utility to be " + totalRandomUtility + "\n");
			                    	                    	                    
			                    if(totalRandomUtility > highestValue) {
			                    	highestValue = totalRandomUtility;
			                    	bestPlant = plant;
			                    } 
		                    
		                    } else {
		                    	logger.log(Level.INFO, "Because the project value is negative (loss), " + agent + " does not consider this investment option.");
		                    }
		                    
		                    
	                        if (projectValue > 0 && projectValue / plant.getActualNominalCapacity() > highestValue) {
	                        	
	                            highestValue = projectValue / plant.getActualNominalCapacity();
	                            bestPlant = plant;
	
	                        }
	                    }
	                }
                }
            }
        }
        
        decideToInvestInPlant(bestPlant);

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
    
    
// TODO This is also different. Especially the for (TargetInvestor targetInvestor) part..
// check if this also works wihout technolgy target. getReps().findAllPowerGeneratingTechnologyTargetsByMarket(market)?

//private class MarketInformation {
//
//      Map<Segment, Double> expectedElectricityPricesPerSegment;
//      double maxExpectedLoad = 0d;
//      Map<PowerPlant, Double> meritOrder;
//      double capacitySum;
//
//      MarketInformation(ElectricitySpotMarket market, Map<ElectricitySpotMarket, Double> expectedDemand, Map<Substance, Double> fuelPrices, double co2price, long time) {
//          // determine expected power prices
//          expectedElectricityPricesPerSegment = new HashMap<Segment, Double>();
//          Map<PowerPlant, Double> marginalCostMap = new HashMap<PowerPlant, Double>();
//          capacitySum = 0d;
//
//          // get merit order for this market
//          for (PowerPlant plant : getReps().findExpectedOperationalPowerPlantsInMarket(market, time)) {
//
//              double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
//              marginalCostMap.put(plant, plantMarginalCost);
//              capacitySum += plant.getActualNominalCapacity();
//          }
//
//          //get difference between technology target and expected operational capacity
//          // TODO NOT IMPLEMENTED??
//          for (TargetInvestor targetInvestor : getReps().findAllTargetInvestorsByMarket(market)) {
//              if (!(targetInvestor instanceof StochasticTargetInvestor)) {
//                  for (PowerGeneratingTechnologyTarget pggt : targetInvestor.getPowerGenerationTechnologyTargets()) {
//                      double expectedTechnologyCapacity = getReps()
//                              .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market,
//                                      pggt.getPowerGeneratingTechnology(), time);
//                      double targetDifference = pggt.getTrend().getValue(time) - expectedTechnologyCapacity;
//                      if (targetDifference > 0) {
//                          PowerPlant plant = getReps().createAndSpecifyTemporaryPowerPlant(getCurrentTick(), new EnergyProducer(),
//                                  getReps().findFirstPowerGridNodeByElectricitySpotMarket(market),
//                                  pggt.getPowerGeneratingTechnology());
//                          plant.setActualNominalCapacity(targetDifference);
//                          double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
//                          marginalCostMap.put(plant, plantMarginalCost);
//                          capacitySum += targetDifference;
//                      }
//                  }
//              } else {
//                  for (PowerGeneratingTechnologyTarget pggt : targetInvestor.getPowerGenerationTechnologyTargets()) {
//                      double expectedTechnologyCapacity = getReps()
//                              .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market,
//                                      pggt.getPowerGeneratingTechnology(), time);
//                      double expectedTechnologyAddition = 0;
//                      long contructionTime = getCurrentTick()
//                              + pggt.getPowerGeneratingTechnology().getExpectedLeadtime()
//                              + pggt.getPowerGeneratingTechnology().getExpectedPermittime();
//                      for (long investmentTimeStep = contructionTime + 1; investmentTimeStep <= time; investmentTimeStep = investmentTimeStep + 1) {
//                          expectedTechnologyAddition += (pggt.getTrend().getValue(investmentTimeStep) - pggt
//                                  .getTrend().getValue(investmentTimeStep - 1));
//                      }
//                      if (expectedTechnologyAddition > 0) {
//                          PowerPlant plant = getReps().createAndSpecifyTemporaryPowerPlant(getCurrentTick(), new EnergyProducer(),
//                                  getReps().findFirstPowerGridNodeByElectricitySpotMarket(market),
//                                  pggt.getPowerGeneratingTechnology());
//                          plant.setActualNominalCapacity(expectedTechnologyAddition);
//                          double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
//                          marginalCostMap.put(plant, plantMarginalCost);
//                          capacitySum += expectedTechnologyAddition;
//                      }
//                  }
//              }
//
//          }
//
//          MapValueComparator comp = new MapValueComparator(marginalCostMap);
//          meritOrder = new TreeMap<PowerPlant, Double>(comp);
//          meritOrder.putAll(marginalCostMap);
//
//          long numberOfSegments = getReps().segments.size();
//
//          double demandFactor = expectedDemand.get(market).doubleValue();
//
//          // find expected prices per segment given merit order
//          for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
//
//              double expectedSegmentLoad = segmentLoad.getBaseLoad() * demandFactor;
//
//              if (expectedSegmentLoad > maxExpectedLoad) {
//                  maxExpectedLoad = expectedSegmentLoad;
//              }
//
//              double segmentSupply = 0d;
//              double segmentPrice = 0d;
//              double totalCapacityAvailable = 0d;
//
//              for (Entry<PowerPlant, Double> plantCost : meritOrder.entrySet()) {
//                  PowerPlant plant = plantCost.getKey();
//                  double plantCapacity = 0d;
//                  // Determine available capacity in the future in this
//                  // segment
//                  plantCapacity = plant.getExpectedAvailableCapacity(time, segmentLoad.getSegment(), numberOfSegments);
//                  totalCapacityAvailable += plantCapacity;
//                  // logger.warn("Capacity of plant " + plant.toString() +
//                  // " is " +
//                  // plantCapacity/plant.getActualNominalCapacity());
//                  if (segmentSupply < expectedSegmentLoad) {
//                      segmentSupply += plantCapacity;
//                      segmentPrice = plantCost.getValue();
//                  }
//
//              }
//
//              // logger.warn("Segment " +
//              // segmentLoad.getSegment().getSegmentID() + " supply equals " +
//              // segmentSupply + " and segment demand equals " +
//              // expectedSegmentLoad);
//              // Find strategic reserve operator for the market.
//              double reservePrice = 0;
//              double reserveVolume = 0;
//              for (StrategicReserveOperator operator : getReps().strategicReserveOperators) {
//                  ElectricitySpotMarket market1 = getReps().findElectricitySpotMarketForZone(operator
//                          .getZone());
//                  if (market.equals(market1)) {
//                      reservePrice = operator.getReservePriceSR();
//                      reserveVolume = operator.getReserveVolume();
//                  }
//              }
//
//              if (segmentSupply >= expectedSegmentLoad
//                      && ((totalCapacityAvailable - expectedSegmentLoad) <= (reserveVolume))) {
//                  expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), reservePrice);
//                  // logger.warn("Price: "+
//                  // expectedElectricityPricesPerSegment);
//              } else if (segmentSupply >= expectedSegmentLoad
//                      && ((totalCapacityAvailable - expectedSegmentLoad) > (reserveVolume))) {
//                  expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), segmentPrice);
//                  // logger.warn("Price: "+
//                  // expectedElectricityPricesPerSegment);
//              } else {
//                  expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), market.getValueOfLostLoad());
//              }
//
//          }
//      }
//  }    

}
