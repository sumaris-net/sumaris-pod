package net.sumaris.core.vo.data.aggregatedLanding;

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

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.model.data.IWithVesselSnapshotEntity;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.*;

@Data
@FieldNameConstants
public class AggregatedLandingVO implements IValueObject<Integer>,
    IWithVesselSnapshotEntity<Integer, VesselSnapshotVO> {

    private VesselSnapshotVO vesselSnapshot;

    private List<VesselActivityVO> vesselActivities;

    public AggregatedLandingVO() {
        vesselActivities = new ArrayList<>();
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public Date getVesselDateTime() {
        return null;
    }

    @Override
    public Integer getId() {
        return vesselSnapshot.getId();
    }

    @Override
    public void setId(Integer integer) {

    }
}
