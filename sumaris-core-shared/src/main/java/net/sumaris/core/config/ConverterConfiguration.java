package net.sumaris.core.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;

@Configuration
public class ConverterConfiguration {

    /**
     * Default conversion service
     */
    @Bean(name = {"conversionService"})
    @ConditionalOnMissingBean(name = {"conversionService", "mvcConversionService"})
    @Lazy
    public ConversionService conversionService() {
       return new GenericConversionService();
    }
}
