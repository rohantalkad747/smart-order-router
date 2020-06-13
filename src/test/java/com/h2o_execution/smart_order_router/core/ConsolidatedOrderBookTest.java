package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Currency;
import com.h2o_execution.smart_order_router.domain.Name;
import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

@Slf4j
class ConsolidatedOrderBookTest
{

    Venue nasdaq = Venue.builder().avgLatency(25).close(new Venue.Bell(16, 0)).country(Country.CAN).currency(Currency.CAD).name(Name.NASDAQ).open(new Venue.Bell(9, 30)).build();

    @Test
    public void testBook()
    {
        FXRatesService fxRatesService = new FXRatesServiceImpl(new RestTemplate());
        ConsolidatedOrderBook consolidatedOrderBook = new ConsolidatedOrderBookImpl(fxRatesService);
        consolidatedOrderBook.addOrder(nasdaq, new Order());
    }
}