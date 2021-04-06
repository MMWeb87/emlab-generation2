/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emlab.gen.reporters;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.writer.CSVEntryConverter;
import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;
import emlab.gen.domain.market.electricity.FinancialPowerPlantReport;
import emlab.gen.engine.AbstractReporter;
import emlab.gen.engine.Schedule;
import emlab.gen.repository.Reps;
import emlab.gen.role.investment.CapacityExpectationReport;
import emlab.gen.role.investment.FinancialExpectationReport;
import emlab.gen.role.investment.MarketInformationReport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 *
 * @author ejlchappin
 */
public class DefaultReporter extends AbstractReporter {

	public DefaultReporter() {
		super();
	}

	public void initiate() {

	}

	@Override
	public void report(Schedule schedule) {

		//Default file of reporters over time
		logger.info("Writing log for tick " + schedule.getCurrentTick());

		Boolean writeReportMain = true;
		Boolean writeReportPowerplants = true;
		Boolean writeReportMarketInformation = false;
		Boolean writeReportFinancialExpectation = true;
		Boolean writeReportSegments = false;
		Boolean writeCapacityExpectation = true;
		
		String outputDirectoryName = this.getReporterDirectoryName();


		if(writeReportMain) {
			
			// Main csv
			String outputFileName = schedule.runID + "-" + "main.csv";
			File outputfile = new File(outputDirectoryName + outputFileName);

			//Write header if needed
			if (!outputfile.exists()) {
				logger.warning("File does not exist yet, writing header in csv, for tick: " + schedule.getCurrentTick() + ", and iteration " + schedule.getIteration());

				try {
					schedule.reporter.lockMainCSV.lock();
					FileWriter fileWriter = new FileWriter(outputfile, false);
					CSVWriter<Schedule> csvWriter = new CSVWriterBuilder<Schedule>(fileWriter)
							.entryConverter(new DefaultCSVConverterHeaders())
							.strategy(CSVStrategy.DEFAULT)
							.build();

					csvWriter.write(schedule); //write headers in first tick
					fileWriter.flush();
					fileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					//release writing lock for file
					schedule.reporter.lockMainCSV.unlock();
				}
			}

			//Write current tick data to main csv 
			try {
				// get writing lock
				schedule.reporter.lockMainCSV.lock();
				FileWriter fileWriter = new FileWriter(outputfile, true);
				CSVWriter<Schedule> csvWriter = new CSVWriterBuilder<Schedule>(fileWriter)
						.entryConverter(new DefaultCSVConverter())
						.strategy(CSVStrategy.DEFAULT)
						.build();

				csvWriter.write(schedule);

				fileWriter.flush();
				fileWriter.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				//release writing lock for file
				schedule.reporter.lockMainCSV.unlock();
			}

		}




		if(writeReportPowerplants) {


			//Power plant file               
			String powerplantFileName = schedule.runID + "-" + "Powerplants.csv";
			File powerplantfile = new File(outputDirectoryName + powerplantFileName);

			//Write header if needed
			if (!powerplantfile.exists()) {
				try {
					schedule.reporter.lockPowerPlantCSV.lock();
					FileWriter powerplantfileWriter = new FileWriter(powerplantfile, false);
					CSVWriter<FinancialPowerPlantReport> powerplantCSVWriter = new CSVWriterBuilder<FinancialPowerPlantReport>(powerplantfileWriter)
							.entryConverter(new FinancialPowerPlantReportCSVConverterHeaders())
							.strategy(CSVStrategy.DEFAULT)
							.build();
					powerplantCSVWriter.write(null);
					powerplantfileWriter.flush();
					powerplantfileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					//release writing lock for file
					schedule.reporter.lockPowerPlantCSV.unlock();
				}
			}

			try {
				schedule.reporter.lockPowerPlantCSV.lock();
				FileWriter powerplantfileWriter = new FileWriter(powerplantfile, true);
				CSVWriter<FinancialPowerPlantReport> powerplantCSVWriter = new CSVWriterBuilder<FinancialPowerPlantReport>(powerplantfileWriter)
						.entryConverter(new FinancialPowerPlantReportCSVConverter())
						.strategy(CSVStrategy.DEFAULT)
						.build();

				//write report per power plant
				powerplantCSVWriter.writeAll(schedule.reps.findAllFinancialPowerPlantReportsForTime(schedule.getCurrentTick()));
				powerplantfileWriter.flush();
				powerplantfileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				//release writing lock for file
				schedule.reporter.lockPowerPlantCSV.unlock();
			}

		}


		if(writeReportMarketInformation) {




			// MarketInformation file               
			String marketinfoFileName = schedule.runID + "-" + "MarketInformation.csv";
			File marketinfofile = new File(outputDirectoryName + marketinfoFileName);

			//Write header if needed
			if (!marketinfofile.exists()) {
				try {
					schedule.reporter.lockMarketInformationCSV.lock();
					FileWriter marketinfoFileWriter = new FileWriter(marketinfofile, false);
					CSVWriter<MarketInformationReport> marketinfoCSVWriter = new CSVWriterBuilder<MarketInformationReport>(marketinfoFileWriter)
							.entryConverter(new MarketInformationReportCSVConverterHeaders())
							.strategy(CSVStrategy.DEFAULT)
							.build();
					//                marketinfoCSVWriter.write(schedule.reps.marketInformationReports.get(1));
					marketinfoCSVWriter.write(null);
					marketinfoFileWriter.flush();
					marketinfoFileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					//release writing lock for file
					schedule.reporter.lockMarketInformationCSV.unlock();
				}
			}

			try {
				schedule.reporter.lockMarketInformationCSV.lock();
				FileWriter marketinfoFileWriter = new FileWriter(marketinfofile, true);
				CSVWriter<MarketInformationReport> marketinfoCSVWriter = new CSVWriterBuilder<MarketInformationReport>(marketinfoFileWriter)
						.entryConverter(new MarketInformationReportCSVConverter())
						.strategy(CSVStrategy.DEFAULT)
						.build();

				//write report per power plant
				marketinfoCSVWriter.writeAll(schedule.reps.marketInformationReports);
				marketinfoFileWriter.flush();
				marketinfoFileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				//release writing lock for file
				schedule.reporter.lockMarketInformationCSV.unlock();

			}


		}

		if(writeReportFinancialExpectation) {



			// FinancialExpectation file               
			String financialExpectationFileName = schedule.runID + "-" + "FinancialExpectation.csv";
			File financialExpectationFile = new File(outputDirectoryName + financialExpectationFileName);

			//Write header if needed
			if (!financialExpectationFile.exists()) {
				try {
					schedule.reporter.lockFinancialExpectationCSV.lock();
					FileWriter financialExpectationFileWriter = new FileWriter(financialExpectationFile, false);
					CSVWriter<FinancialExpectationReport> financialExpectationCSVWriter = new CSVWriterBuilder<FinancialExpectationReport>(financialExpectationFileWriter)
							.entryConverter(new FinancialExpectationReportCSVConverterHeaders())
							.strategy(CSVStrategy.DEFAULT)
							.build();
					//                marketinfoCSVWriter.write(schedule.reps.marketInformationReports.get(1));
					financialExpectationCSVWriter.write(null);
					financialExpectationFileWriter.flush();
					financialExpectationFileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					//release writing lock for file
					schedule.reporter.lockFinancialExpectationCSV.unlock();
				}
			}

			try {
				schedule.reporter.lockFinancialExpectationCSV.lock();
				FileWriter financialExpectationFileWriter = new FileWriter(financialExpectationFile, true);
				CSVWriter<FinancialExpectationReport> financialExpectationCSVWriter = new CSVWriterBuilder<FinancialExpectationReport>(financialExpectationFileWriter)
						.entryConverter(new FinancialExpectationReportCSVConverter())
						.strategy(CSVStrategy.DEFAULT)
						.build();

				//write report per power plant
				financialExpectationCSVWriter.writeAll(schedule.reps.financialExpectationReports);
				financialExpectationFileWriter.flush();
				financialExpectationFileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				//release writing lock for file
				schedule.reporter.lockFinancialExpectationCSV.unlock();


			}

		}
		
		
		if(writeCapacityExpectation) {



			// FinancialExpectation file               
			String capacityExpectationFileName = schedule.runID + "-" + "CapacityExpectation.csv";
			File capacityExpectationFile = new File(outputDirectoryName + capacityExpectationFileName);

			//Write header if needed
			if (!capacityExpectationFile.exists()) {
				try {
					schedule.reporter.lockCapacityExpectationCSV.lock();
					FileWriter capacityExpectationFileWriter = new FileWriter(capacityExpectationFile, false);
					CSVWriter<CapacityExpectationReport> capacityExpectationCSVWriter = new CSVWriterBuilder<CapacityExpectationReport>(capacityExpectationFileWriter)
							.entryConverter(new CapacityExpectationReportCSVConverterHeaders())
							.strategy(CSVStrategy.DEFAULT)
							.build();
					//                marketinfoCSVWriter.write(schedule.reps.marketInformationReports.get(1));
					capacityExpectationCSVWriter.write(null);
					capacityExpectationFileWriter.flush();
					capacityExpectationFileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					//release writing lock for file
					schedule.reporter.lockCapacityExpectationCSV.unlock();
				}
			}

			try {
				schedule.reporter.lockCapacityExpectationCSV.lock();
				FileWriter capacityExpectationFileWriter = new FileWriter(capacityExpectationFile, true);
				CSVWriter<CapacityExpectationReport> capacityExpectationCSVWriter = new CSVWriterBuilder<CapacityExpectationReport>(capacityExpectationFileWriter)
						.entryConverter(new CapacityExpectationReportCSVConverter())
						.strategy(CSVStrategy.DEFAULT)
						.build();

				//write report per power plant
				capacityExpectationCSVWriter.writeAll(schedule.reps.capacityExpectationReports);
				capacityExpectationFileWriter.flush();
				capacityExpectationFileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				//release writing lock for file
				schedule.reporter.lockCapacityExpectationCSV.unlock();


			}

		}


		if(writeReportSegments) {

			//Segment file               
			String segmentFileName = schedule.runID + "-" + "Segments.csv";
			File segmentfile = new File(outputDirectoryName + segmentFileName);

			//Write header if needed
			if (!segmentfile.exists()) {
				try {
					schedule.reporter.lockSegmentCSV.lock();
					FileWriter segmentfileWriter = new FileWriter(segmentfile, false);
					CSVWriter<Schedule> segmentCSVWriter = new CSVWriterBuilder<Schedule>(segmentfileWriter)
							.entryConverter(new SegmentCSVConverterHeaders())
							.strategy(CSVStrategy.DEFAULT)
							.build();
					segmentCSVWriter.write(schedule);
					segmentfileWriter.flush();
					segmentfileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					//release writing lock for file
					schedule.reporter.lockSegmentCSV.unlock();
				}
			}

			try {
				schedule.reporter.lockSegmentCSV.lock();
				FileWriter segmentfileWriter = new FileWriter(segmentfile, true);
				CSVWriter<Schedule> segmentCSVWriter = new CSVWriterBuilder<Schedule>(segmentfileWriter)
						.entryConverter(new SegmentCSVConverter())
						.strategy(CSVStrategy.DEFAULT)
						.build();

				//write report
				segmentCSVWriter.write(schedule);
				segmentfileWriter.flush();
				segmentfileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				//release writing lock for file
				schedule.reporter.lockSegmentCSV.unlock();

			}

		}



	}


}
