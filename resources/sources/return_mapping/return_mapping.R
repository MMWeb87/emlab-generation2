## This script generates the "mapping-code" to transform ROEs from the original runs to values for which we have part-worth utilities.


prefix <- "1598437148250-Scenario_NL_DE_intermittent_auction_pref-EMlabModelRole-DefaultReporter" # This run should be from a "pre-run", i.e. linear mapping applied in this mappingfunction. Several iterations needed

upper_percentile <- 0.9 # not implemented yet
lower_percentile <- 0.1

output <- "output/"


# Script ------------------------------------------------------------------

library(tidyverse)
library(readxl)
library(glue)

source(file = "../../../analysis/config.R")

raw_financialexpectations_results <- read_delim(
  file = file.path(emlab_results_directory, paste0(prefix, "-FinancialExpectation.csv")),
  delim = ";",
  col_types = cols(.default = "n", producer = "c", market = "c", technology = "c", plant = "c", node = "c"),
  locale = locale(decimal_mark = ".", grouping_mark =  "'")) %>% 
  arrange(iteration, tick)

# Determine modelled return levels.

return_analysis_data <- raw_financialexpectations_results %>% 
  select(iteration, tick, market, producer, technology, investmentRound, ROE = ROE.modelled, ROI)

# I want to see per tick what the maximum ROE is

return_analysis_per_tick <- return_analysis_data %>%   
  group_by(market, technology, tick) %>% 
  summarise(
    max_ROE = max(ROE),
    min_ROE = min(ROE),
    ROE_average = mean(ROE),
    ROE_sd = sd(ROE))


return_analysis_data_2 <- return_analysis_data %>%
  left_join(return_analysis_per_tick, by = c("market", "technology", "tick")) %>% 
  mutate(relative_ROE = ROE / max_ROE) 

# return_analysis_data_2%>% 
#   arrange(market,technology,tick)

return_analysis_data_2%>% 
  group_by(market, technology, investmentRound) %>% 
  summarise(
    ROE_average = mean(ROE),
    ROE_sd = sd(ROE),
    ROE_average_rel = mean(relative_ROE),
    ROE_sd_rel = sd(relative_ROE)
  ) %>% 
  ggplot(mapping = aes(x = investmentRound, y = ROE_average_rel, color = technology)) +
  #geom_line() +
  geom_smooth(se = F) +
  facet_grid( ~ market, scales = "free")


# ROE relative are relative to the highest ROE (of a technology and market, estimated by all producers) per tick. 
## needs to be percent of max

return_analysis_data %>% 
  group_by(market, technology, tick) %>% 
  filter(technology %in% c("Photovoltaic PGT", "Onshore wind PGT", "Offshore wind PGT")) %>% 
  summarise(
    ROE_average = mean(ROE),
    ROE_sd = sd(ROE)
  ) %>% 
  ggplot(mapping = aes(x = tick, y = ROE_average, color = technology)) +
  geom_line() +
  scale_y_log10(labels = scales::percent) +
  #geom_smooth(se = F) +
  facet_grid( ~ market, scales = "free")



return_analysis_data %>% 
  filter(technology %in% c("Photovoltaic PGT", "Onshore wind PGT", "Offshore wind PGT")) %>% 
  ggplot(mapping = aes(x = tick, y = ROE, color = technology)) +
  #geom_boxplot() +
  geom_line() +
  scale_y_continuous(labels = scales::percent) +
  facet_grid( ~ market, scales = "free")


# taking as average the median as there, most expectations took place. 
# for the scalling, take quartiles, as between them, most cases are. 


# Problem analysis --------------------------------------------------------

# These figures show the expected ROEs of all iterations in investments

return_analysis_data %>%
  ungroup() %>% 
  ggplot(mapping = aes(x = tick, y = ROE, color = technology)) +
  geom_boxplot() +
  #scale_y_log10(labels = scales::percent) +
  facet_grid( ~ market, scales = "free")


return_analysis_data %>%
  ungroup() %>% 
  group_by(technology) %>% 
  summarise(
    mean = mean(ROE))



#  calculate boundaries of function

quibble <- function(x, q = c(0.25, 0.5, 0.75)) {
  tibble(x = quantile(x, q), q = q)
}


#' This returns from a df with market and technology fields, the lowst porint near the 25%, the highest near the 75% percentile, as well as the mean of a variable (e.g. ROE). Not so efficient thoug.
#' 
#'
#' @param df 
#' @param var 
#' @param market 
#' @param technology 
#'
#' @return
#' @export
#'
#' @examples
quantile_summary <- function(df, var, market, technology){
  
  var <- enquo(var)
  
  df_specific <- df %>%
    filter(
      technology == !!technology,
      market == !!market) %>% 
    ungroup()
  
  
  quantile_tibble <- df_specific %>%  
    group_by(iteration) %>% 
    nest() %>% 
    mutate(quantiles = map(data, function(data){
      
      data %>% 
        summarise(
          #quibble(ROE, c(0.25, 0.75))) %>% 
          quibble(ROE, c(0.1, 0.9))) %>% 
        
        spread(key = q, value = x)
      
    } ))
  
  
  average_roes_in_50_quantiles <- quantile_tibble %>% 
    mutate(
      summary = map2(quantiles, data, function(quantiles, data) {
        
        data %>%
          filter(
            between(!!var, quantiles$`0.1`, quantiles$`0.9`)) %>% 
          summarise(
            modelled_roe_mean = mean(ROE),
            modelled_roe_max = max(ROE),
            modelled_roe_min = min(ROE)
          )
      })) %>% 
    add_column(
      market = !!market,
      technology = !!technology
    ) %>% 
    unnest(cols = c(summary)) %>% 
    select(-data) %>% 
    ungroup()
  
  
  average_roes_in_50_quantiles
  
}

# These boundaries are the ROEs in which most ROEs data points lie (mean over several iterations)
# I treat others as outliers in the mapping function

tech_combinations <- crossing(
  market = c("DutchMarket","GermanMarket"),
  technology = c("Onshore wind PGT", "Offshore wind PGT", "Photovoltaic PGT")
)

boundaries <- map2_dfr(
  .x = tech_combinations$market, 
  .y = tech_combinations$technology, 
  .f = ~ quantile_summary(return_analysis_data, ROE, .x, .y))

final_boundaries <- boundaries %>% 
  group_by(market, technology) %>% 
  summarise(
    modelled_roe_mean = mean(modelled_roe_mean),
    modelled_roe_max = mean(modelled_roe_max),
    modelled_roe_min = mean(modelled_roe_min)
  )


# In the next step, I construct the formula used to rescale returns from the model to returns one would observe in our context
# For onshore wind, most ROEs in 2017, in Germany were between 6% ±2% (EGLI). We expect that there are also outliers which are higher or lower.
# 


calculate_mapping_function <- function(roe_data){
  
  # actual returns (for which data is available or can be extrapolated)
  actual_utils <- seq(
    from = roe_data$real_roe_average - roe_data$real_roe_variation, 
    to = roe_data$real_roe_average + roe_data$real_roe_variation, 
    by = 0.01)
  
  # the range of utils from the model
  # to get the slope of the linear function, I divide the range between max and min of the 
  # actual returns by the number of actual_utils
  
  
  final_boundaries_function <- roe_data %>%
    select(mean = modelled_roe_mean, max = modelled_roe_max, min = modelled_roe_min) %>% 
    mutate(
      range = max - min,
      delta_y = 0.01,
      delta_x = range / (length(actual_utils) - 1),
      slope = delta_y / delta_x
    )
  
  # construct formula:
  # y = a + b*x; b => slope
  # a = y - b*x
  # I construc the intercept using the mean between max and mean, an the middle actual_utils
  
  
  final_boundaries_function_final <- final_boundaries_function %>% 
    mutate(
      intercept = median(actual_utils) - slope * mean 
    )
  
  
  tibble(
    intercept = final_boundaries_function_final$intercept,
    slope = final_boundaries_function$slope
  )
}

actual_utils_df <- read_xlsx("country_roes.xlsx")

final_function_parameters <- actual_utils_df %>% 
  left_join(final_boundaries, by = c("technology", "market")) %>% 
  group_by(technology, market) %>% 
  nest() %>% 
  mutate(
    mapping = map(data, calculate_mapping_function)
  ) %>% 
  unnest(cols = c(mapping, data))

final_function_parameters


# Java code generation ----------------------------------------------------

filename_mappingfunction <- paste0(output,"/", prefix, "_mappingfunction_javacode.txt")
filename_Rdata <- paste0(output,"/", prefix, "_final_function_parameters.Rdata")

write_lines(
  x = glue("		// Based on: {prefix}\n\n"), 
  path = filename_mappingfunction, 
  append = FALSE)

name_to_var <- tribble(
  ~"var", ~"name",
  "pv", "Photovoltaic PGT",
  "windOnshore", "Onshore wind PGT",
  "windOffshore", "Offshore wind PGT",
  "netherlandsElectricitySpotMarket", "DutchMarket",
  "germanyElectricitySpotMarket", "GermanMarket"         
)

name_to_var_trans <- name_to_var$var
names(name_to_var_trans) <- name_to_var$name

save(final_function_parameters, file = filename_Rdata)

for(line in 1:nrow(final_function_parameters)){
  
  modelledRoeMin = final_function_parameters[[line,"modelled_roe_min"]]
  modelledRoeMax = final_function_parameters[[line,"modelled_roe_max"]]
  intercept = final_function_parameters[[line,"intercept"]]
  slope = final_function_parameters[[line,"slope"]]
  market = name_to_var_trans[[final_function_parameters[[line,"market"]]]]
  technology = name_to_var_trans[[final_function_parameters[[line,"technology"]]]]
  
  my_output <- glue(        
    "
    EmpiricalMappingFunctionParameter empiricalMappingFunction{line} = new EmpiricalMappingFunctionParameter();
		empiricalMappingFunction{line}.setModelledRoeMin({modelledRoeMin});
    empiricalMappingFunction{line}.setModelledRoeMax({modelledRoeMax});
    empiricalMappingFunction{line}.setIntercept({intercept});
    empiricalMappingFunction{line}.setSlope({slope});
    empiricalMappingFunction{line}.setMarket({market});
    empiricalMappingFunction{line}.setTechnology({technology}); 
    reps.empiricalMappingFunctionParameters.add(empiricalMappingFunction{line});\n"
  )
  
  write_lines(
    x = my_output, 
    path = filename_mappingfunction, 
    append = TRUE)
  
}
