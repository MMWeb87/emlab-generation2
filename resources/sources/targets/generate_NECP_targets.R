library(tidyverse)

emlab_data_folder <- "../../data" 

filename_potentials <- "NecpPotentials.csv"
filename_limits <- "NecpPotentialLimits.csv"

do_write_csvs <- TRUE
prefix_for_result <- "From2015"

adjustment_factor_for_limits <- 1 # To reduce the limits for sensitivity analyses

# year definition
limits_year_begin <- 2015
limits_year_end <- 2070 # Need to be higher than runtime in EMLab
limits_year_timestep <- 0 # to calibrate which year is year 0

# Function ----------------------------------------------------------------


extrapolate_values <- function(df, var, year_begin, year_end, year_timestep, method = "linear"){
  
  new_years <- seq(year_begin, year_end, 1)
  
  if(method == "linear"){
    model <- lm(as.formula(paste(var, "~ year")), df) 
    
    new_y <- predict.lm(
      object = model,
      newdata = data.frame(year = new_years)) 
    
  } else if(method == "capped_glm"){
    
    # maximal value in y will be 100% and resulting curve will look nice
    y_max <- max(df[[var]], na.rm = TRUE)
    df[[var]] <- df[[var]] / y_max 
    
    model <- glm(as.formula(paste(var, "~ year")), family ="quasibinomial", df)
    
    new_y <- predict.glm(
      object = model,
      newdata = data.frame(year = new_years),
      type = "response")
    
    # original scale
    new_y <- new_y * y_max
    
  }
  
  tibble(
    timestep =  seq(from = year_timestep, to = year_end - year_begin + year_timestep, by = 1),
    year = new_years,
    predicted_y = new_y
  ) 
  
}

# NREPs in original model -------------------------------------------------
# Preparations and some data extraction for new goals.

# Renewable energies’ share of gross electricity consumption

original_potentials <- read_csv(file = "data/nodeAndTechSpecificPotentialsDummy.csv")
  
original_potentials_df <- original_potentials %>% 
  rename(target = "realtime") %>% 
  gather(key = "year", value = "target_val", -target)

# Figure out how the ratio of targets for wind and pv was in nreaps 
# because new NECPs have a shared goal now
original_potentials_df %>% 
  filter(target %in% c("nl_target_windon","nl_target_photovoltaics")) %>% 
  spread(key = target, value = target_val) %>% 
  mutate(
    sum = nl_target_windon + nl_target_photovoltaics,
    ration = nl_target_windon / sum) %>% 
  summarise(
    mean = mean(ration),
    sd = sd(ration)
  )
  
# so less wind target
# ratio for wind / pv is about 0.45 -> enter into NECPs_sources.xlsx

# To get 2020 targets
original_potentials_df %>% 
  filter(
    target %in% c("nl_target", "nl_target_windon","nl_target_windoff","nl_target_photovoltaics","nl_target_biomass"),
    year %in% c(2020,2021)
    ) 

# Potential as shares for techs and neutral ------------------------------------------


# New targets are shares on electricity consumption in year
necp_targets <- read_csv(
  file = "data/NECPs_sources.csv") %>%
  filter(country %in% c("DE", "NL"))
  
necp_targets_original <- necp_targets %>%
  gather(key = "technology", value = "goal", -country, -year) %>% 
  group_by(country, technology)


necp_targets_predicted <-  necp_targets_original %>%
  nest() %>%
  mutate(prediction = map(data, extrapolate_values, 
                          var = "goal", 
                          year_begin = limits_year_begin, 
                          year_end = limits_year_end, 
                          year_timestep = limits_year_timestep,
                          method ="capped_glm")) %>% 
  select(-data) %>% 
  unnest(cols = c("prediction")) %>% 
  ungroup() %>% 
  rename(predicted_goal = predicted_y)

# visual check
necp_targets_predicted %>% 
  ggplot(mapping = aes(x = year, y = predicted_goal)) +
    geom_point() +
    geom_point(mapping = aes(y = goal), data = necp_targets_original, col = "red") +
    scale_y_continuous(labels = scales::percent) +
    theme(axis.text.x = element_text(angle = 90)) +
    facet_grid(country ~ technology)



## Format to the weired format needed by EMLab Generation
# like in original_potentials

necp_targets_predicted_formated_header <- necp_targets_predicted %>% 
  select(timestep, year) %>% 
  distinct() %>% 
  spread(key = "year", value = "timestep") %>% 
  add_column(realtime = "timestep", .before = 0)

necp_targets_predicted_formated <- necp_targets_predicted %>% 
  select(-timestep) %>% 
  mutate(name = paste(country, "target", technology, sep = "_")) %>% 
  spread(key = "year", value = "predicted_goal") %>% 
  select(-country, -technology, realtime = name)
    
necp_targets_predicted_formated <- rbind(
  necp_targets_predicted_formated_header,
  necp_targets_predicted_formated
) 
  
final_filename <- paste0(prefix_for_result, filename_potentials)
if(do_write_csvs){
  write_csv(necp_targets_predicted_formated, path =  file.path(emlab_data_folder, final_filename))
} else {
  necp_targets_predicted_formated
  warning(paste("Not saved as", final_filename))
}




# Potential Limits (needed in check function) ------------------------------------------------------------------


# Potential Limits in the EMLab Generation Auction model set an upper limit to investments (see check())

# 1. Total electricity consumption is based on Eurostat and NECP goals and set in NECPs_sourcses.xlsx (exported to consumption.csv)

total_elec_consumption <- read_csv(
  file = "data/consumption.csv") %>%
  filter(
    country %in% c("DE", "NL"),
    year %in% c(2020, 2030, 2060)
  ) %>% 
  gather(key = "technology", value = "consumption", -country, -year)

# 2. we assume that future electricity consumption increases linearly

total_elec_consumption_prediction <- total_elec_consumption %>%
  filter(technology == "Total") %>% 
  group_by(technology, country) %>% 
  nest() %>%
  mutate(prediction = map(data, extrapolate_values, 
                          var = "consumption", 
                          year_begin = limits_year_begin, 
                          year_end = limits_year_end, 
                          year_timestep = limits_year_timestep,
                          method = "linear")) %>% 
  select(-data) %>% 
  unnest(cols = c("prediction")) %>% 
  ungroup() %>% 
  select(country, year, timestep, total_consumption = predicted_y)

# validate: in tick 40 (year 2020 + 40 = 2060), in DE it should be about 600TWh (more or less the demand development in emlab)

total_elec_consumption_prediction %>% 
  filter(timestep == 40)

# 3. Calculate predicted NECP shares for all year that are in total_elec_consumption

necp_targets_predicted_for_consumption <- necp_targets_original %>%
  nest() %>%
  mutate(prediction = map(data, extrapolate_values, 
                          var = "goal", 
                          year_begin = limits_year_begin, 
                          year_end = limits_year_end, 
                          year_timestep = limits_year_timestep,
                          method ="capped_glm")) %>% 
  select(-data) %>% 
  unnest(cols = c("prediction")) %>% 
  ungroup() %>% 
  rename(predicted_share = predicted_y)


# 4. Multiply the predicted NECP shares with the extrapolated total consumption to estimate the realistic deployment of RES
# 5. And spply manual limit reduction

renewables_limits <- total_elec_consumption_prediction %>% 
  left_join(necp_targets_predicted_for_consumption) %>% 
  mutate(predicted_production = total_consumption * predicted_share * adjustment_factor_for_limits) %>% 
  select(-predicted_share)

# 6. CHECK: Visual check

renewables_limits %>% 
  ggplot(mapping = aes(x = year, y = predicted_production)) +
    geom_point() +
    theme(axis.text.x = element_text(angle = 90)) +
    facet_grid(country ~ technology) +
    labs(title = "Potential limits for renewables")

# 7. CHECK: Comparing with previous data

original_potential_limits <-  read_csv(file = "data/old_potentialLimits.csv")
original_potential_limits_long <- original_potential_limits %>% 
  gather(key = "time", value = "limit", -timestep) %>% 
  mutate(
    time = as.double(time),
    limit = limit / 10^6,
    technology = fct_recode(timestep, Onshore = "limitWindOn", Offshore =  "limitWindOff", PV = "limitPV")
    ) %>% 
  add_column(country = "NL") %>% 
  select(time, technology, country, limit_old = limit)

renewables_limits_check <- renewables_limits %>% 
  select(time = timestep, technology, country, limit_new = predicted_production)

# The limit_old is the one that was used in Kaveris papers for NL. As it's based on older data, I would expect this to be different 
left_join(original_potential_limits_long, renewables_limits_check) %>% 
  gather(key = "data", value = "limit", limit_old, limit_new) %>% 
  rename(timestep = time) %>% 
  ggplot(mapping = aes(x = timestep, y = limit, color = data)) +
  geom_line() +
  facet_wrap(technology ~ country) + 
  labs(title = "Comparison of old and new data")

# 8. Save in the format for EMLab in data folder, name of original_potential_limits

# original_potential_limits

renewables_limits_formated <- renewables_limits %>% 
  mutate(
    variable = paste("limit", country, technology, sep = "_"),
    predicted_production = predicted_production * 10^6
    ) %>% 
  select(variable, timestep, predicted_production) %>%
  spread(key = timestep, value = predicted_production) %>%
  rename(timestep = variable)
  

final_filename <- paste0(prefix_for_result,filename_limits)
if(do_write_csvs){
  write_csv(renewables_limits_formated, path =  file.path(emlab_data_folder, final_filename))
} else {
  renewables_limits_formated
  warning(paste("Not saved as", final_filename), call. = FALSE)
}



# Additional analyses -----------------------------------------------------

# To compare model demand trend to the one input here.

renewables_limits %>% 
  filter(country == "DE") %>%
  mutate(display = if_else(technology == "Total", "Total", "Techs")) %>% 
  ggplot(mapping = aes(x = timestep, y = predicted_production, fill = technology)) +
    geom_area() +
    # geom_hline(yintercept = 188) + # NL intercepts
    # geom_vline(xintercept = 40) +
    geom_hline(yintercept = 600) + # NL intercepts
    geom_vline(xintercept = 40) +
      theme(axis.text.x = element_text(angle = 90)) +
    facet_grid(display ~ country) +
    labs(
      title = "Demand trend given by NECP and extrapolation",
      subtitle = "To compare to the modeled trend which is linear")

# TODO: check with modelled DE demand trend
# -> There is too much renewable capacity built compared to the demand trend. 

