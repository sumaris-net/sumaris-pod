package net.sumaris.core.util.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.Date;

@ReadingConverter
public class LongToDateConverter implements Converter<Long, Date> {

    @Override
    public Date convert(Long source) {
        return source == null ? null : new Date(source);
    }
}
