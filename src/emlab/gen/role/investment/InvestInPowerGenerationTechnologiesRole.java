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

import java.util.logging.Level;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;

/**
 * {@link EnergyProducer}s decide to invest in new {@link PowerPlant}
 *
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 * <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas Chmieliauskas</a>
 * @author JCRichstein
 * @author marcmel
 */
public class InvestInPowerGenerationTechnologiesRole<T extends EnergyProducer> extends AbstractInvestInPowerGenerationTechnologiesRole<T> implements Role<T> {

    public InvestInPowerGenerationTechnologiesRole(Schedule schedule) {
        super(schedule);
    }


    public void act(T agent) {
    	
    	initEvaluationForEnergyProducer(agent, agent.getInvestorMarket());
    	
        PowerPlant bestPlant = null;
        double highestValue = Double.MIN_VALUE;

        for (PowerGeneratingTechnology technology : getReps().powerGeneratingTechnologies) {

            PowerPlant plant = createPowerPlant(technology);
            FutureCapacityExpectation futureCapacityExpectation = new FutureCapacityExpectation(technology, plant);
            
            if(futureCapacityExpectation.isViableInvestment()) {
                
            	FutureFinancialExpectation financialExpectation = new FutureFinancialExpectation(plant);

                if (financialExpectation.plantHasRequiredRunningHours()) {
                	financialExpectation.calculateDiscountedValues();
                	
                    if (financialExpectation.getProjectValue() > 0) {
                    	
                    	logger.log(Level.FINE, "The project value " + financialExpectation.getProjectValue() + " for " + technology + " and " + this.getMarket().getName() + " is positive.");
	
	                    // Only for reporting
                    	double projectDiscountedReturnOnInvestment = financialExpectation.calculateDiscountedReturnOnInvestment(financialExpectation.getProjectValue());			                    
	                    logger.log(Level.FINER, "Agent " + agent + " finds the discounted per lifetime year ROI for " + technology + " to be " + projectDiscountedReturnOnInvestment);
	                    double projectDiscountedReturnOnEquity = projectDiscountedReturnOnInvestment / (1 - agent.getDebtRatioOfInvestments());
	                    logger.log(Level.FINER, "Agent " + agent + " finds the discounted per lifetime year  ROE (debt: " + agent.getDebtRatioOfInvestments() +") for " + technology + " to be " + projectDiscountedReturnOnEquity);

	                    // Reporter
	                    FinancialExpectationReport report = new FinancialExpectationReport();
	                    
	                    report.schedule = schedule;
	                    report.setMarket(agent.getInvestorMarket());
	                    report.setTime(schedule.getCurrentTick()); 
	                    report.setAgent(agent);
	                    report.setTechnology(technology);
	                    report.setPlant(plant);
	                    report.setNode(plant.getLocation());
	                    report.setInvestmentRound(this.getCurrentTnvestmentRound());

	                    
	                    report.setProjectReturnOnInvestment(projectDiscountedReturnOnInvestment);
	                    report.setProjectReturnOnEquity(projectDiscountedReturnOnEquity);
	                    
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
	                    report.setTotalUtility(0);
	                    
	                     
	                    getReps().financialExpectationReports.add(report);
	
	                    // Divide by capacity, in order not to favour large power plants (which have the single largest NP)
	                    if (financialExpectation.getProjectValue() > 0 && financialExpectation.getProjectValue() / plant.getActualNominalCapacity() > highestValue) {
	                        highestValue = financialExpectation.getProjectValue() / plant.getActualNominalCapacity();
	                        bestPlant = plant;
	                    }
                    }
                }
            }
        }
        
        decideToInvestInPlant(bestPlant);
        
    }

}
