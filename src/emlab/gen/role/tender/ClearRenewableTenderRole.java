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

import emlab.gen.domain.market.Bid;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.TenderBid;
import emlab.gen.domain.policy.renewablesupport.TenderClearingPoint;
import emlab.gen.engine.AbstractRole;
import emlab.gen.engine.Role;
import emlab.gen.repository.Reps;

/**
 * @author rjjdejeu
 */
public class ClearRenewableTenderRole extends AbstractRole<RenewableSupportSchemeTender>
        implements Role<RenewableSupportSchemeTender> {

	Reps reps;

    @Override
    public void act(RenewableSupportSchemeTender scheme) {

        // logger.warn("Clear Renewable Tender Role started for: " + scheme);

        // Zone zone = regulator.getZone();
        // RenewableSupportSchemeTender scheme =
        // reps.renewableSupportSchemeTenderRepository
        // .determineSupportSchemeForZone(zone);

        // logger.warn("scheme is: " + scheme);

        // Initialize a sorted list for tender bids
        Iterable<TenderBid> sortedTenderBidsbyPriceAndScheme = null;
        sortedTenderBidsbyPriceAndScheme = reps
                .findAllSubmittedSortedTenderBidsbyTime(getCurrentTick(), scheme);

        double tenderQuota = scheme.getAnnualRenewableTargetInMwh();
        logger.log(Level.INFO, "For scheme: " + scheme.getName() + " Tender Quota IN TENDER CLEARING; " + tenderQuota);
        double sumOfTenderBidQuantityAccepted = 0d;
        double acceptedSubsidyPrice = 0d;
        boolean isTheTenderCleared = false;

        if (tenderQuota == 0) {
            isTheTenderCleared = true;
            acceptedSubsidyPrice = 0;
        }

        // This epsilon is to account for rounding errors for java (only
        // relevant for exact clearing)
        double clearingEpsilon = 0.000000001d;

        // Goes through the list of the bids that are sorted on ascending order
        // by price
        double noOfBids = 0d;
        int noOfBidsAccepted = 0;
        double pgtNodeLimit = Double.MAX_VALUE;
        for (TenderBid currentTenderBid : sortedTenderBidsbyPriceAndScheme) {
            noOfBids++;

            // if the tender is not cleared yet, it collects complete bids
            if (isTheTenderCleared == false) {
                if (tenderQuota - (sumOfTenderBidQuantityAccepted + currentTenderBid.getAmount()) >= -clearingEpsilon) {
                    noOfBidsAccepted++;
                    acceptedSubsidyPrice = currentTenderBid.getPrice();
                    currentTenderBid.setStatus(Bid.ACCEPTED);
                    currentTenderBid.setAcceptedAmount(currentTenderBid.getAmount());

                    // logger.warn("Fully Accepted: bidder; " +
                    // currentTenderBid.getBidder() + "Technology; "
                    // + currentTenderBid.getTechnology() + "bidAmount; " +
                    // currentTenderBid.getAmount()
                    // + "acceptedSubsidyPrice; " + acceptedSubsidyPrice);

                    // logger.warn("Status; " + currentTenderBid.getStatus());

                    sumOfTenderBidQuantityAccepted = sumOfTenderBidQuantityAccepted + currentTenderBid.getAmount();

                    logger.log(Level.INFO, "sumOfTenderBidQuantityAccepted; " + sumOfTenderBidQuantityAccepted);

                }

                // it collects a bid partially if that bid fulfills the quota
                // partially
                // TODO MM ?
                else if (tenderQuota
                        - (sumOfTenderBidQuantityAccepted + currentTenderBid.getAmount()) < clearingEpsilon) {

                    logger.log(Level.INFO, "Partially Accepted: bidder; " + currentTenderBid.getBidder() + "Technology; "
                            + currentTenderBid.getTechnology() + "bidAmount; " + currentTenderBid.getAmount()
                            + "acceptedSubsidyPrice; " + acceptedSubsidyPrice);

                    currentTenderBid.setStatus(Bid.FAILED);
                    currentTenderBid.setAcceptedAmount(0);

                    isTheTenderCleared = true;

                }
                // the tenderQuota is reached and the bids after that are not
                // accepted

            } else {
                currentTenderBid.setStatus(Bid.FAILED);
                currentTenderBid.setAcceptedAmount(0);

            }
            
            //currentTenderBid.persist();

        } // FOR Loop ends here

        logger.log(Level.INFO, "Total No of Bids Accepted " + noOfBidsAccepted + "Accepted subsidy price " + acceptedSubsidyPrice
                + "accepted subsidy quantity" + sumOfTenderBidQuantityAccepted);
        // This creates a clearing point that contains general information about
        // the cleared tender
        // volume, subsidy price, current tick, and stores it in the graph
        // database
        
        TenderClearingPoint tenderClearingPoint = new TenderClearingPoint();
        tenderClearingPoint.setPrice(acceptedSubsidyPrice);
        tenderClearingPoint.setVolume(sumOfTenderBidQuantityAccepted);
        tenderClearingPoint.setRenewableSupportSchemeTender(scheme);
        tenderClearingPoint.setTime(getCurrentTick());

        if (isTheTenderCleared == true) {
            //tenderClearingPoint.persist();
            // logger.warn("Tender CLEARED at price {} and volume " +
            // tenderClearingPoint.getVolume(),
            // tenderClearingPoint.getPrice());

        } else {
            //enderClearingPoint.persist();
            // logger.warn("Tender UNCLEARED at price {} and volume " +
            // tenderClearingPoint.getVolume(),
            // tenderClearingPoint.getPrice());

        }

    }

}
