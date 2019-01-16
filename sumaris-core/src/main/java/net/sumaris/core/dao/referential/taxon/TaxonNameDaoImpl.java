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
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.core.model.referential.taxon.TaxonomicLevelId;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.stream.Collectors;

@Repository("taxonNameDao")
public class TaxonNameDaoImpl extends HibernateDaoSupport implements TaxonNameDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(TaxonNameDaoImpl.class);

    @Override
    public List<TaxonNameVO> getAll(boolean withSynonyms) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<TaxonName> query = builder.createQuery(TaxonName.class);
        Root<TaxonName> root = query.from(TaxonName.class);

        ParameterExpression<Boolean> withSynonymParam = builder.parameter(Boolean.class);

        query.select(root)
                .where(builder.and(
                        // Filter on taxonomic level (species+ subspecies)
                        builder.in(root.get(TaxonName.PROPERTY_TAXONOMIC_LEVEL).get(TaxonomicLevel.PROPERTY_ID))
                               .value(ImmutableList.of(TaxonomicLevelId.SPECIES.getId(), TaxonomicLevelId.SUBSPECIES.getId())),
                        // Filter on is_referent
                        builder.or(
                                builder.isNull(withSynonymParam),
                                builder.equal(root.get(TaxonName.PROPERTY_IS_REFERENT), Boolean.TRUE)
                        )
                ));

        TypedQuery<TaxonName> q = getEntityManager().createQuery(query)
                .setParameter(withSynonymParam, Boolean.valueOf(withSynonyms));
        return toTaxonNameVOs(q.getResultList());
    }

    @Override
    public TaxonNameVO getTaxonNameReferent(Integer referenceTaxonId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<TaxonName> query = builder.createQuery(TaxonName.class);
        Root<TaxonName> root = query.from(TaxonName.class);

        ParameterExpression<Integer> idParam = builder.parameter(Integer.class);

        query.select(root)
                .where(builder.equal(root.get(TaxonName.PROPERTY_REFERENCE_TAXON).get(ReferenceTaxon.PROPERTY_ID), idParam));

        TypedQuery<TaxonName> q = getEntityManager().createQuery(query)
                .setParameter(idParam, referenceTaxonId);
        return toTaxonNameVO(q.getSingleResult());
    }

    /* -- protected methods -- */

    protected List<TaxonNameVO> toTaxonNameVOs(List<TaxonName> source) {
        return source.stream().map(this::toTaxonNameVO).collect(Collectors.toList());
    }

    protected TaxonNameVO toTaxonNameVO(TaxonName source) {
        if (source == null) return null;

        TaxonNameVO target = new TaxonNameVO();

        Beans.copyProperties(source, target);

        // Reference taxon
        target.setReferenceTaxonId(source.getReferenceTaxon().getId());
        return target;
    }
}
