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
package emlab.gen.role.pricewarranty;

import java.util.logging.Level;

import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.engine.Schedule;

/**
 * @author Kaveri3012 This role loops through eligible technologies, eligible
 *         nodes,s
 * 
 *         computes LCOE per technology per node and creates an object,
 *         BaseCost, to store it.
 * 
 *         In technology neutral mode, after computing LCOE per technology, it
 *         should store LCOE per technology and create a merit order upto which
 *         a cetrain target is filled.
 * @author marcmel
 * 
 */

public class ComputePremiumRoleExPost extends AbstractComputePremiumRole{

	public ComputePremiumRoleExPost(Schedule schedule) {
	    super(schedule);
	}
	
    @Override
    public void act(RenewableSupportFipScheme scheme) {
        logger.log(Level.INFO, "Compute Premium Ex-Post");
        super.act(scheme);
    }
	
    // Marginal costs
	@Override
	protected void calculateCostPerMWh(double biasFactorValue, double generation, PowerPlant plant) {
		
        double lcoe = 0d;

        double discountedCapitalCosts = evaluateInvestment.financialExpectation.getDiscountedCapitalCosts();
        double discountedOpCost = evaluateInvestment.financialExpectation.getDiscountedOperatingCost();
        
        // This is the cost that investors calculate with. It does not yet consider the revenue from the electricty market.

        lcoe = (discountedCapitalCosts + discountedOpCost) * biasFactorValue / generation;
        
        if (lcoe < 0) {
        	lcoe = -lcoe; // (Need to be of the same sign as those calculated for Ex-ante)
        }
        
         logger.log(Level.FINER, "expectedBaseCost in PremiumRoleExPost for plant" + plant + 
        		 "in tick" + evaluateInvestment.getFutureTimePoint() + "is " + lcoe);
         
         setCostPerMWh(lcoe);
         
	}
}