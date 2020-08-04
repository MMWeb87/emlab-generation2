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
 *         nodes,
 * 
 *         computes LCOE per technology per node and creates an object,
 *         BaseCost, to store it.
 * 
 *         In technology neutral mode, after computing LCOE per technology, it
 *         should store LCOE per technology and create a merit order upto which
 *         a cetrain target is filled.
 * 
 */

public class ComputePremiumRoleExAnte extends AbstractComputePremiumRole{


    public ComputePremiumRoleExAnte(Schedule schedule) {
        super(schedule);
    }
    
    @Override
    public void act(RenewableSupportFipScheme scheme) {
        logger.log(Level.INFO, "Compute Premium Ex-Ante");
        super.act(scheme);
    }
    
	// Marginal costs
	@Override
	protected void calculateCostPerMWh(double biasFactorValue, double generation, PowerPlant plant) {
		
        double projectValue = evaluateInvestment.financialExpectation.getProjectValue();
        double fiPremium = 0d;
        if (projectValue < 0) {
            fiPremium = -projectValue * biasFactorValue / generation;
        }
        
         logger.log(Level.FINE, "expectedBaseCost in PremiumRoleExAnte for plant" + plant + 
        		 "in tick" + evaluateInvestment.getFutureTimePoint() + "is " + fiPremium);
         
         setCostPerMWh(fiPremium);        
	}
}