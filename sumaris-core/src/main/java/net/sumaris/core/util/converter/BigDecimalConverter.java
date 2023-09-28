package net.sumaris.core.util.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.math.BigDecimal;

@ReadingConverter
public class BigDecimalConverter implements Converter<BigDecimal, Long> {

    @Override
    public Long convert(BigDecimal source) {
        return source == null ? null : source.longValue();
    }
}
