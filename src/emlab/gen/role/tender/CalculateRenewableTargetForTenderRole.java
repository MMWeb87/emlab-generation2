package emlab.gen.role.tender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.PowerPlantDispatchPlan;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.repository.Reps;
import emlab.gen.util.GeometricTrendRegression;

/**
 * @author Kaveri, rjjdejeu
 *
 */

@RoleComponent
public class CalculateRenewableTargetForTenderRole extends AbstractRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {

    @Autowired
    Reps reps;

    @Override
    @Transactional
    public void act(RenewableSupportSchemeTender scheme) {

        long futureStartingTenderTimePoint = getCurrentTick() + scheme.getFutureTenderOperationStartTime();
        double demandFactor;
        double targetFactor;
        double targetFactorAchievementForecast;
        Zone zone = scheme.getRegulator().getZone();

        // logger.warn("Calculate Renewable Target Role started of zone: " +
        // zone);

        ElectricitySpotMarket market = reps.marketRepository.findElectricitySpotMarketForZone(zone);

        // get demand factor
        demandFactor = predictDemandForElectricitySpotMarket(market,
                scheme.getRegulator().getNumberOfYearsLookingBackToForecastDemand(), futureStartingTenderTimePoint);

        // logger.warn("regulator name" + scheme.getRegulator().getName());
        // logger.warn("calculate technology name" +
        // scheme.getPowerGeneratingTechnologiesEligible().iterator().next().getName());

        if (scheme.isTechnologySpecificityEnabled()) {

            PowerGeneratingTechnology technology = scheme.getPowerGeneratingTechnologiesEligible().iterator().next();
            targetFactor = reps.renewableTargetForTenderRepository
                    .findTechnologySpecificRenewableTargetTimeSeriesForTenderByScheme(scheme, technology.getName())
                    .getValue(getCurrentTick() + scheme.getFutureTenderOperationStartTime());
            // targetFactorAchievementForecast =
            // getForecastedRenewableGeneration(scheme, technology);
        } else {
            targetFactor = reps.renewableTargetForTenderRepository
                    .findTechnologyNeutralRenewableTargetForTenderByRegulator(scheme.getRegulator())
                    .getYearlyRenewableTargetTimeSeries()
                    .getValue(getCurrentTick() + scheme.getFutureTenderOperationStartTime());
            // targetFactorAchievementForecast =
            // getForecastedRenewableGeneration(scheme, null);

        }

        // get totalLoad in MWh
        double totalExpectedConsumption = 0d;

        for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
            totalExpectedConsumption += segmentLoad.getBaseLoad() * demandFactor
                    * segmentLoad.getSegment().getLengthInHours();

        }

        scheme.setAnnualExpectedConsumption(totalExpectedConsumption);
        // logger.warn("totalExpectedConsumption; " + totalExpectedConsumption);

        // renewable target for tender operation start year in MWh is

        double renewableTargetInMwh = targetFactor * totalExpectedConsumption;
        // logger.warn("Policy based (total ) renewableTargetInMwh; " +
        // renewableTargetInMwh + "for scheme "
        // + scheme.getName());

        // calculate expected generation, and subtract that from annual
        // target.
        // will be ActualTarget

        double totalExpectedGenerationAvailable = 0d;
        double expectedGenerationPerTechnologyAvailable = 0d;

        for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {
            expectedGenerationPerTechnologyAvailable = 0d;

            // logger.warn("For PGT - technology; " + technology);
            // logger.warn("For PGT - technology; " + technology);
            scheme.setCurrentTechnologyUnderConsideration(technology);

            // expectedGenerationPerTechnologyAvailable =
            // computeGenerationFromRenUsingPPDP(technology, market,
            // futureStartingTenderTimePoint);

            expectedGenerationPerTechnologyAvailable = computeRenGenerationUsingExpectedGeneration(technology, market,
                    futureStartingTenderTimePoint);
            totalExpectedGenerationAvailable += expectedGenerationPerTechnologyAvailable;
        }

        // logger.warn("Calc target role: totalExpectedRenGeneration; " +
        // totalExpectedGenerationAvailable);

        scheme.setYearlyTenderDemandTarget(renewableTargetInMwh); // Tender
                                                                  // target
                                                                  // without
                                                                  // taking
                                                                  // actual
                                                                  // generation
                                                                  // into
                                                                  // account.
        scheme.setExpectedRenewableGeneration(totalExpectedGenerationAvailable);

        // when using expected Generation from corresponding FiPScenario
        // SHOULD it be actual values of generation instead of factors? - no
        // differnce cause demand data should be exactly the same.
        // scheme.setExpectedRenewableGeneration(totalExpectedConsumption*targetFactorAchievementForecast);

        renewableTargetInMwh = renewableTargetInMwh - totalExpectedGenerationAvailable;

        if (renewableTargetInMwh < 0) {
            renewableTargetInMwh = 0;
        }

        // logger.warn("actualRenewableTargetInMwh; " + renewableTargetInMwh + "
        // for year" + futureStartingTenderTimePoint
        // + "for scheme " + scheme.getName());
        scheme.setAnnualRenewableTargetInMwh(renewableTargetInMwh);

    }

    private double computeGenerationFromRenUsingPPDP(PowerGeneratingTechnology technology, ElectricitySpotMarket market,
            long futureTimePoint) {
        double expectedGenerationPerTechnologyAvailable = 0d;

        for (PowerPlant plant : reps.powerPlantRepository.findExpectedOperationalPowerPlantsInMarketByTechnology(market,
                technology, futureTimePoint)) {
            double totalGenerationOfPlantInMwh = 0d;
            for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
                // logger.warn("Inside segment loop for
                // calculating
                // total production");

                double hours = segmentLoad.getSegment().getLengthInHours();

                PowerPlantDispatchPlan ppdp = reps.powerPlantDispatchPlanRepository
                        .findOnePowerPlantDispatchPlanForPowerPlantForSegmentForTime(plant, segmentLoad.getSegment(),
                                getCurrentTick(), false);

                if (ppdp == null || ppdp.getStatus() < 0) {

                } else if (ppdp.getStatus() >= 2) {
                    // do a sensitivity here to different
                    // averages of electricity prices.
                    totalGenerationOfPlantInMwh += hours * ppdp.getAcceptedAmount();
                }

            }
            expectedGenerationPerTechnologyAvailable += totalGenerationOfPlantInMwh;

        }
        // logger.warn("");

        return expectedGenerationPerTechnologyAvailable;

    }

    private double computeRenGenerationUsingExpectedGeneration(PowerGeneratingTechnology technology,
            ElectricitySpotMarket market, long futureTimePoint) {
        double expectedGenerationPerTechnologyAvailable = 0d;
        int count = 0;

        for (PowerPlant plant : reps.powerPlantRepository.findExpectedOperationalPowerPlantsInMarketByTechnology(market,
                technology, futureTimePoint)) {
            count++;
            double totalGenerationOfPlantInMwh = plant.getAnnualFullLoadHours() * plant.getActualNominalCapacity();
            long numberOfSegments = reps.segmentRepository.count();
            // for (Segment segment : reps.segmentRepository.findAll()) {
            // double availablePlantCapacity =
            // plant.getAvailableCapacity(futureTimePoint, segment,
            // numberOfSegments);

            // double lengthOfSegmentInHours = segment.getLengthInHours();
            // totalGenerationOfPlantInMwh += availablePlantCapacity *
            // lengthOfSegmentInHours;
            // logger.warn("availablePlantCapacity" + numberOfSegments +
            // "lengthOfSegmentInHours"
            // + segment.getLengthInHours() +
            // "expectedGenerationPerPlantAvailable"
            // + expectedGenerationPerPlantAvailable);
            // }
            expectedGenerationPerTechnologyAvailable += totalGenerationOfPlantInMwh;

        }
        // logger.warn("No of power plants of technology " +
        // technology.getName() + "is " + count);
        logger.warn("Expected generation from technology " + technology.getName() + "is "
                + expectedGenerationPerTechnologyAvailable);

        return expectedGenerationPerTechnologyAvailable;

    }

    public double predictDemandForElectricitySpotMarket(ElectricitySpotMarket market,
            long numberOfYearsBacklookingForForecasting, long futureTimePoint) {

        GeometricTrendRegression gtr = new GeometricTrendRegression();
        for (long time = getCurrentTick(); time > getCurrentTick() - numberOfYearsBacklookingForForecasting
                && time >= 0; time = time - 1) {
            gtr.addData(time, market.getDemandGrowthTrend().getValue(time));
        }
        double forecast = gtr.predict(futureTimePoint);
        if (Double.isNaN(forecast))
            forecast = market.getDemandGrowthTrend().getValue(getCurrentTick());
        return forecast;
    }
}