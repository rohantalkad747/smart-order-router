package com.h2o_execution.smart_order_router.config;

import com.h2o_execution.smart_order_router.domain.CountryToHolidayMapper;
import com.h2o_execution.smart_order_router.domain.CountryToHolidayYAMLMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class Beans {

    @Bean
    public CountryToHolidayMapper countryToHolidayYAMLMapper() {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:holidays.yaml");
        return new CountryToHolidayYAMLMapper(resource);
    }
}
