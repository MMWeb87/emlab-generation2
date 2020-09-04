# The aim of this script is to read current power plant data and transform it into a format suitable for emlab:
# Name, Technology, Location, Age, Owner, Capacity, Efficiency


library(tidyverse)
library(readxl)
source("prepare_power_plants_functions.R")


# Config ------------------------------------------------------------------

random_seed <- "3498563"
save <- TRUE
show_debug <- TRUE
data_folder <- "/Users/marcmel/Documents/PhD/Papers/05 ABM PhD paper 2/data/powerplants"
output_folder <- "../../data/"
prefix_for_result <- "separated"

#my_countries <- c("NL", "FR", "BE", "LU", "DE")
my_countries <- c("NL", "DE")
my_countries_A <- c("DE")
my_countries_B <- c("NL")
my_countries_A_node <- "deNode"
my_countries_B_node <- "nlNode"

# Owners
# make sure that the names correspond to the java scenario file

owners <- list()
owners[["countries_A_renewables"]] <- c(
  "Pref Investor Small DE",
  "Pref Investor Medium DE",
  "Pref Investor Large DE",
  "Pref Investor Verylarge DE")

owners[["countries_A_conventional"]] <- c(
  "Energy Producer DE A",
  "Energy Producer DE B",
  "Energy Producer DE C"
)

owners[["countries_B_renewables"]]  <- c(
  "Pref Investor Small NL",
  "Pref Investor Medium NL",
  "Pref Investor Large NL",
  "Pref Investor Verylarge NL")
  
owners[["countries_B_conventional"]]  <- c(
  "Energy Producer NL A",
  "Energy Producer NL B",
  "Energy Producer NL C"
)

# list of any of the owners defined above
my_countries_A_renwables_owners <- owners$countries_A_renewables
my_countries_B_renwables_owners <- owners$countries_B_renewables
my_countries_A_conventional_owners <- owners$countries_A_conventional
my_countries_B_conventional_owners <- owners$countries_B_conventional

# Technologies

plants <- list()
plants_and_owners <- list()
technologies <- list()
technologies[["conventional"]] <- c("Biomass CHP", "Coal PSC", "Lignite PSC", "Hydroelectric", "OCGT", "CCGT","Nuclear PGT", "Fuel oil PGT")
technologies[["renewables"]] <- c("Offshore wind PGT", "Onshore wind PGT", "Photovoltaic PGT")

technology_translations <- read_excel(path = "translation table.xlsx")

typical_age_for_plants<- tribble(
  ~technology, ~typical_age,
  "Offshore wind PGT", 25,
  "Onshore wind PGT", 25,
  "Hydroelectric", 50,
  "Photovoltaic PGT", 25,
  "Biomass CHP", 30,
  "Coal PSC", 40,
  "Lignite PSC", 40,
  "CCGT", 30,
  "OCGT", 30,
  "Fuel oil PGT", 30,
  "Nuclear PGT", 40
)

typical_capacities_for_renewables<- tribble(
  ~technology, ~typical_capacity,
  "Offshore wind PGT", 600,
  "Onshore wind PGT", 600,
  "Hydroelectric", 250,
  "Photovoltaic PGT", 500,
  "Biomass CHP", 500
)

## How to get typical efficiencies:
# typical_efficiencies <- read_csv(file = "~/Development/java-projects/emlab-generation2/resources/data/learningCurves.csv") %>% 
#   select(Technology = 1, Efficiency = `2015`) %>%
#   filter(grepl("Eff",Technology)) %>% 
#   mutate(Technology = fct_recode(Technology, !!!recode_technologies_from_eff_to_emlab))

typical_efficiencies <- tribble(
  ~technology, ~efficiency,
  "Photovoltaic PGT", 1.10, 
  "Onshore wind PGT", 1,    
  "Offshore wind PGT", 1.02,
  "Biomass CHP", 0.35, 
  "Hydroelectric", 0.9, # source scenario file
  "OCGT", 0.38,
  "Coal PSC", 0.44,
  "Lignite PSC", 0.45,
  "CCGT", 0.59, 
  "Fuel oil PGT", 0.35,
  "Nuclear PGT", 0.33
)


# Data --------------------------------------------------------------------


if(!exists("raw_data")){
  
  raw_data <- list()

  national_generation_capacity <- read_csv(
    file.path(data_folder,"opsd-national_generation_capacity-2019-12-02/national_generation_capacity_stacked.csv"))
  
  raw_data[["opsd_renewable_power_plants"]] <- read_csv(
    file.path(data_folder,"opsd-renewable_power_plants-2019-04-05/renewable_power_plants_EU.csv"))
  
  raw_data[["opsd_conventional_power_plants_DE"]] <- read_csv(
    file.path(data_folder,"opsd-conventional_power_plants-2018-12-20/conventional_power_plants_DE.csv"))
  
  raw_data[["opsd_conventional_power_plants_EU"]] <- read_csv(
    file.path(data_folder,"opsd-conventional_power_plants-2018-12-20/conventional_power_plants_EU.csv"), 
    col_types = cols(
      chp = col_character(),
      type = col_character(),
      additional_info = col_character()))
  
  raw_data[["elia_conventional_power_plants_BE"]] <- read_xls(
    path = file.path(data_folder,"elia-be-ProductionParkOverview-Belgium-2020_edited.xls"), range = "B2:M112")

}
# Total capacity (ENTSOE-E) ----------------------------------------------------------

national_generation_capacity_data <- national_generation_capacity %>% 
  select(technology, source, year, type, country, capacity) %>% 
  filter(
    country %in% my_countries,
    year == 2015, source == "ENTSO-E SOAF") %>%
  filter(technology %in% c("Solar", "Onshore", "Offshore", "Nuclear", "Oil", "Natural gas", "Lignite", "Hydro", "Hard coal", "Biomass and biogas")) %>% 
  mutate(technology = normalise_technology_names(technology, from = "opsd_stats_name"))
  
if(show_debug){
  national_generation_capacity_data %>% 
    group_by(technology, country) %>% 
    summarise(total_cap = sum(capacity)) %>% 
    ggplot(mapping = aes(x = technology, y = total_cap, fill = country)) + 
    geom_col() +
    coord_flip() 
}


# Renewables all countries ----------------------------------------------------------

if(show_debug){
  # There are too many plants in the dataset to handle efficiently in EMlab!
  # Hence, estimate plants based on capacity
  raw_data[["opsd_renewable_power_plants"]] %>% 
    filter(
      country %in% my_countries) %>% 
    group_by(country, energy_source_level_2) %>% 
    count() %>% 
    arrange(n) 
}


plants[["renewables"]] <- national_generation_capacity_data %>% 
  filter(technology %in% technologies[["renewables"]]) %>% 
  left_join(typical_capacities_for_renewables, by = "technology") %>% 
  mutate(
    number_of_plants = floor(capacity / typical_capacity), # number of plants without last one
    last_plant_capacity = capacity %% typical_capacity) %>% 
  select(technology, country, typical_capacity, number_of_plants, last_plant_capacity) %>% 
  pmap_dfr(new_renewables_list) %>% 
  add_age_and_efficiencies() 
  

plants_and_owners[["renewables_countries_A"]] <- plants[["renewables"]] %>%
  filter(country %in% my_countries_A) %>% 
  add_owners(my_countries_A_renwables_owners) %>% 
  add_column(node = my_countries_A_node)

plants_and_owners[["renewables_countries_B"]] <- plants[["renewables"]] %>% 
  filter(country %in% my_countries_B) %>% 
  add_owners(my_countries_B_renwables_owners) %>% 
  add_column(node = my_countries_B_node)


if(show_debug){
  plants_and_owners[["renewables_countries_A"]] %>% 
    group_by(technology,node) %>% 
    count() %>% 
    print()
  
  plants_and_owners[["renewables_countries_B"]] %>% 
    group_by(technology,node) %>% 
    count() %>% 
    print()
}

# Conventional A countries like DE -----------------------------------------------------------------

# taking net capacity because gross is less relevant (includes capacity used by power plant istself)

if("DE" %in% my_countries_A){
  
  plants[["conventional_countries_A_raw"]] <-  raw_data[["opsd_conventional_power_plants_DE"]] %>%
    mutate(
      technology = normalise_technology_names(fuel, from = "opsd_stats_name"),
      age = 2015-commissioned
    ) %>%
    filter(
      !is.na(capacity_net_bnetza),
      age >= 0,
      status == "operating",
      technology %in% technologies[["conventional"]]) %>%
    select(
      name = name_bnetza,
      technology,
      age,
      capacity = capacity_net_bnetza,
      efficiency_estimate = efficiency_estimate) %>% 
    add_age_and_efficiencies() %>% 
    mutate(
      final_efficiency = ifelse(is.na(efficiency_estimate), efficiency, efficiency_estimate)) %>% 
    select(-efficiency_estimate, -efficiency) %>% 
    rename(efficiency = final_efficiency) %>% 
    add_column(country = "DE")
    
}

# Conventional B countries like FR, NL, BE ----------------------------------------------------------------------

plants[["conventional_countries_B_raw"]] <- raw_data[["opsd_conventional_power_plants_EU"]] %>% 
  select(
    name,
    country,
    capacity, 
    commissioned,
    technology = energy_source
  ) %>% 
  filter(
    country %in% my_countries_B,
    commissioned < 2015 | is.na(commissioned)
    ) %>% 
  mutate(
    technology = normalise_technology_names(technology, from = "opsd_stats_name"),
    age = 2015 - commissioned
  ) %>% 
  filter(    
    technology %in% technologies[["conventional"]]) 

if("BE" %in% my_countries_B){
  
  raw_data[["elia_conventional_power_plants_BE"]] %>% 
    group_by(`Fuel for publication`,`Plant Type`) %>% 
    count()
  
  plants[["elia_conventional_power_plants_BE"]] <- raw_data[["elia_conventional_power_plants_BE"]] %>% 
    select(
      name = `Generation plant`, 
      technology = `Fuel for publication`, 
      capacity = `Technical Nominal Power (MW)`) %>% 
    mutate(
      technology = normalise_technology_names(technology, from = "elia_be"),
      technology = fct_recode(technology, `Biomass CHP` = "Other"),
      age = NA,
      commissioned = NA,
      country = "BE")
  
  plants[["conventional_countries_B_raw"]] <- bind_rows(
    plants[["conventional_countries_B_raw"]], plants[["elia_conventional_power_plants_BE"]])
  
}

plants[["conventional_countries_B_raw"]] <- plants[["conventional_countries_B_raw"]] %>% 
  add_age_and_efficiencies()
  

# All Conventional (add missing data) --------------------------------------------------------

# A country
plants_and_owners[["conventional_DE"]] <- plants[["conventional_countries_A_raw"]] %>% 
  add_owners(owners$countries_A_conventional)

  
plants_and_owners[["conventional_DE_scaled"]] <- calculate_difference_in_capacities(
  plants_and_owners[["conventional_DE"]], countries = c("DE"), scale = TRUE) %>% 
  select(-capacity, difference) %>% 
  rename(capacity = capacity_scaled) %>% 
  add_column(node = "deNode")

# B country
plants_and_owners[["conventional_countries_B"]] <- plants[["conventional_countries_B_raw"]] %>%
  add_owners(owners$countries_B_conventional) 

plants_and_owners[["conventional_countries_B_scaled"]] <- calculate_difference_in_capacities(
  plants_and_owners[["conventional_countries_B"]], countries = my_countries_B, scale = TRUE) %>% 
  select(-capacity, difference) %>% 
  rename(capacity = capacity_scaled) %>% 
  add_column(node = my_countries_B_node)



if(show_debug){
  plants_and_owners[["conventional_DE_scaled"]] %>% 
    group_by(technology,node) %>% 
    count() %>% 
    print()
  
  plants_and_owners[["conventional_countries_B_scaled"]] %>% 
    group_by(technology,node) %>% 
    count() %>% 
    print()
}

# Combine data --------------------------------------------------------

# And Remove double counting of power plants such as hydro and Biomass

plants_and_owners[["all"]] <- bind_rows(
  plants_and_owners[["conventional_DE_scaled"]],
  plants_and_owners[["renewables_countries_A"]],
  plants_and_owners[["conventional_countries_B_scaled"]], 
  plants_and_owners[["renewables_countries_B"]]
  ) %>% 
  select(-commissioned, -difference)

if(save){
  #format for emlab: # Name, Technology, Location, Age, Owner, Capacity, Efficiency
  plants_and_owners[["all"]]  %>%
    select(Name = name, Technology = technology, Location = node, Age = age, Owner, Capacity = capacity, Efficiency = efficiency) %>% 
    write_csv(path = file.path(output_folder, paste(prefix_for_result, "final_plants.csv", sep = "_")))
} else {
  warning("Not saved. Output is here:")
}



# Validation --------------------------------------------------------------

# Distribution of different producers

# plants[["all"]] %>% 
#   ggplot(mapping = aes(x = technology)) +
#   geom_bar() + 
#   coord_flip() +
#   facet_wrap(country ~ Owner)

plants_and_owners[["all"]] %>% 
  ggplot(mapping = aes(x = technology, y = capacity)) +
  geom_col() + 
  coord_flip() +
  facet_wrap(country ~ Owner)


# Final df must have the same capacity as in the ENTSOE-E STATS

validation_stats_df <- national_generation_capacity_data %>% 
  select(technology, country, capacity_stats = capacity)

validation_calc_df <- plants_and_owners[["all"]] %>%
  group_by(technology, country) %>% 
  summarise(capacity_calculated = sum(capacity, na.rm = TRUE)) %>%  ungroup()

full_join(validation_stats_df, validation_calc_df) %>% 
  mutate(diff = capacity_calculated / capacity_stats)


