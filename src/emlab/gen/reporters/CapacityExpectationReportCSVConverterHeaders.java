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
import emlab.gen.role.investment.CapacityExpectationReport;
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
public class CapacityExpectationReportCSVConverterHeaders implements CSVEntryConverter<CapacityExpectationReport> {

    CapacityExpectationReportCSVConverterHeaders() {

    }

    public String[] convertEntry(CapacityExpectationReport report) {
        List<String> row = new ArrayList();
        
        row.add("iteration");
        row.add("tick");
        row.add("market");
        row.add("producer");
        row.add("technology");
        row.add("plant");
        row.add("node");
        row.add("viable");
        row.add("viablereason");

       
        return row.toArray(new String[row.size()]);

    }
}
