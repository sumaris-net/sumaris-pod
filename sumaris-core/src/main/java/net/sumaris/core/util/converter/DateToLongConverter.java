package net.sumaris.core.util.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.math.BigDecimal;
import java.util.Date;

@ReadingConverter
public class DateToLongConverter implements Converter<Date, Long> {

    @Override
    public Long convert(Date source) {
        return source == null ? null : source.getTime();
    }
}
