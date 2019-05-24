package net.sumaris.core.extraction.vo.trip;

import lombok.Data;
import net.sumaris.core.dao.technical.Pageable;
import net.sumaris.core.vo.filter.TripFilterVO;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
public class ExtractionTripFilterVO extends TripFilterVO {

    private boolean preview;

    private String sheetName;

    private Pageable pageable;
}
