package net.sumaris.core.dao.referential.location;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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

import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationHierarchy;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.LocationVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author peck7 on 18/08/2020.
 */
public interface LocationSpecifications extends ReferentialSpecifications<Integer, Location> {

    String PARAMETER_ANCESTOR_IDS = "ancestorId";

    default Specification<Location> hasAncestors(Integer... ancestorIds) {
        if (ArrayUtils.isEmpty(ancestorIds)) return null;

        return BindableSpecification.where((root, query, builder) -> {

            Root<LocationHierarchy> lhRoot = query.from(LocationHierarchy.class);
            ParameterExpression<Collection> ancestorIdsParam = builder.parameter(Collection.class, PARAMETER_ANCESTOR_IDS);
            return builder.and(
                builder.equal(root, lhRoot.get(LocationHierarchy.Fields.CHILD_LOCATION)),
                builder.in(
                    Daos.composePath(lhRoot, StringUtils.doting(LocationHierarchy.Fields.PARENT_LOCATION, Location.Fields.ID)))
                    .value(ancestorIdsParam)
                );
        }).addBind(PARAMETER_ANCESTOR_IDS, Arrays.asList(ancestorIds));
    }

    boolean hasAssociation(int childLocationId, int parentLocationId);

    void addAssociation(int childLocationId, int parentLocationId, double childSurfaceRatio);

    /**
     * Update technical table LOCATION_HIERARCHY, from child/parent links found in LOCATION
     */
    void updateLocationHierarchy();
}
