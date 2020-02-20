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

import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.engine.AbstractRole;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;
import emlab.gen.repository.Reps;

/**
 * @author Kaveri3012
 * @author marcmelliger
 *
 */

public class TenderMainRole extends AbstractRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {


    Reps reps;

    CalculateRenewableTargetForTenderRole calculateRenewableTargetForTenderRole;

    SubmitTenderBidRole submitTenderBidRole;

    FilterTenderBidsByTechnologyPotentialRole filterTenderBidsByTechnologyPotentialRole;

    ClearRenewableTenderRole clearRenewableTenderRole;

    CreatePowerPlantsOfAcceptedTenderBidsRole createPowerPlantsOfAcceptedTenderBidsRole;

    OrganizeRenewableTenderPaymentsRole organizeRenewableTenderPaymentsRole;

    VerificationTargetCalculationRole verificationTargetCalculationRole;
    
    public TenderMainRole(Schedule schedule) {
        super(schedule);
    }

    @Override
    public void act(RenewableSupportSchemeTender scheme) {

        calculateRenewableTargetForTenderRole.act(scheme);

        submitTenderBidRole.act(scheme);

        if (scheme.getAnnualRenewableTargetInMwh() > 0) {
            filterTenderBidsByTechnologyPotentialRole.act(scheme);

            clearRenewableTenderRole.act(scheme);

            createPowerPlantsOfAcceptedTenderBidsRole.act(scheme);
        }

        organizeRenewableTenderPaymentsRole.act(scheme);

        verificationTargetCalculationRole.act(scheme);

    }

}
