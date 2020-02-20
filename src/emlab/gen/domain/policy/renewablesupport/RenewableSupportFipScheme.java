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

import java.util.Set;

import emlab.gen.domain.agent.EMLabAgent;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.technology.PowerGeneratingTechnology;

/**
 * @author Kaveri3012 A generic renewable support scheme role, meant to be able
 *         to model both price based and quantity based schemes.
 */

public class RenewableSupportFipScheme extends EMLabAgent {

    //@RelatedTo(type = "WITH_REGULATOR", elementClass = Regulator.class, direction = Direction.OUTGOING)
    private Regulator regulator;

    //@RelatedTo(type = "RES_SCHEME_FOR_ZONE", elementClass = Zone.class, direction = Direction.OUTGOING)
    private Zone zone;

    //@RelatedTo(type = "TECHNOLOGIES_ELIGIBLE_ARE", elementClass = PowerGeneratingTechnology.class, direction = Direction.OUTGOING)
    private Set<PowerGeneratingTechnology> powerGeneratingTechnologiesEligible;

    private boolean technologySpecificityEnabled;

    private boolean locationSpecificityEnabled;

    private boolean costContainmentMechanismEnabled;

    private boolean avgElectricityPriceBasedPremiumEnabled;

    private boolean emRevenuePaidExpost;

    private long futureSchemeStartTime;

    //@SimulationParameter(label = "Support Scheme Duration", from = 0, to = 50)
    private long supportSchemeDuration;

    private String name;

    public String getName() {
        return name;
    }

    public long getFutureSchemeStartTime() {
        return futureSchemeStartTime;
    }

    public void setFutureSchemeStartTime(long futureSchemeStartTime) {
        this.futureSchemeStartTime = futureSchemeStartTime;
    }

    public boolean isAvgElectricityPriceBasedPremiumEnabled() {
        return avgElectricityPriceBasedPremiumEnabled;
    }

    public void setAvgElectricityPriceBasedPremiumEnabled(boolean avgElectricityPriceBasedPremiumEnabled) {
        this.avgElectricityPriceBasedPremiumEnabled = avgElectricityPriceBasedPremiumEnabled;
    }

    public boolean isCostContainmentMechanismEnabled() {
        return costContainmentMechanismEnabled;
    }

    public void setCostContainmentMechanismEnabled(boolean costContainmentMechanismEnabled) {
        this.costContainmentMechanismEnabled = costContainmentMechanismEnabled;
    }

    public boolean isEmRevenuePaidExpost() {
        return emRevenuePaidExpost;
    }

    public void setEmRevenuePaidExpost(boolean emRevenuePaidExpost) {
        this.emRevenuePaidExpost = emRevenuePaidExpost;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<PowerGeneratingTechnology> getPowerGeneratingTechnologiesEligible() {
        return powerGeneratingTechnologiesEligible;
    }

    public void setPowerGeneratingTechnologiesEligible(
            Set<PowerGeneratingTechnology> powerGeneratingTechnologiesEligible) {
        this.powerGeneratingTechnologiesEligible = powerGeneratingTechnologiesEligible;
    }

    public Regulator getRegulator() {
        return regulator;
    }

    public void setRegulator(Regulator regulator) {
        this.regulator = regulator;
    }

    public boolean isTechnologySpecificityEnabled() {
        return technologySpecificityEnabled;
    }

    public void setTechnologySpecificityEnabled(boolean technologySpecificityEnabled) {
        this.technologySpecificityEnabled = technologySpecificityEnabled;
    }

    public boolean isLocationSpecificityEnabled() {
        return locationSpecificityEnabled;
    }

    public void setLocationSpecificityEnabled(boolean locationSpecificityEnabled) {
        this.locationSpecificityEnabled = locationSpecificityEnabled;
    }

    public long getSupportSchemeDuration() {
        return supportSchemeDuration;
    }

    public void setSupportSchemeDuration(long supportSchemeDuration) {
        this.supportSchemeDuration = supportSchemeDuration;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

}
