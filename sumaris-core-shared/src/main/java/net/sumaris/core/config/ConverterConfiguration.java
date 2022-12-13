package net.sumaris.core.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.support.GenericConversionService;

@Configuration
public class ConverterConfiguration {

    /**
     * Default conversion service
     */
    @Bean(name = {"conversionService"})
    @Lazy
    public GenericConversionService conversionService() {
       return new GenericConversionService();
    }
}
