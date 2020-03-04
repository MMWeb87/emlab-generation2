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
package emlab.gen.domain.policy.renewablesupport;

import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;

/**
 * @author Kaveri for tender
 *
 */
public class TenderBid extends Bid {

    //@RelatedTo(type = "FOR_NODE", elementClass = PowerGridNode.class, direction = Direction.OUTGOING)
    private PowerGridNode powerGridNode;

    //@RelatedTo(type = "FOR_TECHNOLOGY", elementClass = PowerGeneratingTechnology.class, direction = Direction.OUTGOING)
    private PowerGeneratingTechnology technology;

    //@RelatedTo(type = "POWERPLANT_DISPATCHPLAN", elementClass = PowerPlant.class, direction = Direction.OUTGOING)
    private PowerPlant powerPlant;

    //@RelatedTo(type = "TENDERBID_SUPPORTSCHEME", elementClass = RenewableSupportSchemeTender.class, direction = Direction.OUTGOING)
    private RenewableSupportSchemeTender renewableSupportSchemeTender;

    //@RelatedTo(type = "TENDERBID_ZONE", elementClass = Zone.class, direction = Direction.INCOMING)
    private Zone zone;

    private long start;

    private long finish;

    private String investor;

    public String getInvestor() {
        return investor;
    }

    public void setInvestor(String investor) {
        this.investor = investor;
    }

    private double cashNeededForPlantDownpayments;

    public double getCashNeededForPlantDownpayments() {
        return cashNeededForPlantDownpayments;
    }

    public void setCashNeededForPlantDownpayments(double cashNeededForPlantDownpayments) {
        this.cashNeededForPlantDownpayments = cashNeededForPlantDownpayments;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public RenewableSupportSchemeTender getRenewableSupportSchemeTender() {
        return renewableSupportSchemeTender;
    }

    public void setRenewableSupportSchemeTender(RenewableSupportSchemeTender renewableSupportSchemeTender) {
        this.renewableSupportSchemeTender = renewableSupportSchemeTender;
    }

    public long getFinish() {
        return finish;
    }

    public void setFinish(long finish) {
        this.finish = finish;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public PowerGridNode getPowerGridNode() {
        return powerGridNode;
    }

    public void setPowerGridNode(PowerGridNode powerGridNode) {
        this.powerGridNode = powerGridNode;
    }

    public PowerPlant getPowerPlant() {
        return powerPlant;
    }

    public void setPowerPlant(PowerPlant powerPlant) {
        this.powerPlant = powerPlant;
    }

    public PowerGeneratingTechnology getTechnology() {
        return technology;
    }

    public void setTechnology(PowerGeneratingTechnology technology) {
        this.technology = technology;
    }

    @Override
    public String toString() {
        return "TenderBid for " + getBidder() + "; price: " + getPrice() + "; amount: " + getAmount() + "; cash needed for downpayment "
                + cashNeededForPlantDownpayments + "; " + getRenewableSupportSchemeTender() + "; technology " + getTechnology();
    }
}
