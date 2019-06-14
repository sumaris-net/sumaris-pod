package net.sumaris.core.extraction.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;

import java.util.Date;
import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationTypeVO extends ExtractionTypeVO implements
        IWithRecorderPersonEntity<Integer, PersonVO> {

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public class Strata {
        List<String> space;
        List<String> time;
        List<String> tech;
    }

    String description;
    Date updateDate;

    PersonVO recorderPerson;

    Strata strata;
}
