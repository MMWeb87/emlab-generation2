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

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.math.stat.regression.SimpleRegression;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.gis.Zone;
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
 * @author marcmel
 */

public abstract class AbstractInvestInPowerGenerationTechnologiesWithTenderRole<T extends EnergyProducer> extends AbstractInvestInPowerGenerationTechnologiesRole<T> implements Role<T> {

    public AbstractInvestInPowerGenerationTechnologiesWithTenderRole(Schedule schedule) {
    	
    	super(schedule);    	
       
    }
    
    protected RenewableSupportFipScheme scheme = null;

    @Override
    public void act(T agent) {
    	
    	initEvaluationForEnergyProducer(agent, agent.getInvestorMarket());

        PowerPlant bestPlant = null;
        double highestValue = Double.MIN_VALUE;
        
        // Limit the potentially available technologies for some agents
        ArrayList<PowerGeneratingTechnology> technologies; 
        if(agent.getPotentialPowerGeneratingTechnologies() != null) {
        	technologies = agent.getPotentialPowerGeneratingTechnologies();
        } else {
        	technologies = getReps().powerGeneratingTechnologies;
        }

        for (PowerGeneratingTechnology technology : technologies) {

            for (PowerGridNode node : findPossibleInstallationNodes(technology)) {
            	
                PowerPlant plant = createPowerPlant(technology, node);
                scheme = getRenewableSupportFipScheme(technology); // TODO check if market and grid work properly
                FutureCapacityExpectationWithScheme futureCapacityExpectation = new FutureCapacityExpectationWithScheme(technology, plant, node);
                
                // See check for custom rules
                if(futureCapacityExpectation.isViableInvestment()) {
                                    	
                	FutureFinancialExpectationWithScheme financialExpectation = new FutureFinancialExpectationWithScheme(plant);
                	
                    if (financialExpectation.plantHasRequiredRunningHours()) {
                    	
                        financialExpectation.calculateDiscountedValues(); // Without scheme
                        financialExpectation.calculateExpectedBaseCostWithScheme();
                    	double projectValue = financialExpectation.getProjectValue();
                    	
                    	logger.fine("For plant before subsidy:" + plant.getName() +
                    			"ProjectValue " + projectValue);


                    	if (scheme != null && financialExpectation.getExpectedBaseCost() > 0
                    			&& (scheme.getPowerGeneratingTechnologiesEligible().contains(technology))) {

                    		if(scheme.getFutureSchemePhaseoutTime().containsKey(technology)) {

                    			if(schedule.getCurrentTick() + plant.getActualPermittime() + plant.getActualLeadtime() < scheme.getFutureSchemePhaseoutTime().get(technology)) {


                    				financialExpectation.calculateProjectValueWithScheme();
                    				double oldValue = projectValue;
                    				projectValue  = financialExpectation.getProjectValueWithScheme();
                    				logger.finer("projectValue old: "+ oldValue + " and projectValue new: "+ projectValue);



                    			} else {
                    				logger.finer("No support, since phased out");
                    			}
                    		}
                    	}

                        if (projectValue > 0 && projectValue / plant.getActualNominalCapacity() > highestValue) {
                            highestValue = projectValue / plant.getActualNominalCapacity();
                            bestPlant = plant;
                            bestPlant.setInvestmentOrigin(6);
                            
                        }

                    }

                }
                plant = null;

            }
        }
        decideToInvestInPlant(bestPlant);

    }
    
   

    
    /**
     * Checks if a technology specific FipScheme exists, and returns it 
     * @param market
     * @param technology
     * @
     * @return technology specific FipScheme  
     */
    protected RenewableSupportFipScheme getRenewableSupportFipScheme(PowerGeneratingTechnology technology) {
    	    	
        Set<RenewableSupportFipScheme> schemeSet = getReps().findSchemesGivenZone(getMarket().getZone()); //TODO correct?
        logger.log(Level.FINER, "scheme Set is " + schemeSet);

        if ((schemeSet.size() >= 1) && (!schemeSet.isEmpty())) {

            for (RenewableSupportFipScheme i : schemeSet) {
            	                
            	// TODO MM ADJUST IF LOOP FOR LOCATION SPECIFICITY WHEN
                // ENABLED - simply add a logical AND toi the iff
                // statemnt below to filter for location

            	Zone zoneScheme = i.getRegulator().getZone();
            	
                if (i.getPowerGeneratingTechnologiesEligible().contains(technology)
                        && getMarket().getZone().getName() == zoneScheme.getName()) {
                    scheme = i;
                    logger.log(Level.FINER, "scheme is " + i.getName());
                    break;
                } else {
                    scheme = null;
                }
            }

        }
        return scheme;
    }
    
    
    
    public class FutureCapacityExpectationWithScheme extends FutureCapacityExpectation {
	   
		
		public FutureCapacityExpectationWithScheme(PowerGeneratingTechnology technology, PowerPlant plant, PowerGridNode node){
		    
			super(technology, plant, node);
		
		}
			
		      
		@Override
		public void calculateNodeLimit() {
				
			super.calculateNodeLimit();
		
		    if (scheme != null) {
		        double technologyPotential;
		        
                TimeSeriesCSVReader technologyPotentialTS = getReps()
                        .findTechnologySpecificRenewablePotentialLimitTimeSeriesByRegulator(scheme.getRegulator(),
                                technology.getName());
                 
                if(technologyPotentialTS != null) {
                	technologyPotential = technologyPotentialTS.getValue(futureTimePoint);
                } else {
                	technologyPotential = Double.MAX_VALUE;
                	logger.log(Level.WARNING, "No technologyPotentialLimit set for " + technology);
                }
		
		        pgtNodeLimit = technologyPotential / plant.getAnnualFullLoadHours();
		        
		        logger.log(Level.FINER, "For technology " + technology.getName() + "plant annual full load hours " + plant.getAnnualFullLoadHours());
		        logger.log(Level.FINER, "technology potential in MW " + pgtNodeLimit);
		        logger.log(Level.FINER, "technology potential " + technologyPotential);
		
		    }
		    
		}
		
        
        
        @Override
        public void check() {
        	
        	double inviableReason = 0;


        	// Limiting rule 1: nodeLimit
        	if ((expectedInstalledCapacityOfTechnologyInNode + plant.getActualNominalCapacity()) > pgtNodeLimit) {
        		logger.log(Level.INFO, agent + " will not invest in " + technology.getName() + " because of node limit: " + pgtNodeLimit);
        		inviableReason = 1;
        		
        		

        	// Limiting rule 2: too much by own agent
        	} else if (expectedOwnedCapacityInMarketOfThisTechnology > expectedOwnedTotalCapacityInMarket
        			* technology.getMaximumInstalledCapacityFractionPerAgent()) {
        		logger.log(Level.INFO, 
        				agent + " will not invest in " + technology.getName() + " because there's too much capacity planned by him");
        		inviableReason = 2;

        	// Limiting rule 3: avoid not being able to pay downpayments
        	} else if (plant.getActualInvestedCapital() * (1 - agent.getDebtRatioOfInvestments()) > agent
        			.getDownpaymentFractionOfCash() * agent.getCash()) {
        		logger.log(Level.INFO, agent + " will not invest in " + technology.getName() + " as he does not have enough money for downpayment");
        		inviableReason = 3;
        	} 

        	// these two rules were removed for Iychettira (2017) because capacity (GW) was not a good measure to limit renewable generation, especially when targets were in terms of energy (GWh). 
        	// For instance, limiting solar capacity (in GW) to 20% of peak demand (say, 70 GW for Germany) would limit investments to 14GW of solar capacity, 
        	// which provides too little energy from solar!

        	//          if ((expectedInstalledCapacityOfTechnology + plant.getActualNominalCapacity())
        	//          / (marketInformation.maxExpectedLoad + plant.getActualNominalCapacity()) > technology
        	//          .getMaximumInstalledCapacityFractionInCountry()) {
        	//  	
        	//      logger.log(Level.FINE, 
        	//      		agent + " will not invest in {} technology because there's too much of this type in the market", technology);
        	//  
        	//  } else  
        	//        	            
        	//} else if (capacityInPipelineInMarket > 0.2 * marketInformation.maxExpectedLoad) {
        	//	logger.log(Level.FINE, "Not investing because more than 20% of demand in pipeline.");
        	//
        	//
        	//} else if ((capacityOfTechnologyInPipeline > 2.0 * operationalCapacityOfTechnology)
        	//        && capacityOfTechnologyInPipeline > 9000) { // TODO: reflects that you cannot expand a technology out of zero.
        	//	logger.log(Level.FINE, agent +" will not invest in {} technology because there's too much capacity in the pipeline", technology);


        	else {

        		// Passes all hard limits in terms of capacity
        		logger.log(Level.INFO, agent + " considers " + technology.getName()  + " to be viable.");
        		setViableInvestment(true);

        	}
        	
        	
        	// Reporter
        	
        	
    		CapacityExpectationReport report = new CapacityExpectationReport();
    		

    		report.schedule = schedule;
    		report.setMarket(agent.getInvestorMarket());
    		report.setTime(schedule.getCurrentTick()); 
    		report.setAgent(agent);
    		report.setTechnology(technology);
    		report.setPlant(plant);
    		report.setNode(node);
    		
    		report.setViable(isViableInvestment());
    		report.setViableReason(inviableReason);
    		
    		getReps().capacityExpectationReports.add(report);


        
       }
       


    }
 

    public double predictSubsidyFip(EnergyProducer agent, long futureTimeStartScheme, PowerGridNode node,
            PowerGeneratingTechnology technology, boolean isTechSpecificityEnabled) {
        // Fuel Prices
        double expectedBaseCostFip = 0d;
        // Find Clearing Points for the last 5 years (counting current year
        // as one of the last 5 years).
        Iterable<BaseCostFip> BaseCostFipSet = null;

        if (isTechSpecificityEnabled) {
            BaseCostFipSet = getReps()
            		.findAllBaseCostFipsForTechnologyLocationAndTimeRange(node.getName(), technology,
                            getCurrentTick() + futureTimeStartScheme
                                    - (agent.getNumberOfYearsBacklookingForForecasting() - 1),
                            getCurrentTick() + futureTimeStartScheme);
            // logger.warn("baseCostFipSet: " + BaseCostFipSet);
        } else {
            BaseCostFipSet = getReps().
            		findAllTechnologyNeutralBaseCostForTimeRange(
                    getCurrentTick() + futureTimeStartScheme - (agent.getNumberOfYearsBacklookingForForecasting() - 1),
                    getCurrentTick() + futureTimeStartScheme);
            // logger.warn("baseCostFipSet: " + BaseCostFipSet);
        }

        SimpleRegression gtr = new SimpleRegression();
        if (BaseCostFipSet != null) {
            for (BaseCostFip baseCostFip : BaseCostFipSet) {
                // logger.warn("Base cost FIP {} , in predictSubsidyRole" +
                // baseCostFip.getCostPerMWh());

                gtr.addData(baseCostFip.getStartTime(), baseCostFip.getCostPerMWh());
            }
            expectedBaseCostFip = gtr.predict(agent.getInvestmentFutureTimeHorizon());
        }
        // logger.warn("Forecast {}: in Step " + futureTimeStartScheme,
        // gtr.predict(futureTimeStartScheme));
        return expectedBaseCostFip;

    }
    
    
   public class FutureFinancialExpectationWithScheme extends FutureFinancialExpectation{
	   	   
	   protected double expectedBaseCost = 0d;
	   protected double expectedAnnualVariableRevenueByRenewableScheme = 0d;

	   private double projectValueWithScheme;
       private double discountedGeneration = 0d;
       
       protected double discountedOperatingRevenue;
       protected double waccAdjusted; // Not implemented yet


       public FutureFinancialExpectationWithScheme(PowerPlant plant){
		   
		   super(plant);
		   		   
           waccAdjusted = (1 - agent.getDebtRatioOfInvestments())
                   * (agent.getEquityInterestRate() + agent.getEquityRatePriceRiskComponent())
                   + agent.getDebtRatioOfInvestments() * agent.getLoanInterestRate();
           
           double test = waccAdjusted;

    	}
	   

		/**
    	 * Calculates the expected base costs per MWh for the technology and if it is eligible for the current scheme
    	 */
    	public void calculateExpectedBaseCostWithScheme() {
            
            if (scheme != null && (scheme.getPowerGeneratingTechnologiesEligible().contains(technology))) {
                BaseCostFip baseCostFip = null;
                if ((scheme.getFutureSchemeStartTime() + getCurrentTick()) == getFutureTimePoint()
                        && scheme.isTechnologySpecificityEnabled()) {
                    
                	baseCostFip = getReps().findOneBaseCostForTechnologyAndNodeAndTime(
                            node.getName(), getTechnology(), getFutureTimePoint());
                    expectedBaseCost = baseCostFip.getCostPerMWh();
                    
                    logger.log(Level.FINER, 
                    		"For technology" + technology.getName() + "for node" + node.getName() 
                    + "Expected Base cost " + baseCostFip.getCostPerMWh());
                    
                } else if ((scheme.getFutureSchemeStartTime() + getCurrentTick()) == getFutureTimePoint()
                        && !scheme.isTechnologySpecificityEnabled()) {
                    
                	baseCostFip = getReps().findOneTechnologyNeutralBaseCostForTime(getFutureTimePoint());
                    expectedBaseCost = baseCostFip.getCostPerMWh();
                    
                    logger.log(Level.FINER, 
                    		"2: For technology" + technology.getName() + "for node" + node.getName() 
                    		+ "Expected Base cost " + expectedBaseCost);
 
                } else {

                    expectedBaseCost = predictSubsidyFip(agent, scheme.getFutureSchemeStartTime(), node,
                            technology, scheme.isTechnologySpecificityEnabled());
                    logger.log(Level.FINER, "3: For technology" + technology.getName() + "for node" + node.getName()
                     + "Expected Base cost " + expectedBaseCost);
                    
                }

            }
		
    	}

    	
    	@Override
    	protected void calculateFinancialIndicatorsForOneSegment(double expectedElectricityPrice, double hours, double capacity, double generationInSegment) {
            
    		    		
    		if (scheme != null && scheme.getPowerGeneratingTechnologiesEligible().contains(getTechnology())) {
	        	   expectedAnnualVariableRevenueByRenewableScheme += expectedElectricityPrice * generationInSegment;
	        }
    		
    		super.calculateFinancialIndicatorsForOneSegment(expectedElectricityPrice, hours, capacity, generationInSegment);
	             
    	}
    	
		protected double calculateDiscountedCashFlowForPlantAdjusted(int depriacationTime,
                double totalInvestment, double operatingProfit) {
    		
    		return calculateDiscountedCashFlowForPlant(depriacationTime,
                    totalInvestment, operatingProfit, waccAdjusted);
    	  	
    	}
    	
		/**
		 * Calculates the discounted Generation
		 * 
		 * @return discountedCapitalCosts
		 */
		private double calculateDiscountedGeneration() {
			
            double discountedGeneration = calculateDiscountedCashFlowForPlant(
            		(int) scheme.getSupportSchemeDuration(), 0, 1);
		   
		    logger.log(Level.FINER, 
		    		"Agent " + agent +  " found the discounted generation for " + technology + " to be " + discountedGeneration);
		
		    return discountedGeneration;  
		}
		
		/**
		 * Calculates the discounted operating revenue for the electricity market
		 * Because of the uncertainty of future prices, we adjusted WACCs here for risks.
		 * TODO:: Not always wacc_adjusted! > maybe calculateDiscountedCashFlowForPlantAdjusted is nonsense
		 * 
		 * @return discountedCapitalCosts
		 */
		// TODO rename withRisk
		// TODO it is confusing that I use this here, although these values are only for without the scheme... but it should be correct
    	private double calculateDiscountedOperatingRevenue() {
    		
    		// TODO check usage here,
			
			double operatingRevenue = expectedAnnualVariableRevenue;
            double discountedOperatingRevenue = calculateDiscountedCashFlowForPlantAdjusted(
                    technology.getDepreciationTime(), 0, operatingRevenue);
	
	        logger.log(Level.FINER, 
	       		"Agent " + agent +  " found the discounted operating revenue for " + technology + " to be " + discountedOperatingRevenue);
	
	        return discountedOperatingRevenue;  
		}

    	/* 
    	 * calculateProjectValue and with higher risks in the discountedOperatingRevenue.
    	 * (non-Javadoc)
    	 */
		
		@Override
		protected double calculateProjectValue() {
			
		
			double calcVariant1 = discountedOperatingProfit +                             discountedCapitalCosts;
			double calcVariant2 = discountedOperatingRevenue + discountedOperatingCost  + discountedCapitalCosts;
			
			if(Math.abs(calcVariant1 - calcVariant2) > 1e-4) {
				logger.warning("PROBLEM, values are not equal. Var 1: " + calcVariant1 + " and Var 2: " + calcVariant2);
			}

			return calcVariant2;          

    	}
		
    	/**
    	 * Calculates capital costs, operating costs & revenue, profit and project values
    	 */
    	@Override
		public void calculateDiscountedValues() {
    		
    		discountedOperatingRevenue = calculateDiscountedOperatingRevenue();
    		
    		if(scheme != null) {
    			discountedGeneration = calculateDiscountedGeneration();
    		}
    		
    		super.calculateDiscountedValues();	

    			
    	}

		
		
    	
     	public void calculateProjectValueWithScheme() {

            double operatingCost = expectedMarginalCost * plant.getAnnualFullLoadHours()
                    * plant.getActualNominalCapacity() + fixedOMCost;
            double discountedOpCost = calculateDiscountedCashFlowForPlant(
                    technology.getDepreciationTime(), 0, -operatingCost);
            
            
            double discountedOpRevenue;
            if (scheme.isEmRevenuePaidExpost()) {

            	double operatingRevenue = expectedBaseCost * plant.getAnnualFullLoadHours()
                        * plant.getActualNominalCapacity();

                // Is NOT adjusted (WACC), as risk are smaller in Expost tenders
                discountedOpRevenue = calculateDiscountedCashFlowForPlant(
                		(int) scheme.getSupportSchemeDuration(), 0, operatingRevenue);


            } else {
                
            	double operatingRevenueFromElecMarket = expectedAnnualVariableRevenueByRenewableScheme;
            	double operatingRevenueFromSubsidy = expectedBaseCost * plant.getAnnualFullLoadHours()
                        * plant.getActualNominalCapacity();
                                
                // IS adjusted (WACC), as risks due to uncertain elec. prices is higher for agent in in EX-Ante tenders
            	double discountedOpRevenueWithoutSubsidy = calculateDiscountedCashFlowForPlantAdjusted(
                		technology.getDepreciationTime(), 0, operatingRevenueFromElecMarket);
               
            	double discountedOpRevenueWithSubsidy = calculateDiscountedCashFlowForPlant(
                		(int) scheme.getSupportSchemeDuration(),0, operatingRevenueFromSubsidy);

                discountedOpRevenue = discountedOpRevenueWithoutSubsidy
                        + discountedOpRevenueWithSubsidy;
            }
            
            projectValueWithScheme = discountedOpRevenue + getDiscountedCapitalCosts() + discountedOpCost;
       		
	}





		public double getExpectedBaseCost() {
			return expectedBaseCost;
		}





		public void setExpectedBaseCost(double expectedBaseCost) {
			this.expectedBaseCost = expectedBaseCost;
		}


		public double getProjectValueWithScheme() {
			return projectValueWithScheme;
		}


		public double getDiscountedGeneration() {
			return discountedGeneration;
		}
		
		public double getWaccAdjusted() {
			return waccAdjusted;
		}
		
		public double getDiscountedOperatingRevenue() {
			return discountedOperatingRevenue;
		}

    	
   }


public void setScheme(RenewableSupportFipScheme scheme) {
	this.scheme = scheme;
}

  

}