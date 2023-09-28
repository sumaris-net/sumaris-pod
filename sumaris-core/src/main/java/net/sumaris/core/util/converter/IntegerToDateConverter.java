package net.sumaris.core.util.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.Date;

@ReadingConverter
@Slf4j
public class IntegerToDateConverter implements Converter<Integer, Date> {

    @Override
    public Date convert(Integer source) {
        if (source == null) return null;
        log.debug("Converting integer ({}) to date. Please check your index mapping", source);
        return new Date(source.longValue());
    }
}
