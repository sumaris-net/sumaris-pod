package net.sumaris.core.extraction.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductStrataVO;

import java.util.Date;
import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationTypeVO extends ExtractionTypeVO implements
        IWithRecorderPersonEntity<Integer, PersonVO> {

    public static final String PROPERTY_STRATUM  = "stratum";

    String description;
    String comments;
    Date updateDate;

    PersonVO recorderPerson;

    List<ExtractionProductStrataVO> stratum;
}
