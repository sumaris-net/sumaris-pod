package net.sumaris.core.dao.referential.taxon;

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

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.criteria.JoinType;
import java.util.Date;
import java.util.List;

@NoRepositoryBean
public interface TaxonGroupSpecifications
    extends ReferentialSpecifications<TaxonGroup> {

    default Specification<TaxonGroup> hasType(Integer taxonGroupTypeId) {
        if (taxonGroupTypeId == null) return null;
        return (root, query, cb) -> cb.equal(
                root.get(TaxonGroup.Fields.TAXON_GROUP_TYPE).get(Status.Fields.ID),
                taxonGroupTypeId);
    }

    default Specification<TaxonGroup> inGearIds(Integer[] gearIds) {
        if (ArrayUtils.isEmpty(gearIds)) return null;
        return (root, query, cb) -> cb.in(
                root.joinList(TaxonGroup.Fields.METIERS, JoinType.INNER)
                        .join(Metier.Fields.GEAR, JoinType.INNER)
                        .get(Gear.Fields.ID))
                .value(ImmutableList.copyOf(gearIds));
    }

    void updateTaxonGroupHierarchies();

    void updateTaxonGroupHierarchy();

    void updateTaxonGroup2TaxonHierarchy();

    long countTaxonGroupHierarchy();

    List<TaxonGroupVO> findTargetSpeciesByFilter(
            IReferentialFilter filter,
            int offset,
            int size,
            String sortAttribute,
            SortDirection sortDirection);

    List<ReferentialVO> getAllDressingByTaxonGroupId(int taxonGroupId, Date startDate, Date endDate, int locationId);

    List<ReferentialVO> getAllPreservingByTaxonGroupId(int taxonGroupId, Date startDate, Date endDate, int locationId);

    List<Integer> getAllIdByReferenceTaxonId(int referenceTaxonId, Date startDate, Date endDate);
}
