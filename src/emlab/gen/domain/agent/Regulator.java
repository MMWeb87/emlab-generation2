/*******************************************************************************
 * Copyright 2015 the original author or authors.
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
package emlab.gen.domain.agent;

import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.agent.EMLabAgent;
import emlab.gen.engine.Agent;


/**
 * @author Kaveri
 * @author marcmelliger
 * 
 */

public class Regulator extends EMLabAgent implements Agent {

    private Zone zone;

    // Investment related parameters
    private int numberOfYearsLookingBackToForecastDemand;
    private double equityInterestRate;

    // this parameter is to indicate price risk component of equity rate, in
    // particular for renewable support policies, of the ex-post type
    private double equityRatePriceRiskComponent;

    private double debtRatioOfInvestments;
    // Loan
    private double loanInterestRate;

    // Capacity Market Related Parameters

    private double capacityMarketPriceCap;

    private double reserveMargin;

    private double reserveDemandLowerMargin;

    private double reserveDemandUpperMargin;

    private double demandTarget;

    // Capacity Market Target Period
    private int targetPeriod; // number of years in the future that the capacity
    // is being planned for - set to zero

    // Feed-in-Premium Related Parameters
    private double feedInPremiumFactor;

    // Tender parameters
    // moved to RenewableSupportSchemeTender

    public double getEquityInterestRate() {
        return equityInterestRate;
    }

    public void setEquityInterestRate(double equityInterestRate) {
        this.equityInterestRate = equityInterestRate;
    }

    public double getEquityRatePriceRiskComponent() {
        return equityRatePriceRiskComponent;
    }

    public void setEquityRatePriceRiskComponent(double equityRatePriceRiskComponent) {
        this.equityRatePriceRiskComponent = equityRatePriceRiskComponent;
    }

    public double getDebtRatioOfInvestments() {
        return debtRatioOfInvestments;
    }

    public void setDebtRatioOfInvestments(double debtRatioOfInvestments) {
        this.debtRatioOfInvestments = debtRatioOfInvestments;
    }

    public double getLoanInterestRate() {
        return loanInterestRate;
    }

    public void setLoanInterestRate(double loanInterestRate) {
        this.loanInterestRate = loanInterestRate;
    }

    public double getDemandTarget() {
        return demandTarget;
    }

    public void setDemandTarget(double demandTarget) {
        this.demandTarget = demandTarget;
    }

    public double getCapacityMarketPriceCap() {
        return capacityMarketPriceCap;
    }

    public void setCapacityMarketPriceCap(double capacityMarketPriceCap) {
        this.capacityMarketPriceCap = capacityMarketPriceCap;
    }

    public int getNumberOfYearsLookingBackToForecastDemand() {
        return numberOfYearsLookingBackToForecastDemand;
    }

    public void setNumberOfYearsLookingBackToForecastDemand(int numberOfYearsLookingBackToForecastDemand) {
        this.numberOfYearsLookingBackToForecastDemand = numberOfYearsLookingBackToForecastDemand;
    }

    public int getTargetPeriod() {
        return targetPeriod;
    }

    public double getFeedInPremiumFactor() {
        return feedInPremiumFactor;
    }

    public void setFeedInPremiumFactor(double feedInPremiumFactor) {
        this.feedInPremiumFactor = feedInPremiumFactor;
    }

    public void setTargetPeriod(int targetPeriod) {
        this.targetPeriod = targetPeriod;
    }

    public double getReserveDemandLowerMargin() {
        return reserveDemandLowerMargin;
    }

    public void setReserveDemandLowerMargin(double reserveDemandLowerMargin) {
        this.reserveDemandLowerMargin = reserveDemandLowerMargin;
    }

    public double getReserveDemandUpperMargin() {
        return reserveDemandUpperMargin;
    }

    public void setReserveDemandUpperMargin(double reserveDemandUpperMargin) {
        this.reserveDemandUpperMargin = reserveDemandUpperMargin;
    }

    public double getReserveMargin() {
        return reserveMargin;
    }

    public void setReserveMargin(double reserveMargin) {
        this.reserveMargin = reserveMargin;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

}
