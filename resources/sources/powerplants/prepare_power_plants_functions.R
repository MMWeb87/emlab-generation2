normalise_technology_names <- function(technologies_vector, from, to = "emlab_name", translation_table = technology_translations){
  
  recode_var <- technology_translations %>% pull(from)
  names(recode_var) <- technology_translations %>% pull(to)
  
  suppressWarnings(
    fct_recode(technologies_vector, !!!recode_var) %>% 
      as.character()
  )
  
}

#' Add randmon age if missing (or keep).
#' Also adds typical efficiencies and typical age of plants
#'
#' @param plants_df dataframe with columns technology and age
#' @param max_age maximal age
#' @param seed 
#'
#' @return df
add_age_and_efficiencies <- function(plants_df, max_age = 30, seed = random_seed){

  n <- nrow(plants_df)

  set.seed(seed)

  random_ages <- sample(1:max_age, n , replace = T) # TODO: actually use typical age
  
  plants_df %>% 
    left_join(typical_efficiencies, by = "technology") %>% 
    left_join(typical_age_for_plants, by = "technology") %>% 
    mutate(
      random_age = random_ages,
      final_age = ifelse(is.na(age), random_age, age)) %>%
    select(-age, -typical_age, -random_age) %>% 
    rename(age = final_age)
  
}

#' Add randmon owners from list to power plants df
#'
#' @param plants_df dataframe with plants
#' @param seed 
#'
#' @return df
add_owners <- function(plants_df, owners_vector, seed = random_seed){
  # adding Age and Owners randomly
  # expects:  technology age
  
  owners_df <- tibble(Owner = owners_vector)
  
  n <- nrow(plants_df)
  
  set.seed(seed)
  random_owners <- sample_n(owners_df, size = n, replace = TRUE) %>% pull(Owner)

  
  plants_df %>% 
    add_column(Owner = random_owners)
  
}

#' Generates a list of renewable plants with number of plants
new_renewables_list <- function(technology, country, typical_capacity, number_of_plants, last_plant_capacity, owners = all_owners){
  
  # Generate as many rows as there are number of plants
  if(number_of_plants > 0){
    tibble(
      name = paste(technology, "plant", country, seq(number_of_plants)),
      country,
      capacity = typical_capacity,
      technology,
      age = NA
    ) %>% 
      add_row(
        name = paste("Last", technology, "plant", country),
        country,
        capacity = last_plant_capacity,
        technology,
        age = NA
      )
  
  } else {
    # Only one plant if number of number_of_plants = 0
    if(last_plant_capacity > 0){
      tibble(
        name = paste("Only", technology, "plant", country),
        country,
        capacity = last_plant_capacity,
        technology,
        age = NA
      ) 
    }
  }

}



#' Calculates the difference to capacity statistics which we assume to be accurate
#' to compensate for missing power plants, we increase/decrease the capacity of power plants 
#' in plants_df.
#'
#' @param plants_df 
#' @param countries 
#' @param scale 
#'
#' @return plants_df
calculate_difference_in_capacities <- function(plants_df, countries, scale = TRUE){
  
  capacities <- list()
  
  # Verify capacity with opsd stats
  capacities[["actual"]] <- national_generation_capacity_data %>% 
    filter(
      country %in% countries,
      technology %in% technologies[["conventional"]]) %>%
    group_by(technology) %>% 
    summarise(total_capacity = sum(capacity))
  
  capacities[["generated"]] <- plants_df %>% 
    group_by(technology) %>% 
    summarise(total_capacity = sum(capacity))
  
  capacities[["diff"]] <- capacities$actual %>% 
    left_join(capacities$generated, by = c("technology"), suffix = c(".actual", ".generated")) %>% 
    mutate(difference = total_capacity.actual / total_capacity.generated)
  
  if(show_debug){
    print("Differences for")
    print(capacities[["diff"]])
  }

  if(scale){
    plants_df <- plants_df %>% 
      left_join(capacities$diff %>% select(technology, difference), by = "technology") %>% 
      mutate(capacity_scaled = capacity * difference)
  } else {
    plants_df <- plants_df %>% 
      mutate(capacity_scaled = capacity)
  }
  
  plants_df
  
}