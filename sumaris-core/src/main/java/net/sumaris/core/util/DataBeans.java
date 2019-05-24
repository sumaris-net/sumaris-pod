package net.sumaris.core.util;

import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntityBean;
import net.sumaris.core.model.data.IWithRecorderPersonEntityBean;
import net.sumaris.core.vo.data.IWithVesselFeaturesVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;

import java.io.Serializable;

/**
 * Helper class for data beans
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class DataBeans extends Beans {

    protected DataBeans() {
        super();
        // helper class does not instantiate
    }

    public static <T extends Serializable, D extends IEntity<Integer>> void setDefaultRecorderDepartment(
            IWithRecorderDepartmentEntityBean<T, D> target,
            D defaultValue) {
        if (target == null) return;

        // Copy recorder department from the parent
        if (target.getRecorderDepartment() == null || target.getRecorderDepartment().getId() == null) {
            target.setRecorderDepartment(defaultValue);
        }
    }

    public static <T extends Serializable, D extends IEntity<Integer>> void setDefaultRecorderPerson(
            IWithRecorderPersonEntityBean<T, D> target,
            D defaultValue) {
        if (target == null) return;

        // Copy recorder person from the parent
        if (target.getRecorderPerson() == null || target.getRecorderPerson().getId() == null) {
            target.setRecorderPerson(defaultValue);
        }
    }

    public static <T extends Serializable, D extends VesselFeaturesVO> void setDefaultVesselFeatures(
            IWithVesselFeaturesVO<T, D> target,
            D defaultValue) {
        if (target == null) return;

        // Copy recorder person from the parent
        if (target.getVesselFeatures() == null || target.getVesselFeatures().getVesselId() == null) {
            target.setVesselFeatures(defaultValue);
        }
    }
}
