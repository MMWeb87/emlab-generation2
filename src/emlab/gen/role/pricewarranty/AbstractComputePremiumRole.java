package emlab.gen.role.pricewarranty;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.stat.regression.SimpleRegression;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.CO2Auction;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.renewablesupport.BiasFactor;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.policy.renewablesupport.RenewableTarget;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.engine.Role;
import emlab.gen.engine.Schedule;
import emlab.gen.repository.Reps;
import emlab.gen.role.AbstractEnergyProducerRole;

public abstract class AbstractComputePremiumRole extends AbstractEnergyProducerRole<EnergyProducer> implements Role<EnergyProducer>{
	

	Reps reps;

	public AbstractComputePremiumRole(Schedule schedule) {
	    super(schedule);
	}
	
    public void act(EnergyProducer arg0) {
        // DO Nothing
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

        ElectricitySpotMarket market = reps.findElectricitySpotMarketForZone(zone);

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
            target = reps
                    .findTechnologyNeutralRenewableTargetForTenderByRegulator(scheme.getRegulator());
        } else {
            target = reps.findTechnologySpecificRenewableTargetForTenderByRegulator(
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
        ElectricitySpotMarket market = reps
                .findElectricitySpotMarketForZone(scheme.getRegulator().getZone());

        if (scheme.isTechnologySpecificityEnabled() == false) {
            for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {
                expectedGenerationPerTechnology = 0d;
                for (PowerPlant plant : reps.findOperationalPowerPlantsByMarketAndTechnology(
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
            for (PowerPlant plant : reps.findOperationalPowerPlantsByMarketAndTechnology(market,
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




}
