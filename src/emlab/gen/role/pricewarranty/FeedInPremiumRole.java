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

import java.util.HashSet;
import java.util.Set;

import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PowerPlantDispatchPlan;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.renewablesupport.BaseCostFip;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.policy.renewablesupport.SupportPriceContract;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.AbstractRole;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;
import emlab.gen.repository.Reps;

/**
 * @author Kaveri3012 for loop through eligible, operational power plants,
 *         create support price contract for each technology, using
 * 
 *         supportPrice = baseCost*TotalGeneration - ElectricityMarketRevenue
 * 
 *         Assumption: when the policy is implemented for a certain country, all
 *         operational, eligible plants - plants that have in that zone receive
 *         the premium by default. there is no need for an energy producer agent
 *         to voluntarily apply for the scheme.
 * 
 * 
 */
public class FeedInPremiumRole extends AbstractRole<RenewableSupportFipScheme> 
	implements Role<RenewableSupportFipScheme> {

    public FeedInPremiumRole(Schedule schedule) {
        super(schedule);
    }
    
    public void act(RenewableSupportFipScheme renewableSupportScheme) {

        Regulator regulator = new Regulator();
        regulator = renewableSupportScheme.getRegulator();

        Set<PowerGeneratingTechnology> technologySet = new HashSet<PowerGeneratingTechnology>();
        technologySet = renewableSupportScheme.getPowerGeneratingTechnologiesEligible();

        ElectricitySpotMarket eMarket = getReps().findElectricitySpotMarketForZone(regulator.getZone());
        SupportPriceContract contract = null;

        for (PowerGeneratingTechnology technology : technologySet) {

            Iterable<PowerGridNode> possibleInstallationNodes = getReps()
                    .findAllPowerGridNodesByZone(regulator.getZone());

            // logger.warn("Calculating FEED IN PREMIUM for " +
            // technology.getName() + ", for Nodes: "
            // + possibleInstallationNodes.toString());

            for (PowerGridNode node : possibleInstallationNodes) {

                // logger.warn("Inside power grid node loop");

                Iterable<PowerPlant> plantSet;

                if (getCurrentTick() >= 1) {
                    plantSet = getReps()
                            .findPowerPlantsStartingOperationThisTickByPowerGridNodeAndTechnology(node,
                                    technology.getName(), getCurrentTick());
                                    // logger.warn("FIP role, plantSet");
                                    // } else {
                                    // plantSet =
                                    // reps.powerPlantRepository.findOperationalPowerPlantsByPowerGridNodeAndTechnology(node,
                                    // technology, getCurrentTick());
                                    // }

                    // query to find power plants by node and technology who
                    // have
                    // finished construction this tick
                    // TODO MM (Comment): Can't find any plants here because no investing has happend. I guess here it's only paying.(Generatinig the cashflow)
                    for (PowerPlant plant : plantSet) {
                    	
//		            	if(renewableSupportScheme.getFutureSchemePhaseoutTime().containsKey(technology)) {	
//		            		// Only make a contract for this... Relly??
//		        			logger.info(technology + "");
//		            		
//		        			// Make no new contracts if support schemes are phased out.
//		        			// e.g. CASHFLOW if phase-out at year 15 > current tick 2 -> TRUE -> cashflow 
//		            		if(renewableSupportScheme.getFutureSchemePhaseoutTime().get(technology) > getCurrentTick()) {
 		                    	
		                    	long finishedConstruction = plant.getConstructionStartTime() + plant.calculateActualPermittime()
		                                + plant.calculateActualLeadtime();
		
		                        long contractFinishTime = finishedConstruction
		                                + renewableSupportScheme.getSupportSchemeDuration();
		
		                        // logger.warn("Printing finished construction" +
		                        // finishedConstruction
		                        // + "and construction start time" +
		                        // plant.getConstructionStartTime());
		
		                        // logger.warn("Inside contract creation loop");
		                        BaseCostFip baseCost = null;
		                        // create a query to get base cost.
		                        if (renewableSupportScheme.isTechnologySpecificityEnabled()) {
		                            baseCost = getReps().findOneBaseCostForTechnologyAndNodeAndTime(
		                                    node.getName(), technology, plant.getConstructionStartTime()
		                                            + renewableSupportScheme.getFutureSchemeStartTime());
		                            // logger.warn("expected base cost query test FIP is
		                            // " + baseCost);
		                        } else {
		                            baseCost = getReps()
		                                    .findOneTechnologyNeutralBaseCostForTime(plant.getConstructionStartTime()
		                                            + renewableSupportScheme.getFutureSchemeStartTime());
		                            // logger.warn("expected base cost query test FIP is
		                            // " + baseCost);
		                        }
		
		                        if (baseCost != null) {
		
		                            contract = new SupportPriceContract();
		                            contract.setStart(getCurrentTick());
		                            contract.setPricePerUnit(baseCost.getCostPerMWh());
		                            contract.setFinish(contractFinishTime);
		                            contract.setPlant(plant);
		
		                            // logger.warn("Contract price for plant of
		                            // technology " + plant.getTechnology().getName()
		                            // + "for node " + node.getNodeId() + " is , " +
		                            // contract.getPricePerUnit());
		                            
		                            
		                            getReps().supportPriceContracts.add(contract);
		                            
//		                        }
//		                        
//		            		}
			            	
		            	}
                	
                    }	
              	
                }

                for (PowerPlant plant : getReps()
                        .findOperationalPowerPlantsByPowerGridNodeAndTechnology(node, technology, getCurrentTick())) {
                    // .findAllPowerPlantsWithConstructionStartTimeInTick(getCurrentTick())
                    // //findOperationalPowerPlantsByMarketAndTechnology(eMarket,
                    // technology, getCurrentTick())) {

                    // logger.warn("Compute FiP for power plant" +
                    // plant.getName());

                    // existing eligible plants at the start of the simulation
                    // (tick
                    // 0) do not get contracts.

                    // if plant is new (begins operation this year), get
                    // corresponding base cost, and create supportPriceContract
                    // for it, with base cost, start tick and end tick.

                    // for all eligible plants, the support price is calculated,
                    // and
                    // payment is made.
                    contract = getReps().findOneContractByPowerPlant(plant);
                    if (contract != null) {
                        if (getCurrentTick() <= (contract.getStart()
                                + renewableSupportScheme.getSupportSchemeDuration())
                                && getCurrentTick() >= contract.getStart()) {
                            // logger.warn("Inside contract payment loop for
                            // plant " + plant);
                            double sumEMR = 0d;
                            double emAvgPrice = 0d;
                            double electricityPrice = 0d;
                            double totalGenerationOfPlantInMwh = 0d;
                            double totalHoursOfAnnualGeneration = 0d;
                            double sumRevenueOfElectricity = 0d;
                            // the for loop below calculates the electricity
                            // market
                            // price the plant earned
                            // throughout the year, for its total production

                            for (SegmentLoad segmentLoad : eMarket.getLoadDurationCurve()) {
                                // logger.warn("Inside segment loop for
                                // calculating
                                // total production");

                                electricityPrice = getReps()
                                        .findOneSegmentClearingPointForMarketSegmentAndTime(getCurrentTick(),
                                                segmentLoad.getSegment(), eMarket, false)
                                        .getPrice();
                                double hours = segmentLoad.getSegment().getLengthInHours();
                                totalHoursOfAnnualGeneration += hours;
                                sumRevenueOfElectricity += electricityPrice * hours;

                                PowerPlantDispatchPlan ppdp = getReps()
                                        .findOnePowerPlantDispatchPlanForPowerPlantForSegmentForTime(plant,
                                                segmentLoad.getSegment(), getCurrentTick(), false);

                                if (ppdp.getStatus() < 0 || ppdp == null) {
                                    electricityPrice = 0d;
                                } else if (ppdp.getStatus() >= 2) {
                                    // do a sensitivity here to different
                                    // averages of electricity prices.
                                    sumEMR = sumEMR + electricityPrice * hours * ppdp.getAcceptedAmount();
                                    totalGenerationOfPlantInMwh += hours * ppdp.getAcceptedAmount();
                                }

                            }
                            double supportPrice = 0d;
                            emAvgPrice = sumRevenueOfElectricity / totalHoursOfAnnualGeneration;

                            if (renewableSupportScheme.isAvgElectricityPriceBasedPremiumEnabled() == true) {
                                if (renewableSupportScheme.isEmRevenuePaidExpost() == true) {
                                    supportPrice = (contract.getPricePerUnit() - emAvgPrice)
                                            * totalGenerationOfPlantInMwh;
                                    double supportPriceExact = contract.getPricePerUnit() * totalGenerationOfPlantInMwh
                                            - sumEMR;
                                    // logger.warn("supportPrice considering avg
                                    // EM price" + supportPrice
                                    // + "support price exact" +
                                    // supportPriceExact);
                                } else {
                                    supportPrice = contract.getPricePerUnit() * totalGenerationOfPlantInMwh;
                                }

                            } else {
                                if (renewableSupportScheme.isEmRevenuePaidExpost() == true) {

                                    supportPrice = contract.getPricePerUnit() * totalGenerationOfPlantInMwh - sumEMR;
                                    // logger.warn("support price EP true" +
                                    // supportPrice);
                                } else {
                                    supportPrice = contract.getPricePerUnit() * totalGenerationOfPlantInMwh;
                                    // logger.warn("support price EP false" +
                                    // supportPrice);
                                }

                            }

                            createCashFlow(regulator, plant, supportPrice);

                        }
                        // delete contract. not sure if necessary. contract has
                        // been mainly used to control period of payment
                        if (getCurrentTick() > (contract.getStart()
                                + renewableSupportScheme.getSupportSchemeDuration())) {
                            contract = null;
                        }

                    }

                }

            }

        }
    }

    public void createCashFlow(Regulator regulator, PowerPlant plant, double supportPrice) {

    	getReps().createCashFlow(regulator, plant.getOwner(), supportPrice,
                CashFlow.FEED_IN_PREMIUM, getCurrentTick(), plant);

         logger.fine("(DBG123) Fip Premium of " + supportPrice + " from regulator " + regulator.getName() + " to " + plant.getOwner());

    }

}
