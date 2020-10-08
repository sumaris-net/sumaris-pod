package net.sumaris.core.vo.filter;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @author peck7 on 01/09/2020.
 */
@Data
@Builder
public class SampleFilterVO implements IRootDataFilter {

    private Date startDate;

    private Date endDate;

    private String programLabel;

    private Integer locationId;

    private Integer recorderDepartmentId;

    private Integer recorderPersonId;

    private Integer operationId;

    private Integer landingId;

}
