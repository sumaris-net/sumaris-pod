package net.sumaris.core.vo.filter;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldNameConstants
public class TaxonNameFilterVO {

    private String searchText;
    private String searchAttribute;

    private Integer taxonGroupId;
    private Integer[] taxonGroupIds;

    private Integer[] taxonomicLevelIds;
    private Integer[] statusIds;

    private Boolean withSynonyms;
}
