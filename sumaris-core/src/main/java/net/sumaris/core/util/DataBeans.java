package net.sumaris.core.util;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.data.IWithVesselSnapshotEntity;
import net.sumaris.core.vo.data.VesselSnapshotVO;

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

    public static <T extends Serializable, D extends VesselSnapshotVO> void setDefaultVesselFeatures(
            IWithVesselSnapshotEntity<T, D> target,
            D defaultValue) {
        if (target == null) return;

        // Copy recorder person from the parent
        if (target.getVesselSnapshot() == null || target.getVesselSnapshot().getId() == null) {
            target.setVesselSnapshot(defaultValue);
        }
    }
}
