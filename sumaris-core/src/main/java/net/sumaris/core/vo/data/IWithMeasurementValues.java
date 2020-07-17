package net.sumaris.core.vo.data;

import java.util.Map;

/**
 * @author peck7 on 15/07/2020.
 */
public interface IWithMeasurementValues {

    Map<Integer, String> getMeasurementValues();

    void setMeasurementValues(Map<Integer, String> measurementValues);

}
