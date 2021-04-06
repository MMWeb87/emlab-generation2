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

import java.util.logging.Level;

import emlab.gen.domain.agent.BigBank;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.PowerPlantManufacturer;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.TenderBid;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGeneratingTechnologyNodeLimit;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.AbstractRole;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;
import emlab.gen.role.investment.AbstractInvestInPowerGenerationTechnologiesRole;
import emlab.gen.role.investment.AbstractInvestInPowerGenerationTechnologiesRole.FutureCapacityExpectation;
import emlab.gen.role.investment.AbstractInvestInPowerGenerationTechnologiesWithTenderRole;
import emlab.gen.role.investment.AbstractInvestInPowerGenerationTechnologiesWithTenderRole.FutureCapacityExpectationWithScheme;

/**
 * @author rjjdejeu
 *
 */

public class CreatePowerPlantsOfAcceptedTenderBidsRole extends AbstractRole<RenewableSupportSchemeTender>
implements Role<RenewableSupportSchemeTender> {

	public CreatePowerPlantsOfAcceptedTenderBidsRole(Schedule schedule) {
		super(schedule);
	}

	class EvaluateInvestmentRole extends AbstractInvestInPowerGenerationTechnologiesWithTenderRole<EnergyProducer>{
		
		public EvaluateInvestmentRole(Schedule schedule) {
			super(schedule);			
		}
		
		public Boolean isCapacityExpansionViable(PowerGeneratingTechnology technology, PowerPlant plant, PowerGridNode node) {
            
			// Check in FutureCapacityExpectationWithScheme the check function to see if viable from capacity point of view
			FutureCapacityExpectationWithScheme futureCapacityExpectation = new FutureCapacityExpectationWithScheme(technology, plant, node); 
            return(futureCapacityExpectation.isViableInvestment());
            
		}				
		
	}

	@Override
	public void act(RenewableSupportSchemeTender scheme) {

		logger.fine("Create Power Plants Of Accepted Tender Bids Role started for: " + scheme);

		// Zone zone = regulator.getZone();
		// RenewableSupportSchemeTender scheme =
		// reps.renewableSupportSchemeTenderRepository
		// .determineSupportSchemeForZone(zone);

		// Initialize the accepted bids
		Iterable<TenderBid> acceptedTenderBidsByTime = null;
		acceptedTenderBidsByTime = getReps().findAllAcceptedTenderBidsByTime(scheme, getCurrentTick());

		for (TenderBid currentTenderBid : acceptedTenderBidsByTime) {

			EnergyProducer bidder = (EnergyProducer) currentTenderBid.getBidder();
			EvaluateInvestmentRole evaluateInvestment = new EvaluateInvestmentRole(schedule);
			
			evaluateInvestment.setAgent(bidder);
			evaluateInvestment.setMarket(bidder.getInvestorMarket());
			evaluateInvestment.setFutureTimePoint(getCurrentTick()); // Need to set here because investment happens now
			//evaluateInvestment.setExpectations();
			
			PowerPlant plant = getReps().createAndSpecifyTemporaryPowerPlant(
					getCurrentTick(), bidder, currentTenderBid.getPowerGridNode(), currentTenderBid.getTechnology());
			

			if(!evaluateInvestment.isCapacityExpansionViable(plant.getTechnology(), plant, currentTenderBid.getPowerGridNode())){
				getReps().tenderBids.remove(currentTenderBid);
				
			} else {

				getReps().createPowerPlantFromPlant(plant);
				
				plant.setInvestmentOrigin(3);

				currentTenderBid.setPowerPlant(plant);  


				PowerPlantManufacturer manufacturer = getReps().powerPlantManufacturer;
				BigBank bigbank = getReps().bigBank;

				double investmentCostPayedByEquity = plant.getActualInvestedCapital()
						* (1 - bidder.getDebtRatioOfInvestments());
				double investmentCostPayedByDebt = plant.getActualInvestedCapital() * bidder.getDebtRatioOfInvestments();
				double downPayment = investmentCostPayedByEquity;
				evaluateInvestment.createSpreadOutDownPayments(bidder, manufacturer, downPayment, plant);

				double amount = evaluateInvestment.determineLoanAnnuities(investmentCostPayedByDebt,
						plant.getTechnology().getDepreciationTime(), bidder.getLoanInterestRate());

				logger.info("Loan amount is: " + amount);
				logger.info("current accepted bid: " + currentTenderBid + " for (new) power plant " + currentTenderBid.getPowerPlant());

						Loan loan = getReps().createLoan(currentTenderBid.getBidder(), bigbank, amount,
								plant.getTechnology().getDepreciationTime(), getCurrentTick(), plant);
						// Create the loan
						plant.createOrUpdateLoan(loan);

			}

		}

	}


}
