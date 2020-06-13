package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Currency;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
class FXRatesServiceImplTest
{

    @Test
    void getRealTimeFXRate() throws ExecutionException
    {

        FXRatesService fxRatesService = new FXRatesServiceImpl(new RestTemplate());

        Currency[] currencies = Currency.values();
        for (Currency c1 : currencies)
        {
            for (Currency c2 : currencies)
            {
                double fxRate = fxRatesService.getFXRate(c1, c2);
                log.info("FX Rate " + c1 + c2 + ": " + fxRate);
                if (c1 == c2)
                {
                    assertEquals(1, fxRate, 0.1);
                }
            }
        }

    }
}