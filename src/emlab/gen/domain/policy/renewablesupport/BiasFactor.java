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

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import agentspring.simulation.SimulationParameter;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;

/**
 * @author Kaveri3012
 *
 */
@NodeEntity
public class BiasFactor {

    @SimulationParameter(label = "FeedInPremiumFactor", from = 0, to = 2)
    private double feedInPremiumBiasFactor;

    @RelatedTo(type = "BIASFACTOR_FOR_TECHNOLOGY", elementClass = PowerGeneratingTechnology.class, direction = Direction.OUTGOING)
    private PowerGeneratingTechnology technology;

    @RelatedTo(type = "BIASFACTOR_FOR_NODE", elementClass = PowerGridNode.class, direction = Direction.OUTGOING)
    private PowerGridNode node;

    @RelatedTo(type = "BIASFACTOR_FOR_SUPPORTSCHEME", elementClass = RenewableSupportFipScheme.class, direction = Direction.OUTGOING)
    private RenewableSupportFipScheme scheme;

    @SimulationParameter(label = "DegressionFactor", from = 0, to = 1)
    private double degressionFactor;

    public double getDegressionFactor() {
        return degressionFactor;
    }

    public void setDegressionFactor(double degressionFactor) {
        this.degressionFactor = degressionFactor;
    }

    public double getFeedInPremiumBiasFactor() {
        return feedInPremiumBiasFactor;
    }

    public void setFeedInPremiumBiasFactor(double feedInPremiumBiasFactor) {
        this.feedInPremiumBiasFactor = feedInPremiumBiasFactor;
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

    public RenewableSupportFipScheme getScheme() {
        return scheme;
    }

    public void setScheme(RenewableSupportFipScheme scheme) {
        this.scheme = scheme;
    }

}