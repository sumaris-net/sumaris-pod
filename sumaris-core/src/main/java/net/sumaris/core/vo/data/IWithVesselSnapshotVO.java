package net.sumaris.core.vo.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithVesselEntity;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.io.Serializable;
import java.util.Date;

public interface IWithVesselSnapshotVO<ID extends Serializable>
    extends IValueObject<ID>,
    IWithVesselEntity<ID, VesselSnapshotVO> {

    VesselSnapshotVO getVesselSnapshot();
    void setVesselSnapshot(VesselSnapshotVO vesselSnapshot);

    default VesselSnapshotVO getVessel() {
        return getVesselSnapshot();
    }

    default void setVessel(VesselSnapshotVO vesselSnapshot) {
        this.setVesselSnapshot(vesselSnapshot);
    }


}
