package emlab.gen.role.pricewarranty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

import emlab.gen.domain.agent.EMLabModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.renewablesupport.BaseCostFip;
import emlab.gen.domain.policy.renewablesupport.BiasFactor;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.policy.renewablesupport.RenewableTarget;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;
import emlab.gen.role.AbstractEnergyProducerRole;
import emlab.gen.role.investment.InvestInPowerGenerationTechnologiesWithTenderRole;
import emlab.gen.trend.TimeSeriesCSVReader;
import emlab.gen.util.MapValueComparator;

public abstract class AbstractComputePremiumRole extends AbstractEnergyProducerRole<EnergyProducer> implements Role<EnergyProducer>{
	
	private double costPerMWh;
	
	public AbstractComputePremiumRole(Schedule schedule) {
	    super(schedule);
	}
	
   public class Key2D {
        private final PowerGeneratingTechnology techKey;
        private final PowerGridNode nodeKey;

        public Key2D(PowerGeneratingTechnology key1, PowerGridNode key2) {
            this.techKey = key1;
            this.nodeKey = key2;
        }
    }
    
    class EvaluateInvestmentRole extends InvestInPowerGenerationTechnologiesWithTenderRole<EnergyProducer>{
    	
  		public FutureFinancialExpectationWithScheme financialExpectation;

  		
  		public EvaluateInvestmentRole(Schedule schedule) {
  			super(schedule);
  		}
  		
  		@Override	
  		public void initEvaluationForEnergyProducer(EnergyProducer agent, ElectricitySpotMarket market) {
  			
            evaluateInvestment.setFutureTimePoint(futureTimePoint);
            evaluateInvestment.setMarket(market);
        	evaluateInvestment.setScheme(scheme);
            evaluateInvestment.setAgent(agent);
            evaluateInvestment.setExpectations();      
  			
  		}

  		
  		public void createFinancialExpectationWithScheme(PowerPlant plant) {
  			
  			financialExpectation = new FutureFinancialExpectationWithScheme(plant);
  			
  		}

      }
    
  	protected EvaluateInvestmentRole evaluateInvestment;

	
    public void act(EnergyProducer arg0) {
        // DO Nothing
    }
    
    public void act(RenewableSupportFipScheme scheme) {
        
    	evaluateInvestment = new EvaluateInvestmentRole(schedule);
    	evaluateInvestment.setScheme(scheme);
        evaluateInvestment.setUseFundamentalCO2Forecast(true);

    	//ForecastingInformationReport fReport;
        Regulator regulator = scheme.getRegulator();
    	long futureTimePoint = getCurrentTick() + scheme.getFutureSchemeStartTime();
        ElectricitySpotMarket market = getReps().findElectricitySpotMarketForZone(regulator.getZone());
        Iterable<PowerGeneratingTechnology> eligibleTechnologies = scheme.getPowerGeneratingTechnologiesEligible();
        Map<Key2D, Double> baseCostMap = new HashMap<Key2D, Double>();

        for (PowerGeneratingTechnology technology : eligibleTechnologies) {

            EMLabModel model = getReps().emlabModel;
            if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                continue;

            for (PowerGridNode node : getReps().findAllPowerGridNodesByZone(regulator.getZone())) {
            	
                // TODO MM CHECK
                // Why a random Producer? They influence soutcomes by waccs, e.g, so it matters!
                // And why for every node a new one? Is slow, but more randomness?
                EnergyProducer randomProducer =  getReps().energyProducers.iterator().next();

                evaluateInvestment.initEvaluationForEnergyProducer(randomProducer, market);
                PowerPlant plant = evaluateInvestment.createPowerPlant(technology, node);                                
                evaluateInvestment.createFinancialExpectationWithScheme(plant);
                evaluateInvestment.financialExpectation.calculateDiscountedValues();
                                
                BiasFactor biasFactor = getReps()
                        .findBiasFactorGivenTechnologyNodeAndScheme(technology.getName(), node.getName(), scheme);

                if (scheme.isCostContainmentMechanismEnabled() && scheme.isTechnologySpecificityEnabled() == false) {
                    computeDegressionAndResetBiasFactor(scheme, biasFactor, null);
                } else if (scheme.isCostContainmentMechanismEnabled()
                        && scheme.isTechnologySpecificityEnabled() == true) {
                    computeDegressionAndResetBiasFactor(scheme, biasFactor, technology);
                }
                double biasFactorValue = biasFactor.getFeedInPremiumBiasFactor();
                
                double totalGenerationinMWh = plant.getAnnualFullLoadHours() * plant.getActualNominalCapacity();
                
                double factorDiscountedGeneration = evaluateInvestment.financialExpectation.getDiscountedGeneration();
                double generation = totalGenerationinMWh * factorDiscountedGeneration;

                calculateCostPerMWh(biasFactorValue, generation, plant);       
                
            	// TODO add reporting again?
				//fReport = new ForecastingInformationReport();
				//fReport.setTick(getCurrentTick());
				//fReport.setForecastingForTick(getCurrentTick() + scheme.getFutureSchemeStartTime());
				//fReport.setAgent(regulator.getName());
				//fReport.setProjectValuePerMwWithoutSubsidy(projectValue / plant.getActualNominalCapacity());
				//fReport.setExpectedOpRevenueElectricityMarketWithoutSubsidy(operatingRevenue);
				//fReport.setTechnologyName(technology.getName());
				//fReport.setNodeName(node.getName());
				//fReport.setExpectedAnnualGeneration(totalAnnualExpectedGenerationOfPlant);
				//fReport.setProjectValuePerMwWithSubsidy(0);
				//fReport.setExpectedOpRevenueElectricityMarketWithSubsidy(0);           
                
                if (scheme.isTechnologySpecificityEnabled() == true) {
                    BaseCostFip baseCostFip = new BaseCostFip();

                    baseCostFip.setCostPerMWh(getCostPerMWh());
                    baseCostFip.setStartTime(futureTimePoint);
                    baseCostFip.setNode(node);
                    baseCostFip.setTechnology(technology);
                    baseCostFip.setEndTime(futureTimePoint + scheme.getSupportSchemeDuration());
                    
                    // TODO MM: So does this assume that different producers will the same base costs for different projects?
                    // Namely does of the random Agent here?
                    logger.log(Level.FINER, "Creating BaseCost object: technology " +
                    		technology.getName() + "premium " + getCostPerMWh());
                    
                    getReps().baseCostFips.add(baseCostFip);
                    
                } else {

                    logger.log(Level.FINER, "Creating base cost map: technology " + technology.getName() + "premium " + getCostPerMWh());
                    baseCostMap.put(new Key2D(technology, node), getCostPerMWh());
                }

            }
        }

        if (scheme.isTechnologySpecificityEnabled() == false) {

            MapValueComparator comp = new MapValueComparator(baseCostMap);
            TreeMap<Key2D, Double> meritOrderBaseCost = new TreeMap<Key2D, Double>(comp);
            meritOrderBaseCost.putAll(baseCostMap);
            // logger.warn("Technology Cost Map" + baseCostMap);

            double renewableGenerationAccepted = 0d;
            double baseCostFipTechNeutral = 0d;
            // double sumOfPotentialsAccepted = 0d;

            double renewableTargetInMwh = computeRenewableGenerationTarget(scheme, null);

            logger.log(Level.FINER, "Actual Target for tick " + futureTimePoint + "in MWh is " + renewableTargetInMwh);

            for (Entry<Key2D, Double> technologyCost : meritOrderBaseCost.entrySet()) {
                Key2D baseCostKey = technologyCost.getKey();
                PowerGeneratingTechnology technology = baseCostKey.techKey;
                PowerGridNode node = baseCostKey.nodeKey;
                double technologyPotential = 0d;
                ArrayList<PowerGridNode> nodeList = new ArrayList<PowerGridNode>();
                nodeList.add(node);

                boolean nodeUnique = (Collections.frequency(nodeList, node) == 1) ? true : false;

                TimeSeriesCSVReader technologyPotentialTS = getReps()
                        .findTechnologySpecificRenewablePotentialLimitTimeSeriesByRegulator(scheme.getRegulator(),
                                technology.getName());
                 
                if(technologyPotentialTS != null) {
                	technologyPotential = technologyPotentialTS.getValue(futureTimePoint);
                } else {
                	technologyPotential = Double.MAX_VALUE;
                	logger.log(Level.WARNING, "No technologyPotentialLimit set for " + technology);
                }
                	

                // This condition will end the loop, and hence adding baseCostsFips to reps when renewableTarget is met.
                // The maximum amount of technologies added is given by potential
                
                if ((renewableTargetInMwh - (renewableGenerationAccepted + technologyPotential) > 0)) {
                	// Target not reached. Add as much of current tech as there is potential.
                	
                    if ((!technology.isIntermittent() && nodeUnique) || technology.isIntermittent()) {
                        renewableGenerationAccepted += technologyPotential;
                        baseCostFipTechNeutral = technologyCost.getValue();
                    }

                    // logger.warn("If condition 1");
                
                } else if (renewableTargetInMwh - (renewableGenerationAccepted + technologyPotential) <= 0) {
                	// Limit Target reached, potentialLimit not. Add current tech until target reached and end loop
                	
                    if ((!technology.isIntermittent() && nodeUnique) || technology.isIntermittent()) {
                        renewableGenerationAccepted += (renewableTargetInMwh - renewableGenerationAccepted);
                        baseCostFipTechNeutral = technologyCost.getValue();
                        // logger.warn("If condition 2");
                        break;
                    }
                }
            }

            // logger.warn("renewable generation accepted is" +
            // renewableGenerationAccepted + "fip set is "
            // + baseCostFipTechNeutral);
            BaseCostFip baseCostFip = new BaseCostFip();

            baseCostFip.setCostPerMWh(baseCostFipTechNeutral);
            baseCostFip.setStartTime(futureTimePoint);
            // baseCostFip.setNode(node);
            // baseCostFip.setTechnology(technology);
            baseCostFip.setEndTime(futureTimePoint + scheme.getSupportSchemeDuration());
            
            getReps().baseCostFips.add(baseCostFip);
        }

    }
    
    protected void calculateCostPerMWh(double biasFactorValue, double generation, PowerPlant plant) {
    	//implement in classes 
    	logger.log(Level.SEVERE, "method need to be implemented in concrete class");
    }

	

    
    protected void computeDegressionAndResetBiasFactor(RenewableSupportFipScheme scheme, BiasFactor biasFactor,
            PowerGeneratingTechnology technology) {

        // get target value of renewable generation
        double renewableTargetInMwh = 0d; // computeRenewableGenerationTarget(scheme);
        double generationFromRenewables = 0d; // totalExpectedGenerationFromRenewables(scheme);

        double degressionFactor = biasFactor.getDegressionFactor();
        double newBiasFactor = 0d;

        if (scheme.isTechnologySpecificityEnabled()) {
            renewableTargetInMwh = computeRenewableGenerationTarget(scheme, technology);
            generationFromRenewables = totalExpectedGenerationFromRenewables(scheme, technology);
        }

        if (generationFromRenewables > renewableTargetInMwh) {
            newBiasFactor = biasFactor.getFeedInPremiumBiasFactor() * (1 - degressionFactor);
            biasFactor.setFeedInPremiumBiasFactor(newBiasFactor);
            // logger.warn("DEGRESSING!!!");
        } else if (generationFromRenewables < renewableTargetInMwh) {
            newBiasFactor = biasFactor.getFeedInPremiumBiasFactor() * (1 - degressionFactor);
            biasFactor.setFeedInPremiumBiasFactor(newBiasFactor);
        }
        // if expected generation exceeds target, degress by a certain
        // percentage.
        // else if expected generation is lower than a certain margin, increase
        // bias factor. - will have to create targetLowerMargin and
        // targetUpperMargin for it, best created in target object.

    }

    protected double computeRenewableGenerationTarget(RenewableSupportFipScheme scheme,
            PowerGeneratingTechnology technology) {
        double demandFactor;
        double targetFactor;
        Zone zone = scheme.getRegulator().getZone();

        // logger.warn("Calculate Renewable Target Role started of zone: " +
        // zone);

        ElectricitySpotMarket market = getReps().findElectricitySpotMarketForZone(zone);

        // Assuming perfect knowledge of demand
        demandFactor = market.getDemandGrowthTrend().getValue(getCurrentTick() + scheme.getFutureSchemeStartTime());
        /*
         * it aggregates segments from both countries, so the boolean should
         * actually be true here and the code adjusted to FALSE case. Or a query
         * should be adjusted what probably will take less time.
         */

        RenewableTarget target = new RenewableTarget();
        // get renewable energy target in factor (percent)
        if (scheme.isTechnologySpecificityEnabled() == false) {
            target = getReps()
                    .findTechnologyNeutralRenewableTargetForTenderByRegulator(scheme.getRegulator());
        } else {
            target = getReps().findTechnologySpecificRenewableTargetForTenderByRegulator(
                    scheme.getRegulator(), technology.getName());
        }

        // logger.warn("Renewable Target is " + target);

        targetFactor = target.getYearlyRenewableTargetTimeSeries()
                .getValue(getCurrentTick() + scheme.getFutureSchemeStartTime());
                // logger.warn("targetFactor is " + targetFactor);

        // get totalLoad in MWh
        double totalExpectedConsumption = 0d;

        for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
            // logger.warn("segmentLoad: " + segmentLoad);
            totalExpectedConsumption += segmentLoad.getBaseLoad() * demandFactor
                    * segmentLoad.getSegment().getLengthInHours();

            // logger.warn("demand factor is: " + demandFactor);

        }

        double renewableTargetInMwh = targetFactor * totalExpectedConsumption;

        return renewableTargetInMwh;
    }


    protected double totalExpectedGenerationFromRenewables(RenewableSupportFipScheme scheme,
            PowerGeneratingTechnology technologySpecified) {

        double totalExpectedGeneration = 0d;
        double expectedGenerationPerTechnology = 0d;
        double expectedGenerationPerPlant = 0d;
        long numberOfSegments = getReps().segments.size();
        // logger.warn("numberOfsegments: " + numberOfSegments);
        ElectricitySpotMarket market = getReps()
                .findElectricitySpotMarketForZone(scheme.getRegulator().getZone());

        if (scheme.isTechnologySpecificityEnabled() == false) {
            for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {
                expectedGenerationPerTechnology = 0d;
                for (PowerPlant plant : getReps().findOperationalPowerPlantsByMarketAndTechnology(
                        market, technology, getCurrentTick() + scheme.getFutureSchemeStartTime())) {
                    expectedGenerationPerPlant = 0d;
                    for (Segment segment : getReps().segments) {
                        double availablePlantCapacity = plant.getAvailableCapacity(
                                getCurrentTick() + scheme.getFutureSchemeStartTime(), segment, numberOfSegments);
                        double lengthOfSegmentInHours = segment.getLengthInHours();
                        expectedGenerationPerPlant += availablePlantCapacity * lengthOfSegmentInHours;
                    }
                    expectedGenerationPerTechnology += expectedGenerationPerPlant;
                }
                totalExpectedGeneration += expectedGenerationPerTechnology;

            }
        } else {
            for (PowerPlant plant : getReps().findOperationalPowerPlantsByMarketAndTechnology(market,
                    technologySpecified, getCurrentTick() + scheme.getFutureSchemeStartTime())) {
                expectedGenerationPerPlant = 0d;
                for (Segment segment : getReps().segments) {
                    double availablePlantCapacity = plant.getAvailableCapacity(
                            getCurrentTick() + scheme.getFutureSchemeStartTime(), segment, numberOfSegments);
                    double lengthOfSegmentInHours = segment.getLengthInHours();
                    expectedGenerationPerPlant += availablePlantCapacity * lengthOfSegmentInHours;
                }
                expectedGenerationPerTechnology += expectedGenerationPerPlant;
            }
            totalExpectedGeneration += expectedGenerationPerTechnology;
        }
        return totalExpectedGeneration;
    }

	public double getCostPerMWh() {
		return costPerMWh;
	}

	public void setCostPerMWh(double costPerMWh) {
		this.costPerMWh = costPerMWh;
	}




}
