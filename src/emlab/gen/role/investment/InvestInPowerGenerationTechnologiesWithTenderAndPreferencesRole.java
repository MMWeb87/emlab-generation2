/*******************************************************************************
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
 ******************************************************************************/
package emlab.gen.role.investment;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.apache.commons.math.stat.regression.SimpleRegression;

import emlab.gen.domain.agent.EMLabModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.policy.EmpiricalMappingFunctionParameter;
import emlab.gen.domain.policy.renewablesupport.BaseCostFip;
import emlab.gen.domain.policy.renewablesupport.ForecastingInformationReport;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;
import emlab.gen.trend.TimeSeriesCSVReader;

/**
 * {@link EnergyProducer}s decide to invest in new {@link PowerPlant}
 *
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * @author JCRichstein
 */

public class InvestInPowerGenerationTechnologiesWithTenderAndPreferencesRole<T extends EnergyProducer> extends AbstractInvestInPowerGenerationTechnologiesWithTenderRole<T> implements Role<T> {

    //Map<ElectricitySpotMarket, MarketInformation> marketInfoMap = new HashMap<ElectricitySpotMarket, MarketInformation>();

    public InvestInPowerGenerationTechnologiesWithTenderAndPreferencesRole(Schedule schedule) {
    	
    	super(schedule);    	
        
    }
    
    protected RenewableSupportFipScheme scheme = null;

    private double randomUtilityBound = 0;


    @Override
    public void act(T agent) {
    	
    	initEvaluationForEnergyProducer(agent, agent.getInvestorMarket());

        PowerPlant bestPlant = null;
        //ForecastingInformationReport fReport = null;
        double highestValue = Double.MIN_VALUE;

        for (PowerGeneratingTechnology technology : getReps().powerGeneratingTechnologies) {
        	
        	// Agent only considers investing in technologies for which there are preferences
        	if(agent.getUtilityTechnology().containsKey(technology.getName())) {
        		
	            EMLabModel model = getReps().emlabModel;
	        	
	            if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment()) {
	                continue;
	            }

	            for (PowerGridNode node : findPossibleInstallationNodes(technology)) {
	            	
	                PowerPlant plant = createPowerPlant(technology, node);
	                scheme = getRenewableSupportFipScheme(technology);
	                FutureCapacityExpectationWithScheme futureCapacityExpectation = new FutureCapacityExpectationWithScheme(technology, plant, node);
	                
	                // See check() for custom rules
	                if(futureCapacityExpectation.isViableInvestment()) {
	                                    	
	                	FutureFinancialExpectationWithScheme financialExpectation = new FutureFinancialExpectationWithScheme(plant);
	                	
	                    if (financialExpectation.plantHasRequiredRunningHours()) {
	                    	
	                        financialExpectation.calculateDiscountedValues(); // Without scheme
	                        financialExpectation.calculateExpectedBaseCostWithScheme();
	                    	double projectValue = financialExpectation.getProjectValue();
	                    	
	                        logger.fine("For plant before subsidy:" + plant.getName() + "ProjectValue " + projectValue);                   
	                        
	                        // Phasing out support as indicated in scenario
	                        Boolean supportSchemeAvailable = false;
	                        if (scheme != null && financialExpectation.getExpectedBaseCost() > 0
	                                && (scheme.getPowerGeneratingTechnologiesEligible().contains(technology))) {
	                        	
	                            if(scheme.getFutureSchemePhaseoutTime().containsKey(technology)) {
	    	                        if(schedule.getCurrentTick() + plant.getActualPermittime() + plant.getActualLeadtime() < scheme.getFutureSchemePhaseoutTime().get(technology)) {
	    	                        	
	    	                        	supportSchemeAvailable = true;
	    	                        	
			                        	financialExpectation.calculateProjectValueWithScheme();
			                        	double oldValue = projectValue;
			                        	projectValue  = financialExpectation.getProjectValueWithScheme();
			                        	logger.finer("projectValue old: "+ oldValue + " and projectValue new: "+ projectValue);
			
				                        
			                        } else {
			                        	logger.finer("No support, since phased out");
			                        }
	                            }
	                        }
	                        
	                        if (projectValue > 0) {
	                        	
                        		double projectDiscountedReturnOnInvestment = financialExpectation.calculateDiscountedReturnOnInvestment(projectValue);			                    
                        		logger.log(Level.FINER, "Agent " + agent + " finds the discounted per lifetime year ROI for " + technology + " to be " + projectDiscountedReturnOnInvestment);
                        		double projectDiscountedReturnOnEquity = projectDiscountedReturnOnInvestment / (1 - agent.getDebtRatioOfInvestments());
                        		logger.log(Level.FINER, "Agent " + agent + " finds the discounted per lifetime year  ROE (debt: " + agent.getDebtRatioOfInvestments() +") for " + technology + " to be " + projectDiscountedReturnOnEquity);

                        		double mappedProjectDiscountedReturnOnEquity = mapReturnToEmpiricalRange(projectDiscountedReturnOnEquity, technology, this.getMarket());
                        		double totalUtility = 0; // not relevant if empirical deactivated
                        		
	                        	if(model.isEmpiricalPreferenceActive()) {

	                        		logger.log(Level.FINE, "The project value " + projectValue + " for " + technology + " and " + this.getMarket().getName() + " is positive.");

	                        		double partWorthUtilityReturn = determineUtilityReturn(mappedProjectDiscountedReturnOnEquity, agent);
	                        		double partWorthUtilityTechnology = determineUtilityTechnology(technology, agent);
	                        		double partWorthUtilityCountry = determineUtilityCountry(this.getMarket(), agent);
	                        		double partWorthUtilityPolicy = determineUtilityPolicy(supportSchemeAvailable); 

	                        		totalUtility = partWorthUtilityReturn + partWorthUtilityTechnology + partWorthUtilityPolicy + partWorthUtilityCountry; 
	                        		double totalRandomUtility = totalUtility * (1 + ThreadLocalRandom.current().nextDouble(-1 * getRandomUtilityBound(), getRandomUtilityBound()));

	                        		logger.log(Level.FINER, "Agent " + agent + " finds " + partWorthUtilityTechnology + " as part-worth utility for technology " + technology);
	                        		logger.log(Level.FINER, "Agent " + agent + " finds " + partWorthUtilityReturn + " as part-worth utility for ROE " + projectDiscountedReturnOnEquity);
	                        		logger.log(Level.FINER, "Agent " + agent + " finds " + partWorthUtilityCountry + " as part-worth utility for market " + this.getMarket());
	                        		logger.log(Level.FINER, "Agent " + agent + " finds " + totalUtility + " as total utility");
	                        		logger.log(Level.FINER, "Agent " + agent + " finds " + totalRandomUtility + " as total RANDOM utility");

	                        		if(totalRandomUtility > highestValue) {
	                        			highestValue = totalRandomUtility;
	                        			bestPlant = plant;
	                        		} 
	                        	
	                        	} else {

	                        		// empirical preference is deactivated -> evaluate like normal 
	                        		if (projectValue / plant.getActualNominalCapacity() > highestValue) {
	                        			highestValue = projectValue / plant.getActualNominalCapacity();
	                        			bestPlant = plant;
	                        		}
	                        	}
	                        	
                        		// Reporter
                        		FinancialExpectationReport report = new FinancialExpectationReport();

                        		report.schedule = schedule;
                        		report.setMarket(agent.getInvestorMarket());
                        		report.setTime(schedule.getCurrentTick()); 
                        		report.setAgent(agent);
                        		report.setTechnology(technology);
                        		report.setPlant(plant);
                        		report.setNode(node);
                        		report.setInvestmentRound(this.getCurrentTnvestmentRound());

                        		report.setProjectReturnOnInvestment(projectDiscountedReturnOnInvestment);
                        		report.setProjectReturnOnEquity(projectDiscountedReturnOnEquity);
                        		report.setMappedProjectReturnOnEquity(mappedProjectDiscountedReturnOnEquity);

                        		report.setDebtRatioOfInvestments(agent.getDebtRatioOfInvestments());
                        		report.setDiscountedCapitalCosts(financialExpectation.getDiscountedCapitalCosts());
                        		report.setDiscountedOperatingCost(financialExpectation.getDiscountedOperatingCost());
                        		report.setDiscountedOperatingProfit(financialExpectation.getDiscountedOperatingProfit());

                        		report.setExpectedGeneration(financialExpectation.getExpectedGeneration());
                        		report.setExpectedGrossProfit(financialExpectation.getExpectedGrossProfit());
                        		report.setExpectedMarginalCost(financialExpectation.getExpectedMarginalCost());
                        		report.setExpectedOperatingCost(financialExpectation.getExpectedOperatingCost());
                        		report.setExpectedOperatingRevenue(financialExpectation.getExpectedOperatingRevenue());

                        		report.setProjectCost(financialExpectation.getProjectCost());
                        		report.setProjectValue(financialExpectation.getProjectValue());

                        		report.setRunningHours(financialExpectation.getRunningHours());
                        		report.setWacc(financialExpectation.getWacc());
                        		report.setTotalUtility(totalUtility);
                        		
                        		
                        		report.setLCOE(financialExpectation.getProjectCost() / financialExpectation.getDiscountedGeneration());


                        		getReps().financialExpectationReports.add(report);

	                        } else {
	                        	logger.log(Level.FINER, "Because the project value is negative, " + agent + " does not consider this investment option.");
	                        }
	
	                    }
	
	                }
	                plant = null;
	
	            }
            
        	} // only if prefs for techs
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
    protected double determineUtilityReturn(double projectReturnOnEquity, EnergyProducer agent) {


    	// Here, I extrapolate linearly based on the slope between 6 and 7% return -> specific for data of Melliger, Lilliestam (2020)
    	// TODO MM andwhat if 5-6% is different from 6-7% (which it will be probably) ? ->  Maybe mean of slopes
    	double utilityReturnExtrapolationSlope = (agent.getUtilityReturn().get("7%") - agent.getUtilityReturn().get("6%")) / 0.01;
    	double utilityReturnExtrapolationIntercept = agent.getUtilityReturn().get("6%") - utilityReturnExtrapolationSlope * 0.06;

    	double utility = utilityReturnExtrapolationIntercept + utilityReturnExtrapolationSlope *  projectReturnOnEquity;        	
    
        return utility;
    }
    
    /**
     * This function transforms a return value for a specific country or technology to
     * a value that is more realistic (based on empirical paper by Melliger et al.).
     * See R script for actual function.
     * 
     * To get the EmpiricalMappingFunctionParameter, the model should first be run with the linear method.
     *
     * @param projectReturnOnEquity 	ROE of the project
     * @param agent						EnergyProducer
     * @return double utility value
     *
     */
    protected double mapReturnToEmpiricalRange(double projectReturnOnEquity, PowerGeneratingTechnology technology, ElectricitySpotMarket market) {
    	
    	double finalReturnForCalculation = 0;
    	
    	if(this.schedule.preRunForMapping) {
    		finalReturnForCalculation = projectReturnOnEquity;       	

    	} else {
    		 		
    		EmpiricalMappingFunctionParameter empiricalMappingParameter = getReps().findEmpiricalMappingParameters(market, technology);
    		
    		double intercept = empiricalMappingParameter.getIntercept();
    		double slope = empiricalMappingParameter.getSlope();
    		double upper_quartile = empiricalMappingParameter.getModelledRoeMax();
    		double lower_quartile = empiricalMappingParameter.getModelledRoeMin();
    		
    		double capped_return = projectReturnOnEquity; 
        	if(projectReturnOnEquity >= upper_quartile) {    		
        		capped_return = upper_quartile;
        	} else if(projectReturnOnEquity <= lower_quartile) {
        		capped_return = lower_quartile;
        	}
        	
        	finalReturnForCalculation = intercept + slope * capped_return;
        	
    	}


    	return finalReturnForCalculation;        	
    
    }
     
    /**
     * Determines part-worth utility from technology
     * @param agent 
     *
     * @param projectReturnOnEquity
     * @return double utility value
     */
    protected double determineUtilityTechnology(PowerGeneratingTechnology technology, EnergyProducer agent) {
        
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
    protected double determineUtilityCountry(ElectricitySpotMarket market, EnergyProducer agent) {
        
    	double utility;
    	
    	if(agent.getInvestorMarket() == market) {
    		// TODO generalize
    		utility = agent.getUtilityCountry().get("Own country");
    		
    	} else {
    		utility = agent.getUtilityCountry().get("Known country");
    		
    	}
    	// TODO unkonwn country? Not implementable at all?
    	
    	
        return utility;
    }
    
    /**
     * Should probably be specific and check if support is still available at this tick
     * @return
     */
    protected double determineUtilityPolicy(Boolean supportSchemeAvailable) {
    	
    	double utility;
    	
    	utility = agent.getUtilityPolicy().get("None");
    	
    	// TODO: in the auction case, the investment decisions is not performed here, and hence utility is only None.
    	
//    	if(getReps().emlabModel.isFeedInPremiumImplemented() 
//    			&& getReps().emlabModel.isRenewableTenderSchemeImplemented()
//    			&& supportSchemeAvailable) {
//    		
//    			utility = agent.getUtilityPolicy().get("Auction");	
//    		
//    	}
    	
    	return utility;
    }


    protected double getRandomUtilityBound() {
		return randomUtilityBound;
	}

    public void setRandomUtilityBound(double randomUtilityBound) {
		this.randomUtilityBound = randomUtilityBound;
	}  

}