# The aim of this script is to format some of the config data for the producers

library(tidyverse)
library(glue)

utility_data_path <- "/Users/marcmel/Documents/PhD/Papers/03 Conjoint PhD paper 1/Analysis/output/tables/utility_table/main/"
main_simulation_name <- "missing-unavailable-one-interaction-partial-constrained_"

investors <- c("Small", "Medium", "Large", "Very large")
countries <- c("DE", "NL")
country_markets <- c(DE = "germanyElectricitySpotMarket", NL = "netherlandsElectricitySpotMarket")
investment_role <- "tenderAndPreferenceInvestmentRole"

uval <- function(segment, attribute, level, df){
  
  df %>%
    filter(attribute == !!attribute, level == !!level) %>% 
    pull(mean) %>% 
    format(nsmall=1)
  
  
}

text <- ""

for(country in countries){
  
  for(segment in investors){
    
    filename <- paste0(main_simulation_name, segment, ".csv")
    data <- read_csv(paste0(utility_data_path, filename))
    
    if(segment == "Very large")
      segment <- "Verylarge"
    
    agentName <- glue("Pref Investor {segment} {country}")
    objectName <- glue("prefInvestors{segment}{country}")
    
    new_text <- glue('
                 
		EnergyProducer {objectName} = reps.createEnergyProducer();
		{objectName}.setName("{agentName}");
		{objectName}.setNumberOfYearsBacklookingForForecasting(5);
		{objectName}.setPriceMarkUp(1.0);
		{objectName}.setWillingToInvest(true);
		{objectName}.setDownpaymentFractionOfCash(.5);
		{objectName}.setDismantlingRequiredOperatingProfit(0);
		{objectName}.setDismantlingProlongingYearsAfterTechnicalLifetime(0);
		{objectName}.setDebtRatioOfInvestments(0.7);
		{objectName}.setLoanInterestRate(0.1);
		{objectName}.setEquityInterestRate(0.1);
		{objectName}.setPastTimeHorizon(5);
		{objectName}.setInvestmentFutureTimeHorizon(7);
		{objectName}.setLongTermContractPastTimeHorizon(3);
		{objectName}.setLongTermContractMargin(0.1);
		{objectName}.setCash(3e9);
		{objectName}.setInvestmentRole({investment_role}); 
		
		preferenceMap = new HashMap<>();
		preferenceMap.put(utilityLevelsCountryOwn, {uval(segment, "Country","Own",data)});
		preferenceMap.put(utilityLevelsCountryKnown, {uval(segment, "Country","Known EU",data)});
		preferenceMap.put(utilityLevelsCountryUnknown, {uval(segment, "Country","Unknown EU",data)});
		{objectName}.setUtilityCountry(preferenceMap);
		preferenceMap = new HashMap<>();
		preferenceMap.put(utilityLevelsReturn5, {uval(segment, "Return","5%",data)});
		preferenceMap.put(utilityLevelsReturn6, {uval(segment, "Return","6%",data)});
		preferenceMap.put(utilityLevelsReturn7, {uval(segment, "Return","7%",data)});
		{objectName}.setUtilityReturn(preferenceMap);
		preferenceMap = new HashMap<>();
		preferenceMap.put(utilityLevelsPolicyFIT, {uval(segment, "Policy","FIT",data)});
		preferenceMap.put(utilityLevelsPolicyAuction, {uval(segment, "Policy","Prem. auc.",data)});
		preferenceMap.put(utilityLevelsPolicyNone, {uval(segment, "Policy","No support",data)});
		{objectName}.setUtilityPolicy(preferenceMap);
		preferenceMap = new HashMap<>();
		preferenceMap.put(utilityLevelsTechnologyPV, {uval(segment, "Technology","PV",data)});
		preferenceMap.put(utilityLevelsTechnologyOnshore, {uval(segment, "Technology","Onshore",data)});
		preferenceMap.put(utilityLevelsTechnologyOffshore, {uval(segment, "Technology","Offshore",data)});
		{objectName}.setUtilityTechnology(preferenceMap);
                 
		potentialInvestorMarkets = new HashSet<>();
		//potentialInvestorMarkets.add();
		potentialInvestorMarkets.add({country_markets[country]});
		{objectName}.setInvestorMarket({country_markets[country]});
		{objectName}.setPotentialInvestorMarkets(potentialInvestorMarkets);'
    )
    
    text <- paste(text, new_text, sep ="\n")
  }
  
}

write_file(text, path = paste0("output/code.java"))

