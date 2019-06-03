package net.sumaris.core.vo.filter;

import lombok.Data;

import java.util.Date;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
public class LandingFilterVO implements IRootDataFilter, IVesselFilter {

    private Date startDate;

    private Date endDate;

    private String programLabel;

    private Integer recorderDepartmentId;

    private Integer vesselId;

    private Integer locationId;

    // Parent
    private Integer observedLocationId;
    private Integer tripId;
}
