package net.sumaris.core.dao.referential.taxon;

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
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.technical.optimization.taxon.TaxonGroup2TaxonHierarchy;
import net.sumaris.core.vo.referential.taxon.TaxonNameVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author peck7 on 31/07/2020.
 */
public interface TaxonNameSpecifications extends ReferentialSpecifications<Integer, TaxonName> {

    // TODO use BindableSpecification

    default Specification<TaxonName> withReferenceTaxonId(Integer referentTaxonId) {
        if (referentTaxonId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(TaxonName.Fields.REFERENCE_TAXON).get(ReferenceTaxon.Fields.ID), referentTaxonId);
    }

    default Specification<TaxonName> withSynonyms(Boolean withSynonyms) {
        if (Boolean.TRUE.equals(withSynonyms)) return null;
        return (root, query, cb) -> cb.equal(root.get(TaxonName.Fields.IS_REFERENT), Boolean.TRUE);
    }

    default Specification<TaxonName> withTaxonGroupId(Integer taxonGroupId) {
        if (taxonGroupId == null) return null;
        return (root, query, cb) -> cb.equal(
            root.join(TaxonName.Fields.REFERENCE_TAXON, JoinType.INNER)
                .joinList(ReferenceTaxon.Fields.PARENT_TAXON_GROUPS, JoinType.INNER)
                .get(TaxonGroup2TaxonHierarchy.Fields.PARENT_TAXON_GROUP)
                .get(TaxonGroup.Fields.ID),
            taxonGroupId
        );
    }

    default Specification<TaxonName> withTaxonGroupIds(Integer[] taxonGroupIds) {
        if (ArrayUtils.isEmpty(taxonGroupIds)) return null;
        return (root, query, cb) -> cb.in(
            root.join(TaxonName.Fields.REFERENCE_TAXON, JoinType.INNER)
                .joinList(ReferenceTaxon.Fields.PARENT_TAXON_GROUPS, JoinType.INNER)
                .get(TaxonGroup2TaxonHierarchy.Fields.PARENT_TAXON_GROUP)
                .get(TaxonGroup.Fields.ID)
        ).value(Arrays.asList(taxonGroupIds));
    }

    Optional<TaxonNameVO> findReferentByReferenceTaxonId(int referenceTaxonId);

    List<TaxonNameVO> findAllReferentByReferenceTaxonId(int referenceTaxonId);

    List<TaxonNameVO> getAllByTaxonGroupId(int taxonGroupId);

    Integer getReferenceTaxonIdById(int id);
}
