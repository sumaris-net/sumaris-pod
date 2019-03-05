package net.sumaris.core.extraction.vo.trip.survivalTest;

import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.extraction.vo.trip.ices.ExtractionIcesContextVO;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionSurvivalTestContextVO extends ExtractionIcesContextVO {

    String survivalTestTableName; // ST (survival test) table

    String releaseTableName; // RL (release) table

}
