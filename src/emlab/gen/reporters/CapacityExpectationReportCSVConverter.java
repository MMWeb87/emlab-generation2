/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emlab.gen.reporters;

import com.googlecode.jcsv.writer.CSVEntryConverter;

import emlab.gen.role.investment.CapacityExpectationReport;
import emlab.gen.role.investment.FinancialExpectationReport;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ejlchappin
 */
public class CapacityExpectationReportCSVConverter implements CSVEntryConverter<CapacityExpectationReport> {

    CapacityExpectationReportCSVConverter() {

    }

    public String[] convertEntry(CapacityExpectationReport report) {
        
    	List<String> row = new ArrayList();
        
        row.add(String.valueOf(report.schedule.iteration));
        row.add(String.valueOf(report.getTime()));
        row.add(String.valueOf(report.getMarket().getName()));
        row.add(String.valueOf(report.getAgent().getName()));
        row.add(String.valueOf(report.getTechnology().getName()));
		row.add(String.valueOf(report.getPlant().getName()));
		row.add(String.valueOf(report.getNode().getName()));
		row.add(Boolean.valueOf(report.getViable()));
		row.add(Double.valueOf(report.getViableReason()));


	    		
        return row.toArray(new String[row.size()]);
        




    }
}
