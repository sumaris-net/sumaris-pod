package net.sumaris.core.extraction.dao.trip;

import com.google.common.collect.Lists;
import net.sumaris.core.extraction.dao.ExtractionDao;
import net.sumaris.core.extraction.vo.ExtractionFilterCriterionVO;
import net.sumaris.core.extraction.vo.ExtractionFilterOperatorEnum;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripFilterVO;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionTripDao extends ExtractionDao {

    String TR_SHEET_NAME = "TR";

    default ExtractionTripFilterVO toTripFilterVO(ExtractionFilterVO source){
        ExtractionTripFilterVO target = new ExtractionTripFilterVO();
        if (source == null) return target;

        Beans.copyProperties(source, target);

        if (CollectionUtils.isNotEmpty(source.getCriteria())) {

            source.getCriteria().stream()
                    .filter(criterion ->
                            org.apache.commons.lang3.StringUtils.isNotBlank(criterion.getValue())
                                    && "=".equals(criterion.getOperator()))
                    .forEach(criterion -> {
                        switch (criterion.getName().toLowerCase()) {
                            case "project":
                                target.setProgramLabel(criterion.getValue());
                                break;
                            case "year":
                                int year = Integer.parseInt(criterion.getValue());
                                target.setStartDate(Dates.getFirstDayOfYear(year));
                                target.setEndDate(Dates.getLastSecondOfYear(year));
                                break;
                        }
                    });
        }
        return target;
    }

    default ExtractionFilterVO toExtractionFilterVO(ExtractionTripFilterVO source){
        ExtractionFilterVO target = new ExtractionFilterVO();
        if (source == null) return target;

        Beans.copyProperties(source, target);

        List<ExtractionFilterCriterionVO> criteria = Lists.newArrayList();
        target.setCriteria(criteria);

        if (StringUtils.isNotBlank(source.getProgramLabel())) {
            ExtractionFilterCriterionVO criterion = new ExtractionFilterCriterionVO();
            criterion.setName("project");
            criterion.setOperator(ExtractionFilterOperatorEnum.EQUALS.getSymbol());
            criterion.setValue(source.getProgramLabel());

            criterion.setSheetName(TR_SHEET_NAME);

            criteria.add(criterion);
        }

        if (source.getStartDate() != null && source.getEndDate() != null) {
            // TODO convert into a criterion 'year', if first/last day of a year
        }

        return target;
    }
}
