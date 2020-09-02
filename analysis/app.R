#
# This is a Shiny web application of EMLab Generation 2. 
# You can run the application by clicking the 'Run App' button above.
#

library(shiny)

rm(list=ls())

# Comment to always load latest reporters and logs
# Uncomment and specify to load specific reporters and logs


#id_to_load <- 1598451150039 #Baseline B1
#id_to_load <- 1598514258803

source(file = "emlab/init.R")

# Run the application (ui and server are in the folder emlab)
runApp("emlab")