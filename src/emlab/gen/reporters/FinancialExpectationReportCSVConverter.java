/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emlab.gen.reporters;

import com.googlecode.jcsv.writer.CSVEntryConverter;

import emlab.gen.role.investment.FinancialExpectationReport;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ejlchappin
 */
public class FinancialExpectationReportCSVConverter implements CSVEntryConverter<FinancialExpectationReport> {

    FinancialExpectationReportCSVConverter() {

    }

    public String[] convertEntry(FinancialExpectationReport report) {
        
    	List<String> row = new ArrayList();
        
        row.add(String.valueOf(report.schedule.iteration));
        row.add(String.valueOf(report.getTime()));
        row.add(String.valueOf(report.getMarket().getName()));
        row.add(String.valueOf(report.getAgent().getName()));
        row.add(String.valueOf(report.getTechnology().getName()));
		row.add(String.valueOf(report.getPlant().getName()));
		row.add(String.valueOf(report.getNode().getName()));
		row.add(String.valueOf(report.getInvestmentRound()));

      
		row.add(String.valueOf(report.getProjectReturnOnInvestment()));
		row.add(String.valueOf(report.getProjectReturnOnEquity()));
		row.add(String.valueOf(report.getMappedProjectReturnOnEquity()));
		
		row.add(String.valueOf(report.getDebtRatioOfInvestments()));
		row.add(String.valueOf(report.getDiscountedCapitalCosts()));
		row.add(String.valueOf(report.getDiscountedOperatingCost()));
		row.add(String.valueOf(report.getDiscountedOperatingProfit()));
		row.add(String.valueOf(report.getExpectedGeneration()));
		row.add(String.valueOf(report.getExpectedGrossProfit()));
		row.add(String.valueOf(report.getExpectedMarginalCost()));
		row.add(String.valueOf(report.getExpectedOperatingCost()));
		row.add(String.valueOf(report.getExpectedOperatingRevenue()));
		row.add(String.valueOf(report.getProjectCost()));
		row.add(String.valueOf(report.getProjectValue()));
		row.add(String.valueOf(report.getRunningHours()));
		row.add(String.valueOf(report.getWacc()));
		row.add(String.valueOf(report.getTotalUtility()));
  
	    		
        return row.toArray(new String[row.size()]);
        




    }
}
