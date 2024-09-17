package net.sumaris.core.dao.data.vessel;

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

import net.sumaris.core.dao.referential.ISearchTextSpecifications;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.data.VesselOwner;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.vessel.VesselOwnerVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface VesselOwnerSpecifications extends ISearchTextSpecifications<Integer, VesselOwner> {


    String[] DEFAULT_SEARCH_ATTRIBUTES = new String[]{
            VesselOwner.Fields.EMAIL,
            VesselOwner.Fields.FIRST_NAME,
            VesselOwner.Fields.LAST_NAME
    };

    default Specification<VesselOwner> searchText(VesselFilterVO filter) {
        if (StringUtils.isBlank(filter.getSearchText())) return null;

        String[] searchAttributes = filter.getSearchAttributes();

        // No search attribute(s) define: use defaults
        if (ArrayUtils.isEmpty(searchAttributes)) {
            searchAttributes = DEFAULT_SEARCH_ATTRIBUTES;
        }

        return searchText(searchAttributes, filter.getSearchText(), true);
    }

    Specification<VesselOwner> toSpecification(VesselFilterVO filter);

    List<VesselOwnerVO> findAll(VesselFilterVO filter, Page page);

}
