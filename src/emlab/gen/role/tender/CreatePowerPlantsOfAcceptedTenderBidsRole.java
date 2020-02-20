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

import emlab.gen.domain.agent.BigBank;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.PowerPlantManufacturer;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.TenderBid;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.AbstractRole;
import emlab.gen.engine.Role;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractRoleWithFunctionsRole;

/**
 * @author rjjdejeu
 *
 */

public class CreatePowerPlantsOfAcceptedTenderBidsRole extends AbstractRoleWithFunctionsRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {
    Reps reps;

    @Override
    public void act(RenewableSupportSchemeTender scheme) {

        // logger.warn("Create Power Plants Of Accepted Tender Bids Role started
        // for: " + scheme);
        // for: " + regulator);

        // Zone zone = regulator.getZone();
        // RenewableSupportSchemeTender scheme =
        // reps.renewableSupportSchemeTenderRepository
        // .determineSupportSchemeForZone(zone);

        // Initialize the accepted bids
        Iterable<TenderBid> acceptedTenderBidsByTime = null;
        acceptedTenderBidsByTime = reps.findAllAcceptedTenderBidsByTime(scheme, getCurrentTick());

        for (TenderBid currentTenderBid : acceptedTenderBidsByTime) {

            // logger.warn(
            // "current accepted bid: " + currentTenderBid + "for power plant" +
            // currentTenderBid.getPowerPlant());

        	EnergyProducer bidder = (EnergyProducer) currentTenderBid.getBidder();
            PowerPlant plant = reps.createAndSpecifyTemporaryPowerPlant(
            		getCurrentTick(), bidder, currentTenderBid.getPowerGridNode(), currentTenderBid.getTechnology());
            currentTenderBid.setPowerPlant(plant);                            
                    
            PowerPlantManufacturer manufacturer = getReps().powerPlantManufacturer;
            BigBank bigbank = getReps().bigBank;

            double investmentCostPayedByEquity = plant.getActualInvestedCapital()
                    * (1 - bidder.getDebtRatioOfInvestments());
            double investmentCostPayedByDebt = plant.getActualInvestedCapital() * bidder.getDebtRatioOfInvestments();
            double downPayment = investmentCostPayedByEquity;
            createSpreadOutDownPayments(bidder, manufacturer, downPayment, plant);

            double amount = determineLoanAnnuities(investmentCostPayedByDebt,
                    plant.getTechnology().getDepreciationTime(), bidder.getLoanInterestRate());
            // logger.warn("Loan amount is: " + amount);
            Loan loan = reps.createLoan(currentTenderBid.getBidder(), bigbank, amount,
                    plant.getTechnology().getDepreciationTime(), getCurrentTick(), plant);
            // Create the loan
            plant.createOrUpdateLoan(loan);

        }

    }


}