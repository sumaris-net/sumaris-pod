package net.sumaris.core.vo.filter;

import java.io.Serializable;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public interface IVesselFilter extends Serializable {

    Integer getVesselId();

    void setVesselId(Integer vesselId);
}
