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


/**
 * @author kaveri
 * 
 *         This object serves as a means to store information about
 *         energyProducers and regulator's forecasted revenue, and electricity
 *         market price.
 * 
 *         Can be modified to store any other forecasting information computed
 *         by any decarbonizationAgent
 *
 */

public class ForecastingInformationReport {

    long tick;

    long forecastingForTick;

    private String agent;

    private String technologyName;

    private String nodeName;

    private double projectValuePerMwWithoutSubsidy;

    private double projectValuePerMwWithSubsidy;

    private double expectedOpRevenueElectricityMarketWithSubsidy;

    private double expectedOpRevenueElectricityMarketWithoutSubsidy;

    private double expectedAnnualGeneration;

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public double getExpectedOpRevenueElectricityMarketWithoutSubsidy() {
        return expectedOpRevenueElectricityMarketWithoutSubsidy;
    }

    public void setExpectedOpRevenueElectricityMarketWithoutSubsidy(
            double expectedOpRevenueElectricityMarketWithoutSubsidy) {
        this.expectedOpRevenueElectricityMarketWithoutSubsidy = expectedOpRevenueElectricityMarketWithoutSubsidy;
    }

    public double getProjectValuePerMwWithoutSubsidy() {
        return projectValuePerMwWithoutSubsidy;
    }

    public void setProjectValuePerMwWithoutSubsidy(double projectValuePerMwWithoutSubsidy) {
        this.projectValuePerMwWithoutSubsidy = projectValuePerMwWithoutSubsidy;
    }

    public double getProjectValuePerMwWithSubsidy() {
        return projectValuePerMwWithSubsidy;
    }

    public void setProjectValuePerMwWithSubsidy(double projectValuePerMwWithSubsidy) {
        this.projectValuePerMwWithSubsidy = projectValuePerMwWithSubsidy;
    }

    public double getExpectedOpRevenueElectricityMarketWithSubsidy() {
        return expectedOpRevenueElectricityMarketWithSubsidy;
    }

    public void setExpectedOpRevenueElectricityMarketWithSubsidy(double expectedOpRevenueElectricityMarketWithSubsidy) {
        this.expectedOpRevenueElectricityMarketWithSubsidy = expectedOpRevenueElectricityMarketWithSubsidy;
    }

    public String getTechnologyName() {
        return technologyName;
    }

    public void setTechnologyName(String technologyName) {
        this.technologyName = technologyName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public double getExpectedAnnualGeneration() {
        return expectedAnnualGeneration;
    }

    public void setExpectedAnnualGeneration(double expectedAnnualGeneration) {
        this.expectedAnnualGeneration = expectedAnnualGeneration;
    }

    public long getForecastingForTick() {
        return forecastingForTick;
    }

    public void setForecastingForTick(long forecastingForTick) {
        this.forecastingForTick = forecastingForTick;
    }


}
