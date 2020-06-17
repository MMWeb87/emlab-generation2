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

import java.util.TreeMap;
import java.util.logging.Level;

import emlab.gen.domain.agent.EMLabModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.AbstractRole;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;
import emlab.gen.role.investment.AbstractInvestInPowerGenerationTechnologiesRole;

/**
 * @author kaveri, rjjdejeu, marcmel
 */

public class SubmitTenderBidRole extends AbstractRole<RenewableSupportSchemeTender> implements Role<RenewableSupportSchemeTender> {

    public SubmitTenderBidRole(Schedule schedule) {
        super(schedule);
    }

    class EvaluateInvestmentRole extends AbstractInvestInPowerGenerationTechnologiesRole<EnergyProducer>{

    	private boolean submitBid = false;
    	private double totalAnnualExpectedGenerationOfPlant;
    	
		public EvaluateInvestmentRole(Schedule schedule) {
			super(schedule);
		}
				
		public double calculateBidPrice(PowerPlant plant, RenewableSupportSchemeTender scheme) {
	        
			FutureFinancialExpectationWithBid financialExpectation = new FutureFinancialExpectationWithBid(plant);

			financialExpectation.calculateDiscountedValues();
			double projectValue = financialExpectation.getProjectValue();  // For Ex-Ante Calculations.
            double projectCost = financialExpectation.getProjectCost();  // For Ex-Post Calculations.
            double projectValueFinal = (scheme.isExpostRevenueCalculation() == true) ? projectCost : projectValue;
           
	        double discountedTenderReturnFactor = financialExpectation.calculateDiscountedTenderReturnFactor(scheme.getSupportSchemeDuration());
            
	        double bidPricePerMWh = 0d;

            if (discountedTenderReturnFactor != 0) {
            	
            	submitBid = true;
            	totalAnnualExpectedGenerationOfPlant = financialExpectation.getExpectedGeneration();
            	
                // calculate generation in MWh per year
            	bidPricePerMWh = -projectValueFinal
                        / (discountedTenderReturnFactor * totalAnnualExpectedGenerationOfPlant);

	            if (bidPricePerMWh < 0) {
	                    bidPricePerMWh = 0;
	            }
            
            }
		
            return bidPricePerMWh;				
		}


		
		
		public class FutureFinancialExpectationWithBid extends FutureFinancialExpectation{
			
			
			public FutureFinancialExpectationWithBid(PowerPlant plant) {
				super(plant);
			}

			public double calculateDiscountedTenderReturnFactor(double tenderSchemeDuration) {
				
		        TreeMap<Integer, Double> discountedTenderReturnFactorSummingTerm = calculateSimplePowerPlantInvestmentCashFlow(
		                (int) tenderSchemeDuration, (int) (plant.calculateActualLeadtime()), 0, 1);
		        double discountedTenderReturnFactor = npv(discountedTenderReturnFactorSummingTerm, wacc);
		        return discountedTenderReturnFactor;
				
			}

			
			
		}

		public boolean isSubmitBid() {
			return submitBid;
		}

		public double getTotalAnnualExpectedGenerationOfPlant() {
			return totalAnnualExpectedGenerationOfPlant;
		}

    	
    }
    
	EvaluateInvestmentRole evaluateInvestment = new EvaluateInvestmentRole(schedule);


    @Override
    public void act(RenewableSupportSchemeTender scheme) {

        Regulator regulator = scheme.getRegulator();
        ElectricitySpotMarket market = getReps().findElectricitySpotMarketForZone(regulator.getZone());

        double tenderTarget = scheme.getAnnualRenewableTargetInMwh();
        if (tenderTarget > 0) {

            for (EnergyProducer agent : getReps().findEnergyProducersByMarketAtRandom(market)) {
            	
            	evaluateInvestment.initEvaluationForEnergyProducer(agent, market);            

                Zone zone = agent.getInvestorMarket().getZone();

                for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {

                    EMLabModel model = getReps().emlabModel;
                    
                    if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                        continue;

                    for (PowerGridNode node : evaluateInvestment.findPossibleInstallationNodes(technology)) {
                    	
                        PowerPlant plant = evaluateInvestment.createPowerPlant(technology, node);

                        // Calculate bid quantity. Number of plants to be bid -
                        // as many as the node permits
                        double cashNeededPerPlant = plant.getActualInvestedCapital()
                                * (1 - agent.getDebtRatioOfInvestments()) / plant.getActualLeadtime();

                        double noOfPlantsByTarget = scheme.getAnnualRenewableTargetInMwh()
                                / (plant.getAnnualFullLoadHours() * plant.getActualNominalCapacity());

                        // Target should equal node potential.
                        long noOfPlants = (long) Math.ceil(noOfPlantsByTarget);
                        logger.log(Level.FINE, "NUMBER OF PLANTS TO BE BID FOR" + noOfPlants);

                        
                        evaluateInvestment.setFuelMixForPlant(technology, plant);
                        
                        double bidPricePerMWh = evaluateInvestment.calculateBidPrice(plant, scheme);
                        
                        if(evaluateInvestment.isSubmitBid()) {

                        
                            // logger.warn("for scheme" + scheme.getName() +
                            // "bidding for " + noOfPlants + "at price"
                            // + bidPricePerMWh);
                            for (long i = 1; i <= noOfPlants; i++) {

                                long start = getCurrentTick() + plant.calculateActualLeadtime()
                                        + plant.calculateActualPermittime();
                                long finish = getCurrentTick() + plant.calculateActualLeadtime()
                                        + plant.calculateActualPermittime() + scheme.getSupportSchemeDuration();

                                String investor = agent.getName();
                                
                                // INFO plant is null as plant will be created in investment decision role 
                                getReps().submitTenderBidToMarket(evaluateInvestment.getTotalAnnualExpectedGenerationOfPlant(), null, agent, zone, node,
                                        start, finish, bidPricePerMWh, technology, getCurrentTick(), Bid.SUBMITTED,
                                        scheme, cashNeededPerPlant, investor);
                                
                                 logger.log(Level.FINER, "SubmitBid to tender - Agent " +
                                 agent + " ,generation "
                                 + evaluateInvestment.getTotalAnnualExpectedGenerationOfPlant() +
                                 " ,plant " + plant + " ,zone "
                                 + zone + " ,node " + node + " ,start " +
                                 start + " ,finish " + finish
                                 + " ,bid price " + bidPricePerMWh +
                                 " ,tech " + technology
                                 + " ,current tick " + getCurrentTick() +
                                 " ,status " + Bid.SUBMITTED
                                 + " ,scheme " + scheme +
                                 //", cash downpayment; "
                                 //+ cashNeededForPlantDownpayments,
                                 " ,investor " + investor);
                                 
                                 

                            } // end for loop for tender bids

                        } // end else calculate generation in MWh per year

                        // } // end else calculate discounted tender return
                        // factor
                        // term
                        plant = null;

                    } // end for loop possible installation nodes

                } // end 
                
                
//                for (PowerGeneratingTechnology technology :
//                   reps.genericRepository.findAll(PowerGeneratingTechnology.class))
//                   logger.warn("Number of tender bids made" + noOfPlantsBid +
//                   "by producer" + agent.getName()
//                   + "for scheme " + scheme.getName());
                
            } // end For schemes
        }
    }

 

}
