/*******************************************************************************
 * Copyright 2012 the original author or authors.
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
package emlab.gen.role.market;

import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PowerPlantDispatchPlan;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentClearingPoint;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;

public class ProcessAcceptedPowerPlantDispatchRole extends AbstractMarketRole<ElectricitySpotMarket> implements Role<ElectricitySpotMarket> {

    public ProcessAcceptedPowerPlantDispatchRole(Schedule schedule) {
        super(schedule);
    }

    public void act(ElectricitySpotMarket esm) {

        for (Segment segment : getReps().segments) {
            SegmentClearingPoint scp = getReps().findOneSegmentClearingPointForMarketSegmentAndTime(
                    getCurrentTick(), segment, esm, false);
            for (PowerPlantDispatchPlan plan : getReps().findAllAcceptedPowerPlantDispatchPlansForMarketSegmentAndTime(esm, segment, getCurrentTick(),
                            false)) {

                getReps().createCashFlow(esm, plan.getBidder(), plan.getAcceptedAmount() * scp.getPrice()
                        * segment.getLengthInHours(), CashFlow.ELECTRICITY_SPOT, getCurrentTick(), plan.getPowerPlant());
            }

        }

    }

}
