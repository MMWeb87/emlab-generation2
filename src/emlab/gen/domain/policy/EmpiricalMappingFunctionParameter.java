package emlab.gen.domain.policy;

import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.technology.PowerGeneratingTechnology;

public class EmpiricalMappingFunctionParameter {
		
	private double modelledRoeMax; 
	private double modelledRoeMin;
	private double slope;
	private double intercept;
	private ElectricitySpotMarket market;
	private PowerGeneratingTechnology technology;
	
	
	public double getModelledRoeMax() {
		return modelledRoeMax;
	}
	public void setModelledRoeMax(double modelledRoeMax) {
		this.modelledRoeMax = modelledRoeMax;
	}
	public double getModelledRoeMin() {
		return modelledRoeMin;
	}
	public void setModelledRoeMin(double modelledRoeMin) {
		this.modelledRoeMin = modelledRoeMin;
	}
	public double getSlope() {
		return slope;
	}
	public void setSlope(double slope) {
		this.slope = slope;
	}
	public double getIntercept() {
		return intercept;
	}
	public void setIntercept(double intercept) {
		this.intercept = intercept;
	}
	public ElectricitySpotMarket getMarket() {
		return market;
	}
	public void setMarket(ElectricitySpotMarket market) {
		this.market = market;
	}
	public PowerGeneratingTechnology getTechnology() {
		return technology;
	}
	public void setTechnology(PowerGeneratingTechnology technology) {
		this.technology = technology;
	}


	
}
