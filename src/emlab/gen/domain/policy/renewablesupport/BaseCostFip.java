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

import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;

/**
 * @author Kaveri3012
 *
 */

public class BaseCostFip {

    private double costPerMWh;

    //@RelatedTo(type = "BASECOST_FOR_TECHNOLOGY", elementClass = PowerGeneratingTechnology.class, direction = Direction.OUTGOING)
    private PowerGeneratingTechnology technology;

    //@RelatedTo(type = "BASECOST_FOR_LOCATION", elementClass = PowerGridNode.class, direction = Direction.OUTGOING)
    private PowerGridNode node;

    private long startTime;

    private long endTime;

    public double getCostPerMWh() {
        return costPerMWh;
    }

    public void setCostPerMWh(double costPerMWh) {
        this.costPerMWh = costPerMWh;
    }

    public PowerGeneratingTechnology getTechnology() {
        return technology;
    }

    public void setTechnology(PowerGeneratingTechnology technology) {
        this.technology = technology;
    }

    public PowerGridNode getNode() {
        return node;
    }

    public void setNode(PowerGridNode node) {
        this.node = node;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

}
