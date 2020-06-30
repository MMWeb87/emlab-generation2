/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emlab.gen.reporters;

import com.googlecode.jcsv.writer.CSVEntryConverter;

import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.FinancialPowerPlantReport;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.Substance;
import emlab.gen.engine.Schedule;
import emlab.gen.role.investment.FinancialExpectationReport;
import emlab.gen.role.investment.MarketInformationReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ejlchappin
 * @author marcmel
 */
public class FinancialExpectationReportCSVConverterHeaders implements CSVEntryConverter<FinancialExpectationReport> {

    FinancialExpectationReportCSVConverterHeaders() {

    }

    public String[] convertEntry(FinancialExpectationReport report) {
        List<String> row = new ArrayList();
        
        row.add("iteration");
        row.add("tick");
        row.add("market");
        row.add("producer");
        row.add("technology");
        row.add("plant");
        row.add("node");
        row.add("investmentRound");

        
        row.add("ROI");
        row.add("ROE");
        row.add("debtratio");
        row.add("discounted.capital_cost");
        row.add("discounted.operating_cost");
        row.add("discounted.operating_profit");
        
        row.add("expected.generation");
        row.add("expected.gross_profit");
        row.add("expected.marginal_cost");
        
        row.add("expected.operating_cost");
        row.add("expected.operating_revenue");
        
        row.add("project.cost");
        row.add("project.value");
        row.add("runninghours");
        row.add("wacc");
        
        row.add("utility.total");


        return row.toArray(new String[row.size()]);

    }
}
