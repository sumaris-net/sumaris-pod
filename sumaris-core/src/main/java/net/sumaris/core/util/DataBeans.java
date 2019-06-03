package net.sumaris.core.util;

import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.data.IWithVesselFeaturesEntity;
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
            IWithRecorderDepartmentEntity<T, D> target,
            D defaultValue) {
        if (target == null) return;

        // Copy recorder department from the parent
        if (target.getRecorderDepartment() == null || target.getRecorderDepartment().getId() == null) {
            target.setRecorderDepartment(defaultValue);
        }
    }

    public static <T extends Serializable, D extends IEntity<Integer>> void setDefaultRecorderPerson(
            IWithRecorderPersonEntity<T, D> target,
            D defaultValue) {
        if (target == null) return;

        // Copy recorder person from the parent
        if (target.getRecorderPerson() == null || target.getRecorderPerson().getId() == null) {
            target.setRecorderPerson(defaultValue);
        }
    }

    public static <T extends Serializable, D extends VesselFeaturesVO> void setDefaultVesselFeatures(
            IWithVesselFeaturesEntity<T, D> target,
            D defaultValue) {
        if (target == null) return;

        // Copy recorder person from the parent
        if (target.getVesselFeatures() == null || target.getVesselFeatures().getVesselId() == null) {
            target.setVesselFeatures(defaultValue);
        }
    }
}
