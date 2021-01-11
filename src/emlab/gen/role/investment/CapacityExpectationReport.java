package emlab.gen.role.investment;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.Schedule;

public class CapacityExpectationReport {

	
    long time;
    
    long iteration;
    
    public Schedule schedule;
    
    private ElectricitySpotMarket market;
    
    private EnergyProducer agent;
    
    private PowerGeneratingTechnology technology;
    
    private PowerPlant plant;
    
    private PowerGridNode node;

        
    private boolean viable;
    private double viableReason;
   
	
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



	public PowerGeneratingTechnology getTechnology() {
		return technology;
	}

	public void setTechnology(PowerGeneratingTechnology technology) {
		this.technology = technology;
	}
	
	

	public boolean getViable() {
		return viable;
	}

	public void setViable(boolean viable) {
		this.viable = viable;
	}

	public double getViableReason() {
		return viableReason;
	}

	public void setViableReason(double viableReason) {
		this.viableReason = viableReason;
	}


}
