/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emlab.gen.scenarios;

import emlab.gen.domain.agent.BigBank;
import emlab.gen.engine.Scenario;
import emlab.gen.domain.agent.EMLabModel;
import emlab.gen.domain.agent.EnergyConsumer;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.NationalGovernment;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.agent.TargetInvestor;
import emlab.gen.domain.factory.FuelFactory;
import emlab.gen.domain.factory.LDCFactory;
import emlab.gen.domain.factory.PowerPlantCSVFactory;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.PowerGeneratingTechnologyTarget;
import emlab.gen.domain.policy.renewablesupport.BiasFactor;
import emlab.gen.domain.policy.renewablesupport.RenewablePotentialLimit;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.RenewableTarget;
import emlab.gen.domain.technology.Interconnector;
import emlab.gen.domain.technology.IntermittentResourceProfile;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.engine.Schedule;
import emlab.gen.repository.Reps;
import emlab.gen.role.investment.InvestInPowerGenerationTechnologiesRole;
import emlab.gen.role.investment.InvestInPowerGenerationTechnologiesWithTenderRole;
import emlab.gen.role.investment.TargetInvestmentRole;
import emlab.gen.trend.GeometricTrend;
import emlab.gen.trend.HourlyCSVTimeSeries;
import emlab.gen.trend.StepTrend;
import emlab.gen.trend.TimeSeriesCSVReader;
import emlab.gen.trend.TriangularTrend;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author ejlchappin
 * @author marcmel
 */
public class Scenario_NL_intermittent_auction_toy implements Scenario {

    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void build(Schedule schedule) {
        
        Reps reps = schedule.reps;
        
        //Main simulation
        reps.emlabModel = new EMLabModel();
        reps.emlabModel.setExitSimulationAfterSimulationLength(true);
        reps.emlabModel.setSimulationLength(20); // input number of ticks to run
        //reps.emlabModel.setDeletionAge(5);
        reps.emlabModel.setDeletionOldPPDPBidsAndCashFlowsEnabled(true);//speed up simulation by deleting old objects
        reps.emlabModel.setCapDeviationCriterion(0.03);
        reps.emlabModel.setIterationSpeedCriterion(0.005);
        reps.emlabModel.setIterationSpeedFactor(3);
        reps.emlabModel.setRealRenewableDataImplemented(true);
        reps.emlabModel.setCo2TradingImplemented(true);
        
        reps.emlabModel.setFeedInPremiumImplemented(true);
        reps.emlabModel.setRenewableTenderSchemeImplemented(true);
       
        reps.emlabModel.setName("EMLab Model");

        //Demand
        TriangularTrend demandGrowthTrendNL = new TriangularTrend();
        demandGrowthTrendNL.setTop(1.02);
        demandGrowthTrendNL.setMax(1.03);
        demandGrowthTrendNL.setMin(0.98);
        demandGrowthTrendNL.setStart(1.00);

//        TimeSeriesImpl demandGrowthTrendNL = new TimeSeriesImpl();
//        double[] demandGrowth = {1,1,1,1,.9,1,1,1,1,1,1.2,1,1,1,1,1,1,1,1,1,1,1,1,1};
//        demandGrowthTrendNL.setTimeSeries(demandGrowth);
        

        FuelFactory fuelFactory = new FuelFactory(reps);
        Substance biomass = fuelFactory.createFuel("biomass",0,25000,0.5,new TriangularTrend(1.01,1.05,.97,112.5));
        Substance uranium = fuelFactory.createFuel("uranium",0,3.8e9,1,new TriangularTrend(1.01,1.02,1,5000000));
        Substance fuelOil = fuelFactory.createFuel("fueloil",7.5,11600,1,new TriangularTrend(1.01,1.04,.96,2.5));
        Substance hardCoal = fuelFactory.createFuel("hardcoal",2.784,29000,1,new TriangularTrend(1,1.04,.97,60));
        Substance ligniteCoal = fuelFactory.createFuel("lignitecoal",0.41,3600,1,new TriangularTrend(1,1.02,.98,22));
        Substance naturalGas = fuelFactory.createFuel("naturalgas",0.00187,36,1,new TriangularTrend(1.01,1.06,.95,.32));
        
        Substance electricity = new Substance();
        electricity.setName("electricity");
        electricity.setCo2Density(0);
        electricity.setEnergyDensity(0);
        electricity.setQuality(1);
        reps.substances.add(electricity);

        Substance co2 = new Substance();
        co2.setName("CO2");
        co2.setCo2Density(1);
        co2.setEnergyDensity(0);
        co2.setQuality(1);
        reps.substances.add(co2);

        Zone nl = new Zone();
        nl.setName("nl");
        reps.zones.add(nl);
  
        Zone de = new Zone();
        de.setName("de");
        reps.zones.add(de);

        PowerGridNode nlNode = new PowerGridNode();
        nlNode.setName("nlNode");
        nlNode.setCapacityMultiplicationFactor(1.0);
        nlNode.setZone(nl);
        reps.powerGridNodes.add(nlNode);
        
        
        TimeSeriesCSVReader lcReaderNL = new TimeSeriesCSVReader();
        lcReaderNL.setFilename("/data/ldcNLDE-hourly.csv");
        lcReaderNL.setDelimiter(",");
        lcReaderNL.readCSVVariable("NL");
        nlNode.setHourlyDemand(lcReaderNL);

        //  Intermittent Production Profiles
        IntermittentResourceProfile windProfileOffShoreNL = new IntermittentResourceProfile();
        windProfileOffShoreNL.setFilename("/data/renewablesNinja2015Profiles.csv");
        windProfileOffShoreNL.setDelimiter(",");
        windProfileOffShoreNL.readCSVVariable("OFF_NL");
        //windProfileOffShoreNL.setTimeSeries(new Random().doubles(8760).toArray()); //TODO random nrs for now
        windProfileOffShoreNL.setIntermittentProductionNode(nlNode);
        reps.intermittentResourceProfiles.add(windProfileOffShoreNL);

        IntermittentResourceProfile windProfileOnShoreNL = new IntermittentResourceProfile();
        windProfileOnShoreNL.setFilename("/data/renewablesNinja2015Profiles.csv");
        windProfileOnShoreNL.setDelimiter(",");
        windProfileOnShoreNL.readCSVVariable("ON_NL");
        //windProfileOnShoreNL.setTimeSeries(new Random().doubles(8760).toArray()); //TODO random nrs for now
        windProfileOnShoreNL.setIntermittentProductionNode(nlNode);
        reps.intermittentResourceProfiles.add(windProfileOnShoreNL);
        
        IntermittentResourceProfile solarProfileNL = new IntermittentResourceProfile();
        solarProfileNL.setFilename("/data/renewablesNinja2015Profiles.csv");
        solarProfileNL.setDelimiter(",");
        solarProfileNL.readCSVVariable("PV_NL");
        //solarProfileNL.setTimeSeries(new Random().doubles(8760).toArray()); //TODO random nrs for now
        solarProfileNL.setIntermittentProductionNode(nlNode);
        reps.intermittentResourceProfiles.add(solarProfileNL);
        
        
        Interconnector interconnectorNetherlandsGermany = new Interconnector();
        interconnectorNetherlandsGermany.setCapacity(0);
        Set<PowerGridNode> connections = new HashSet<>();
        connections.add(nlNode);
        interconnectorNetherlandsGermany.setConnections(connections);
        reps.interconnector = interconnectorNetherlandsGermany;

        //Load duration curve
        LDCFactory ldcFactory = new LDCFactory(reps);
        TimeSeriesCSVReader ldcReader = new TimeSeriesCSVReader();
        ldcReader.setFilename("/data/ldcNL.csv");
        ldcReader.setDelimiter(",");
       
        ldcReader.readCSVVariable("lengthInHours");
        ldcFactory.createSegments(ldcReader.getTimeSeries());
        
        ldcReader.readCSVVariable("load");
        Set<SegmentLoad> loadDurationCurveNL = ldcFactory.createLDC(ldcReader.getTimeSeries());
        
        
        ElectricitySpotMarket netherlandsElectricitySpotMarket = reps.createElectricitySpotMarket("DutchMarket", 2000, 40, false, electricity, demandGrowthTrendNL, loadDurationCurveNL, nl);
        
        reps.createCO2Auction("CO2Auction", 0, true, co2);      
        
        EnergyConsumer energyConsumer = new EnergyConsumer();
        energyConsumer.setName("EnergyConsumer");
        energyConsumer.setContractWillingnessToPayFactor(1.2);
        energyConsumer.setContractDurationPreferenceFactor(.03);
        energyConsumer.setLtcMaximumCoverageFraction(0.8);
        reps.energyConsumers.add(energyConsumer);

        //InvestInPowerGenerationTechnologiesRole defaultInvestmentRole = new InvestInPowerGenerationTechnologiesRole(schedule);
        InvestInPowerGenerationTechnologiesWithTenderRole tenderInvestmentRole = new InvestInPowerGenerationTechnologiesWithTenderRole(schedule);


        EnergyProducer energyProducerNLA = reps.createEnergyProducer();
        energyProducerNLA.setName("Energy Producer NL A");
        energyProducerNLA.setInvestorMarket(netherlandsElectricitySpotMarket);
        energyProducerNLA.setNumberOfYearsBacklookingForForecasting(5);
        energyProducerNLA.setPriceMarkUp(1.0);
        energyProducerNLA.setWillingToInvest(true);
        energyProducerNLA.setDownpaymentFractionOfCash(.5);
        energyProducerNLA.setDismantlingRequiredOperatingProfit(0);
        energyProducerNLA.setDismantlingProlongingYearsAfterTechnicalLifetime(0);
        energyProducerNLA.setDebtRatioOfInvestments(0.7);
        energyProducerNLA.setLoanInterestRate(0.1);
        energyProducerNLA.setEquityInterestRate(0.1);
        energyProducerNLA.setPastTimeHorizon(5);
        energyProducerNLA.setInvestmentFutureTimeHorizon(7);
        energyProducerNLA.setLongTermContractPastTimeHorizon(3);
        energyProducerNLA.setLongTermContractMargin(0.1);
        energyProducerNLA.setCash(3e9);
        energyProducerNLA.setInvestmentRole(tenderInvestmentRole);

        EnergyProducer energyProducerNLB = reps.createEnergyProducer();
        energyProducerNLB.setName("Energy Producer NL B");
        energyProducerNLB.setInvestorMarket(netherlandsElectricitySpotMarket);
        energyProducerNLB.setNumberOfYearsBacklookingForForecasting(5);
        energyProducerNLB.setPriceMarkUp(1.0);
        energyProducerNLB.setWillingToInvest(true);
        energyProducerNLB.setDownpaymentFractionOfCash(.5);
        energyProducerNLB.setDismantlingRequiredOperatingProfit(0);
        energyProducerNLB.setDismantlingProlongingYearsAfterTechnicalLifetime(0);
        energyProducerNLB.setDebtRatioOfInvestments(0.7);
        energyProducerNLB.setLoanInterestRate(0.1);
        energyProducerNLB.setEquityInterestRate(0.1);
        energyProducerNLB.setPastTimeHorizon(5);
        energyProducerNLB.setInvestmentFutureTimeHorizon(7);
        energyProducerNLB.setLongTermContractPastTimeHorizon(3);
        energyProducerNLB.setLongTermContractMargin(0.1);
        energyProducerNLB.setCash(3e9);
        energyProducerNLB.setInvestmentRole(tenderInvestmentRole);
        
//        EnergyProducer energyProducerINTA = reps.createEnergyProducer();
//        energyProducerINTA.setName("Energy Producer International A");
//        energyProducerINTA.setInvestorMarket(netherlandsElectricitySpotMarket);
//        energyProducerINTA.setNumberOfYearsBacklookingForForecasting(5);
//        energyProducerINTA.setPriceMarkUp(1.0);
//        energyProducerINTA.setWillingToInvest(true);
//        energyProducerINTA.setDownpaymentFractionOfCash(.5);
//        energyProducerINTA.setDismantlingRequiredOperatingProfit(0);
//        energyProducerINTA.setDismantlingProlongingYearsAfterTechnicalLifetime(0);
//        energyProducerINTA.setDebtRatioOfInvestments(0.7);
//        energyProducerINTA.setLoanInterestRate(0.1);
//        energyProducerINTA.setEquityInterestRate(0.1);
//        energyProducerINTA.setPastTimeHorizon(5);
//        energyProducerINTA.setInvestmentFutureTimeHorizon(7);
//        energyProducerINTA.setLongTermContractPastTimeHorizon(3);
//        energyProducerINTA.setLongTermContractMargin(0.1);
//        energyProducerINTA.setCash(3e9);
//        energyProducerINTA.setInvestmentRole(tenderInvestmentRole);
//
//        EnergyProducer energyProducerINTB = reps.createEnergyProducer();
//        energyProducerINTB.setName("Energy Producer International B");
//        energyProducerINTB.setInvestorMarket(netherlandsElectricitySpotMarket);
//        energyProducerINTB.setNumberOfYearsBacklookingForForecasting(5);
//        energyProducerINTB.setPriceMarkUp(1.0);
//        energyProducerINTB.setWillingToInvest(true);
//        energyProducerINTB.setDownpaymentFractionOfCash(.5);
//        energyProducerINTB.setDismantlingRequiredOperatingProfit(0);
//        energyProducerINTB.setDismantlingProlongingYearsAfterTechnicalLifetime(0);
//        energyProducerINTB.setDebtRatioOfInvestments(0.7);
//        energyProducerINTB.setLoanInterestRate(0.1);
//        energyProducerINTB.setEquityInterestRate(0.1);
//        energyProducerINTB.setPastTimeHorizon(5);
//        energyProducerINTB.setInvestmentFutureTimeHorizon(7);
//        energyProducerINTB.setLongTermContractPastTimeHorizon(3);
//        energyProducerINTB.setLongTermContractMargin(0.1);
//        energyProducerINTB.setCash(3e9);
//        energyProducerINTB.setInvestmentRole(tenderInvestmentRole);
//        
//        EnergyProducer energyProducerINTC = reps.createEnergyProducer();
//        energyProducerINTC.setName("Energy Producer International C");
//        energyProducerINTC.setInvestorMarket(netherlandsElectricitySpotMarket);
//        energyProducerINTC.setNumberOfYearsBacklookingForForecasting(5);
//        energyProducerINTC.setPriceMarkUp(1.0);
//        energyProducerINTC.setWillingToInvest(true);
//        energyProducerINTC.setDownpaymentFractionOfCash(.5);
//        energyProducerINTC.setDismantlingRequiredOperatingProfit(0);
//        energyProducerINTC.setDismantlingProlongingYearsAfterTechnicalLifetime(0);
//        energyProducerINTC.setDebtRatioOfInvestments(0.7);
//        energyProducerINTC.setLoanInterestRate(0.1);
//        energyProducerINTC.setEquityInterestRate(0.1);
//        energyProducerINTC.setPastTimeHorizon(5);
//        energyProducerINTC.setInvestmentFutureTimeHorizon(7);
//        energyProducerINTC.setLongTermContractPastTimeHorizon(3);
//        energyProducerINTC.setLongTermContractMargin(0.1);
//        energyProducerINTC.setCash(3e9);
//        energyProducerINTC.setInvestmentRole(tenderInvestmentRole);
        
        reps.bigBank = new BigBank();

        StepTrend co2TaxTrend = new StepTrend();
        co2TaxTrend.setDuration(1);
        co2TaxTrend.setStart(5);
        co2TaxTrend.setMinValue(5);
        co2TaxTrend.setIncrement(0);

        StepTrend co2CapTrend = new StepTrend();
        co2CapTrend.setDuration(1);
        co2CapTrend.setStart(10e9);
        co2CapTrend.setMinValue(0);
        co2CapTrend.setIncrement(0);
        
        
        StepTrend minCo2PriceTrend = new StepTrend();
        minCo2PriceTrend.setDuration(1);
        minCo2PriceTrend.setStart(7);
        minCo2PriceTrend.setMinValue(0);
        minCo2PriceTrend.setIncrement(1.5);

        
        reps.government = new Government();
        reps.government.setName("EuropeanGov");
        reps.government.setCo2Penalty(500);
        reps.government.setCo2TaxTrend(co2TaxTrend);
        reps.government.setCo2CapTrend(co2CapTrend);
        reps.government.setMinCo2PriceTrend(minCo2PriceTrend);
        
        NationalGovernment governmentNL = reps.createNationalGovernment("DutchGov", nl, minCo2PriceTrend);  
        
        GeometricTrend coalPSCInvestmentCostTimeSeries = new GeometricTrend();
        coalPSCInvestmentCostTimeSeries.setStart(1365530);

        GeometricTrend coalPSCFixedOperatingCostTimeSeries = new GeometricTrend();
        coalPSCFixedOperatingCostTimeSeries.setStart(40970);

        GeometricTrend coalPSCEfficiencyTimeSeries = new GeometricTrend();
        coalPSCEfficiencyTimeSeries.setStart(.44);

        PowerGeneratingTechnology coalPSC = reps.createPowerGeneratingTechnology();
        coalPSC.setName("Coal PSC");
        coalPSC.setCapacity(750);
        coalPSC.setIntermittent(false);
        coalPSC.setApplicableForLongTermContract(true);
        coalPSC.setPeakSegmentDependentAvailability(1);
        coalPSC.setBaseSegmentDependentAvailability(1);
        coalPSC.setMaximumInstalledCapacityFractionPerAgent(0);
        coalPSC.setMaximumInstalledCapacityFractionInCountry(0);
        coalPSC.setMinimumFuelQuality(.95);
        coalPSC.setExpectedPermittime(1);
        coalPSC.setExpectedLeadtime(4);
        coalPSC.setExpectedLifetime(40);
        coalPSC.setFixedOperatingCostModifierAfterLifetime(.05);
        coalPSC.setMinimumRunningHours(5000);
        coalPSC.setDepreciationTime(20);
        coalPSC.setEfficiencyTimeSeries(coalPSCEfficiencyTimeSeries);
        coalPSC.setFixedOperatingCostTimeSeries(coalPSCFixedOperatingCostTimeSeries);
        coalPSC.setInvestmentCostTimeSeries(coalPSCInvestmentCostTimeSeries);
        Set<Substance> coalPSCFuels = new HashSet<>();
        coalPSCFuels.add(hardCoal);
        coalPSC.setFuels(coalPSCFuels);
       
        GeometricTrend lignitePSCInvestmentCostTimeSeries = new GeometricTrend();
        lignitePSCInvestmentCostTimeSeries.setStart(1700000);

        GeometricTrend lignitePSCFixedOperatingCostTimeSeries = new GeometricTrend();
        lignitePSCFixedOperatingCostTimeSeries.setStart(41545);

        GeometricTrend lignitePSCEfficiencyTimeSeries = new GeometricTrend();
        lignitePSCEfficiencyTimeSeries.setStart(.45);

        PowerGeneratingTechnology lignitePSC = reps.createPowerGeneratingTechnology();
        lignitePSC.setName("Lignite PSC");
        lignitePSC.setCapacity(1000);
        lignitePSC.setIntermittent(false);
        lignitePSC.setApplicableForLongTermContract(true);
        lignitePSC.setPeakSegmentDependentAvailability(1);
        lignitePSC.setBaseSegmentDependentAvailability(1);
        lignitePSC.setMaximumInstalledCapacityFractionPerAgent(0);
        lignitePSC.setMaximumInstalledCapacityFractionInCountry(0);
        lignitePSC.setMinimumFuelQuality(.95);
        lignitePSC.setExpectedPermittime(1);
        lignitePSC.setExpectedLeadtime(5);
        lignitePSC.setExpectedLifetime(40);
        lignitePSC.setFixedOperatingCostModifierAfterLifetime(.05);
        lignitePSC.setMinimumRunningHours(5000);
        lignitePSC.setDepreciationTime(20);
        lignitePSC.setEfficiencyTimeSeries(lignitePSCEfficiencyTimeSeries);
        lignitePSC.setFixedOperatingCostTimeSeries(lignitePSCFixedOperatingCostTimeSeries);
        lignitePSC.setInvestmentCostTimeSeries(lignitePSCInvestmentCostTimeSeries);
        Set<Substance> lignitePSCFuels = new HashSet<>();
        lignitePSCFuels.add(ligniteCoal);
        lignitePSC.setFuels(lignitePSCFuels);        
        
        GeometricTrend biomassCHPInvestmentCostTimeSeries = new GeometricTrend();
        biomassCHPInvestmentCostTimeSeries.setStart(1703320);

        GeometricTrend biomassCHPFixedOperatingCostTimeSeries = new GeometricTrend();
        biomassCHPFixedOperatingCostTimeSeries.setStart(59620);

        GeometricTrend biomassCHPEfficiencyTimeSeries = new GeometricTrend();
        biomassCHPEfficiencyTimeSeries.setStart(.35);

        PowerGeneratingTechnology biomassCHP = reps.createPowerGeneratingTechnology();
        biomassCHP.setName("Biomass CHP");
        biomassCHP.setCapacity(500);
        biomassCHP.setIntermittent(false);
        biomassCHP.setApplicableForLongTermContract(true);
        biomassCHP.setPeakSegmentDependentAvailability(0.7);
        biomassCHP.setBaseSegmentDependentAvailability(0.7);
        biomassCHP.setMaximumInstalledCapacityFractionPerAgent(1);
        biomassCHP.setMaximumInstalledCapacityFractionInCountry(1);
        biomassCHP.setMinimumFuelQuality(0.5);
        biomassCHP.setExpectedPermittime(1);
        biomassCHP.setExpectedLeadtime(3);
        biomassCHP.setExpectedLifetime(30);
        biomassCHP.setFixedOperatingCostModifierAfterLifetime(.05);
        biomassCHP.setMinimumRunningHours(0);
        biomassCHP.setDepreciationTime(15);
        biomassCHP.setEfficiencyTimeSeries(biomassCHPEfficiencyTimeSeries);
        biomassCHP.setFixedOperatingCostTimeSeries(biomassCHPFixedOperatingCostTimeSeries);
        biomassCHP.setInvestmentCostTimeSeries(biomassCHPInvestmentCostTimeSeries);
        Set<Substance> biomassCHPFuels = new HashSet<>();
        biomassCHPFuels.add(biomass);
        biomassCHP.setFuels(biomassCHPFuels);
        
        GeometricTrend ccgtInvestmentCostTimeSeries = new GeometricTrend();
        ccgtInvestmentCostTimeSeries.setStart(646830);

        GeometricTrend ccgtFixedOperatingCostTimeSeries = new GeometricTrend();
        ccgtFixedOperatingCostTimeSeries.setStart(29470);

        GeometricTrend ccgtEfficiencyTimeSeries = new GeometricTrend();
        ccgtEfficiencyTimeSeries.setStart(.59);

        PowerGeneratingTechnology ccgt = reps.createPowerGeneratingTechnology();
        ccgt.setName("CCGT");
        ccgt.setCapacity(775);
        ccgt.setIntermittent(false);
        ccgt.setApplicableForLongTermContract(true);
        ccgt.setPeakSegmentDependentAvailability(1);
        ccgt.setBaseSegmentDependentAvailability(1);
        ccgt.setMaximumInstalledCapacityFractionPerAgent(1);
        ccgt.setMaximumInstalledCapacityFractionInCountry(1);
        ccgt.setMinimumFuelQuality(1);
        ccgt.setExpectedPermittime(1);
        ccgt.setExpectedLeadtime(2);
        ccgt.setExpectedLifetime(30);
        ccgt.setFixedOperatingCostModifierAfterLifetime(.05);
        ccgt.setMinimumRunningHours(0);
        ccgt.setDepreciationTime(15);
        ccgt.setEfficiencyTimeSeries(ccgtEfficiencyTimeSeries);
        ccgt.setFixedOperatingCostTimeSeries(ccgtFixedOperatingCostTimeSeries);
        ccgt.setInvestmentCostTimeSeries(ccgtInvestmentCostTimeSeries);
        Set<Substance> ccgtFuels = new HashSet<>();
        ccgtFuels.add(naturalGas);
        ccgt.setFuels(ccgtFuels);

        GeometricTrend ocgtInvestmentCostTimeSeries = new GeometricTrend();
        ocgtInvestmentCostTimeSeries.setStart(359350);

        GeometricTrend ocgtFixedOperatingCostTimeSeries = new GeometricTrend();
        ocgtFixedOperatingCostTimeSeries.setStart(14370);

        GeometricTrend ocgtEfficiencyTimeSeries = new GeometricTrend();
        ocgtEfficiencyTimeSeries.setStart(.38);

        PowerGeneratingTechnology ocgt = reps.createPowerGeneratingTechnology();
        ocgt.setName("OCGT");
        ocgt.setCapacity(150);
        ocgt.setIntermittent(false);
        ocgt.setApplicableForLongTermContract(true);
        ocgt.setPeakSegmentDependentAvailability(1);
        ocgt.setBaseSegmentDependentAvailability(1);
        ocgt.setMaximumInstalledCapacityFractionPerAgent(1);
        ocgt.setMaximumInstalledCapacityFractionInCountry(1);
        ocgt.setMinimumFuelQuality(1);
        ocgt.setExpectedPermittime(0);
        ocgt.setExpectedLeadtime(1);
        ocgt.setExpectedLifetime(30);
        ocgt.setFixedOperatingCostModifierAfterLifetime(.05);
        ocgt.setMinimumRunningHours(0);
        ocgt.setDepreciationTime(15);
        ocgt.setEfficiencyTimeSeries(ocgtEfficiencyTimeSeries);
        ocgt.setFixedOperatingCostTimeSeries(ocgtFixedOperatingCostTimeSeries);
        ocgt.setInvestmentCostTimeSeries(ocgtInvestmentCostTimeSeries);
        Set<Substance> ocgtFuels = new HashSet<>();
        ocgtFuels.add(naturalGas);
        ocgt.setFuels(ocgtFuels);
        
        GeometricTrend oilPGTInvestmentCostTimeSeries = new GeometricTrend();
        oilPGTInvestmentCostTimeSeries.setStart(250000);

        GeometricTrend oilPGTFixedOperatingCostTimeSeries = new GeometricTrend();
        oilPGTFixedOperatingCostTimeSeries.setStart(10000);

        GeometricTrend oilPGTEfficiencyTimeSeries = new GeometricTrend();
        oilPGTEfficiencyTimeSeries.setStart(.35);

        PowerGeneratingTechnology oilPGT = reps.createPowerGeneratingTechnology();
        oilPGT.setName("Fuel oil PGT");
        oilPGT.setCapacity(50);
        oilPGT.setIntermittent(false);
        oilPGT.setApplicableForLongTermContract(true);
        oilPGT.setPeakSegmentDependentAvailability(1);
        oilPGT.setBaseSegmentDependentAvailability(1);
        oilPGT.setMaximumInstalledCapacityFractionPerAgent(1);
        oilPGT.setMaximumInstalledCapacityFractionInCountry(1);
        oilPGT.setMinimumFuelQuality(1);
        oilPGT.setExpectedPermittime(0);
        oilPGT.setExpectedLeadtime(1);
        oilPGT.setExpectedLifetime(30);
        oilPGT.setFixedOperatingCostModifierAfterLifetime(.05);
        oilPGT.setMinimumRunningHours(0);
        oilPGT.setDepreciationTime(15);
        oilPGT.setEfficiencyTimeSeries(oilPGTEfficiencyTimeSeries);
        oilPGT.setFixedOperatingCostTimeSeries(oilPGTFixedOperatingCostTimeSeries);
        oilPGT.setInvestmentCostTimeSeries(oilPGTInvestmentCostTimeSeries);
        Set<Substance> oilPGTFuels = new HashSet<>();
        oilPGTFuels.add(fuelOil);
        oilPGT.setFuels(oilPGTFuels);
        
        GeometricTrend nuclearPGTInvestmentCostTimeSeries = new GeometricTrend();
        nuclearPGTInvestmentCostTimeSeries.setStart(2874800);

        GeometricTrend nuclearPGTFixedOperatingCostTimeSeries = new GeometricTrend();
        nuclearPGTFixedOperatingCostTimeSeries.setStart(71870);

        GeometricTrend nuclearPGTEfficiencyTimeSeries = new GeometricTrend();
        nuclearPGTEfficiencyTimeSeries.setStart(.33);

        PowerGeneratingTechnology nuclearPGT = reps.createPowerGeneratingTechnology();
        nuclearPGT.setName("Nuclear PGT");
        nuclearPGT.setCapacity(1000);
        nuclearPGT.setIntermittent(false);
        nuclearPGT.setApplicableForLongTermContract(true);
        nuclearPGT.setPeakSegmentDependentAvailability(1);
        nuclearPGT.setBaseSegmentDependentAvailability(1);
        nuclearPGT.setMaximumInstalledCapacityFractionPerAgent(0);
        nuclearPGT.setMaximumInstalledCapacityFractionInCountry(0);
        nuclearPGT.setMinimumFuelQuality(1);
        nuclearPGT.setExpectedPermittime(2);
        nuclearPGT.setExpectedLeadtime(5);
        nuclearPGT.setExpectedLifetime(40);
        nuclearPGT.setFixedOperatingCostModifierAfterLifetime(.05);
        nuclearPGT.setMinimumRunningHours(5000);
        nuclearPGT.setDepreciationTime(25);
        nuclearPGT.setEfficiencyTimeSeries(nuclearPGTEfficiencyTimeSeries);
        nuclearPGT.setFixedOperatingCostTimeSeries(nuclearPGTFixedOperatingCostTimeSeries);
        nuclearPGT.setInvestmentCostTimeSeries(nuclearPGTInvestmentCostTimeSeries);
        Set<Substance> nuclearPGTFuels = new HashSet<>();
        nuclearPGTFuels.add(uranium);
        nuclearPGT.setFuels(nuclearPGTFuels);
        
        GeometricTrend pvInvestmentCostTimeSeries = new GeometricTrend();
        pvInvestmentCostTimeSeries.setStart(2048300);

        GeometricTrend pvFixedOperatingCostTimeSeries = new GeometricTrend();
        pvFixedOperatingCostTimeSeries.setStart(20480);

        GeometricTrend pvEfficiencyTimeSeries = new GeometricTrend();
        pvEfficiencyTimeSeries.setStart(1);
        
        PowerGeneratingTechnology pv = reps.createPowerGeneratingTechnology();
        pv.setName("Photovoltaic PGT");
        pv.setCapacity(500);
        pv.setIntermittent(true);
        pv.setApplicableForLongTermContract(true);
        pv.setPeakSegmentDependentAvailability(0.08);
        pv.setBaseSegmentDependentAvailability(0.16);
        pv.setMaximumInstalledCapacityFractionPerAgent(1);
        pv.setMaximumInstalledCapacityFractionInCountry(1);
        pv.setMinimumFuelQuality(1);
        pv.setExpectedPermittime(0);
        pv.setExpectedLeadtime(1);
        pv.setExpectedLifetime(25);
        pv.setFixedOperatingCostModifierAfterLifetime(.05);
        pv.setMinimumRunningHours(0);
        pv.setDepreciationTime(15);
        pv.setEfficiencyTimeSeries(pvEfficiencyTimeSeries);
        pv.setFixedOperatingCostTimeSeries(pvFixedOperatingCostTimeSeries);
        pv.setInvestmentCostTimeSeries(pvInvestmentCostTimeSeries);
        Set<Substance> pvPGTFuels = new HashSet<>();
        pv.setFuels(pvPGTFuels);
        
        GeometricTrend hydroInvestmentCostTimeSeries = new GeometricTrend();
        hydroInvestmentCostTimeSeries.setStart(800000);

        GeometricTrend hydroFixedOperatingCostTimeSeries = new GeometricTrend();
        hydroFixedOperatingCostTimeSeries.setStart(10000);

        GeometricTrend hydroEfficiencyTimeSeries = new GeometricTrend();
        hydroEfficiencyTimeSeries.setStart(.9);
        
        PowerGeneratingTechnology hydro = reps.createPowerGeneratingTechnology();
        hydro.setName("Hydroelectric");
        hydro.setCapacity(250);
        hydro.setApplicableForLongTermContract(true);
        hydro.setPeakSegmentDependentAvailability(0.08);
        hydro.setBaseSegmentDependentAvailability(0.16);
        hydro.setMaximumInstalledCapacityFractionPerAgent(0);
        hydro.setMaximumInstalledCapacityFractionInCountry(0);
        hydro.setMinimumFuelQuality(1);
        hydro.setExpectedPermittime(2);
        hydro.setExpectedLeadtime(5);
        hydro.setExpectedLifetime(50);
        hydro.setFixedOperatingCostModifierAfterLifetime(.05);
        hydro.setMinimumRunningHours(0);
        hydro.setDepreciationTime(30);
        hydro.setEfficiencyTimeSeries(hydroEfficiencyTimeSeries);
        hydro.setFixedOperatingCostTimeSeries(hydroFixedOperatingCostTimeSeries);
        hydro.setInvestmentCostTimeSeries(hydroInvestmentCostTimeSeries);
        Set<Substance> hydroPGTFuels = new HashSet<>();
        hydro.setFuels(hydroPGTFuels);
        
        GeometricTrend windOnshoreInvestmentCostTimeSeries = new GeometricTrend();
        windOnshoreInvestmentCostTimeSeries.setStart(1214600);

        GeometricTrend windOnshoreFixedOperatingCostTimeSeries = new GeometricTrend();
        windOnshoreFixedOperatingCostTimeSeries.setStart(18220);

        GeometricTrend windOnshoreEfficiencyTimeSeries = new GeometricTrend();
        windOnshoreEfficiencyTimeSeries.setStart(1);
        
        PowerGeneratingTechnology windOnshore = reps.createPowerGeneratingTechnology();
        windOnshore.setName("Onshore wind PGT");
        windOnshore.setCapacity(600);
        windOnshore.setIntermittent(true);
        windOnshore.setApplicableForLongTermContract(true);
        windOnshore.setPeakSegmentDependentAvailability(0.05);
        windOnshore.setBaseSegmentDependentAvailability(0.40);
        windOnshore.setMaximumInstalledCapacityFractionPerAgent(1);
        windOnshore.setMaximumInstalledCapacityFractionInCountry(1);
        windOnshore.setMinimumFuelQuality(1);
        windOnshore.setExpectedPermittime(1);
        windOnshore.setExpectedLeadtime(1);
        windOnshore.setExpectedLifetime(25);
        windOnshore.setFixedOperatingCostModifierAfterLifetime(.05);
        windOnshore.setMinimumRunningHours(0);
        windOnshore.setDepreciationTime(15);
        windOnshore.setEfficiencyTimeSeries(windOnshoreEfficiencyTimeSeries);
        windOnshore.setFixedOperatingCostTimeSeries(windOnshoreFixedOperatingCostTimeSeries);
        windOnshore.setInvestmentCostTimeSeries(windOnshoreInvestmentCostTimeSeries);        
        Set<Substance> windOnshorePGTFuels = new HashSet<>();
        windOnshore.setFuels(windOnshorePGTFuels);
        
        GeometricTrend windOffshoreInvestmentCostTimeSeries = new GeometricTrend();
        windOffshoreInvestmentCostTimeSeries.setStart(2450770);

        GeometricTrend windOffshoreFixedOperatingCostTimeSeries = new GeometricTrend();
        windOffshoreFixedOperatingCostTimeSeries.setStart(73520);

        GeometricTrend windOffshoreEfficiencyTimeSeries = new GeometricTrend();
        windOffshoreEfficiencyTimeSeries.setStart(1);
        
        PowerGeneratingTechnology windOffshore = reps.createPowerGeneratingTechnology();
        windOffshore.setName("Offshore wind PGT");
        windOffshore.setCapacity(600);
        windOffshore.setIntermittent(true);
        windOffshore.setApplicableForLongTermContract(true);
        windOffshore.setPeakSegmentDependentAvailability(0.08);
        windOffshore.setBaseSegmentDependentAvailability(0.65);
        windOffshore.setMaximumInstalledCapacityFractionPerAgent(1);
        windOffshore.setMaximumInstalledCapacityFractionInCountry(1);
        windOffshore.setMinimumFuelQuality(1);
        windOffshore.setExpectedPermittime(1);
        windOffshore.setExpectedLeadtime(2);
        windOffshore.setExpectedLifetime(25);
        windOffshore.setFixedOperatingCostModifierAfterLifetime(.05);
        windOffshore.setMinimumRunningHours(0);
        windOffshore.setDepreciationTime(15);
        windOffshore.setEfficiencyTimeSeries(windOnshoreEfficiencyTimeSeries);
        windOffshore.setFixedOperatingCostTimeSeries(windOnshoreFixedOperatingCostTimeSeries);
        windOffshore.setInvestmentCostTimeSeries(windOnshoreInvestmentCostTimeSeries); 
        Set<Substance> windOffshorePGTFuels = new HashSet<>();
        windOffshore.setFuels(windOffshorePGTFuels);
        
        
        PowerGeneratingTechnologyTarget windTarget = new PowerGeneratingTechnologyTarget();
        windTarget.setPowerGeneratingTechnology(windOffshore);
        StepTrend windTargetTrend = new StepTrend();
        windTargetTrend.setStart(700);
        windTargetTrend.setIncrement(700);
        windTargetTrend.setDuration(1);
        windTargetTrend.setMinValue(700);
        windTarget.setTrend(windTargetTrend);
 
        PowerGeneratingTechnologyTarget pvTarget = new PowerGeneratingTechnologyTarget();
        pvTarget.setPowerGeneratingTechnology(pv);
        StepTrend pvTargetTrend = new StepTrend();
        pvTargetTrend.setStart(10000);
        pvTargetTrend.setIncrement(0);
        pvTargetTrend.setDuration(1);
        pvTargetTrend.setMinValue(10000);
        pvTarget.setTrend(pvTargetTrend);
                       
        Set<PowerGeneratingTechnologyTarget> targets = new HashSet<>();
        targets.add(pvTarget);
        targets.add(windTarget);
        TargetInvestor investor = new TargetInvestor();
        investor.setName("TargetInvestorNL");
        investor.setPowerGenerationTechnologyTargets(targets);
        investor.setInvestmentRole(new TargetInvestmentRole(schedule));
        investor.setInvestorMarket(netherlandsElectricitySpotMarket);
        reps.targetInvestors.add(investor);
        
        
        
        windProfileOnShoreNL.setIntermittentTechnology(windOnshore);
        windProfileOffShoreNL.setIntermittentTechnology(windOffshore);
        solarProfileNL.setIntermittentTechnology(pv);
                
        PowerPlantCSVFactory powerPlantCSVFactory = new PowerPlantCSVFactory(reps);
        powerPlantCSVFactory.setCsvFile("/data/dutchPlants2015-toy.csv");
        for (PowerPlant plant : powerPlantCSVFactory.read()) {
            reps.createPowerPlantFromPlant(plant);

        }
        
        // Policy
        
    	// Regulator NL
        Regulator regulatorNl = new Regulator();
        regulatorNl.setName("regulatorNetherlands");
        regulatorNl.setNumberOfYearsLookingBackToForecastDemand(4);
        regulatorNl.setZone(nl);
        reps.regulators.add(regulatorNl);
        
        //<!-- Feed in Premium -->
        
        RenewableSupportFipScheme premiumNL = new RenewableSupportFipScheme();
        premiumNL.setName("Renewable Support Scheme");
        premiumNL.setSupportSchemeDuration(10);
        premiumNL.setEmRevenuePaidExpost(true);
        premiumNL.setFutureSchemeStartTime(5);
        premiumNL.setAvgElectricityPriceBasedPremiumEnabled(false);
        premiumNL.setTechnologySpecificityEnabled(false);
        premiumNL.setCostContainmentMechanismEnabled(false);
        premiumNL.setRegulator(regulatorNl);
        premiumNL.setZone(nl);
        HashSet<PowerGeneratingTechnology> powerGeneratingTechnologiesEligible = new HashSet<>();
        powerGeneratingTechnologiesEligible.add(windOnshore);
        powerGeneratingTechnologiesEligible.add(biomassCHP);
        premiumNL.setPowerGeneratingTechnologiesEligible(powerGeneratingTechnologiesEligible);
        reps.renewableSupportFipSchemes.add(premiumNL);
        
        BiasFactor biasFactorNL1Wind  = new BiasFactor();
        biasFactorNL1Wind.setFeedInPremiumBiasFactor(1);
        biasFactorNL1Wind.setDegressionFactor(0.06);
        biasFactorNL1Wind.setNode(nlNode);
        biasFactorNL1Wind.setScheme(premiumNL);
        biasFactorNL1Wind.setTechnology(windOnshore);
        reps.biasFactors.add(biasFactorNL1Wind);
        
        BiasFactor biasFactorNL1biomass  = new BiasFactor();
        biasFactorNL1biomass.setFeedInPremiumBiasFactor(1);
        biasFactorNL1biomass.setDegressionFactor(0.06);
        biasFactorNL1biomass.setNode(nlNode);
        biasFactorNL1biomass.setScheme(premiumNL);
        biasFactorNL1biomass.setTechnology(biomassCHP);
        reps.biasFactors.add(biasFactorNL1biomass);
        
        
        // Renewable Tender Scheme
        
        RenewableSupportSchemeTender renewableSupportSchemeNL = new RenewableSupportSchemeTender();
        renewableSupportSchemeNL.setName("Tender");
        renewableSupportSchemeNL.setFutureTenderOperationStartTime(2);
        renewableSupportSchemeNL.setSupportSchemeDuration(10);
        renewableSupportSchemeNL.setTechnologySpecificityEnabled(false);
        renewableSupportSchemeNL.setExpostRevenueCalculation(true);
        renewableSupportSchemeNL.setRegulator(regulatorNl);
        
        Set<PowerGeneratingTechnology> powerGeneratingTechnologiesEligibleNLTender = new HashSet<>();
        powerGeneratingTechnologiesEligibleNLTender.add(pv);
        powerGeneratingTechnologiesEligibleNLTender.add(windOnshore);
        
        renewableSupportSchemeNL.setPowerGeneratingTechnologiesEligible(powerGeneratingTechnologiesEligibleNLTender);
        reps.renewableSupportSchemeTenders.add(renewableSupportSchemeNL);
        
        // Renewable Energy Target NL
        
    	
        TimeSeriesCSVReader nl_nreap = new TimeSeriesCSVReader();
        nl_nreap.setFilename("/data/nodeAndTechSpecificPotentialsDummy.csv");
        nl_nreap.setDelimiter(",");
        nl_nreap.setStartingYear(0);
        nl_nreap.setVariableName("nl_dummy");
        //nl_nreap.readCSVVariable("nl_dummy");
                
        RenewableTarget renewableTenderTarget = new RenewableTarget();
        renewableTenderTarget.setYearlyRenewableTargetTimeSeries(nl_nreap);
        renewableTenderTarget.setTargetTechnologySpecific(false);
        renewableTenderTarget.setRegulator(regulatorNl);
        reps.renewableTargets.add(renewableTenderTarget);
        
        TimeSeriesCSVReader nl_nreap_photovoltaicPGT = new TimeSeriesCSVReader();
        nl_nreap.setFilename("/data/nodeAndTechSpecificPotentialsDummy.csv");
        nl_nreap.setDelimiter(",");
        nl_nreap.setStartingYear(0);
        nl_nreap.setVariableName("nl_dummyTargetPV");
        //nl_nreap.readCSVVariable("nl_dummyTargetPV"); //TODO? necessary?
        
        
        RenewableTarget renewableTenderTargetNLphotovoltaicPGT = new RenewableTarget();
        renewableTenderTargetNLphotovoltaicPGT.setYearlyRenewableTargetTimeSeries(nl_nreap_photovoltaicPGT);
        renewableTenderTargetNLphotovoltaicPGT.setTargetTechnologySpecific(true);
        renewableTenderTargetNLphotovoltaicPGT.setPowerGeneratingTechnology(pv);
        renewableTenderTargetNLphotovoltaicPGT.setRegulator(regulatorNl);
        reps.renewableTargets.add(renewableTenderTargetNLphotovoltaicPGT);
        
        
        TimeSeriesCSVReader nl_nreap_windPGT = new TimeSeriesCSVReader();
        nl_nreap.setFilename("/data/nodeAndTechSpecificPotentialsDummy.csv");
        nl_nreap.setDelimiter(",");
        nl_nreap.setStartingYear(0);
        nl_nreap.setVariableName("nl_dummyTargetWind");
        
        RenewableTarget renewableTenderTargetNLwindPGT = new RenewableTarget();
        renewableTenderTargetNLwindPGT.setYearlyRenewableTargetTimeSeries(nl_nreap_windPGT);
        renewableTenderTargetNLwindPGT.setTargetTechnologySpecific(true);
        renewableTenderTargetNLwindPGT.setPowerGeneratingTechnology(windOnshore);
        renewableTenderTargetNLwindPGT.setRegulator(regulatorNl);
        reps.renewableTargets.add(renewableTenderTargetNLwindPGT);
        
//		<!-- Technology Specific POTENTIAL LIMITS for Intermittent Technologies -->
//
//
        TimeSeriesCSVReader nl_photovoltaicPGT_limit = new TimeSeriesCSVReader();
        nl_photovoltaicPGT_limit.setFilename("/data/potentialLimits.csv");
        nl_photovoltaicPGT_limit.setDelimiter(",");
        nl_photovoltaicPGT_limit.setStartingYear(-4);
        nl_photovoltaicPGT_limit.setVariableName("limitPV");
        
        RenewablePotentialLimit renewablePotentialLimitNLphotovoltaic = new RenewablePotentialLimit();
        renewablePotentialLimitNLphotovoltaic.setYearlyRenewableTargetTimeSeries(nl_photovoltaicPGT_limit);
        renewablePotentialLimitNLphotovoltaic.setTargetTechnologySpecific(true);
        renewablePotentialLimitNLphotovoltaic.setPowerGeneratingTechnology(pv);
        renewablePotentialLimitNLphotovoltaic.setRegulator(regulatorNl);
        reps.renewablePotentialLimits.add(renewablePotentialLimitNLphotovoltaic);
      
        
        TimeSeriesCSVReader nl_windOffshore_limit = new TimeSeriesCSVReader();
        nl_windOffshore_limit.setFilename("/data/potentialLimits.csv");
        nl_windOffshore_limit.setDelimiter(",");
        nl_windOffshore_limit.setStartingYear(-4);
        nl_windOffshore_limit.setVariableName("limitWindOff");
        
        RenewablePotentialLimit renewablePotentialLimitNLwindOffshore = new RenewablePotentialLimit();
        renewablePotentialLimitNLwindOffshore.setYearlyRenewableTargetTimeSeries(nl_windOffshore_limit);
        renewablePotentialLimitNLwindOffshore.setTargetTechnologySpecific(true);
        renewablePotentialLimitNLwindOffshore.setPowerGeneratingTechnology(windOffshore);
        renewablePotentialLimitNLwindOffshore.setRegulator(regulatorNl);
        reps.renewablePotentialLimits.add(renewablePotentialLimitNLwindOffshore);
        
       
        TimeSeriesCSVReader nl_windOnshore_limit = new TimeSeriesCSVReader();
        nl_windOnshore_limit.setFilename("/data/potentialLimits.csv");
        nl_windOnshore_limit.setDelimiter(",");
        nl_windOnshore_limit.setStartingYear(-4);
        nl_windOnshore_limit.setVariableName("limitWindOn");
        
        RenewablePotentialLimit renewablePotentialLimitNLwindOnshore = new RenewablePotentialLimit();
        renewablePotentialLimitNLwindOnshore.setYearlyRenewableTargetTimeSeries(nl_windOnshore_limit);
        renewablePotentialLimitNLwindOnshore.setTargetTechnologySpecific(true);
        renewablePotentialLimitNLwindOnshore.setPowerGeneratingTechnology(windOnshore);
        renewablePotentialLimitNLwindOnshore.setRegulator(regulatorNl);
        reps.renewablePotentialLimits.add(renewablePotentialLimitNLwindOnshore);
        
        

    }
}
