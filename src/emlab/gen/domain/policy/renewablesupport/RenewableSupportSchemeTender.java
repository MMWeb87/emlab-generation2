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

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import agentspring.agent.Agent;
import agentspring.simulation.SimulationParameter;
import emlab.gen.domain.agent.DecarbonizationAgent;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.technology.PowerGeneratingTechnology;

/**
 * @author Kaveri3012 A generic renewable support scheme role, meant to be able
 *         to model both price based and quantity based schemes.
 */
@NodeEntity
public class RenewableSupportSchemeTender extends DecarbonizationAgent implements Agent {

    @RelatedTo(type = "WITH_REGULATOR", elementClass = Regulator.class, direction = Direction.OUTGOING)
    private Regulator regulator;

    @RelatedTo(type = "TECHNOLOGIES_ELIGIBLE_ARE", elementClass = PowerGeneratingTechnology.class, direction = Direction.OUTGOING)
    private Set<PowerGeneratingTechnology> powerGeneratingTechnologiesEligible;

    // This target is for the 'actual target' that the regulator must set each
    // year
    // taking into account the existing/planned investment for that year.
    private double annualRenewableTargetInMwh;

    public double getAnnualRenewableTargetInMwh() {
        return annualRenewableTargetInMwh;
    }

    public void setAnnualRenewableTargetInMwh(double annualRenewableTargetInMwh) {
        this.annualRenewableTargetInMwh = annualRenewableTargetInMwh;
    }

    private PowerGeneratingTechnology currentTechnologyUnderConsideration;

    public PowerGeneratingTechnology getCurrentTechnologyUnderConsideration() {
        return currentTechnologyUnderConsideration;
    }

    public void setCurrentTechnologyUnderConsideration(PowerGeneratingTechnology currentTechnologyUnderConsideration) {
        this.currentTechnologyUnderConsideration = currentTechnologyUnderConsideration;
    }

    private boolean technologySpecificityEnabled;

    private boolean jointTargetImplemented;

    private boolean locationSpecificityEnabled;

    private boolean expostRevenueCalculation;

    private boolean revenueByAverageElectricityPrice;

    @SimulationParameter(label = "Support Scheme Duration", from = 0, to = 50)
    private long supportSchemeDuration;

    private long futureTenderOperationStartTime;

    private double yearlyTenderDemandTarget;

    private double expectedRenewableGeneration;

    // private long time;
    //
    private double annualExpectedConsumption;

    public double getAnnualExpectedConsumption() {
        return annualExpectedConsumption;
    }

    public void setAnnualExpectedConsumption(double annualExpectedConsumption) {
        this.annualExpectedConsumption = annualExpectedConsumption;
    }

    public boolean isExpostRevenueCalculation() {
        return expostRevenueCalculation;
    }

    public void setExpostRevenueCalculation(boolean expostRevenueCalculation) {
        this.expostRevenueCalculation = expostRevenueCalculation;
    }

    public double getYearlyTenderDemandTarget() {
        return yearlyTenderDemandTarget;
    }

    public void setYearlyTenderDemandTarget(double yearlyTenderDemandTarget) {
        this.yearlyTenderDemandTarget = yearlyTenderDemandTarget;
    }

    public long getFutureTenderOperationStartTime() {
        return futureTenderOperationStartTime;
    }

    public void setFutureTenderOperationStartTime(long futureTimePointTender) {
        this.futureTenderOperationStartTime = futureTimePointTender;
    }

    public double getExpectedRenewableGeneration() {
        return expectedRenewableGeneration;
    }

    public void setExpectedRenewableGeneration(double expectedRenewableGeneration) {
        this.expectedRenewableGeneration = expectedRenewableGeneration;
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

    public boolean isJointTargetImplemented() {
        return jointTargetImplemented;
    }

    public void setJointTargetImplemented(boolean jointTargetImplemented) {
        this.jointTargetImplemented = jointTargetImplemented;
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

    public boolean isRevenueByAverageElectricityPrice() {
        return revenueByAverageElectricityPrice;
    }

    public void setRevenueByAverageElectricityPrice(boolean revenueByAverageElectricityPrice) {
        this.revenueByAverageElectricityPrice = revenueByAverageElectricityPrice;
    }
    // public double getTotalExpectedConsumption() {
    // return totalExpectedConsumption;
    // }
    //
    // public void setTotalExpectedConsumption(double totalExpectedConsumption)
    // {
    // this.totalExpectedConsumption = totalExpectedConsumption;
    // }
    //
    // public long getTime() {
    // return time;
    // }
    //
    // public void setTime(long time) {
    // this.time = time;
    // }

    // public void specifyNotPersist(double totalExpectedConsumption, long
    // currentTime) {
    // this.setTotalExpectedConsumption(totalExpectedConsumption);
    // this.setTime(currentTime);
    //
    // }

    // /**
    // * @param plant
    // */
    //
    // // All transactional methods below are signified by starting with update
    // @Transactional
    // public void specifyAndPersist(double totalExpectedConsumption, long
    // currentTime) {
    // this.persist();
    // this.specifyNotPersist(totalExpectedConsumption, currentTime);
    //
    // } Consumer

}
