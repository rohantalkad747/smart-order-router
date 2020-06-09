package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ProbabilisticExecutionVenueProviderImpl implements ProbabilisticExecutionVenueProvider
{
    private final List<Venue> venues;
    private Map<String, List<Venue>> symbolVenueMap;
    private static final List<Country> CROSS_BORDER = Arrays.asList(Country.CAN, Country.USA, Country.UK);

    public ProbabilisticExecutionVenueProviderImpl(List<Venue> venues)
    {
        this.venues = venues;
        initSymbolVenueMap();
    }

    private void initSymbolVenueMap()
    {
        for (Venue venue : venues)
        {
            for (String symbol : venue.getSymbols())
            {
                symbolVenueMap.computeIfAbsent(symbol, k -> new ArrayList<>()).add(venue);
            }
        }
    }

    @Override
    public List<VenuePropertyPair<Double>> getVenueExecutionProbabilityPairs(String symbol, RoutingStage stage, RoutingConfig routingConfig)
    {
        List<Venue> venueSymbolList = symbolVenueMap.get(symbol);
        if (venueSymbolList == null || venueSymbolList.isEmpty())
        {
            throw new RuntimeException("No venue supports this symbol!");
        }
        List<Venue> candidateVenues = getCandidateVenues(routingConfig, stage, venueSymbolList);
        return getVenueExecutionPairs(symbol, candidateVenues);
    }

    private List<VenuePropertyPair<Double>> getVenueExecutionPairs(String symbol, List<Venue> candidateVenues)
    {
        double totalRank = calculateTotalRank(symbol, candidateVenues);
        return populateIndividualVenueRankings(symbol, candidateVenues, totalRank);
    }

    private double calculateTotalRank(String symbol, List<Venue> candidateVenues)
    {
        double totalRank = 0;
        for (Venue venue : candidateVenues)
        {
            Map<String, Rank> symbolRankMap = venue.getSymbolRankMap();
            Rank rank = symbolRankMap.get(symbol);
            totalRank += rank.calculate();
        }
        return totalRank;
    }

    private List<VenuePropertyPair<Double>> populateIndividualVenueRankings(String symbol, List<Venue> candidateVenues, double totalRank)
    {
        List<VenuePropertyPair<Double>> venueExecutionPairs = new ArrayList<>();
        for (Venue venue : candidateVenues)
        {
            Map<String, Rank> symbolRankMap = venue.getSymbolRankMap();
            Rank rank = symbolRankMap.get(symbol);
            double executionProbability = rank.calculate() / totalRank;
            VenuePropertyPair<Double> venueExecutionPair = new VenuePropertyPair<>(executionProbability, venue);
            venueExecutionPairs.add(venueExecutionPair);
        }
        return venueExecutionPairs;
    }

    private List<Venue> getCandidateVenues(RoutingConfig routingConfig, RoutingStage stage, List<Venue> venueSymbolList)
    {
        Stream<Venue> venueStream = venueSymbolList.stream();
        List<Venue> excludedVenues = routingConfig.getExcludedVenues();
        if (excludedVenues != null)
        {
            venueStream = venueStream.filter(x -> !(excludedVenues.contains(x)));
        }
        if (stage == RoutingStage.POST)
        {
            venueStream = venueStream.filter(x -> x.getType() == routingConfig.getPostType());
        }
        else
        {
            venueStream = venueStream.filter(x -> x.getType() == routingConfig.getSweepType());
        }
        Set<Country> countrySet = routingConfig.getCountrySet();
        if (!routingConfig.getCountrySet().containsAll(CROSS_BORDER))
        {
            venueStream = venueStream.filter(x -> countrySet.contains(x.getCountry()));
        }
        return venueStream
                .filter(Venue::isAvailable)
                .collect(Collectors.toList());
    }

    @Override
    public void addVenue(Venue v)
    {
        venues.add(v);
    }

    @Override
    public void removeVenue(Venue v)
    {
        venues.remove(v);
    }
}
