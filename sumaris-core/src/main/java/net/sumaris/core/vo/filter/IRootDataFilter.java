package net.sumaris.core.vo.filter;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public interface IRootDataFilter extends Serializable {

    Date getStartDate();

    void setStartDate(Date startDate);

    Date getEndDate();

    void setEndDate(Date endDate);

    String getProgramLabel();

    void setProgramLabel(String programLabel);

    Integer getRecorderDepartmentId();

    void setRecorderDepartmentId(Integer recorderDepartmentId);

    Integer getLocationId();

    void setLocationId(Integer locationId);
}
