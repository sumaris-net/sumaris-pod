package net.sumaris.core.util.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import javax.annotation.Nullable;
import java.sql.Timestamp;

@ReadingConverter
public class TimestampToLongConverter implements Converter<Timestamp, Long> {
    @Override
    public Long convert(@Nullable Timestamp source) {
        return source == null ? null : source.getTime();
    }
}