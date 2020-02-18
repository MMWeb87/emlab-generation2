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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.repository.Reps;

/**
 * @author Kaveri3012
 *
 */
@RoleComponent
public class TenderMainRolePart extends AbstractRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {

    /*
     * (non-Javadoc)
     * 
     * @see agentspring.role.Role#act(agentspring.agent.Agent)
     */
    @Autowired
    Reps reps;

    @Autowired
    CalculateRenewableTargetForTenderRole calculateRenewableTargetForTenderRole;

    @Autowired
    SubmitTenderBidRole submitTenderBidRole;

    @Autowired
    SubmitTenderBidRoleExpostRevenuePayment submitTenderBidRoleExpostRevenuePayment;

    @Autowired
    FilterTenderBidsByTechnologyPotentialRole filterTenderBidsByTechnologyPotentialRole;
    // FilterTenderBidsWithSufficientCashForDownpaymentRole
    // filterTenderBidsWithSufficientCashflowRole;
    //
    @Autowired
    ClearRenewableTenderRole clearRenewableTenderRole;

    @Autowired
    CreatePowerPlantsOfAcceptedTenderBidsRole createPowerPlantsOfAcceptedTenderBidsRole;

    @Autowired
    OrganizeRenewableTenderPaymentsRole organizeRenewableTenderPaymentsRole;

    @Autowired
    VerificationTargetCalculationRole verificationTargetCalculationRole;

    @Override
    @Transactional
    public void act(RenewableSupportSchemeTender scheme) {

        calculateRenewableTargetForTenderRole.act(scheme);

        // if (scheme.isExpostRevenueCalculation() == true) {
        // submitTenderBidRoleExpostRevenuePayment.act(scheme);
        // } else {
        submitTenderBidRole.act(scheme);
        // }

        // this role needs to be adjusted for the techspec feature
        // Regulator regulator = scheme.getRegulator();
        // ElectricitySpotMarket market =
        // reps.marketRepository.findElectricitySpotMarketForZone(regulator.getZone());
        //
        // for (EnergyProducer producer :
        // reps.energyProducerRepository.findEnergyProducersByMarketAtRandom(market))
        // {
        if (scheme.getAnnualRenewableTargetInMwh() > 0) {
            filterTenderBidsByTechnologyPotentialRole.act(scheme);

            clearRenewableTenderRole.act(scheme);

            createPowerPlantsOfAcceptedTenderBidsRole.act(scheme);
        }

        organizeRenewableTenderPaymentsRole.act(scheme);

        verificationTargetCalculationRole.act(scheme);

    }

}
