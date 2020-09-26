package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.*;
import com.h2o_execution.smart_order_router.market_access.FIXGateway;
import com.h2o_execution.smart_order_router.market_access.FIXMessageMediator;
import com.h2o_execution.smart_order_router.market_access.OrderManager;
import com.h2o_execution.smart_order_router.market_access.VenueSessionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class ConsolidatedOrderBookTest {
    static Rank vr1 = new Rank(0.1, 0.5, 0.1, 0.5, 0.5);
    static Rank vr2 = new Rank(0.2, 0.5, 0.3, 0.4, 0.2);
    static Rank vr3 = new Rank(0.3, 0.5, 0.5, 0.3, 0.2);
    static Rank vr4 = new Rank(0.4, 0.5, 0.4, 0.2, 0.1);
    static Rank vr5 = new Rank(0.3, 0.5, 0.5, 0.2, 0.3);

    static String SOR_TEST_CLORID = "SOR-ORDER";
    static Venue nyse, chix, matchNow;
    static ConsolidatedOrderBook consolidatedOrderBook;
    static ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider;

    ConsolidatedOrderBookTest() throws IOException {
    }

    @BeforeAll
    static void init() {
        HolidayMaster hMasterNA = mock(HolidayMaster.class);
        when(hMasterNA.isHoliday(any(), any())).thenReturn(false);
        Venue.BellService bellService = mock(Venue.BellService.class);
        when(bellService.withinBell(any())).thenReturn(true);

        chix = newCHIX(hMasterNA, bellService);
        nyse = newNYSE(hMasterNA, bellService);
        matchNow = newMatchNow(hMasterNA, bellService);


        chix.setRanking("RBI", vr1);
        chix.setRanking("BMO", vr2);
        chix.setRanking("RY", vr2);

        nyse.setRanking("RBI", vr3);
        nyse.setRanking("BMO", vr1);
        nyse.setRanking("RY", vr4);

        matchNow.setRanking("RBI", vr1);
        matchNow.setRanking("BMO", vr2);
        matchNow.setRanking("RY", vr5);

        probabilisticExecutionVenueProvider = new ProbabilisticExecutionVenueProviderImpl(Arrays.asList(nyse, chix, matchNow));

    }

    private static Venue newNYSE(HolidayMaster hMasterNA, Venue.BellService bellService) {
        return Venue
                .builder()
                .avgLatency(50)
                .bellService(bellService)
                .country(Country.USA)
                .currency(Currency.USD)
                .name(Name.NYSE)
                .holidayMaster(hMasterNA)
                .symbols(Arrays.asList("RBI", "BMO", "RY"))
                .timeZone(ZoneId.systemDefault())
                .type(Venue.Type.LIT)
                .build();
    }

    private static Venue newCHIX(HolidayMaster hMasterNA, Venue.BellService bellService) {
        return Venue
                .builder()
                .avgLatency(25)
                .bellService(bellService)
                .country(Country.CAN)
                .currency(Currency.CAD)
                .name(Name.CHI_X)
                .holidayMaster(hMasterNA)
                .symbols(Arrays.asList("RBI", "BMO", "RY"))
                .timeZone(ZoneId.systemDefault())
                .type(Venue.Type.LIT)
                .build();
    }

    private static Venue newMatchNow(HolidayMaster hMasterNA, Venue.BellService bellService) {
        return Venue
                .builder()
                .avgLatency(13)
                .bellService(bellService)
                .country(Country.USA)
                .currency(Currency.USD)
                .name(Name.MATCH_NOW)
                .holidayMaster(hMasterNA)
                .symbols(Arrays.asList("RBI", "BMO", "RY"))
                .timeZone(ZoneId.systemDefault())
                .type(Venue.Type.LIT)
                .build();
    }

    @BeforeEach
    void beforeEach() throws ExecutionException {
        initSampleBook();
    }

    private void initSampleBook() throws ExecutionException {
        FXRatesService fxRatesService = mock(FXRatesService.class);
        when(fxRatesService.getFXRate(Currency.USD, Currency.CAD)).thenReturn(1.3);
        when(fxRatesService.getFXRate(Currency.CAD, Currency.USD)).thenReturn(1d / 1.3);

        consolidatedOrderBook = new ConsolidatedOrderBookImpl(fxRatesService);

        createRBISellorder(30, nyse, 100);
        createRBISellorder(31, nyse, 250);
        createRBISellorder(32, nyse, 250);

        createRBISellorder(30, matchNow, 100);
        createRBISellorder(31, matchNow, 250);
        createRBISellorder(32, matchNow, 250);

        createRBISellorder(40, chix, 50);

    }

    private void createRBISellorder(double price, Venue venue, int quantity) {
        Order order = Order
                .builder()
                .clientOrderId("testing" + randNum())
                .orderType(OrderType.LIMIT)
                .currency(venue.getCurrency())
                .side(Side.SELL)
                .timeInForce(TimeInForce.DAY)
                .symbol("RBI")
                .limitPrice(price)
                .quantity(quantity)
                .venue(venue)
                .build();
        consolidatedOrderBook.addOrder(order);
    }

    private int randNum() {
        return ThreadLocalRandom.current().nextInt(0, 1000);
    }

    @Test
    void testBook() {
        OrderManager orderManager = getOrderManager();
        Map<Currency, Double> currencyDoubleMap = sampleCurrencyPortfolio();
        RoutingConfig routingConfig = sampleRoutingConfig(currencyDoubleMap);
        Router router = new ParallelRouter(orderManager, new OrderIdService(), probabilisticExecutionVenueProvider, consolidatedOrderBook, routingConfig);
        router.route(randomBuyOrder(SOR_TEST_CLORID, "RBI", 32, 250));
        assertThat(router.getTotalRouted(), is(equalTo(250)));
    }

    private Order randomBuyOrder(String clorid, String symbol, double limit, int quantity) {
        return Order
                .builder()
                .clientOrderId(clorid)
                .orderType(OrderType.LIMIT)
                .side(Side.BUY)
                .currency(Currency.USD)
                .symbol(symbol)
                .limitPrice(limit)
                .quantity(quantity)
                .build();
    }

    private OrderManager getOrderManager() {
        VenueSessionRegistry venueSessionRegistry = mock(VenueSessionRegistry.class);
        FIXGateway fixGateway = mock(FIXGateway.class);
        FIXMessageMediator fixMessageMediator = new FIXMessageMediator(fixGateway, venueSessionRegistry);
        fixGateway.setFixMessageMediator(fixMessageMediator);
        return new OrderManager(fixMessageMediator);
    }

    private Map<Currency, Double> sampleCurrencyPortfolio() {
        Map<Currency, Double> currencyDoubleMap = new HashMap<>();
        currencyDoubleMap.put(Currency.CAD, 6_000.0);
        currencyDoubleMap.put(Currency.USD, 240_000.0);
        return currencyDoubleMap;
    }

    private RoutingConfig sampleRoutingConfig(Map<Currency, Double> currencyDoubleMap) {
        return RoutingConfig
                .builder()
                .availableCapital(currencyDoubleMap)
                .countrySet(EnumSet.of(Country.CAN, Country.USA))
                .generation(Generation.SPRAY)
                .sweepType(Venue.Type.LIT)
                .postType(Venue.Type.LIT)
                .build();
    }
}