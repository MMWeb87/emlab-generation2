
# columns to include in every data obtained by (get_data_by_prefix)
meta_cols <- c("iteration", "tick", "market", "producer", "technology", "node", "plant")

# Segment energy ----------------------------------------------------

data[["expected_ROEs"]] <- raw_financialexpectations_results %>% 
  get_var_from_single_column(prefix = NULL, value = "ROE")

#show_filters[["expected_ROEs"]] <- c("market", "producer", "tick")
show_filters[["expected_ROEs"]] <- c("producer")


plots[["expected_ROEs"]] <- function(data, input, average = TRUE){
  
  data <- data %>% 
    filter(producer %in% input$producers_checked) %>% 
    filter(iteration == 1) %>% # TODO
    ggplot(mapping = aes(x = ROE)) +
      geom_histogram(mapping = aes(fill = producer)) +
      facet_grid(tick ~ market, labeller = label_both) +
      scale_x_continuous(labels = scales::percent) +
      scale_fill_custom("producer_colors") +
      labs_default(
        y = glue("Number of occurences"),
        x = "Expected ROE",
        title = get_title_of_selected_plot(input),
        subtitle = "Showing only iteration 1 (TODO)",
        fill = "Energy producer"
        )
}



