package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Currency;
import com.h2o_execution.smart_order_router.domain.*;
import com.h2o_execution.smart_order_router.market_access.FIXGateway;
import com.h2o_execution.smart_order_router.market_access.FIXMessageMediator;
import com.h2o_execution.smart_order_router.market_access.OrderManager;
import com.h2o_execution.smart_order_router.market_access.VenueSessionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import quickfix.ConfigError;

import java.io.IOException;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
class ConsolidatedOrderBookTest
{
    Rank vr1 = new Rank(0.1, 0.5, 0.1, 0.5, 0.5);
    Rank vr2 = new Rank(0.2, 0.5, 0.3, 0.4, 0.2);
    Rank vr3 = new Rank(0.3, 0.5, 0.5, 0.3, 0.2);
    Rank vr4 = new Rank(0.4, 0.5, 0.4, 0.2, 0.1);
    Rank vr5 = new Rank(0.3, 0.5, 0.5, 0.2, 0.3);


    Venue chix = Venue
            .builder()
            .avgLatency(25)
            .close(new Venue.Bell(16, 0))
            .country(Country.CAN)
            .currency(Currency.CAD)
            .name(Name.CHI_X)
            .open(new Venue.Bell(9, 30))
            .close(new Venue.Bell(16, 0))
            .holidayMaster(new HolidayMaster())
            .symbols(Arrays.asList("RBI", "BMO", "RY"))
            .timeZone(ZoneId.systemDefault())
            .type(Venue.Type.LIT)
            .build();
    Venue nyse = Venue
            .builder()
            .avgLatency(50)
            .close(new Venue.Bell(16, 0))
            .country(Country.USA)
            .currency(Currency.USD)
            .name(Name.NYSE)
            .open(new Venue.Bell(9, 30))
            .close(new Venue.Bell(16, 0))
            .holidayMaster(new HolidayMaster())
            .symbols(Arrays.asList("RBI", "BMO", "RY"))
            .timeZone(ZoneId.systemDefault())
            .type(Venue.Type.LIT)
            .build();

    {
        chix.setRanking("RBI", vr1);
        chix.setRanking("BMO", vr2);
        chix.setRanking("RY", vr2);
    }

    {
        nyse.setRanking("RBI", vr3);
        nyse.setRanking("BMO", vr1);
        nyse.setRanking("RY", vr4);
    }


    ConsolidatedOrderBookTest() throws IOException
    {
    }

    @Test
    public void testBook() throws IOException, ConfigError
    {
        FXRatesService fxRatesService = new FXRatesServiceImpl(new RestTemplate());
        ConsolidatedOrderBook consolidatedOrderBook = new ConsolidatedOrderBookImpl(fxRatesService);
        for (int i = 0; i < 100000; i++)
        {
            boolean even = (i % 2) == 0;
            consolidatedOrderBook.addOrder(
                    Order
                            .builder()
                            .clientOrderId("testing" + ThreadLocalRandom.current().nextInt(0, 1000))
                            .orderType(OrderType.LIMIT)
                            .currency(even ? Currency.USD : Currency.CAD)
                            .side(i < 50_000 ? Side.BUY : Side.SELL)
                            .timeInForce(TimeInForce.DAY)
                            .symbol("RBI")
                            .limitPrice(even ? ThreadLocalRandom.current().nextInt(40, 50) : ThreadLocalRandom.current().nextInt(50, 70))
                            .quantity(250_000)
                            .venue(even ? chix : nyse)
                            .build()
            );
        }
        VenueSessionRegistry venueSessionRegistry = new VenueSessionRegistryImpl();
        FIXGateway fixGateway = new FIXGateway();
        FIXMessageMediator fixMessageMediator = new FIXMessageMediator(fixGateway, venueSessionRegistry);
        fixGateway.setFixMessageMediator(fixMessageMediator);
        OrderManager orderManager = new OrderManager(fixMessageMediator);
        List<Venue> venues = Arrays.asList(chix, nyse);
        ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider = new ProbabilisticExecutionVenueProviderImpl(venues);
        Map<Currency, Double> currencyDoubleMap = new HashMap<>();
        currencyDoubleMap.put(Currency.CAD, 6_000.0);
        currencyDoubleMap.put(Currency.USD, 240_000.0);
        RoutingConfig routingConfig =
                RoutingConfig
                .builder()
                .availableCapital(currencyDoubleMap)
                .countrySet(EnumSet.of(Country.CAN, Country.USA))
                .generation(Generation.SPRAY)
                .sweepType(Venue.Type.LIT)
                .postType(Venue.Type.LIT)
                .build();
        Router router = new ParallelRouter(orderManager, new OrderIdService(), probabilisticExecutionVenueProvider, consolidatedOrderBook, routingConfig);
        router.route(
                Order
                        .builder()
                        .clientOrderId("SOR-ORDER")
                        .orderType(OrderType.LIMIT)
                        .side(Side.BUY)
                        .currency(Currency.USD)
                        .symbol("RBI")
                        .limitPrice(66.5)
                        .quantity(500)
                        .build()
        );
    }
}