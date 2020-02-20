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


import java.util.logging.Level;

import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.TenderBid;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.AbstractRole;
import emlab.gen.engine.Role;
import emlab.gen.repository.Reps;

/**
 * @author rjjdejeu
 * @author marcmel adoption to 2.vers
 */

public class FilterTenderBidsByTechnologyPotentialRole extends AbstractRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {
    Iterable<TenderBid> sortedTenderBidsbyPriceTechnologyAndNode;
    Iterable<TenderBid> sortedTenderBidsbyPriceTechnology;
    double technologyPotential;
    double technologyAndNodePotential;
    double expectedInstalledCapacityOfTechnologyInNode;
    double expectedGenerationFromTechnology;
    double limit;


    Reps reps;

    @Override
    public void act(RenewableSupportSchemeTender scheme) {
        ElectricitySpotMarket market = reps.findElectricitySpotMarketForZone(scheme.getRegulator().getZone());

        // 1. Loop through all the technologies in the tech neutral scheme.
        // 2. For each technology, find a list of sorted bids
        // 3. For each technology find potential
        // 4. Go through each bid, sum all the bids cumulative until the
        // potential is met, this is done in the clear bid algorithm anyway!

        for (PowerGeneratingTechnology technology : scheme.getPowerGeneratingTechnologiesEligible()) {

            // POTENTIAL IN MWH ASSUMED TO BE THE SAME AS TARGET
            sortedTenderBidsbyPriceTechnology = reps.findAllSubmittedSortedTenderBidsbyTechnology(getCurrentTick(), scheme, technology.getName());

            technologyPotential = reps
                    .findTechnologySpecificRenewablePotentialLimitTimeSeriesByRegulator(scheme.getRegulator(),
                            technology.getName())
                    .getValue(getCurrentTick() + scheme.getFutureTenderOperationStartTime());

            logger.log(Level.INFO,"verification: technology potential =" + technologyPotential);

            expectedInstalledCapacityOfTechnologyInNode = reps
                    .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market, technology,
                            getCurrentTick() + scheme.getFutureTenderOperationStartTime());
            
            // create dummy power plant here
            // TODO why?

            EnergyProducer producer = getReps().energyProducers.iterator().next();
            PowerGridNode node = reps.findFirstPowerGridNodeByElectricitySpotMarket(market);
            PowerPlant plant = reps.createAndSpecifyTemporaryPowerPlant(getCurrentTick(), producer, node, technology);
            
            limit = technologyPotential
                    - (expectedInstalledCapacityOfTechnologyInNode * plant.getAnnualFullLoadHours());
            logger.log(Level.INFO, "verification: expected generation ="
                    + expectedInstalledCapacityOfTechnologyInNode * plant.getAnnualFullLoadHours());

            logger.log(Level.INFO, "Limit for technology " + technology.getName() + "is " + limit);

            double sumAcceptedBid = 0d;
            for (TenderBid currentBid : sortedTenderBidsbyPriceTechnology) {

                if ((sumAcceptedBid + currentBid.getAmount()) < limit) {
                    sumAcceptedBid += currentBid.getAmount();

                } else {
                    currentBid.setStatus(Bid.FAILED);
                }
            }
        }

        // if (cashAvailableForPlantDownpayment -
        // currentTenderBid.getCashNeededForPlantDownpayments() > 0) {
        // cashAvailableForPlantDownpayment =
        // cashAvailableForPlantDownpayment
        // - currentTenderBid.getCashNeededForPlantDownpayments();
        // else {

        // currentTenderBid.setStatus(Bid.NOT_SUBMITTED);
        // logger.warn("status of bid; " + currentTenderBid.getStatus()
        // + "is of technology"
        // + currentTenderBid.getTechnology());

    }

    private Iterable<TenderBid> getSortedTenderBids(RenewableSupportSchemeTender scheme,
            PowerGeneratingTechnology technology) {

        ElectricitySpotMarket market = reps
                .findElectricitySpotMarketForZone(scheme.getRegulator().getZone());

        for (PowerGridNode node : reps
                .findAllPowerGridNodesByZone(scheme.getRegulator().getZone())) {

            // POTENTIAL IN MWH ASSUMED TO BE THE SAME AS TARGET

            // get
            technologyAndNodePotential = reps
                    .findTechnologyAndNodeSpecificRenewableTargetTimeSeriesForTenderByRegulator(scheme.getRegulator(),
                            technology.getName(), node.getName())
                    .getValue(getCurrentTick() + scheme.getFutureTenderOperationStartTime())
                    * scheme.getAnnualExpectedConsumption();
            expectedInstalledCapacityOfTechnologyInNode = reps
                    .calculateCapacityOfExpectedOperationalPowerPlantsByNodeAndTechnology(node, technology,
                            getCurrentTick() + scheme.getFutureTenderOperationStartTime());
            PowerPlant plant = reps.findOperationalPowerPlantsByMarketAndTechnology(market,
                    technology, getCurrentTick() + scheme.getFutureTenderOperationStartTime()).iterator().next();
            limit = technologyAndNodePotential
                    - (expectedInstalledCapacityOfTechnologyInNode * plant.getAnnualFullLoadHours());

            sortedTenderBidsbyPriceTechnologyAndNode = reps
                    .findAllSubmittedSortedTenderBidsbyTechnologyAndNode(getCurrentTick(), scheme, technology.getName(),
                            node.getName());
        }
        return sortedTenderBidsbyPriceTechnologyAndNode;
        // currentTenderBid.persist();
    }
}
