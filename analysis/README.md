# Analysis script for EMLab generation



## Overview

This shiny app is an attempt to make the analysis of EMLab Generation output convenient and flexible. 

With it is possible to:

- analyse runs from EMlab generation with a graphical and interactive shiny interface
- generate custom reports for further use in publications

## First steps

1. Create your own config file from the example_config.R

config file

To run the shiny app, open app.R and execute all code. By default, the app opens the most recent simulation run. You can customise this behaviour by setting id_to_load in app.R (or alternatively config.R).

Parsing the result files may take a while (we work on making this quicker).
