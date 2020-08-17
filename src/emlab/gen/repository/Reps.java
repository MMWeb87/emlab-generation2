/**
 *
 */
package emlab.gen.repository;

import cern.colt.matrix.DoubleMatrix2D;
import emlab.gen.domain.agent.BigBank;
import emlab.gen.domain.agent.CommoditySupplier;
import emlab.gen.domain.agent.EMLabAgent;
import emlab.gen.domain.agent.EMLabModel;
import emlab.gen.domain.agent.EnergyConsumer;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.NationalGovernment;
import emlab.gen.domain.agent.PowerPlantMaintainer;
import emlab.gen.domain.agent.PowerPlantManufacturer;
import emlab.gen.domain.agent.Regulator;
import emlab.gen.domain.agent.StrategicReserveOperator;
import emlab.gen.domain.agent.TargetInvestor;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.contract.Contract;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.contract.LongTermContract;
import emlab.gen.domain.contract.LongTermContractDuration;
import emlab.gen.domain.contract.LongTermContractOffer;
import emlab.gen.domain.contract.LongTermContractType;
import emlab.gen.domain.factory.ElectricityProducerFactory;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.Bid;
import emlab.gen.domain.market.CO2Auction;
import emlab.gen.domain.market.CO2MarketClearingPoint;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.CommodityMarket;
import emlab.gen.domain.market.DecarbonizationMarket;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.FinancialPowerPlantReport;
import emlab.gen.domain.market.electricity.IntermittentTechnologyNodeLoadFactor;
import emlab.gen.domain.market.electricity.PowerPlantDispatchPlan;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentClearingPoint;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.EmpiricalMappingFunctionParameter;
import emlab.gen.domain.policy.PowerGeneratingTechnologyTarget;
import emlab.gen.domain.policy.renewablesupport.BaseCostFip;
import emlab.gen.domain.policy.renewablesupport.BiasFactor;
import emlab.gen.domain.policy.renewablesupport.RenewablePotentialLimit;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportFipScheme;
import emlab.gen.domain.policy.renewablesupport.RenewableSupportSchemeTender;
import emlab.gen.domain.policy.renewablesupport.RenewableTarget;
import emlab.gen.domain.policy.renewablesupport.SupportPriceContract;
import emlab.gen.domain.policy.renewablesupport.TenderBid;
import emlab.gen.domain.policy.renewablesupport.TenderClearingPoint;
import emlab.gen.domain.technology.Interconnector;
import emlab.gen.domain.technology.IntermittentResourceProfile;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGeneratingTechnologyNodeLimit;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.engine.AbstractAgent;
import emlab.gen.engine.Schedule;
import emlab.gen.trend.TimeSeriesCSVReader;
import emlab.gen.role.investment.FinancialExpectationReport;
import emlab.gen.role.investment.MarketInformationReport;
import emlab.gen.trend.TimeSeriesImpl;
import emlab.gen.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;



/**
 * @author EJL Chappin, marcmel
 *
 */
public class Reps {

    public Logger logger = Logger.getGlobal();
    public Schedule schedule;

    public EMLabModel emlabModel;

    public Government government;

    public CO2Auction co2Auction;

    public BigBank bigBank;

    public Interconnector interconnector;

    public PowerPlantManufacturer powerPlantManufacturer;

    public PowerPlantMaintainer powerPlantMaintainer;

    public ArrayList<StrategicReserveOperator> strategicReserveOperators = new ArrayList<>();

    public ArrayList<CommoditySupplier> commoditySuppliers = new ArrayList<>();

    public ArrayList<PowerPlant> powerPlants = new ArrayList<>();

    public ArrayList<Segment> segments = new ArrayList<>();

    public ArrayList<ElectricitySpotMarket> electricitySpotMarkets = new ArrayList<>();

    public ArrayList<TargetInvestor> targetInvestors = new ArrayList<>();

    public ArrayList<Loan> loanList = new ArrayList<>();

    public ArrayList<Bid> bids = new ArrayList<>();

    public ArrayList<CashFlow> cashFlows = new ArrayList<>();

    public ArrayList<ClearingPoint> clearingPoints = new ArrayList<>();

    public ArrayList<SegmentClearingPoint> segmentClearingPoints = new ArrayList<>();

//    public ArrayList<CO2MarketClearingPoint> co2MarketClearingPoints = new ArrayList<>();
    public ArrayList<LongTermContract> longTermContracts = new ArrayList<>();

    public ArrayList<LongTermContractOffer> longTermContractOffers = new ArrayList<>();

    public ArrayList<FinancialPowerPlantReport> financialPowerPlantReports = new ArrayList<>();

    public ArrayList<EnergyProducer> energyProducers = new ArrayList<>();

    public ArrayList<EnergyConsumer> energyConsumers = new ArrayList<>();

    public ArrayList<IntermittentResourceProfile> intermittentResourceProfiles = new ArrayList<>();

    public ArrayList<PowerGeneratingTechnology> powerGeneratingTechnologies = new ArrayList<>();

    public ArrayList<PowerGridNode> powerGridNodes = new ArrayList<>();

    public ArrayList<IntermittentTechnologyNodeLoadFactor> intermittentTechnologyNodeLoadFactors = new ArrayList<>();

    public ArrayList<NationalGovernment> nationalGovernments = new ArrayList<>();

    public ArrayList<PowerGeneratingTechnologyNodeLimit> powerGeneratingTechnologyNodeLimits = new ArrayList<>();

    public ArrayList<PowerGeneratingTechnologyTarget> powerGeneratingTechnologyTargets = new ArrayList<>();

    public ArrayList<Zone> zones = new ArrayList<>();

    public ArrayList<Substance> substances = new ArrayList<>();

    public ArrayList<Substance> substancesOnCommodityMarkets = new ArrayList<>();

    public ArrayList<PowerPlantDispatchPlan> powerPlantDispatchPlans = new ArrayList<>();

    public ArrayList<SegmentLoad> segmentLoads = new ArrayList<>();

    public ArrayList<LongTermContractType> longTermContractTypes = new ArrayList<>();

    public ArrayList<LongTermContractDuration> longTermContractDurations = new ArrayList<>();

    public ArrayList<CommodityMarket> commodityMarkets = new ArrayList<>();
    
    public ArrayList<MarketInformationReport> marketInformationReports = new ArrayList<>();
    
    public ArrayList<FinancialExpectationReport> financialExpectationReports = new ArrayList<>();

    
    public ArrayList<RenewableTarget> renewableTargets = new ArrayList<>();
    
    public ArrayList<RenewablePotentialLimit> renewablePotentialLimits = new ArrayList<>();
    
    public ArrayList<String> utilityLevelsTechnology = new ArrayList<>();    

    public DoubleMatrix2D intermittentMatrix = null;
    
    public ArrayList<RenewableSupportSchemeTender> renewableSupportSchemeTenders = new ArrayList<>();
    
    public ArrayList<RenewableSupportFipScheme> renewableSupportFipSchemes = new ArrayList<>();

    public ArrayList<TenderBid> tenderBids = new ArrayList<>();
    
    public ArrayList<TenderClearingPoint> tenderClearingPoints = new ArrayList<>();
    
    public ArrayList<BaseCostFip> baseCostFips = new ArrayList<>();
    
    public ArrayList<SupportPriceContract> supportPriceContracts = new ArrayList<>();
    
    public ArrayList<BiasFactor> biasFactors = new ArrayList<>();

    public ArrayList<Regulator> regulators = new ArrayList<>();
    
    
    
    // Empirical mapping
    public ArrayList<EmpiricalMappingFunctionParameter> empiricalMappingFunctionParameters = new ArrayList<>();
    

    //private maps 
    public HashMap<Substance, DecarbonizationMarket> marketForSubstance = new HashMap<>();
    private HashMap<NationalGovernment, ElectricitySpotMarket> electricitySpotMarketForNationalGovernment = new HashMap<>();
    private HashMap<PowerPlant, ElectricitySpotMarket> electricitySpotMarketForPowerPlant = new HashMap<>();
    private HashMap<EMLabAgent, ArrayList<Loan>> loansFromAgent = new HashMap<>();
    private HashMap<EMLabAgent, ArrayList<Loan>> loansToAgent = new HashMap<>();
    private HashMap<EMLabAgent, ArrayList<PowerPlant>> powerPlantsForAgent = new HashMap<>();
    

    /**
     * Gives the electricity spot market for a specific zone
     *
     * @param zone the electricity market should be found for
     * @return the found electricity spot market
     */
    public ElectricitySpotMarket findElectricitySpotMarketForZone(Zone zone) {
        return electricitySpotMarkets.stream().filter(p -> p.getZone().equals(zone)).findFirst().get();
    }

    public ElectricitySpotMarket findElectricitySpotMarketByNationalGovernment(NationalGovernment government) {
        return electricitySpotMarketForNationalGovernment.get(government);
    }

    public ElectricitySpotMarket findElectricitySpotMarketByPowerPlant(PowerPlant plant) {
        if (!electricitySpotMarketForPowerPlant.containsKey(plant)) {
            Logger.getGlobal().log(Level.WARNING, "Electricity spot market cannot be found for plant {0}", plant);
        }
        return electricitySpotMarketForPowerPlant.get(plant);
    }

    public SegmentLoad findSegmentLoadForElectricitySpotMarketForZone(Zone zone, Segment segment) {
        for (SegmentLoad segmentLoad : segmentLoads) {
            if (segmentLoad.getSegment().equals(segment) && segmentLoad.getElectricitySpotMarket().equals(findElectricitySpotMarketForZone(zone))) {
                return segmentLoad;
            }
        }
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    /**
     * Gives the market for a specific substance
     *
     * @param substance the substance the market should be found for
     * @return the found market
     */
    public DecarbonizationMarket findMarketBySubstance(Substance substance) {
        return marketForSubstance.get(substance);
    }

    public SegmentClearingPoint findOneSegmentClearingPointForMarketSegmentAndTime(long tick, Segment segment, ElectricitySpotMarket market, boolean forecast) {
        Optional<SegmentClearingPoint> list = segmentClearingPoints.stream().filter(p -> p.getSegment().equals(segment)).filter(p -> p.getAbstractMarket().equals(market)).filter(p -> p.isForecast() == forecast).filter(p -> p.getTime() == tick).findFirst();
        if (list.isPresent()) {
            return list.get();
        } else {
            return null;
        }

    }

    //TODO not for time...
    public double peakLoadbyZoneMarketandTime(Zone zone, ElectricitySpotMarket market) {
        throw new UnsupportedOperationException();
    }

    public ArrayList<PowerPlantDispatchPlan> findAllPowerPlantDispatchPlansForSegmentForTime(Segment segment, long time, boolean todo) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public ArrayList<PowerPlantDispatchPlan> findDescendingSortedPowerPlantDispatchPlansForSegmentForTime(Segment segment, long tick, boolean todo) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public double determineTotallyBankedCO2Certificates() {
        throw new UnsupportedOperationException();
    }

    public double determinePreviouslyBankedCO2Certificates() {
        throw new UnsupportedOperationException();
    }

    public double calculateCapacityOfExpectedOperationalPowerPlantsByOwnerByTechnology(long tick, AbstractAgent agent, PowerGeneratingTechnology powerGeneratingTechnology) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a loan
     *
     * @param from the seller of the loan
     * @param to the buyer of the loan
     * @param amount the total amount to be payed
     * @param numberOfPayments the number of payments
     * @param loanStartTime the time the loan starts
     * @param plant the power plant the loan is connected to
     * @return
     */
    public Loan createLoan(EMLabAgent from, EMLabAgent to, double amount, long numberOfPayments, long loanStartTime, PowerPlant plant) {
        Loan loan = new Loan();
        loan.setFrom(from);
        loan.setTo(to);
        loan.setAmountPerPayment(amount);
        loan.setTotalNumberOfPayments(numberOfPayments);
        loan.setRegardingPowerPlant(plant);
        loan.setLoanStartTime(loanStartTime);
        loan.setNumberOfPaymentsDone(0);
        plant.setLoan(loan);
        loanList.add(loan);

        if (!loansFromAgent.containsKey(from)) {
            loansFromAgent.put(from, new ArrayList<>());
        }
        loansFromAgent.get(from).add(loan);
        if (!loansToAgent.containsKey(to)) {
            loansToAgent.put(to, new ArrayList<>());
        }
        loansToAgent.get(to).add(loan);

        return loan;
    }

    /**
     * Finds all loans that the agent has been lend to by others.
     *
     * @param agent
     * @return the loans
     */
    public List<Loan> findLoansFromAgent(EMLabAgent agent) {
        return loansFromAgent.get(agent);
    }

    /**
     * Finds all loans that the agent has lend to others
     *
     * @param agent
     * @return the loans
     */
    public List<Loan> findLoansToAgent(EMLabAgent agent) {
        return loansToAgent.get(agent);
    }

    public List<Bid> findAllBidsForForTime(long time) {
        return bids.stream().filter(p -> p.getTime() == time).collect(Collectors.toList());
    }

    public void removeBidsUpToTime(long time) {
        bids.removeIf(p -> (p.getTime() <= time));
    }

    public List<Bid> findDemandBidsForMarketForTime(DecarbonizationMarket market, long time) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public List<Bid> findAllAcceptedDemandBidsForMarketForTime(DecarbonizationMarket market, long time) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> !p.isSupplyBid()).filter(p -> p.getStatus() >= Bid.PARTLY_ACCEPTED).collect(Collectors.toList());

    }

    public List<Bid> findOffersForMarketForTime(DecarbonizationMarket market, long time) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> p.isSupplyBid()).collect(Collectors.toList());
    }

    public List<Bid> findAcceptedOffersForMarketForTime(DecarbonizationMarket market, long time) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> p.isSupplyBid()).filter(p -> p.getStatus() >= Bid.PARTLY_ACCEPTED).collect(Collectors.toList());
    }

    /**
     * Find bids for a market for a time
     *
     * @param market
     * @param time
     * @param isSupply supply or demand bids
     * @return the bids
     */
    public Iterable<Bid> getBidsForMarketForTime(DecarbonizationMarket market, long time, boolean isSupply) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public Iterable<Bid> findOffersForMarketForTimeBelowPrice(DecarbonizationMarket market, long time, double price) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> p.isSupplyBid()).filter(p -> p.getPrice() <= price).collect(Collectors.toList());

    }

    /**
     * Find demand bids above a certain price, and return them in descending
     * order.
     *
     * @param market
     * @param time
     * @param price
     * @return
     */
    public Iterable<Bid> findDemandBidsForMarketForTimeAbovePrice(DecarbonizationMarket market, long time, double price) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> !p.isSupplyBid()).filter(p -> p.getPrice() > price).collect(Collectors.toList());
    }

    public double calculateDemandBidsForMarketForTimeForPrice(DecarbonizationMarket market, long time, double price) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> !p.isSupplyBid()).filter(p -> p.getPrice() == price).collect(Collectors.summarizingDouble(Bid::getAmount)).getSum();
    }

    public Iterable<Bid> findDemandBidsForMarketForTimeForPrice(DecarbonizationMarket market, long time, double price) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> !p.isSupplyBid()).filter(p -> p.getPrice() == price).collect(Collectors.toList());
    }

    public Iterable<Bid> findOffersForMarketForTimeForPrice(DecarbonizationMarket market, long time, double price) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> p.isSupplyBid()).filter(p -> p.getPrice() == price).collect(Collectors.toList());
    }

    public double calculateOffersForMarketForTimeForPrice(DecarbonizationMarket market, long time, double price) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> p.isSupplyBid()).filter(p -> p.getPrice() == price).collect(Collectors.summarizingDouble(Bid::getAmount)).getSum();
    }

    public double calculateTotalDemandForMarketForTime(DecarbonizationMarket market, long time) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> !p.isSupplyBid()).collect(Collectors.summarizingDouble(Bid::getAmount)).getSum();
    }

    public double calculateTotalDemandForMarketForTimeForPrice(DecarbonizationMarket market, long time, double price) {
        //TODO IS THE SIGN > of THE PRICE WRONG?
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> !p.isSupplyBid()).filter(p -> p.getPrice() >= price).collect(Collectors.summarizingDouble(Bid::getAmount)).getSum();
    }

    public double calculateTotalSupplyForMarketForTime(DecarbonizationMarket market, long time) {
        return bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> p.isSupplyBid()).collect(Collectors.summarizingDouble(Bid::getAmount)).getSum();
    }

    public double calculateTotalSupplyPriceForMarketForTime(DecarbonizationMarket market, long time) {
        Bid bid = bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> p.isSupplyBid()).collect(Collectors.maxBy(new BidPriceComparator())).get();
        return bid.getPrice();
    }

    public double calculateMinimumSupplyPriceForMarketForTime(DecarbonizationMarket market, long time) {
        Bid bid = bids.stream().filter(p -> p.getBiddingMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> p.isSupplyBid()).collect(Collectors.minBy(new BidPriceComparator())).get();
        return bid.getPrice();
    }

    public List<CashFlow> findAllCashFlowsForForTime(long time) {
        return cashFlows.stream().filter(p -> p.getTime() == time).collect(Collectors.toList());
    }

    public void removeCashFlowsUpToTime(long time) {
        cashFlows.removeIf(p -> (p.getTime() <= time));
    }

    public ClearingPoint findClearingPointForSegmentAndTime(Segment segment, long time, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public Iterable<ClearingPoint> findClearingPointsForSegmentAndTime(Segment segment, long time, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public ClearingPoint findClearingPointForMarketAndTime(DecarbonizationMarket market, long time, boolean forecast) {
        List<ClearingPoint> list = findClearingPointsForMarketAndTime(market, time, forecast);
        if (list.size() > 0) {
            return findClearingPointsForMarketAndTime(market, time, forecast).get(0);
        } else {
            return null;
        }
    }

    public List<ClearingPoint> findClearingPointsForMarketAndTime(DecarbonizationMarket market, long time,
            boolean forecast) {
        return clearingPoints.stream().filter(p -> p.getAbstractMarket().equals(market)).filter(p -> p.getTime() == time).filter(p -> p.isForecast() == forecast).collect(Collectors.toList());
    }

    public ClearingPoint createOrUpdateClearingPoint(DecarbonizationMarket abstractMarket, double price, double volume,
            long time, boolean forecast) {
        ClearingPoint point = null;
        if (findClearingPointsForMarketAndTime(abstractMarket, time, forecast).iterator().hasNext()) {
            point = findClearingPointsForMarketAndTime(abstractMarket, time, forecast).iterator().next();
        } else {
            point = new ClearingPoint();
            clearingPoints.add(point);
        }
        point.setAbstractMarket(abstractMarket);
        point.setPrice(price);
        point.setTime(time);
        point.setVolume(volume);
        point.setForecast(forecast);
        return point;
    }

    public SegmentClearingPoint createOrUpdateSegmentClearingPoint(Segment segment,
            DecarbonizationMarket abstractMarket, double price, double volume, double interconnectorFlow, long time,
            boolean forecast) {
        SegmentClearingPoint point = null;

        List<SegmentClearingPoint> points = Utils.asCastedList(findClearingPointsForMarketAndTime(abstractMarket, time,
                forecast));
        for (SegmentClearingPoint onepoint : points) {
            if (onepoint.getSegment().equals(segment)) {
                point = onepoint;
            }
        }
        if (point == null) {
            point = new SegmentClearingPoint();
            segmentClearingPoints.add(point);
        }
        point.setAbstractMarket(abstractMarket);
        point.setPrice(price);
        point.setTime(time);
        point.setVolume(volume);
        point.setSegment(segment);
        point.setForecast(forecast);
        point.setInterconnectorFlow(interconnectorFlow);
        return point;
    }

    public CO2MarketClearingPoint createOrUpdateCO2MarketClearingPoint(DecarbonizationMarket abstractMarket,
            double price, double volume, boolean emergencyTriggerActivated, double emergencyTriggerOutflow, long time,
            boolean forecast) {
        CO2MarketClearingPoint point = null;
        // TODO make this a pipe
        if (findClearingPointsForMarketAndTime(abstractMarket, time, forecast).iterator().hasNext()) {
            point = (CO2MarketClearingPoint) findClearingPointsForMarketAndTime(abstractMarket, time, forecast)
                    .iterator().next();
        } else {
            point = new CO2MarketClearingPoint();
            clearingPoints.add(point);
        }
        point.setAbstractMarket(abstractMarket);
        point.setPrice(price);
        point.setTime(time);
        point.setVolume(volume);
        point.setEmergencyTriggerActivated(emergencyTriggerActivated);
        point.setEmergencyTriggerOutflow(emergencyTriggerOutflow);
        point.setForecast(forecast);
        return point;
    }
    
    
 

    public Iterable<LongTermContract> findLongTermContractsForEnergyProducerActiveAtTime(EnergyProducer energyProducer, long time) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        return null;
    }

    public Iterable<LongTermContract> findLongTermContractsForEnergyProducerForSegmentActiveAtTime(EnergyProducer energyProducer,
            Segment segment, long time) {

        return longTermContracts.stream().filter(p -> p.getLongTermContractType().getSegments().contains(segment)).filter(p -> p.getStart() <= time).filter(p -> p.getFinish() >= time).filter(p -> p.getFrom().equals(energyProducer)).collect(Collectors.toList());
    }

    public Iterable<Contract> findLongTermContractsForEnergyConsumerActiveAtTime(EnergyConsumer energyConsumer, long time) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        return null;
    }

    public Iterable<LongTermContract> findLongTermContractsForEnergyConsumerForSegmentActiveAtTime(EnergyConsumer consumer,
            Segment segment, long time) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        return null;
    }

    public LongTermContract findLongTermContractForPowerPlantActiveAtTime(PowerPlant plant, long time) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        return null;
    }

    public List<LongTermContract> findLongTermContractsForEnergyConsumerForSegmentForZoneActiveAtTime(EnergyConsumer consumer,
            Segment segment, Zone zone, long time) {
        return longTermContracts.stream().filter(p -> p.getLongTermContractType().getSegments().contains(segment)).filter(p -> p.getZone().equals(zone)).filter(p -> p.getStart() <= time).filter(p -> p.getFinish() >= time).filter(p -> p.getTo().equals(consumer)).collect(Collectors.toList());
    }

    /**
     * Determine the total CO2 emissions based on all current power plant
     * dispatch plans.
     *
     * @return the total CO2 emissions
     */
    public double calculateTotalEmissionsBasedOnPowerPlantDispatchPlans(boolean forecast, long clearingTick) {
        double totalEmissions = 0d;
        //int counter = 0;
        for (PowerPlantDispatchPlan plan : findAllPowerPlantDispatchPlansForTime(
                clearingTick, forecast)) {
            double operationalCapacity = plan.getCapacityLongTermContract() + plan.getAcceptedAmount();
            double emissionIntensity = plan.getPowerPlant().calculateEmissionIntensity();
            double hours = plan.getSegment().getLengthInHours();
            totalEmissions += operationalCapacity * emissionIntensity * hours;
            //    counter++;
        }
        // logger.warn("Total emissions: {} based on {} power plant dispatch plans", totalEmissions, counter);
        return totalEmissions;
    }

    /**
     * Creates a long term contract
     *
     * @param plant
     * @param seller
     * @param buyer
     * @param zone
     * @param price
     * @param capacity
     * @param longTermContractType
     * @param time
     * @param duration
     * @param signed
     * @param mainFuel
     * @param fuelPassThroughFactor
     * @param co2PassThroughFactor
     * @param fuelPriceStart
     * @param co2PriceStart
     * @return
     */
    public LongTermContract submitLongTermContractForElectricity(PowerPlant plant, EMLabAgent seller, EMLabAgent buyer,
            Zone zone, double price, double capacity, LongTermContractType longTermContractType, long time,
            LongTermContractDuration duration, boolean signed, Substance mainFuel, double fuelPassThroughFactor,
            double co2PassThroughFactor, double fuelPriceStart, double co2PriceStart) {

        LongTermContract contract = new LongTermContract();
        contract.setUnderlyingPowerPlant(plant);
        contract.setFrom(seller);
        contract.setTo(buyer);
        contract.setZone(zone);
        contract.setPricePerUnit(price);
        contract.setCapacity(capacity);
        contract.setLongTermContractType(longTermContractType);
        contract.setStart(time);
        contract.setFinish(time + duration.getDuration() - 1);
        contract.setDuration(duration);
        contract.setSigned(signed);
        contract.setMainFuel(mainFuel);
        contract.setFuelPassThroughFactor(fuelPassThroughFactor);
        contract.setCo2PassThroughFactor(co2PassThroughFactor);
        contract.setFuelPriceStart(fuelPriceStart);
        contract.setCo2PriceStart(co2PriceStart);
        return contract;
    }

    public LongTermContractOffer submitLongTermContractOfferForElectricity(EnergyProducer seller, PowerPlant plant, Zone zone, double price, double capacity, LongTermContractType longTermContractType, long time, LongTermContractDuration duration, Substance mainFuel, double fuelPassThroughFactor, double co2PassThroughFactor, double fuelPriceStart, double co2PriceStart) {

        LongTermContractOffer offer = new LongTermContractOffer();
        offer.setSeller(seller);
        offer.setUnderlyingPowerPlant(plant);
        offer.setZone(zone);
        offer.setPrice(price);
        offer.setCapacity(capacity);
        offer.setLongTermContractType(longTermContractType);
        offer.setStart(time);
        offer.setDuration(duration);
        offer.setMainFuel(mainFuel);
        offer.setFuelPassThroughFactor(fuelPassThroughFactor);
        offer.setCo2PassThroughFactor(co2PassThroughFactor);
        offer.setFuelPriceStart(fuelPriceStart);
        offer.setCo2PriceStart(co2PriceStart);
        return offer;
    }

    public void removeOffer(LongTermContractOffer offer) {
        longTermContractOffers.remove(offer);
    }

    public void removeAllOffers() {
        longTermContractOffers.clear();
    }

    public void reassignLongTermContractToNewPowerPlant(LongTermContract longTermContract, PowerPlant plant) {
        longTermContract.setUnderlyingPowerPlant(plant);
    }

    public List<EnergyProducer> findAllEnergyProducersExceptForRenewableTargetInvestorsAtRandom() {
        List<EnergyProducer> list = energyProducers.stream().filter(p -> p.getClass().equals(EnergyProducer.class)).collect(Collectors.toList());
        Collections.shuffle(list);
        return list;
    }

    public Iterable<FinancialPowerPlantReport> findAllFinancialPowerPlantReportsOfOperationaPlantsFromToForEnergyProducerAndTechnology(long from, long to, EnergyProducer producer, PowerGeneratingTechnology tech) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        return null;
    }

    public double calculateHistoricalCVarRelativePerMWForOperationaPlantsForEnergyProducerAndTechnologyForYearsFromToAndAlphaValue(long from, long to, EnergyProducer producer, PowerGeneratingTechnology tech, double alpha) {
        throw new UnsupportedOperationException();
    }

    public List<FinancialPowerPlantReport> findAllFinancialPowerPlantReportsForTime(long time) {
        return financialPowerPlantReports.stream().filter(p -> p.getTime() == time).collect(Collectors.toList());
    }

    public FinancialPowerPlantReport findFinancialPowerPlantReportsForPlantForTime(PowerPlant plant, long time) {
        Optional<FinancialPowerPlantReport> optional = financialPowerPlantReports.stream().filter(p -> p.getPowerPlant() == plant).filter(p -> p.getTime() == time).findFirst();
        if (optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }

    public void removeFinancialPowerPlantReportsUpToTime(long time) {
        financialPowerPlantReports.removeIf(p -> (p.getTime() <= time));
    }
    
    
    public List<MarketInformationReport> findAllMarketInformationReportsForTime(long time) {
        return marketInformationReports.stream().filter(p -> p.getTime() == time).collect(Collectors.toList());
    }
    
    

    public IntermittentResourceProfile findIntermittentResourceProfileByTechnologyAndNode(PowerGeneratingTechnology technology, PowerGridNode node) {
        return intermittentResourceProfiles.stream()
                .filter(p -> p.getIntermittentTechnology().equals(technology))
                .filter(p -> p.getIntermittentProductionNode().equals(node)).findFirst().get();
    }

    public IntermittentTechnologyNodeLoadFactor findIntermittentTechnologyNodeLoadFactorForPowerPlant(PowerPlant plant) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        return null;
    }

    public IntermittentTechnologyNodeLoadFactor findIntermittentTechnologyNodeLoadFactorForNodeAndTechnology(PowerGridNode node, PowerGeneratingTechnology tech) {
        Optional<IntermittentTechnologyNodeLoadFactor> optional = intermittentTechnologyNodeLoadFactors.stream()
                .filter(p -> p.getTechnology().equals(tech))
                .filter(p -> p.getNode().equals(node))
                .findFirst();
        if (optional.isPresent()){
            return optional.get();
        } else {
            IntermittentTechnologyNodeLoadFactor intTechnologyNodeLoadFactor = new IntermittentTechnologyNodeLoadFactor();
            intTechnologyNodeLoadFactor.setLoadFactors(new double[segments.size()]);
            intTechnologyNodeLoadFactor.setNode(node);
            intTechnologyNodeLoadFactor.setTechnology(tech);
            return intTechnologyNodeLoadFactor;
        }  
    }

    public NationalGovernment findNationalGovernmentByPowerPlant(PowerPlant plant) {
        return nationalGovernments.stream().filter(p -> p.getGovernedZone().equals(plant.getLocation().getZone())).findFirst().get();

    }

    public NationalGovernment findNationalGovernmentByElectricitySpotMarket(ElectricitySpotMarket market) {
        return nationalGovernments.stream().filter(p -> p.getGovernedZone().equals(market.getZone())).findFirst().get();
    }

    /**
     * Creates cash flow
     *
     * @param from
     * @param to
     * @param amount the money transfered
     * @param type what the cashFlow is about
     * @param time the time
     * @param plant the power plant related to this cash flow
     * @return the cash flow
     */
    public CashFlow createCashFlow(EMLabAgent from,
            EMLabAgent to, double amount, int type, long time,
            PowerPlant plant) {
        CashFlow cashFlow = new CashFlow();
        cashFlow.setFrom(from);
        cashFlow.setTo(to);
        cashFlow.setMoney(amount);
        cashFlow.setType(type);
        cashFlow.setTime(time);
        cashFlow.setRegardingPowerPlant(plant);
        from.setCash(from.getCash() - amount);
        if (to != null) {
            to.setCash(to.getCash() + amount);
        }
        cashFlows.add(cashFlow);
        return cashFlow;
    }

    /**
     * Submit bids to a market.
     *
     * @param market
     * @param agent
     * @param time
     * @param isSupply
     * @param price
     * @param amount
     * @return the submitted bid
     */
    public Bid submitBidToMarket(DecarbonizationMarket market, EMLabAgent agent, long time, boolean isSupply, double price,
            double amount) {

        Bid bid = new Bid();
        bid.setBiddingMarket(market);
        bid.setBidder(agent);
        bid.setSupplyBid(isSupply);
        bid.setTime(time);
        bid.setPrice(price);
        bid.setAmount(amount);
        bid.setStatus(Bid.SUBMITTED);
        bids.add(bid);
        return bid;
    }

    public PowerGeneratingTechnologyNodeLimit findOneByTechnologyAndMarket(PowerGeneratingTechnology tech, ElectricitySpotMarket market) {
        Optional<PowerGeneratingTechnologyNodeLimit> optional = powerGeneratingTechnologyNodeLimits.stream().filter(p -> p.getPowerGeneratingTechnology().equals(tech)).filter(p -> p.getPowerGridNode().getZone().equals(market.getZone())).findFirst();
        if (optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }

    public PowerGeneratingTechnologyNodeLimit findOneByTechnologyAndNode(PowerGeneratingTechnology tech, PowerGridNode node) {
        Optional<PowerGeneratingTechnologyNodeLimit> optional = powerGeneratingTechnologyNodeLimits.stream().filter(p -> p.getPowerGeneratingTechnology().equals(tech)).filter(p -> p.getPowerGridNode().equals(node)).findFirst();
        if (optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }

    public Iterable<PowerGeneratingTechnology> findPowerGeneratingTechnologyByName(String name) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        return null;
    }

    public Iterable<PowerGeneratingTechnology> findAllIntermittentPowerGeneratingTechnologies() {
        return powerGeneratingTechnologies.stream().filter(p -> p.isIntermittent()).collect(Collectors.toList());
    }

    public Set<PowerGeneratingTechnologyTarget> findAllPowerGeneratingTechnologyTargetsByMarket(ElectricitySpotMarket market) {
        Optional<TargetInvestor> present = targetInvestors.stream().filter(p -> p.getInvestorMarket().equals(market)).findFirst();
        if (present.isPresent()) {
            return (Set<PowerGeneratingTechnologyTarget>) present.get().getPowerGenerationTechnologyTargets();
        } else {
            return new HashSet<PowerGeneratingTechnologyTarget>();
        }
    }

    public Iterable<PowerGridNode> findAllPowerGridNodesByZone(Zone zone) {
        return powerGridNodes.stream().filter(p -> p.getZone().equals(zone)).collect(Collectors.toList());     
    }

    public PowerGridNode findFirstPowerGridNodeByElectricitySpotMarket(ElectricitySpotMarket esm) {
        return powerGridNodes.stream().filter(p -> p.getZone() == esm.getZone()).findFirst().get();

    }

    public List<PowerPlantDispatchPlan> findAllPowerPlantDispatchPlansForTime(long time, boolean forecast) {
        return powerPlantDispatchPlans.stream().filter(p -> p.getTime() == time).filter(p -> p.isForecast() == forecast).collect(Collectors.toList());
    }

    public void removeAllPowerPlantDispatchPlansUpToTime(long time) {
        powerPlantDispatchPlans.removeIf(p -> (p.getTime() <= time));
    }

    public void removeAllPowerPlantDispatchPlansWithForecast(boolean forecast) {
        powerPlantDispatchPlans.removeIf(p -> (p.isForecast() == forecast));
    }

    public Iterable<PowerPlantDispatchPlan> findAllPowerPlantDispatchPlanForPowerPlantForSegmentForTime(PowerPlant plant,
            Segment segment, long time,
            boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        return null;
    }

    //TODO this is an expensive method!!
    public PowerPlantDispatchPlan findOnePowerPlantDispatchPlanForPowerPlantForSegmentForTime(PowerPlant plant,
            Segment segment, long time,
            boolean forecast) {

        Optional<PowerPlantDispatchPlan> plan = powerPlantDispatchPlans.stream().filter(p -> p.getTime() == time).filter(p -> p.getPowerPlant().equals(plant)).filter(p -> p.getSegment().equals(segment)).filter(p -> p.isForecast() == forecast).findFirst();
        //return plan.get();
       // return plan.orElse(null);
        if (plan.isPresent()) {
            return plan.get();
        } else {
            return null;
        }
    }

    //TODO this is an expensive method!!
    public List<PowerPlantDispatchPlan> findPowerPlantDispatchPlansForPowerPlantForTime(PowerPlant plant,
            long time,
            boolean forecast) {

        return powerPlantDispatchPlans.stream().filter(p -> p.getTime() == time).filter(p -> p.isForecast() == forecast).filter(p -> p.getPowerPlant().equals(plant)).collect(Collectors.toList());
    }

    public List<PowerPlantDispatchPlan> findSortedPowerPlantDispatchPlansForSegmentForTime(Segment segment,
            long time, boolean forecast) {
        List<PowerPlantDispatchPlan> list = powerPlantDispatchPlans.stream().filter(p -> p.getTime() == time).filter(p -> p.getSegment() == segment).filter(p -> p.isForecast() == forecast).collect(Collectors.toList());
        Logger.getGlobal().finer("NUmber of plans: " + list.size());
        list.sort(Comparator.comparing(o -> o.getPrice()));
        return list;
    }

    public List<PowerPlantDispatchPlan> findAllPowerPlantDispatchPlansForPowerPlantForTime(PowerPlant plant,
            long time, boolean forecast) {
        return powerPlantDispatchPlans.stream().filter(p -> p.getTime() == time).filter(p -> p.getPowerPlant().equals(plant)).filter(p -> p.isForecast() == forecast).collect(Collectors.toList());
    }

    public double calculateElecitricityOutputForPlantForTime(PowerPlant plant,
            long time, boolean forecast) {
        return powerPlantDispatchPlans.stream().filter(p -> p.getTime() == time).filter(p -> p.getPowerPlant().equals(plant)).filter(p -> p.isForecast() == forecast).mapToDouble(p -> calculateElectricityOutputForPlan(p)).sum();
    }

    public Iterable<PowerPlantDispatchPlan> findAllPowerPlantDispatchPlansForEnergyProducerForTime(
            EnergyProducer producer, long time, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");

        throw new UnsupportedOperationException();

    }

    public Iterable<PowerPlantDispatchPlan> findAllPowerPlantDispatchPlansForEnergyProducerForTimeForTechnology(
            EnergyProducer producer, long time,
            PowerGeneratingTechnology pgt, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlantDispatchPlan> findAllAcceptedPowerPlantDispatchPlansForEnergyProducerForTime(
            EnergyProducer producer, long time, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public double calculateTotalProductionForEnergyProducerForTime(EnergyProducer producer,
            long time, boolean forecast) {
        throw new UnsupportedOperationException();
    }

    public double calculateTotalProductionForEnergyProducerForTimeForTechnology(
            EnergyProducer producer, long time,
            PowerGeneratingTechnology pgt, boolean forecast) {
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlantDispatchPlan> findAllPowerPlantDispatchPlansForEnergyProducerForTimeAndSegment(
            Segment segment, EnergyProducer producer, long time, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public List<PowerPlantDispatchPlan> findAllAcceptedPowerPlantDispatchPlansForEnergyProducerForTimeAndSegment(
            Segment segment, EnergyProducer producer, long time, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public List<PowerPlantDispatchPlan> findAllAcceptedPowerPlantDispatchPlansForMarketSegmentAndTime(
            ElectricitySpotMarket esm, Segment segment, long time, boolean forecast) {

        return powerPlantDispatchPlans.stream().filter(p -> p.getSegment().equals(segment)).filter(p -> p.getTime() == time).filter(p -> p.getStatus() == PowerPlantDispatchPlan.ACCEPTED).filter(p -> p.getBiddingMarket().equals(esm)).filter(p -> p.isForecast() == forecast).collect(Collectors.toList());
    }

    /**
     * Finds plants by owner.
     *
     * @param owner of the plants
     * @return the list of plants
     */
    public Iterable<PowerPlant> findPowerPlantsByOwner(EnergyProducer owner) {
        return powerPlantsForAgent.get(owner);
    }

    public double countPowerPlantsByOwner(EnergyProducer owner) {
        return powerPlantsForAgent.get(owner).size();
    }
    
    /**
     * Original Gremlin query: "g.v(gridnode).in('LOCATION').filter{(it.__type__=='emlab.gen.domain.technology.PowerPlant')}.as('p')
     * .out('TECHNOLOGY').filter{it.name==technologyName}.back('p')
     * .filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) == tick) && (it.expectedEndOfLife > tick)}"
     * 
     * @param node
     * @param technologyName
     * @param tick
     * @return
     */
    public Iterable<PowerPlant> findPowerPlantsStartingOperationThisTickByPowerGridNodeAndTechnology(
            PowerGridNode node, String technologyName,long tick){
    	
    	return powerPlants.stream()
    			.filter(p -> p.getLocation().equals(node))
    			.filter(p -> p.getTechnology().getName().equals(technologyName))
    			.filter(p -> (p.getConstructionStartTime() + p.getActualPermittime() + p.getActualLeadtime()) == tick)
    			.filter(p -> p.getExpectedEndOfLife() > tick)
    			.collect(Collectors.toList());
    }
    
    
    /**
     * Original Gremlin: "g.v(gridnode).in('LOCATION').filter{(it.__type__=='emlab.gen.domain.technology.PowerPlant')}.as('p').out('TECHNOLOGY').filter{it.name==g.v(technology).name}.back('p').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)}"
     * @param node
     * @param powerGeneratingTechnology
     * @param tick
     * @return
     */
    public Iterable<PowerPlant> findOperationalPowerPlantsByPowerGridNodeAndTechnology(PowerGridNode node,
            PowerGeneratingTechnology powerGeneratingTechnology, long tick){
    	
    	return powerPlants.stream()
    			.filter(p -> p.getLocation().equals(node))
    			.filter(p -> p.getTechnology().equals(powerGeneratingTechnology))
    			.filter(p -> (p.getConstructionStartTime() + p.getActualPermittime() + p.getActualLeadtime()) <= tick)
    			.filter(p -> p.getExpectedEndOfLife() > tick)
    			.collect(Collectors.toList());
    }

    

    /**
     * Finds operational plants (only use for current tick, since only
     * officially dismantled powerplants and plants in the building process will
     * be excluded).
     *
     * @param owner of the plants
     * @param tick at which the operationality it is checked
     * @return the list of plants
     */
    public List<PowerPlant> findOperationalPowerPlants(long tick) {
        return powerPlants.stream().filter(p -> p.isOperational(tick)).collect(Collectors.toList());
    }

    public List<PowerPlant> findExpectedOperationalPowerPlants(long tick) {
        return powerPlants.stream().filter(p -> p.isExpectedToBeOperational(tick)).collect(Collectors.toList());
    }

    public List<PowerPlant> findOperationalPowerPlantsWithFuelsGreaterZero(long tick) {
        return findOperationalPowerPlants(tick).stream().filter(p -> p.getTechnology().getFuels().size() > 0).collect(Collectors.toList());
    }

    public List<PowerPlant> findAllPowerPlantsWhichAreNotDismantledBeforeTick(long tick) {
        return powerPlants.stream().filter(p -> p.isWithinTechnicalLifetime(tick)).collect(Collectors.toList());
    }

    public List<PowerPlant> findAllPowerPlantsDismantledBeforeTick(long tick) {
        return powerPlants.stream().filter(p -> p.getDismantleTime() < tick).collect(Collectors.toList());
    }

    public void removePowerPlantsDismantledUpToTime(long tick) {
        powerPlants.removeIf(p -> (p.getDismantleTime() <= tick));
    }

    public List<PowerPlant> findAllPowerPlantsWithConstructionStartTimeInTick(long tick) {
        return powerPlants.stream().filter(p -> p.getConstructionStartTime() == tick).collect(Collectors.toList());
    }

    public double calculateCapacityOfOperationalPowerPlants(long tick) {
        return calculateCapacityOfPowerPlantList(findOperationalPowerPlants(tick), tick);
    }

    public double calculatePeakCapacityOfOperationalPowerPlants(long tick) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Finds plants by owner and selects only operational plants.
     *
     * @param owner of the plants
     * @param tick at which the operationality it is checked
     * @return the list of plants
     */
    public List<PowerPlant> findOperationalPowerPlantsByOwner(EnergyProducer owner, long tick) {
        //return powerPlantsForAgent.get(owner);
        return powerPlantsForAgent.get(owner).stream().filter(p -> p.isOperational(tick)).collect(Collectors.toList());
    }

    public List<PowerPlant> findOperationalPowerPlantsWithFuelsGreaterZeroByOwner(EnergyProducer owner,
            long tick) {
        return findOperationalPowerPlantsByOwner(owner, tick).stream().filter(p -> p.getTechnology().getFuels().size() > 0).collect(Collectors.toList());
    }

    /**
     * Finds plants by owner and selects only operational plants.
     *
     * @param technology
     * @param owner of the plants
     * @param tick at which the operationality it is checked
     * @return the list of plants
     */
    public List<PowerPlant> findOperationalPowerPlantsByTechnology(PowerGeneratingTechnology technology, long tick) {
        return findOperationalPowerPlants(tick).stream().filter(p -> p.getTechnology().equals(technology)).collect(Collectors.toList());
    }

    /**
     * Finds plants by owner and selects only operational plants.
     *
     * @param technology
     * @param owner of the plants
     * @param tick at which the operationality it is checked
     * @return the list of plants
     */
    public List<PowerPlant> findOperationalPowerPlantsByTechnologyInMarket(PowerGeneratingTechnology technology, ElectricitySpotMarket market, long tick) {
        return findOperationalPowerPlants(tick).stream().filter(p -> p.getTechnology().equals(technology)).filter(p -> findElectricitySpotMarketByPowerPlant(p).equals(market)).collect(Collectors.toList());
    }

    public double calculateCapacityOfOperationalPowerPlantsByTechnology(PowerGeneratingTechnology technology,
            long tick) {
        return calculateCapacityOfPowerPlantList(findOperationalPowerPlantsByTechnology(technology, tick), tick);
    }

    public double calculateCapacityOfOperationalPowerPlantsByTechnologyInMarket(PowerGeneratingTechnology technology, ElectricitySpotMarket market,
            long tick) {
        return calculateCapacityOfPowerPlantList(findOperationalPowerPlantsByTechnologyInMarket(technology, market, tick), tick);
    }

    
    public double calculateProductionOfOperationalPowerPlantsByTechnologyInMarket(PowerGeneratingTechnology technology, ElectricitySpotMarket market,
            long tick) {
        
        
        return powerPlantDispatchPlans.stream()
                .filter(p -> p.getPowerPlant().getTechnology().equals(technology))
                .filter(p -> findElectricitySpotMarketByPowerPlant(p.getPowerPlant()).equals(market))
                .filter(p -> p.getTime() == tick)
                .collect(Collectors.summarizingDouble(PowerPlantDispatchPlan::getElectricityOutput)).getSum();
        
    }
    
    
    
    public PowerPlant findOneOperationalHistoricalCvarDummyPowerPlantsByOwnerAndTechnology(
            PowerGeneratingTechnology technology,
            long tick, EnergyProducer owner) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public double calculateCapacityOfPowerPlantList(List<PowerPlant> list, long tick) {
        double capacity = 0d;
        for (PowerPlant plant : list) {
            capacity += plant.getAvailableCapacity(tick);
        }
        return capacity;
    }

    public double calculateCapacityOfOperationalPowerPlantsByOwnerAndTechnology(PowerGeneratingTechnology technology, long tick, EnergyProducer owner) {
        return calculateCapacityOfPowerPlantList(findOperationalPowerPlantsByOwnerAndTechnology(technology, tick, owner), tick);
    }

    public List<PowerPlant> findOperationalPowerPlantsByOwnerAndTechnology(PowerGeneratingTechnology technology, long tick, EnergyProducer owner) {
        return powerPlantsForAgent.get(owner).stream().filter(p -> p.getTechnology().equals(technology)).filter(p -> p.isOperational(tick)).collect(Collectors.toList());
    }

    public Iterable<PowerPlant> findPowerPlantsByTechnology(PowerGeneratingTechnology technology) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlant> findPowerPlantsByPowerGridNode(PowerGridNode node) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlant> findOperationalPowerPlantsByPowerGridNode(PowerGridNode node,
            long tick) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlant> findPowerPlantsByOwnerAndMarket(EnergyProducer owner,
            ElectricitySpotMarket market) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlant> findOperationalPowerPlantsInMarket(ElectricitySpotMarket market,
            long tick) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public double calculateCapacityOfOperationalPowerPlantsInMarket(ElectricitySpotMarket market,
            long tick) {
        return powerPlants.stream().filter(p -> p.isOperational(tick)).filter(p -> findElectricitySpotMarketByPowerPlant(p).equals(market)).collect(Collectors.summarizingDouble(PowerPlant::getActualNominalCapacity)).getSum();
    }

    public double calculatePeakCapacityOfOperationalPowerPlantsInMarket(ElectricitySpotMarket market,
            long tick) {
        throw new UnsupportedOperationException();
    }

    public List<PowerPlant> findExpectedOperationalPowerPlantsInMarket(
            ElectricitySpotMarket market, long tick) {

        return powerPlants.stream().filter(p -> electricitySpotMarketForPowerPlant.get(p).equals(market)).filter(p -> (p.getConstructionStartTime() + p.getActualPermittime() + p.getActualLeadtime()) <= tick).filter(p -> p.getExpectedEndOfLife() > tick).collect(Collectors.toList());
    }

    public double calculateCapacityOfExpectedOperationalPowerPlantsInMarket(
            ElectricitySpotMarket market, long tick) {
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarketAndTechnology(
            ElectricitySpotMarket market, long tick) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }
    
    /**
     * 
     * Adopted from:
     * <code>   @Query(value = "result = g.v(market).out('ZONE').in('REGION').in('LOCATION').filter{it.__type__=='emlab.gen.domain.technology.PowerPlant'}.as('x').out('TECHNOLOGY').filter{it.name==g.v(tech).name}.back('x').filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime) <= tick) && (it.expectedEndOfLife > tick)};"
     *       + "if(result == null){return 0;} else{return result;}", type = QueryType.Gremlin)
     * public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarketByTechnology(
     *     @Param("market") ElectricitySpotMarket market, @Param("tech") PowerGeneratingTechnology technology,
     *       @Param("tick") long tick);</code
     * @param market
     * @param technology
     * @param tick
     * @return
     */
    public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarketByTechnology(
            ElectricitySpotMarket market, PowerGeneratingTechnology technology, long tick) {

    	return powerPlants.stream()
    			.filter(p -> electricitySpotMarketForPowerPlant.get(p).equals(market))//TODO MM: why did emile implement it like that?
    			.filter(p -> p.getTechnology().equals(technology))
    			.filter(p -> (p.getConstructionStartTime() + p.getActualPermittime() + p.getActualLeadtime()) <= tick)
    			.filter(p -> p.getExpectedEndOfLife() > tick)
    			.collect(Collectors.toList());
    	
    }



    public double calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(
            ElectricitySpotMarket market, PowerGeneratingTechnology technology,
            long tick) {

        return powerPlants.stream().filter(p -> p.isExpectedToBeOperational(tick)).filter(p -> p.getTechnology().equals(technology)).filter(p -> findElectricitySpotMarketByPowerPlant(p).equals(market)).collect(Collectors.summarizingDouble(PowerPlant::getActualNominalCapacity)).getSum();
    }

    public double calculateCapacityOfExpectedOperationalPowerPlantsByNodeAndTechnology(
            PowerGridNode node, PowerGeneratingTechnology technology,
            long tick) {
        return powerPlants.stream().filter(p -> p.isExpectedToBeOperational(tick)).filter(p -> p.getTechnology().equals(technology)).filter(p -> findElectricitySpotMarketByPowerPlant(p).getZone().equals(node.getZone())).collect(Collectors.summarizingDouble(PowerPlant::getActualNominalCapacity)).getSum();
    }

    public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarketByOwnerAndTechnology(
            ElectricitySpotMarket market, PowerGeneratingTechnology technology,
            long tick, EnergyProducer owner) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public double calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwnerAndTechnology(
            ElectricitySpotMarket market, PowerGeneratingTechnology technology,
            long tick, EnergyProducer owner) {
        return powerPlantsForAgent.get(owner).stream().filter(p -> p.isExpectedToBeOperational(tick)).filter(p -> p.getTechnology().equals(technology)).filter(p -> findElectricitySpotMarketByPowerPlant(p).equals(market)).collect(Collectors.summarizingDouble(PowerPlant::getActualNominalCapacity)).getSum();
    }

    public double calculateCapacityOfExpectedOperationalPowerPlantsByOwner(long tick,
            EnergyProducer owner) {
        return powerPlantsForAgent.get(owner).stream().filter(p -> p.isExpectedToBeOperational(tick)).collect(Collectors.summarizingDouble(PowerPlant::getActualNominalCapacity)).getSum();
    }

    public double calculateCapacityOfExpectedOperationalPowerPlantsByOwnerByTechnology(long tick,
            EnergyProducer owner, PowerGeneratingTechnology pgt) {
        return powerPlantsForAgent.get(owner).stream().filter(p -> p.isExpectedToBeOperational(tick)).filter(p -> p.getTechnology().equals(pgt)).collect(Collectors.summarizingDouble(PowerPlant::getActualNominalCapacity)).getSum();
    }

    public double calculateCapacityOfExpectedDismantledPowerPlantsByOwnerByTechnology(long tick,
            EnergyProducer owner, PowerGeneratingTechnology pgt) {
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarketByOwner(
            ElectricitySpotMarket market, long tick,
            EnergyProducer owner) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public double calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwner(
            ElectricitySpotMarket market, long tick,
            EnergyProducer owner) {
        return powerPlantsForAgent.get(owner).stream().filter(p -> p.isExpectedToBeOperational(tick)).filter(p -> findElectricitySpotMarketByPowerPlant(p).equals(market)).collect(Collectors.summarizingDouble(PowerPlant::getActualNominalCapacity)).getSum();
    }

    public List<PowerPlant> findPowerPlantsInMarket(ElectricitySpotMarket market) {
        return powerPlants.stream().filter(p -> findElectricitySpotMarketByPowerPlant(p).equals(market)).collect(Collectors.toList());
    }

    public List<PowerPlant> findOperationalPowerPlantsByOwnerAndMarket(EnergyProducer owner,
            ElectricitySpotMarket market, long tick) {
        return powerPlantsForAgent.get(owner).stream().filter(p -> p.isOperational(tick)).filter(p -> findElectricitySpotMarketByPowerPlant(p).equals(market)).collect(Collectors.toList());

    }
    
    /**
     * Original Gremlin: "g.v(market).out('ZONE').in('REGION').in('LOCATION')
     * .filter{(it.__type__=='emlab.gen.domain.technology.PowerPlant')}.as('p').out('TECHNOLOGY').filter{it==g.v(technology)}.back('p')
     * .filter{((it.constructionStartTime + it.actualPermittime + it.actualLeadtime)<= tick) && (it.expectedEndOfLife > tick)}"
     * @param market
     * @param powerGeneratingTechnology
     * @param tick
     * @return
     */    
    public Iterable<PowerPlant> findOperationalPowerPlantsByMarketAndTechnology(ElectricitySpotMarket market,
            PowerGeneratingTechnology powerGeneratingTechnology, long tick){
    	
        return powerPlants.stream()
        		.filter(p -> findElectricitySpotMarketByPowerPlant(p).equals(market))
        		.filter(p -> p.getTechnology().equals(powerGeneratingTechnology))
    			.filter(p -> (p.getConstructionStartTime() + p.getActualPermittime() + p.getActualLeadtime()) <= tick)
    			.filter(p -> p.getExpectedEndOfLife() > tick)
        		.collect(Collectors.toList());
    }
    

    public Iterable<PowerPlant> findPowerPlantsByOwnerAndMarketInPipeline(EnergyProducer owner,
            ElectricitySpotMarket market, long tick) {
        return powerPlantsForAgent.get(owner).stream().filter(p -> p.isInPipeline(tick)).filter(p -> findElectricitySpotMarketByPowerPlant(p).equals(market)).collect(Collectors.toList());
    }

    public Iterable<PowerPlant> findPowerPlantsByTechnologyInPipeline(
            PowerGeneratingTechnology technology, long tick) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public double calculateCapacityOfPowerPlantsByTechnologyInPipeline(
            PowerGeneratingTechnology technology, long tick) {
        double sum = powerPlants.stream().filter(p -> p.isInPipeline(tick)).filter(p -> p.getTechnology().equals(technology)).collect(Collectors.summarizingDouble(PowerPlant::getActualNominalCapacity)).getSum();
        if (sum > 0) {
            logger.finer("Pipeline capacity for " + technology + " is " + sum + " MW");
        }
        return sum;

    }

    public double calculateCapacityOfPowerPlantsByMarketInPipeline(ElectricitySpotMarket market,
            long tick) {
        return powerPlants.stream().filter(p -> p.isInPipeline(tick)).filter(p -> market.getZone().equals(p.getLocation().getZone())).collect(Collectors.summarizingDouble(PowerPlant::getActualNominalCapacity)).getSum();
    }

    public double calculateSubstanceUsage(Substance substance) {
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlant> findOperationalIntermittentPowerPlantsByPowerGridNode(PowerGridNode node,
            long tick) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public double calculateCapacityOfIntermittentPowerPlantsByPowerGridNode(PowerGridNode node,
            long tick) {
        throw new UnsupportedOperationException();
    }

    public Iterable<PowerPlant> findOperationalIntermittentPowerPlantsByPowerGridNodeAndTechnology(
            PowerGridNode node,
            PowerGeneratingTechnology powerGeneratingTechnology, long tick) {
            return powerPlants.stream()
                .filter(p -> p.isOperational(tick))
                .filter(p -> p.getTechnology().equals(powerGeneratingTechnology))
                .filter(p -> p.getLocation().equals(node))
                .collect(Collectors.toList());
    }
    
    public double calculateCapacityOfOperationalIntermittentPowerPlantsByPowerGridNodeAndTechnology(
            PowerGridNode node,
            PowerGeneratingTechnology powerGeneratingTechnology, long tick) {
         return powerPlants.stream()
                .filter(p -> p.isOperational(tick))
                .filter(p -> p.getTechnology().equals(powerGeneratingTechnology))
                .filter(p -> p.getLocation().equals(node))
                .mapToDouble(p -> p.getAvailableCapacity(tick)).sum();
        
    }

    public List<CashFlow> getCashFlowsForPowerPlant(PowerPlant plant, long tick) {
        return cashFlows.stream().filter(p -> p.getTime() == tick).filter(p -> p.getRegardingPowerPlant() != null).filter(p -> p.getRegardingPowerPlant().equals(plant)).collect(Collectors.toList());
    }

    public double calculateFullLoadHoursOfPowerPlant(PowerPlant plant, long tick) {
        return powerPlantDispatchPlans.stream().filter(p -> p.getPowerPlant().equals(plant)).filter(p -> p.getTime() == tick).mapToDouble(p -> calculateFullLoadHoursForPlan(p)).sum();
    }

    public double calculateFullLoadHoursForPlan(PowerPlantDispatchPlan plan) {
        double totalAmount = plan.getAcceptedAmount() + plan.getCapacityLongTermContract();
        double hoursInSegment = plan.getSegment().getLengthInHours();
        return hoursInSegment * totalAmount / plan.getPowerPlant().getActualNominalCapacity();
    }

    public ArrayList<EnergyProducer> findEnergyProducersAtRandom() {
        ArrayList<EnergyProducer> copy = new ArrayList<>(energyProducers);
        Collections.shuffle(copy);
        return copy;
    } // TODO check: what happened to markets? Could this be source of the bug?
    
    public ArrayList<EnergyProducer> findEnergyProducersByMarketAtRandom(ElectricitySpotMarket market) {
        ArrayList<EnergyProducer> copy = new ArrayList<>(energyProducers.stream().filter(p -> p.getInvestorMarket() == market).collect(Collectors.toList()));
        Collections.shuffle(copy);
        return copy;
    } // TODO MMcheck
    
    
    

    public ArrayList<EnergyConsumer> findEnergyConsumersAtRandom() {
        ArrayList<EnergyConsumer> copy = new ArrayList<>(energyConsumers);
        Collections.shuffle(copy);
        return copy;
    }

    public ArrayList<CommoditySupplier> findCommoditySuppliersAtRandom() {
        ArrayList<CommoditySupplier> copy = new ArrayList<>(commoditySuppliers);
        Collections.shuffle(copy);
        return copy;
    }

    public ArrayList<CommodityMarket> findCommodityMarketsAtRandom() {
        ArrayList<CommodityMarket> copy = new ArrayList<>(commodityMarkets);
        Collections.shuffle(copy);
        return copy;
    }

    public Iterable<SegmentClearingPoint> findAllSegmentClearingPointsForTime(long time, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public Iterable<SegmentClearingPoint> findAllSegmentClearingPointsForSegmentAndTime(long time, Segment segment, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    /**
     * Finds the segment loads for a certain segment.
     *
     * @param segment the segment to find the load for
     * @return the segment load
     */
    public Iterable<SegmentLoad> findAllSegmentLoadsBySegment(Segment segment) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    /**
     * Finds the segment load for a certain segment and market
     *
     * @param segment the segment to find the load for
     * @param market the market to find the load for
     * @return
     */
    public double returnSegmentBaseLoadBySegmentAndMarket(Segment segment, ElectricitySpotMarket market) {
        return segmentLoads.stream().filter(p -> p.getSegment().equals(segment)).filter(p -> p.getElectricitySpotMarket().equals(market)).findFirst().get().getBaseLoad();
    }

    public Iterable<TargetInvestor> findAllTargetInvestorsByMarket(ElectricitySpotMarket electricitySpotMarket) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public List<ClearingPoint> findAllClearingPointsForSubstanceTradedOnCommodityMarkesAndTimeRange(Substance substance, long timeFrom, long timeTo, boolean forecast) {
        return clearingPoints.stream().filter(p -> p.getTime() >= timeFrom).filter(p -> p.getTime() <= timeTo).filter(p -> p.getAbstractMarket().getSubstance().equals(substance)).filter(p -> p.isForecast() == forecast).collect(Collectors.toList());
    }

    public Iterable<ClearingPoint> findAllClearingPointsForMarketAndTimeRange(DecarbonizationMarket market, long timeFrom, long timeTo, boolean forecast) {
        return clearingPoints.stream().filter(p -> p.getAbstractMarket().equals(market)).filter(p -> p.getTime() <= timeTo).filter(p -> p.getTime() >= timeFrom).filter(p -> p.isForecast() == forecast).collect(Collectors.toList());
    }

    public double calculateAverageClearingPriceForMarketAndTimeRange(DecarbonizationMarket market, long timeFrom, long timeTo, boolean forecast) {
        throw new UnsupportedOperationException();
    }

    public Iterable<ClearingPoint> findAllClearingPointsForSubstanceAndTimeRange(Substance substance, long timeFrom, long timeTo, boolean forecast) {
        Logger.getGlobal().log(Level.SEVERE, "Not yet implemented...");
        throw new UnsupportedOperationException();
    }

    public PowerGeneratingTechnologyTarget findPowerGeneratingTechnologyTargetByTechnologyAndMarket(PowerGeneratingTechnology technology, ElectricitySpotMarket market) {
        Optional<PowerGeneratingTechnologyTarget> optional = findAllPowerGeneratingTechnologyTargetsByMarket(market).stream().filter(p -> p.getPowerGeneratingTechnology().equals(technology)).findFirst();
        if (optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }

    public double calculateElectricityOutputForPlan(PowerPlantDispatchPlan plan) {
        return plan.getSegment().getLengthInHours()
                * (plan.getCapacityLongTermContract() + plan
                .getAcceptedAmount());
    }

    public PowerPlant createAndSpecifyTemporaryPowerPlant(long time, EnergyProducer energyProducer,
            PowerGridNode location, PowerGeneratingTechnology technology) {
        String label = energyProducer.getName() + " - " + technology.getName();
        PowerPlant plant = new PowerPlant();
        plant.setName(label);
        plant.setTechnology(technology);
        plant.setOwner(energyProducer);
        plant.setLocation(location);
        plant.setConstructionStartTime(time);
        plant.setActualLeadtime(technology.getExpectedLeadtime());
        plant.setActualPermittime(technology.getExpectedPermittime());
        plant.calculateAndSetActualEfficiency(time);
        plant.setHistoricalCvarDummyPlant(false);
        plant.setActualNominalCapacity(plant.getTechnology().getCapacity() * location.getCapacityMultiplicationFactor());
        assert plant.getActualEfficiency() <= 1 : plant.getActualEfficiency();
        plant.setDismantleTime(1000);
        plant.calculateAndSetActualInvestedCapital(time);
        plant.calculateAndSetActualFixedOperatingCosts(time);
        plant.setExpectedEndOfLife(time + plant.getActualPermittime()
                + plant.getActualLeadtime() + plant.getTechnology().getExpectedLifetime());

        plant.reps = this;
        return plant;
    }

    public PowerPlant createPowerPlantWithRandomAge(PowerGeneratingTechnology technology, EnergyProducer energyProducer, PowerGridNode location) {
        PowerPlant plant = new PowerPlant();
        String label = energyProducer.getName() + " - " + technology.getName();
        plant.setName(label);
        plant.setTechnology(technology);
        plant.setOwner(energyProducer);
        plant.setLocation(location);
        plant.setConstructionStartTime(-(technology.getExpectedLeadtime() + technology.getExpectedPermittime() + Math.round((Math.random() * technology
                .getExpectedLifetime()))) + 2); // TODO: Why include expected lead
        // time and permit time? Wouldn't it
        // be realistic to have some PP in
        // the pipeline at the start?
        plant.setActualLeadtime(plant.getTechnology().getExpectedLeadtime());
        plant.setActualPermittime(plant.getTechnology().getExpectedPermittime());
        plant.setExpectedEndOfLife(plant.getConstructionStartTime() + plant.getActualPermittime() + plant.getActualLeadtime()
                + plant.getTechnology().getExpectedLifetime());
        plant.setActualNominalCapacity(technology.getCapacity() * location.getCapacityMultiplicationFactor());
        plant.calculateAndSetActualInvestedCapital(plant.getConstructionStartTime());
        plant.calculateAndSetActualEfficiency(plant.getConstructionStartTime());
        plant.calculateAndSetActualFixedOperatingCosts(plant.getConstructionStartTime());
        plant.setDismantleTime(1000);
        plant.setHistoricalCvarDummyPlant(false);

        //TODO move determineLoanAnnuities?
        double amountPerPayment = ElectricityProducerFactory.determineLoanAnnuities(plant.getActualInvestedCapital() * energyProducer.getDebtRatioOfInvestments(),
                plant.getTechnology().getDepreciationTime(), energyProducer.getLoanInterestRate());

        Loan loan = createLoan(energyProducer, null, amountPerPayment, plant.getTechnology().getDepreciationTime(), plant.getConstructionStartTime(), plant);
        loan.setNumberOfPaymentsDone(-plant.getConstructionStartTime());// Some payments are already made
        createPowerPlantFromPlant(plant);
        return plant;
    }

    public EnergyProducer createEnergyProducer() {
        EnergyProducer producer = new EnergyProducer();
        energyProducers.add(producer);
        powerPlantsForAgent.put(producer, new ArrayList<>());
        return producer;
    }
    
    public TargetInvestor createTargetInvestor() {
        TargetInvestor producer = new TargetInvestor();
        energyProducers.add(producer);
        targetInvestors.add(producer);
        powerPlantsForAgent.put(producer, new ArrayList<>());
        return producer;
    }
      
   
    public PowerGeneratingTechnology createPowerGeneratingTechnology() {
        PowerGeneratingTechnology tech = new PowerGeneratingTechnology();
        powerGeneratingTechnologies.add(tech);
        return tech;
    }

    public PowerPlant createPowerPlantFromPlant(PowerPlant plant) {
        plant.reps = this;
        powerPlants.add(plant);
        if (!powerPlantsForAgent.containsKey(plant.getOwner())) {
            powerPlantsForAgent.put(plant.getOwner(), new ArrayList<>());
        }
        powerPlantsForAgent.get(plant.getOwner()).add(plant);
        electricitySpotMarketForPowerPlant.put(plant, findElectricitySpotMarketForZone(plant.getLocation().getZone()));
        //logger.warning(plant.toString() + plant.getOwner() + findElectricitySpotMarketByPowerPlant(plant));
                
        return plant;
    }

    public ElectricitySpotMarket createElectricitySpotMarket(String name, double valueOfLostLoad, double referencePrice, boolean isAuction, Substance substance,
            TimeSeriesImpl demandGrowth, Set<SegmentLoad> loadDurationCurve, Zone zone) {
        ElectricitySpotMarket market = new ElectricitySpotMarket();
        market.setName(name);
        market.setValueOfLostLoad(valueOfLostLoad);
        market.setReferencePrice(referencePrice);
        market.setAuction(isAuction);
        market.setSubstance(substance);
        market.setDemandGrowthTrend(demandGrowth);
        market.setLoadDurationCurve(loadDurationCurve);
        market.setZone(zone);
        for (SegmentLoad load : loadDurationCurve) {
            load.setElectricitySpotMarket(market);
        }
        electricitySpotMarkets.add(market);
        marketForSubstance.put(substance, market);//TODO should this be here?
        return market;
    }

    public NationalGovernment createNationalGovernment(String name, Zone zone, TimeSeriesImpl minCO2PriceTrend) {
        NationalGovernment nationalGovernment = new NationalGovernment();
        nationalGovernment.setName(name);
        nationalGovernment.setGovernedZone(zone);
        nationalGovernment.setMinNationalCo2PriceTrend(minCO2PriceTrend);
        nationalGovernments.add(nationalGovernment);
        electricitySpotMarketForNationalGovernment.put(nationalGovernment, findElectricitySpotMarketForZone(zone));
        return nationalGovernment;
    }

    public PowerPlantDispatchPlan createPowerPlantDispatchPlan(PowerPlant plant, EnergyProducer producer, ElectricitySpotMarket market, Segment segment, long time,
            double price, double bidWithoutCO2, double spotMarketCapacity,
            double longTermContractCapacity, int status, boolean forecast) {
        
        //TODO Update if already exists!
         long count = findPowerPlantDispatchPlansForPowerPlantForTime(plant, time, forecast).stream().filter(p -> (p.getSegment().equals(segment))).count();
         if (count > 0) {
             logger.warning("I found a plan already! ERROR should not create a new one");
         }

        PowerPlantDispatchPlan plan = new PowerPlantDispatchPlan();
        plan.setPowerPlant(plant);
        plan.setSegment(segment);
        plan.setTime(time);
        plan.setBidder(producer);
        plan.setBiddingMarket(market);
        plan.setPrice(price);
        plan.setBidWithoutCO2(bidWithoutCO2);
        plan.setAmount(spotMarketCapacity);
        plan.setCapacityLongTermContract(longTermContractCapacity);
        plan.setStatus(status);
        plan.setForecast(forecast);
        powerPlantDispatchPlans.add(plan);
        logger.finer("Plans:" + powerPlantDispatchPlans.size());
        return plan;
    }

    public CO2Auction createCO2Auction(String name, double referencePrice, boolean isAuction, Substance substance) {
        co2Auction = new CO2Auction();
        co2Auction.setName("CO2 auction");
        co2Auction.setReferencePrice(referencePrice);
        co2Auction.setAuction(isAuction);
        co2Auction.setSubstance(substance);
        marketForSubstance.put(substance, co2Auction);
        return co2Auction;
    }
    
    
    // RenewableTargetForTenderReps
 
    /**
     * Read TimesSeries for Tender by a Scheme (Like what? FIT)
     * 
     * Adopted from original version by Kaveri (RenewableTargetForTenderRepositor:45)
     * @param scheme
     * @param technology
     * @return TimeSeriesCSVReader
     * @author marcmel
     */
    public TimeSeriesCSVReader findTechnologySpecificRenewableTargetTimeSeriesForTenderByScheme(RenewableSupportSchemeTender scheme,  String technologyName) {
    	        	
    	Optional<RenewableTarget> optional = renewableTargets.stream()
    			.filter(p -> p.isTargetTechnologySpecific() == true)
    			.filter(p -> p.getRegulator().equals(scheme.getRegulator())) // Only consider target of the regulator of this scheme
    			.filter(p -> p.getPowerGeneratingTechnology().getName().equals(technologyName))
    			.findFirst();
    	
        if (optional.isPresent()) {
            return optional.get().getYearlyRenewableTargetTimeSeries();
        } else {
            return null;
        }
    	
    	// TODO MM transformed technologyName to technology -> may be a problem for definition? Is it?
    	// TODO MM maybe implement isPresent()
    }
    
    /**
     * Original Gremlin query: "g.v(regulator).out('SET_BY_REGULATOR')
     * .filter{it.__type__=='emlab.gen.domain.policy.renewablesupport.RenewableTarget'}.as('x')
     * .out('FOR_TECHNOLOGY').filter{it.name==technologyName}.back('x')
     * .out('AT_NODE').filter{it.name == nodeName}.back('x')
     * .filter{it.targetTechnologySpecific == true}.out('TARGET_TREND')"
     * 
     * @param regulator
     * @param technologyName
     * @param nodeName
     * @return
     */    
    public TimeSeriesCSVReader findTechnologyAndNodeSpecificRenewableTargetTimeSeriesForTenderByRegulator(
            Regulator regulator, String technologyName, String nodeName) {
    	
    	return renewableTargets.stream()
    			.filter(p -> p.getRegulator().equals(regulator)) // Only consider target of the regulator of this scheme
    			.filter(p -> p.getPowerGeneratingTechnology().getName().equals(technologyName))
    			.filter(p -> p.getPowerGridNode().equals(nodeName))
    			.filter(p -> p.isTargetTechnologySpecific() == true)
    			.findFirst().get().getYearlyRenewableTargetTimeSeries();	
    }
    
    // RenewablePotentialLimit Reps
    
    /**
     * Original Germlin query: "g.v(regulator).out('SET_BY_REGULATOR')
     *   .filter{it.__type__=='emlab.gen.domain.policy.renewablesupport.RenewablePotentialLimit'}.as('x')
     *   .out('FOR_TECHNOLOGY').filter{it.name==technologyName}.back('x').filter{it.targetTechnologySpecific == true}.out('TARGET_TREND')"
     * 
     * @param regulator
     * @param technologyName
     * @return
     */
    public TimeSeriesCSVReader findTechnologySpecificRenewablePotentialLimitTimeSeriesByRegulator(
            Regulator regulator, String technologyName) {

    	Optional<RenewablePotentialLimit> optional = renewablePotentialLimits.stream()
    			.filter(p -> p.getRegulator().equals(regulator))
    			.filter(p -> p.getPowerGeneratingTechnology().getName().equals(technologyName))
    			.filter(p -> p.isTargetTechnologySpecific() == true)
    			.findFirst();
       	
        if (optional.isPresent()) {
            return optional.get().getYearlyRenewableTargetTimeSeries();
        } else {
            return null;
        }
    }
    

    
    /**
     * Adopted from original version by Kaveri (RenewableTargetForTenderRepositor:38)
     * @param regulator
     * @return
     */
    public RenewableTarget findTechnologyNeutralRenewableTargetForTenderByRegulator(Regulator regulator) {
    	return renewableTargets.stream().filter(p -> p.getRegulator().equals(regulator)).filter(p -> p.isTargetTechnologySpecific() == false).findFirst().get();
    	
    	
    }
    
    
    /**
     * Gremlin: "g.v(regulator).out('SET_BY_REGULATOR')
     * .filter{it.__type__=='emlab.gen.domain.policy.renewablesupport.RenewableTarget'}.as('x')
     * .out('FOR_TECHNOLOGY').filter{it.name==technologyName}.back('x')
     * .filter{it.targetTechnologySpecific == true}"
     * @param regulator
     * @param technologyName
     * @return
     */
   
    public RenewableTarget findTechnologySpecificRenewableTargetForTenderByRegulator(
            Regulator regulator, String technologyName) {
    	
    	return renewableTargets.stream()
    			.filter(p -> p.isTargetTechnologySpecific() == true)
    			.filter(p -> p.getRegulator().equals(regulator))
    			.filter(p -> p.getPowerGeneratingTechnology().getName().equals(technologyName))
    			.findFirst().get();
    	
    }
    
    
    // TenderBidReps
        
    /**
     * This sorts the submitted tender bids by price
     * Original gremlin query: "@Query(value = "g.v(scheme).in('TENDERBID_SUPPORTSCHEME').filter{it.status == 1}.filter{it.time == tick}.sort{it.price}._()", type = QueryType.Gremlin)"
     * 
     * @param time
     * @param scheme
     * @return
     */
    public Iterable<TenderBid> findAllSubmittedSortedTenderBidsbyTime(long time, RenewableSupportSchemeTender scheme){
    	
    	return tenderBids.stream()
    			.filter(p -> p.getRenewableSupportSchemeTender().equals(scheme))
    			.filter(p -> p.getStatus() == 1)
    			.filter(p -> p.getTime() == time)
    			.sorted(Comparator.comparing(o -> o.getPrice()))
    			.collect(Collectors.toList());  	

    }
    
    
    /**
     * Original Germlin query: "g.v(scheme).in('TENDERBID_SUPPORTSCHEME').as('x').out('FOR_TECHNOLOGY').filter{it.name == technologyName}.back('x').filter{it.status == 1}.filter{it.time == tick}.sort{it.price}._()"
     *  
     * @param time
     * @param scheme
     * @param technologyName
     * @return
     */
    public Iterable<TenderBid> findAllSubmittedSortedTenderBidsbyTechnology(long time, RenewableSupportSchemeTender scheme, String technologyName){
    	
    	return tenderBids.stream()
    			.filter(p -> p.getRenewableSupportSchemeTender().equals(scheme))
    			.filter(p -> p.getTechnology().getName().equals(technologyName))
    			.filter(p -> p.getStatus() == 1)
    			.filter(p -> p.getTime() == time)
    			.sorted(Comparator.comparing(o -> o.getPrice()))
    			.collect(Collectors.toList());  	
    }
    
    /**
     * Original Gremlin: "g.v(scheme).in('TENDERBID_SUPPORTSCHEME').as('x')
     * .out('FOR_TECHNOLOGY').filter{it.name == technologyName}.back('x')
     * .out('FOR_NODE').filter{it.name == nodeName}.back('x')
     * .filter{it.status == 1}
     * .filter{it.time == tick}
     * .sort{it.price}._()"
     * 
     * @param time
     * @param scheme
     * @param technologyName
     * @param nodeName
     * @return
     */
    public Iterable<TenderBid> findAllSubmittedSortedTenderBidsbyTechnologyAndNode(long time,
            RenewableSupportSchemeTender scheme, String technologyName,
            String nodeName){
    	
    	return tenderBids.stream()
    			.filter(p -> p.getRenewableSupportSchemeTender().equals(scheme))
    			.filter(p -> p.getTechnology().getName().equals(technologyName))
    			.filter(p -> p.getPowerGridNode().equals(nodeName))
    			.filter(p -> p.getStatus() == 1)
    			.filter(p -> p.getTime() == time)
    			.sorted(Comparator.comparing(o -> o.getPrice()))
    			.collect(Collectors.toList());  	

    }
    
    
    /**
     * This returns the (partly) accepted bids for the current tick, needed to create the corresponding power plant
     * 
     * Original Gremlin query: "g.v(scheme).in('TENDERBID_SUPPORTSCHEME')"
     *       + ".propertyFilter('time', FilterPipe.Filter.EQUAL, tick)"
     *       + ".propertyFilter('status', FilterPipe.Filter.GREATER_THAN_EQUAL, 2)"
     *
     * @param renewableSupportSchemeTender
     * @param time
     * @return Iterable<TenderBid>
     */
    public Iterable<TenderBid> findAllAcceptedTenderBidsByTime(
    		RenewableSupportSchemeTender scheme, long time){
    	
    	return tenderBids.stream()
    			.filter(p -> p.getRenewableSupportSchemeTender().equals(scheme))
    			.filter(p -> p.getTime() == time)
    			.filter(p -> p.getStatus() >= 2)
    			.collect(Collectors.toList());
    }
    
    
    /**
     * This returns the accepted tender bids by scheme that needs to be paid out
     * 
     * Original Gremlin query: "g.v(scheme).in('TENDERBID_SUPPORTSCHEME')"
     *       + ".propertyFilter('start', FilterPipe.Filter.LESS_THAN_EQUAL, tick)"
     *       + ".propertyFilter('finish', FilterPipe.Filter.GREATER_THAN, tick)"
     *       + ".propertyFilter('status', FilterPipe.Filter.GREATER_THAN_EQUAL, 2)"
     * 
     * @param renewableSupportSchemeTender
     * @param time
     * @return Iterable<TenderBid>
     */    
    public Iterable<TenderBid> findAllTenderBidsThatShouldBePaidInTimeStep(
            RenewableSupportSchemeTender scheme, long time){
    	
    	Iterable<TenderBid> tenderBidTempList = tenderBids.stream()
    			.filter(p -> p.getRenewableSupportSchemeTender().equals(scheme))
    			//.filter(p -> p.getStart() <= time) // eg.g start <= 19 < finish
    			//.filter(p -> p.getFinish() > time)
    			.filter(p -> p.getStatus() >= 2)
    			.collect(Collectors.toList());
    	
    	double size = ((Collection<?>) tenderBidTempList).size();
    	
    	if(size > 0) {
    		logger.fine(tenderBidTempList.toString());
    	}
    	
    	return tenderBidTempList;
    	
    }
    
    
    public TenderBid submitTenderBidToMarket(double amount, PowerPlant plant, EnergyProducer agent, Zone zone, PowerGridNode node,
            long startTime, long finishTime, double bidPricePerMWh, PowerGeneratingTechnology technology,
            long currentTime, int status, RenewableSupportSchemeTender scheme, double cashNeededForPlantDownpayments,
            String investor) {

        TenderBid tenderBid = new TenderBid();
        
        tenderBid.setAmount(amount);
        tenderBid.setPowerPlant(plant);
        tenderBid.setBidder(agent);
        tenderBid.setZone(zone);
        tenderBid.setPowerGridNode(node);
        tenderBid.setStart(startTime);
        tenderBid.setFinish(finishTime);
        tenderBid.setPrice(bidPricePerMWh);
        tenderBid.setTechnology(technology);
        tenderBid.setTime(currentTime);
        tenderBid.setStatus(status);
        tenderBid.setRenewableSupportSchemeTender(scheme);
        tenderBid.setCashNeededForPlantDownpayments(cashNeededForPlantDownpayments);
        tenderBid.setInvestor(investor);
        
        tenderBids.add(tenderBid);
        return tenderBid;
       
    }
    
    
    // TenderClearingPointRepository
    
    /**
     * 
     * Original Gremlin query: "g.v(scheme).in('RENEWABLE_SUPPORT_SCHEME_TENDER_CP').propertyFilter('time', FilterPipe.Filter.EQUAL, time)"
     * 
     * @param time
     * @param renewableSupportSchemeTender
     * @return
     */
    
    public TenderClearingPoint findOneClearingPointForTimeAndRenewableSupportSchemeTender(long time,
            RenewableSupportSchemeTender renewableSupportSchemeTender) {
    	
    	return tenderClearingPoints.stream()
    			.filter(p -> p.getRenewableSupportSchemeTender().equals(renewableSupportSchemeTender))
    			.filter(p -> p.getTime() == time)
    			.findFirst().get();
    	
    }
    
    public TenderClearingPoint createTenderClearingPoint(RenewableSupportSchemeTender scheme , double price, double volume,
            long time) {

    	TenderClearingPoint  point = new TenderClearingPoint();
        tenderClearingPoints.add(point);
        
        point.setPrice(price);
        point.setVolume(volume);
        point.setRenewableSupportSchemeTender(scheme);
        point.setTime(time);
        return point;

    }


    // BaseCostFipRepository
    
    /**
     * Original Gremlin: "g.v(tech).in('BASECOST_FOR_TECHNOLOGY').as('x').out('BASECOST_FOR_LOCATION').filter{it.name==nodeName}.back('x').filter{it.startTime==tick}"

     * 
     * @param nodeName
     * @param technology
     * @param tick
     * @return
     * @author Kaveri3012, marcmel
     */
    public BaseCostFip findOneBaseCostForTechnologyAndNodeAndTime(String nodeName,
    		PowerGeneratingTechnology technology, long tick) {
    	
    	return baseCostFips.stream()
    			.filter(p -> p.getTechnology().equals(technology))
    			.filter(p -> p.getNode().getName().equals(nodeName))
    			.filter(p -> p.getStartTime() == tick)
    			.findFirst().get();
    			
    	
    }

    /**
     * Gremlin query: "g.idx('__types__')[[className:'emlab.gen.domain.policy.renewablesupport.BaseCostFip']].filter{it.startTime==tick}"
     * @param tick
     * @return
     */
    public BaseCostFip findOneTechnologyNeutralBaseCostForTime(long tick) {
    	
    	return baseCostFips.stream()
    			.filter(p -> p.getStartTime() == tick)
    			.findAny().get();

    	// TODO check
    }
    
    /**
     * Gremlin: g.idx('__types__')[[className:'emlab.gen.domain.policy.renewablesupport.BaseCostFip']].filter{it.startTime>=timeFrom && it.startTime<=timeTo}
     * @param timeFrom
     * @param timeTo
     * @return
     */
    public Iterable<BaseCostFip> findAllTechnologyNeutralBaseCostForTimeRange(long timeFrom,
            long timeTo){
    	
    	return baseCostFips.stream()
    			.filter(p -> p.getStartTime() >= timeFrom)
    			.filter(p -> p.getStartTime() <= timeTo)
    			.collect(Collectors.toList());
    	
    }

    
    /**
     * Gremlon query: "g.v(tech).in('BASECOST_FOR_TECHNOLOGY').as('x').out('BASECOST_FOR_LOCATION').filter{it.name==nodeName}.back('x').filter{it.startTime>=timeFrom && it.startTime<=timeTo}
     * 
     * @param nodeName
     * @param technology
     * @param timeFrom
     * @param timeTo
     * @return
     */
    public Iterable<BaseCostFip> findAllBaseCostFipsForTechnologyLocationAndTimeRange(String nodeName,
            PowerGeneratingTechnology technology, long timeFrom,
            long timeTo){
    	
    	return baseCostFips.stream()
    			.filter(p -> p.getTechnology().equals(technology))
    			.filter(p -> p.getNode().getName().equals(nodeName))
    			.filter(p -> p.getStartTime() >= timeFrom)
    			.filter(p -> p.getStartTime() <= timeTo)
    			.collect(Collectors.toList());
    			
    	
    }

    
    // Support Price Contract Reps
    
    /**
     * Gremlin: "g.v(plant).in('CONTRACT_FOR_POWERPLANT')"
     * @param plant
     * @return
     */
    
    public SupportPriceContract findOneContractByPowerPlant(PowerPlant plant) {
    	
        Optional<SupportPriceContract> optional = supportPriceContracts.stream().filter(p -> p.getPlant().equals(plant)).findFirst();
        if(optional.isPresent()){
            return optional.get();
        } else {
        	return null;
        
        }
    	
    	
    	
    }

    
    
    // RenewableSupportSchemeRepository
    
    /**
     * Gremlin: "g.v(scheme).in('BIASFACTOR_FOR_SUPPORTSCHEME').as('x').out('BIASFACTOR_FOR_NODE').filter{it.name==node}.back('x').out('BIASFACTOR_FOR_TECHNOLOGY').filter{it.name==technology}.back('x')"
     * @param technologyName
     * @param nodeName
     * @param scheme
     * @return
     */
    public BiasFactor findBiasFactorGivenTechnologyNodeAndScheme(String technologyName,
            String nodeName, RenewableSupportFipScheme scheme) {
    	
    	return biasFactors.stream()
    			.filter(p -> p.getScheme().equals(scheme))
    			.filter(p -> p.getTechnology().getName().equals(technologyName))
    			.filter(p -> p.getNode().getName().equals(nodeName))
    			.findFirst().get();
    }

    /**
     * Gremlin: "g.v(zone).in('RES_SCHEME_FOR_ZONE')"

     * @param zone
     * @return
     */
    
    public Set<RenewableSupportFipScheme> findSchemesGivenZone(Zone zone){
    	
    	return renewableSupportFipSchemes.stream()
    			.filter(p -> p.getZone().equals(zone))
    			.collect(Collectors.toSet());
    	
    }
    


    

    public Substance findSubstanceByName(String name) {
        return substances.stream().filter(p -> p.getName().equals(name)).findFirst().get();
    }

    public MarketInformationReport findMarketInformationReport(Segment segment, EnergyProducer agent, long time) {
     
        Optional<MarketInformationReport> optional = marketInformationReports.stream()
                .filter(p -> p.getSegment().equals(segment))
                .filter(p -> p.getAgent().equals(agent))
                .filter(p -> p.getTime() == time).findFirst();
        if(optional.isPresent()){
            return optional.get();
        } else {
            MarketInformationReport report = new MarketInformationReport();
            report.setAgent(agent);
            report.setSegment(segment);
            report.setTime(time); 
            marketInformationReports.add(report);
            return report;
        }
                               
                
        
        
    }
    
        

    public class PowerPlantDispatchPlanPriceComparator implements Comparator<PowerPlantDispatchPlan> {

        @Override
        public int compare(PowerPlantDispatchPlan o1, PowerPlantDispatchPlan o2) {
            Double d = (o2.getPrice() - o1.getPrice()) * 100;
            return d.intValue();
        }
    }

    public class BidPriceComparator implements Comparator<Bid> {

        @Override
        public int compare(Bid o1, Bid o2) {
            Double d = (o1.getPrice() - o2.getPrice()) * 100;
            return d.intValue();
        }
    }
    
    /**
     * Usef for empirical data
     * 
     * @param node
     * @param technologyName
     * @param tick
     * @return
     */
    public EmpiricalMappingFunctionParameter findEmpiricalMappingParameters(
    		ElectricitySpotMarket market, PowerGeneratingTechnology technology){
    	
    	return empiricalMappingFunctionParameters.stream()
    			.filter(p -> p.getTechnology().equals(technology))
    			.filter(p -> p.getMarket().equals(market))
    	        .findFirst().get();

    }
    
   


}
