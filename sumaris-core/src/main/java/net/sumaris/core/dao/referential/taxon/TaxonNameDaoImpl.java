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
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.taxon.*;
import net.sumaris.core.model.technical.optimization.taxon.TaxonGroup2TaxonHierarchy;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Repository("taxonNameDao")
public class TaxonNameDaoImpl extends HibernateDaoSupport implements TaxonNameDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(TaxonNameDaoImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    @Override
    public List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, int offset, int size,
                                          String sortAttribute, SortDirection sortDirection) {
        EntityManager em = getEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();

        // With synonym
        boolean withSynonyms = filter.getWithSynonyms() != null ? filter.getWithSynonyms() : false;

        // Taxon group
        ParameterExpression<Collection> taxonGroupIdsParam = builder.parameter(Collection.class);

        // Where clause visitor
        ReferentialDao.QueryVisitor<TaxonName> queryVisitor = (query, root) -> {

            Expression<Boolean> whereClause = null;
            if (!withSynonyms) {
                whereClause = builder.equal(root.get(TaxonName.Fields.IS_REFERENT), Boolean.TRUE);
            }

            Expression<Boolean> taxonGroupClause = null;
            if (ArrayUtils.isNotEmpty(filter.getTaxonGroupIds())) {
                Join<TaxonName, ReferenceTaxon> rt = root.join(TaxonName.Fields.REFERENCE_TAXON, JoinType.INNER);
                ListJoin<ReferenceTaxon, TaxonGroup2TaxonHierarchy> tgh = rt.joinList(ReferenceTaxon.Fields.PARENT_TAXON_GROUPS, JoinType.INNER);
                taxonGroupClause = builder.in(tgh.get(TaxonGroup2TaxonHierarchy.Fields.PARENT_TAXON_GROUP).get(TaxonGroup.Fields.ID))
                        .value(taxonGroupIdsParam);
                whereClause = whereClause == null ?  taxonGroupClause : builder.and(whereClause, taxonGroupClause);
            }

            return whereClause;
        };

        TypedQuery<TaxonName> typedQuery = referentialDao.createFindQuery(TaxonName.class,
                null,
                filter.getTaxonomicLevelIds(),
                StringUtils.trimToNull(filter.getSearchText()),
                StringUtils.trimToNull(filter.getSearchAttribute()),
                filter.getStatusIds(),
                sortAttribute,
                sortDirection,
                queryVisitor);

        if (ArrayUtils.isNotEmpty(filter.getTaxonGroupIds())) {
            typedQuery.setParameter(taxonGroupIdsParam,  ImmutableList.copyOf(filter.getTaxonGroupIds()));
        }

         return typedQuery.getResultStream()
                .map(this::toTaxonNameVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaxonNameVO> getAll(boolean withSynonyms) {
        EntityManager em = getEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<TaxonName> query = builder.createQuery(TaxonName.class);
        Root<TaxonName> root = query.from(TaxonName.class);

        ParameterExpression<Boolean> withSynonymParam = builder.parameter(Boolean.class);

        query.select(root)
                .where(builder.and(
                        // Filter on taxonomic level (species+ subspecies)
                        builder.in(root.get(TaxonName.Fields.TAXONOMIC_LEVEL).get(TaxonomicLevel.Fields.ID))
                               .value(ImmutableList.of(TaxonomicLevelId.SPECIES.getId(), TaxonomicLevelId.SUBSPECIES.getId())),
                        // Filter on is_referent
                        builder.or(
                                builder.isNull(withSynonymParam),
                                builder.equal(root.get(TaxonName.Fields.IS_REFERENT), Boolean.TRUE)
                        )
                ));

        return em.createQuery(query)
                .setParameter(withSynonymParam, withSynonyms ? null : false)
                .getResultStream()
                .map(this::toTaxonNameVO)
                .collect(Collectors.toList());
    }

    @Override
    public TaxonNameVO getTaxonNameReferent(Integer referenceTaxonId) {
        EntityManager em = getEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<TaxonName> query = builder.createQuery(TaxonName.class);
        Root<TaxonName> root = query.from(TaxonName.class);

        ParameterExpression<Integer> idParam = builder.parameter(Integer.class);

        query.select(root)
                .where(builder.equal(root.get(TaxonName.Fields.REFERENCE_TAXON).get(ReferenceTaxon.Fields.ID), idParam));

        TypedQuery<TaxonName> q = em.createQuery(query)
                .setParameter(idParam, referenceTaxonId);
        List<TaxonName> referenceTaxons = q.getResultList();
        if (CollectionUtils.isEmpty(referenceTaxons)) return null;
        if (referenceTaxons.size() > 1)  {
            log.warn(String.format("ReferenceTaxon {id=%} has more than one TaxonNames, with IS_REFERENT=1. Will use the first found.", referenceTaxonId));
        }

        return toTaxonNameVO(referenceTaxons.get(0));
    }

    @Override
    public List<TaxonName> getAllTaxonNameByParentIds(Collection<Integer> taxonNameParentIds) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<TaxonName> query = builder.createQuery(TaxonName.class);
        Root<TaxonName> root = query.from(TaxonName.class);

        ParameterExpression<Collection> parentIdsParam = builder.parameter(Collection.class);

        query.where(builder.in(root.get(TaxonName.Fields.PARENT_TAXON_NAME).get(TaxonName.Fields.ID)).value(parentIdsParam));

        return getEntityManager().createQuery(query)
                .setParameter(parentIdsParam, taxonNameParentIds)
                .getResultList();
    }

    @Override
    public List<TaxonNameVO> getAllByTaxonGroupId(Integer taxonGroupId) {
        EntityManager em = getEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<TaxonName> query = builder.createQuery(TaxonName.class);
        Root<TaxonGroup2TaxonHierarchy> root = query.from(TaxonGroup2TaxonHierarchy.class);

        ParameterExpression<Integer> taxonGroupIdParam = builder.parameter(Integer.class);

        Join<TaxonGroup2TaxonHierarchy, ReferenceTaxon> rt = root.join(TaxonGroup2TaxonHierarchy.Fields.CHILD_REFERENCE_TAXON, JoinType.INNER);
        Join<ReferenceTaxon, TaxonName> tn = rt.joinList(ReferenceTaxon.Fields.TAXON_NAMES, JoinType.INNER);

        query.select(tn)
                .where(builder.and(
                        // Filter on taxon_group
                        builder.equal(root.get(TaxonGroup2TaxonHierarchy.Fields.PARENT_TAXON_GROUP).get(TaxonGroup.Fields.ID), taxonGroupIdParam),
                        // Filter on taxonomic level (species and subspecies)
                        builder.in(tn.get(TaxonName.Fields.TAXONOMIC_LEVEL).get(TaxonomicLevel.Fields.ID))
                                .value(ImmutableList.of(TaxonomicLevelId.SPECIES.getId(), TaxonomicLevelId.SUBSPECIES.getId())),
                        // Filter on is_referent
                        builder.equal(tn.get(TaxonName.Fields.IS_REFERENT), Boolean.TRUE)
                ));

        return em.createQuery(query)
                .setParameter(taxonGroupIdParam, taxonGroupId)
                .getResultStream()
                .map(this::toTaxonNameVO)
                .collect(Collectors.toList());
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
