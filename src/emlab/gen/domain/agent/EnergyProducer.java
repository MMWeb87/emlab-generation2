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
package emlab.gen.domain.agent;

import java.util.HashMap;
import java.util.HashSet;

import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.role.investment.GenericInvestmentRole;

public class EnergyProducer extends EMLabAgent {

//    @RelatedTo(type = "PRODUCER_INVESTMENTROLE", elementClass = GenericInvestmentRole.class, direction = Direction.OUTGOING)
    GenericInvestmentRole<EnergyProducer> investmentRole;

//    @RelatedTo(type = "INVESTOR_MARKET", elementClass = ElectricitySpotMarket.class, direction = Direction.OUTGOING)
    private ElectricitySpotMarket investorMarket;
    private HashSet<ElectricitySpotMarket> potentialInvestorMarkets;

//    @SimulationParameter(label = "Price Mark-Up for spotmarket (as multiplier)", from = 1, to = 2)
    private double priceMarkUp;

//    @SimulationParameter(label = "Long-term contract margin", from = 0, to = 1)
    private double longTermContractMargin;

//    @SimulationParameter(label = "Long-term contract horizon", from = 0, to = 10)
    private double longTermContractPastTimeHorizon;

    //Investment
//    @SimulationParameter(label = "Investment horizon", from = 0, to = 15)
    private int investmentFutureTimeHorizon;
//    @SimulationParameter(label = "Equity Interest Rate", from = 0, to = 1)
    private double equityInterestRate;
    
    // for ex-post renewable policy scenarios where there is zero price risk
    // involved.
    private double equityRatePriceRiskComponent;
    
    private double downpaymentFractionOfCash;
//    @SimulationParameter(label = "Debt ratio in investments", from = 0, to = 1)
    private double debtRatioOfInvestments;
    private boolean willingToInvest;

    // Loan
//    @SimulationParameter(label = "Loan Interest Rate", from = 0, to = 1)
    private double loanInterestRate;

    //Forecasting
    private int numberOfYearsBacklookingForForecasting;

    // Dismantling
    private int dismantlingProlongingYearsAfterTechnicalLifetime;
    private double dismantlingRequiredOperatingProfit;
    private long pastTimeHorizon;

    // Historical CVar Parameters
    private double historicalCVarAlpha;
    private double historicalCVarBeta;
    private double historicalCVarPropensityForNewTechnologies;
    private double historicalCVarInterestRateIncreaseForNewTechnologies;
    private long historicalCvarBacklookingYears;
    private boolean historicalCvarCreateDummyPowerPlantsForNewTechnologies;
    
    // For investors with empirical preferences
    private HashMap<String, Double> utilityTechnology;
    private HashMap<String, Double> utilityReturn;
    private HashMap<String, Double> utilityCountry;
    private HashMap<String, Double> utilityPolicy;


    public boolean isWillingToInvest() {
        return willingToInvest;
    }

    public void setWillingToInvest(boolean willingToInvest) {
        this.willingToInvest = willingToInvest;
    }

    public double getDownpaymentFractionOfCash() {
        return downpaymentFractionOfCash;
    }

    public void setDownpaymentFractionOfCash(double downpaymentFractionOfCash) {
        this.downpaymentFractionOfCash = downpaymentFractionOfCash;
    }

    public double getLoanInterestRate() {
        return loanInterestRate;
    }

    public void setLoanInterestRate(double loanInterestRate) {
        this.loanInterestRate = loanInterestRate;
    }

    public long getPastTimeHorizon() {
        return pastTimeHorizon;
    }

    public void setPastTimeHorizon(long pastTimeHorizon) {
        this.pastTimeHorizon = pastTimeHorizon;
    }

    public int getNumberOfYearsBacklookingForForecasting() {
        return numberOfYearsBacklookingForForecasting;
    }

    public void setNumberOfYearsBacklookingForForecasting(int numberOfYearsBacklookingForForecasting) {
        this.numberOfYearsBacklookingForForecasting = numberOfYearsBacklookingForForecasting;
    }

    public int getDismantlingProlongingYearsAfterTechnicalLifetime() {
        return dismantlingProlongingYearsAfterTechnicalLifetime;
    }

    public void setDismantlingProlongingYearsAfterTechnicalLifetime(int dismantlingProlongingYearsAfterTechnicalLifetime) {
        this.dismantlingProlongingYearsAfterTechnicalLifetime = dismantlingProlongingYearsAfterTechnicalLifetime;
    }

    public double getDismantlingRequiredOperatingProfit() {
        return dismantlingRequiredOperatingProfit;
    }

    public void setDismantlingRequiredOperatingProfit(double dismantlingRequiredOperatingProfit) {
        this.dismantlingRequiredOperatingProfit = dismantlingRequiredOperatingProfit;
    }

    public int getInvestmentFutureTimeHorizon() {
        return investmentFutureTimeHorizon;
    }

    public void setInvestmentFutureTimeHorizon(int investmentFutureTimeHorizon) {
        this.investmentFutureTimeHorizon = investmentFutureTimeHorizon;
    }

    public double getEquityInterestRate() {
        return equityInterestRate;
    }

    public void setEquityInterestRate(double investmentDiscountRate) {
        this.equityInterestRate = investmentDiscountRate;
    }

    // TODO integrate in main EMLab?
    public double getEquityRatePriceRiskComponent() {
		return equityRatePriceRiskComponent;
	}

	public void setEquityRatePriceRiskComponent(double equityRatePriceRiskComponent) {
		this.equityRatePriceRiskComponent = equityRatePriceRiskComponent;
	}

	public double getLongTermContractMargin() {
        return longTermContractMargin;
    }

    public void setLongTermContractMargin(double longTermContractMargin) {
        this.longTermContractMargin = longTermContractMargin;
    }

    public double getLongTermContractPastTimeHorizon() {
        return longTermContractPastTimeHorizon;
    }

    public void setLongTermContractPastTimeHorizon(double longTermContractPastTimeHorizon) {
        this.longTermContractPastTimeHorizon = longTermContractPastTimeHorizon;
    }

    public double getDebtRatioOfInvestments() {
        return debtRatioOfInvestments;
    }

    public void setDebtRatioOfInvestments(double debtRatioOfInvestments) {
        this.debtRatioOfInvestments = debtRatioOfInvestments;
    }

    public double getPriceMarkUp() {
        return priceMarkUp;
    }

    public void setPriceMarkUp(double priceMarkUp) {
        this.priceMarkUp = priceMarkUp;
    }

    public GenericInvestmentRole getInvestmentRole() {
        return investmentRole;
    }

    public void setInvestmentRole(GenericInvestmentRole investmentRole) {
        this.investmentRole = investmentRole;
    }

    public ElectricitySpotMarket getInvestorMarket() {
        return investorMarket;
    }

    public void setInvestorMarket(ElectricitySpotMarket investorMarket) {
        this.investorMarket = investorMarket;
    }

    public HashSet<ElectricitySpotMarket> getPotentialInvestorMarkets() {
		return potentialInvestorMarkets;
	}

	public void setPotentialInvestorMarkets(HashSet<ElectricitySpotMarket> potentialInvestorMarkets) {
		this.potentialInvestorMarkets = potentialInvestorMarkets;
	}

	public double getHistoricalCVarAlpha() {
        return historicalCVarAlpha;
    }

    public void setHistoricalCVarAlpha(double historicalCVarAlpha) {
        this.historicalCVarAlpha = historicalCVarAlpha;
    }

    public double getHistoricalCVarBeta() {
        return historicalCVarBeta;
    }

    public void setHistoricalCVarBeta(double historicalCVarBeta) {
        this.historicalCVarBeta = historicalCVarBeta;
    }

    public double getHistoricalCVarPropensityForNewTechnologies() {
        return historicalCVarPropensityForNewTechnologies;
    }

    public void setHistoricalCVarPropensityForNewTechnologies(double historicalCVarPropensityForNewTechnologies) {
        this.historicalCVarPropensityForNewTechnologies = historicalCVarPropensityForNewTechnologies;
    }

    public double getHistoricalCVarInterestRateIncreaseForNewTechnologies() {
        return historicalCVarInterestRateIncreaseForNewTechnologies;
    }

    public void setHistoricalCVarInterestRateIncreaseForNewTechnologies(
            double historicalCVarInterestRateIncreaseForNewTechnologies) {
        this.historicalCVarInterestRateIncreaseForNewTechnologies = historicalCVarInterestRateIncreaseForNewTechnologies;
    }

    public long getHistoricalCvarBacklookingYears() {
        return historicalCvarBacklookingYears;
    }

    public void setHistoricalCvarBacklookingYears(long historicalCvarBacklookingYears) {
        this.historicalCvarBacklookingYears = historicalCvarBacklookingYears;
    }

    public boolean isHistoricalCvarCreateDummyPowerPlantsForNewTechnologies() {
        return historicalCvarCreateDummyPowerPlantsForNewTechnologies;
    }

    public void setHistoricalCvarCreateDummyPowerPlantsForNewTechnologies(
            boolean historicalCvarCreateDummyPowerPlantsForNewTechnologies) {
        this.historicalCvarCreateDummyPowerPlantsForNewTechnologies = historicalCvarCreateDummyPowerPlantsForNewTechnologies;
    }
    
    
    public HashMap<String, Double> getUtilityTechnology() {
        return utilityTechnology;
    }

    public void setUtilityTechnology(HashMap<String, Double> utilityTechnology) {
                
        this.utilityTechnology = utilityTechnology;
    }

	public HashMap<String, Double> getUtilityReturn() {
		return utilityReturn;
	}

	public void setUtilityReturn(HashMap<String, Double> utilityReturn) {
		this.utilityReturn = utilityReturn;
	}

	public HashMap<String, Double> getUtilityCountry() {
		return utilityCountry;
	}

	public void setUtilityCountry(HashMap<String, Double> utilityCountry) {
		this.utilityCountry = utilityCountry;
	}

	public HashMap<String, Double> getUtilityPolicy() {
		return utilityPolicy;
	}

	public void setUtilityPolicy(HashMap<String, Double> utilityPolicy) {
		this.utilityPolicy = utilityPolicy;
	}
    
}
