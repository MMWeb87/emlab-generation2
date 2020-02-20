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
package emlab.gen.role.tender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.AbstractRole;
import emlab.gen.engine.Role;
import emlab.gen.repository.Reps;

/**
 * @author kaveri
 *
 */

public class VerificationTargetCalculationRole extends AbstractRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {

    Reps reps;
    final Map<Long, ArrayList<Double>> mapStorageGeneration = new HashMap<Long, ArrayList<Double>>();

    /*
     * (non-Javadoc)
     * 
     * @see agentspring.role.Role#act(agentspring.agent.Agent)
     */

    @Override
    public void act(RenewableSupportSchemeTender scheme) {

        double expectedGenInFuture = 0d;
        double actualGenThisTick = 0d;
        Zone zone = scheme.getRegulator().getZone();
        ElectricitySpotMarket market = reps.findElectricitySpotMarketForZone(zone);

        // get expected generation in future tick
        expectedGenInFuture = scheme.getExpectedRenewableGeneration();

        // create array and set expected generation for future tick
        ArrayList<Double> generationRenewablesFuture = new ArrayList<Double>();
        generationRenewablesFuture.add(expectedGenInFuture);

        // Store expected generation for future tick in map
        mapStorageGeneration.put(getCurrentTick() + scheme.getFutureTenderOperationStartTime(),
                generationRenewablesFuture);

        // get actual generation in this tick
        for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {
            actualGenThisTick += computeActualGenerationGivenTechnologyNoMarketConsidered(technology, market);
        }

        // Store actual generation for this tick, renewable policy starts only
        // at tick 1, therefore add 1 to future time.
        if (getCurrentTick() >= (scheme.getFutureTenderOperationStartTime() + 1)) {

            // access old entry in map
            ArrayList<Double> generationRenewablesCurrentTick = mapStorageGeneration.get(getCurrentTick());
            // store current generation in old entry.
            generationRenewablesCurrentTick.add(actualGenThisTick);

            // warn when we expect generation greater than actual generation
            if (generationRenewablesCurrentTick.get(0) <= generationRenewablesCurrentTick.get(1)) {
                // logger.warn("Expected generation" +
                // generationRenewablesCurrentTick.get(0) + " <= actual
                // generation"
                // + generationRenewablesCurrentTick.get(1) + " in tick" +
                // getCurrentTick());
            } else {
                // logger.warn("Expected generation" +
                // generationRenewablesCurrentTick.get(0) + " is > actual
                // generation"
                // + generationRenewablesCurrentTick.get(1) + " in tick" +
                // getCurrentTick());
            }

            // logger.warn("length of map : " + mapStorageGeneration.size());

        }

        // Get amount of generation predicted 'futureTimePoint' years ago,
        // for
        // this tick

        // Calculate actual production in this tick

    }

    private double computeActualGenerationGivenTechnologyNoMarketConsidered(PowerGeneratingTechnology technology,
            ElectricitySpotMarket market) {

        double expectedGenerationPerTechnologyAvailable = 0d;
        double expectedGenerationPerPlantAvailable = 0d;
        long numberOfSegments = getReps().segments.size();
        int count = 0;

        for (PowerPlant plant : reps.findExpectedOperationalPowerPlantsInMarketByTechnology(market,
                technology, getCurrentTick())) {
            count++;
            expectedGenerationPerPlantAvailable = 0d;
            for (Segment segment : getReps().segments) {
                double availablePlantCapacity = plant.getAvailableCapacity(getCurrentTick(), segment, numberOfSegments);

                double lengthOfSegmentInHours = segment.getLengthInHours();
                expectedGenerationPerPlantAvailable += availablePlantCapacity * lengthOfSegmentInHours;
                // logger.warn("availablePlantCapacity" + numberOfSegments +
                // "lengthOfSegmentInHours"
                // + segment.getLengthInHours() +
                // "expectedGenerationPerPlantAvailable"
                // + expectedGenerationPerPlantAvailable);
            }
            expectedGenerationPerTechnologyAvailable += expectedGenerationPerPlantAvailable;
            // logger.warn("expectedGenerationPerTechnologyAvailable" +
            // expectedGenerationPerTechnologyAvailable);
        }
        logger.log(Level.INFO,
                "verification : number of current powerplants for technology " + technology.getName() + " is " + count);

        return expectedGenerationPerTechnologyAvailable;
    }

}
