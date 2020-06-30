package emlab.gen.role.investment;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.Schedule;

public class FinancialExpectationReport {

	
    long time;
    
    long iteration;
    
    public Schedule schedule;
    
    private ElectricitySpotMarket market;
    
    private EnergyProducer agent;
    
    private PowerGeneratingTechnology technology;
    
    private PowerPlant plant;
    
    private PowerGridNode node;

        
    private double projectReturnOnInvestment;
    private double projectReturnOnEquity;
    private double debtRatioOfInvestments;
    private double discountedCapitalCosts;
    private double discountedOperatingCost;
    private double discountedOperatingProfit;
    private double expectedGeneration;
    private double expectedGrossProfit;
    private double expectedMarginalCost;
    private double expectedOperatingCost;
    private double expectedOperatingRevenue;
    private double projectCost;
    private double projectValue;
    private double runningHours;
    private double wacc;
    private double totalUtility;

	private int investmentRound;
   
	
    public EnergyProducer getAgent() {
		return agent;
	}

	public void setAgent(EnergyProducer agent) {
		this.agent = agent;
	}

	public long getIteration() {
        return iteration;
    }
    
	
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	
	public ElectricitySpotMarket getMarket() {
		return market;
	}

	public void setMarket(ElectricitySpotMarket market) {
		this.market = market;
	}

	public PowerPlant getPlant() {
		return plant;
	}

	public void setPlant(PowerPlant plant) {
		this.plant = plant;
	}

	public PowerGridNode getNode() {
		return node;
	}

	public void setNode(PowerGridNode node) {
		this.node = node;
	}

	public double getProjectReturnOnInvestment() {
		return projectReturnOnInvestment;
	}

	public void setProjectReturnOnInvestment(double projectReturnOnInvestment) {
		this.projectReturnOnInvestment = projectReturnOnInvestment;
	}

	public double getProjectReturnOnEquity() {
		return projectReturnOnEquity;
	}

	public void setProjectReturnOnEquity(double projectReturnOnEquity) {
		this.projectReturnOnEquity = projectReturnOnEquity;
	}

	public double getDebtRatioOfInvestments() {
		return debtRatioOfInvestments;
	}

	public void setDebtRatioOfInvestments(double debtRatioOfInvestments) {
		this.debtRatioOfInvestments = debtRatioOfInvestments;
	}

	public double getDiscountedCapitalCosts() {
		return discountedCapitalCosts;
	}

	public void setDiscountedCapitalCosts(double discountedCapitalCosts) {
		this.discountedCapitalCosts = discountedCapitalCosts;
	}

	public double getDiscountedOperatingCost() {
		return discountedOperatingCost;
	}

	public void setDiscountedOperatingCost(double discountedOperatingCost) {
		this.discountedOperatingCost = discountedOperatingCost;
	}

	public double getDiscountedOperatingProfit() {
		return discountedOperatingProfit;
	}

	public void setDiscountedOperatingProfit(double discountedOperatingProfit) {
		this.discountedOperatingProfit = discountedOperatingProfit;
	}

	public double getExpectedGeneration() {
		return expectedGeneration;
	}

	public void setExpectedGeneration(double expectedGeneration) {
		this.expectedGeneration = expectedGeneration;
	}

	public double getExpectedGrossProfit() {
		return expectedGrossProfit;
	}

	public void setExpectedGrossProfit(double expectedGrossProfit) {
		this.expectedGrossProfit = expectedGrossProfit;
	}

	public double getExpectedMarginalCost() {
		return expectedMarginalCost;
	}

	public void setExpectedMarginalCost(double expectedMarginalCost) {
		this.expectedMarginalCost = expectedMarginalCost;
	}

	public double getExpectedOperatingCost() {
		return expectedOperatingCost;
	}

	public void setExpectedOperatingCost(double expectedOperatingCost) {
		this.expectedOperatingCost = expectedOperatingCost;
	}

	public double getExpectedOperatingRevenue() {
		return expectedOperatingRevenue;
	}

	public void setExpectedOperatingRevenue(double expectedOperatingRevenue) {
		this.expectedOperatingRevenue = expectedOperatingRevenue;
	}

	public double getProjectCost() {
		return projectCost;
	}

	public void setProjectCost(double projectCost) {
		this.projectCost = projectCost;
	}

	public double getProjectValue() {
		return projectValue;
	}

	public void setProjectValue(double projectValue) {
		this.projectValue = projectValue;
	}

	public double getRunningHours() {
		return runningHours;
	}

	public void setRunningHours(double runningHours) {
		this.runningHours = runningHours;
	}

	public PowerGeneratingTechnology getTechnology() {
		return technology;
	}

	public void setTechnology(PowerGeneratingTechnology technology) {
		this.technology = technology;
	}

	public double getWacc() {
		return wacc;
	}

	public void setWacc(double wacc) {
		this.wacc = wacc;
	}

	public double getTotalUtility() {
		return totalUtility;
	}

	public void setTotalUtility(double totalUtility) {
		this.totalUtility = totalUtility;
	}

	public void setInvestmentRound(int investmentRound) {
		this.investmentRound = investmentRound;	
	}

	public int getInvestmentRound() {
		return investmentRound;
	} 

}
