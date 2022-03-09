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

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupHistoricalRecord;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeEnum;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.technical.optimization.taxon.TaxonGroup2TaxonHierarchy;
import net.sumaris.core.model.technical.optimization.taxon.TaxonGroupHierarchy;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.referential.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TaxonGroupRepositoryImpl
    extends ReferentialRepositoryImpl<TaxonGroup, TaxonGroupVO, IReferentialFilter, ReferentialFetchOptions>
    implements TaxonGroupSpecifications {

    @Autowired
    private TaxonNameRepository taxonNameRepository;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PmfmRepository pmfmRepository;

    public TaxonGroupRepositoryImpl(EntityManager entityManager) {
        super(TaxonGroup.class, TaxonGroupVO.class, entityManager);
    }

    @Override
    public List<TaxonGroupVO> findTargetSpeciesByFilter(
            IReferentialFilter filter,
            int offset,
            int size,
            String sortAttribute,
            SortDirection sortDirection) {

        Preconditions.checkNotNull(filter);

        TypedQuery<TaxonGroup> query = getQuery(toSpecification(filter), TaxonGroup.class,
            Pageables.create(offset, size, sortAttribute, sortDirection));

        return query.getResultStream()
            .map(this::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public long countTaxonGroupHierarchy() {
        return (Long) getEntityManager().createQuery("select count(*) from TaxonGroupHierarchy").getSingleResult();
    }

    @Override
    public void updateTaxonGroupHierarchies() {
        log.info("Updating technical tables {TAXON_GROUP_HIERARCHY} and {TAXON_GROUP2TAXON_HIERARCHY}...");

        updateTaxonGroupHierarchy();
        updateTaxonGroup2TaxonHierarchy();
    }

    @Override
    public void updateTaxonGroupHierarchy() {
        final EntityManager em = getEntityManager();

        // Get existing hierarchy
        final Multimap<Integer, Integer> existingLinksToRemove = ArrayListMultimap.create();
        final Multimap<Integer, Integer> newLinks = ArrayListMultimap.create();
        CriteriaQuery<TaxonGroupHierarchy> query = em.getCriteriaBuilder().createQuery(TaxonGroupHierarchy.class);
        query.from(TaxonGroupHierarchy.class);
        em.createQuery(query).getResultStream()
            .forEach(th -> existingLinksToRemove.put(th.getParentTaxonGroup().getId(), th.getChildTaxonGroup().getId()));

        final MutableInt insertCounter = new MutableInt();

        // Get all taxon group
        findAll(Sort.by(TaxonGroup.Fields.ID))
            .forEach(tg -> {
                Integer childId = tg.getId();
                // Link to himself
                if (!existingLinksToRemove.remove(childId, childId)
                        && !newLinks.containsEntry(childId, childId)) {
                    TaxonGroupHierarchy tgh = new TaxonGroupHierarchy();
                    tgh.setParentTaxonGroup(getReference(TaxonGroup.class, childId));
                    tgh.setChildTaxonGroup(getReference(TaxonGroup.class, childId));
                    em.persist(tgh);
                    insertCounter.increment();
                    newLinks.put(childId, childId);
                    if (log.isDebugEnabled()) log.debug(String.format("Adding TAXON_GROUP_HISTORY: TaxonGroup#%s -> himself",
                            childId,
                            childId));
                }

                TaxonGroup parent = tg.getParentTaxonGroup();
                while (parent != null) {
                    Integer parentId = parent.getId();
                    if (!existingLinksToRemove.remove(parentId, childId)
                            && !newLinks.containsEntry(parentId, childId)) {
                        TaxonGroupHierarchy tgh = new TaxonGroupHierarchy();
                        tgh.setParentTaxonGroup(getReference(TaxonGroup.class, parentId));
                        tgh.setChildTaxonGroup(getReference(TaxonGroup.class, childId));
                        em.persist(tgh);
                        insertCounter.increment();
                        newLinks.put(parentId, childId);
                        if (log.isDebugEnabled()) log.debug(String.format("Adding TAXON_GROUP_HISTORY: Parent(TaxonGroup#%s) -> Child(TaxonGroup#%s)",
                                parentId,
                                childId));
                    }
                    parent = parent.getParentTaxonGroup();
                }
            });

        em.flush();
        em.clear();

        // Remove unused values
        final MutableInt deleteCounter = new MutableInt();
        if (!existingLinksToRemove.isEmpty()) {
            existingLinksToRemove.entries().forEach(entry -> {
                int deletedRowCount = em.createQuery("delete from TaxonGroupHierarchy where parentTaxonGroup.id=:parentId and childTaxonGroup.id=:childId")
                    .setParameter("parentId", entry.getKey())
                    .setParameter("childId", entry.getValue())
                    .executeUpdate();
                deleteCounter.add(deletedRowCount);
            });
        }

        log.info("Technical table TAXON_GROUP_HISTORY successfully updated. (inserts: {}, deletes: {})",
            insertCounter.getValue(), deleteCounter.getValue());
    }

    @Override
    public void updateTaxonGroup2TaxonHierarchy() {
        final EntityManager em = getEntityManager();

        // Get existing hierarchy
        final Multimap<Integer, Integer> existingLinksToRemove = ArrayListMultimap.create();
        final Multimap<Integer, Integer> newLinks = ArrayListMultimap.create();
        {
            CriteriaQuery<TaxonGroup2TaxonHierarchy> query = em.getCriteriaBuilder().createQuery(TaxonGroup2TaxonHierarchy.class);
            query.from(TaxonGroup2TaxonHierarchy.class);
            em.createQuery(query).getResultStream()
                .forEach(th -> existingLinksToRemove.put(th.getParentTaxonGroup().getId(), th.getChildReferenceTaxon().getId()));
        }

        // Get all taxon group
        final MutableInt insertCounter = new MutableInt();
        {
            CriteriaQuery<TaxonGroupHistoricalRecord> query = em.getCriteriaBuilder().createQuery(TaxonGroupHistoricalRecord.class);
            query.from(TaxonGroupHistoricalRecord.class);
            em.createQuery(query).getResultStream()
                .forEach(history -> {
                    Integer parentId = history.getTaxonGroup().getId();
                    Integer directChildId = history.getReferenceTaxon().getId();
                    // Direct link
                    if (!existingLinksToRemove.remove(parentId, directChildId)
                            && !newLinks.containsEntry(parentId, directChildId)) {
                        TaxonGroup2TaxonHierarchy hierarchy = new TaxonGroup2TaxonHierarchy();
                        hierarchy.setParentTaxonGroup(history.getTaxonGroup());
                        hierarchy.setChildReferenceTaxon(history.getReferenceTaxon());
                        hierarchy.setStartDate(history.getStartDate());
                        hierarchy.setEndDate(history.getEndDate());
                        hierarchy.setIsInherited(false);
                        em.persist(hierarchy);
                        insertCounter.increment();
                        newLinks.put(parentId, directChildId);
                        if (log.isDebugEnabled()) log.debug(String.format("Adding TAXON_GROUP2TAXON_HISTORY: Parent(TaxonGroup#%s) -> Child(ReferenceTaxon#%s) at %s",
                                parentId,
                                directChildId,
                                DateFormat.getDateInstance().format(history.getStartDate())));
                    }

                    TaxonNameVO parent = taxonNameRepository.findReferentByReferenceTaxonId(directChildId)
                            .orElseThrow(() -> new SumarisTechnicalException("Cannot find taxon name for referenceTaxonId=" + directChildId));
                    List<TaxonName> children = taxonNameRepository.getAllTaxonNameByParentIdInAndIsReferentTrue(ImmutableList.of(parent.getId()));
                    while (CollectionUtils.isNotEmpty(children)) {
                        children.forEach(child -> {
                            // Inherited link
                            Integer inheritedChildId = child.getReferenceTaxon().getId();
                            if (!existingLinksToRemove.remove(parentId, inheritedChildId)
                                    && !newLinks.containsEntry(parentId, inheritedChildId)) {
                                TaxonGroup2TaxonHierarchy hierarchy = new TaxonGroup2TaxonHierarchy();
                                hierarchy.setParentTaxonGroup(history.getTaxonGroup());
                                hierarchy.setChildReferenceTaxon(child.getReferenceTaxon());
                                hierarchy.setStartDate(history.getStartDate());
                                hierarchy.setEndDate(history.getEndDate());
                                hierarchy.setIsInherited(true); // Mark has inherited
                                em.persist(hierarchy);
                                insertCounter.increment();
                                newLinks.put(parentId, inheritedChildId);
                                if (log.isDebugEnabled()) log.debug(String.format("Adding TAXON_GROUP2TAXON_HISTORY (inherited): Parent(TaxonGroup#%s) -> Child(ReferenceTaxon#%s) at %s",
                                        parentId,
                                        inheritedChildId,
                                        DateFormat.getDateInstance().format(history.getStartDate())));
                            }
                        });
                        children = taxonNameRepository.getAllTaxonNameByParentIdInAndIsReferentTrue(
                            children.stream()
                                .map(TaxonName::getId)
                                .collect(Collectors.toList()));
                    }
                });
        }

        em.flush();
        em.clear();

        // Remove unused values
        final MutableInt deleteCounter = new MutableInt();
        if (!existingLinksToRemove.isEmpty()) {
            existingLinksToRemove.entries().forEach(entry -> {
                int nbRow = em.createQuery("delete from TaxonGroup2TaxonHierarchy where parentTaxonGroup.id=:parentId and childReferenceTaxon.id=:childId")
                    .setParameter("parentId", entry.getKey())
                    .setParameter("childId", entry.getValue())
                    .executeUpdate();
                deleteCounter.add(nbRow);
            });
        }

        log.info(String.format("Technical table TAXON_GROUP2TAXON_HISTORY successfully updated. (inserts: %s, deletes: %s)",
            insertCounter.getValue(), deleteCounter.getValue()));
    }

    @Override
    public List<ReferentialVO> getAllDressingByTaxonGroupId(int taxonGroupId, Date startDate, Date endDate, int locationId) {
        Preconditions.checkNotNull(startDate);

        List<ReferentialVO> result = getEntityManager()
            .createNamedQuery("RoundWeightConversion.dressingByTaxonGroupId", QualitativeValue.class)
            .setParameter("taxonGroupId", taxonGroupId)
            .setParameter("startDate", startDate, TemporalType.DATE)
            // If no end date, use start date
            .setParameter("endDate", endDate != null ? endDate : startDate, TemporalType.DATE)
            .setParameter("locationId", locationId)
            .getResultStream()
            .map(p -> referentialDao.toVO(p))
            .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(result)) {
            return result;
        }

        // Nothing found for this taxon group, so return all dressings
        PmfmVO pmfm = pmfmRepository.get(PmfmEnum.DRESSING.getId());
        if (pmfm == null) {
            throw new DataRetrievalFailureException("PMFM for dressing not found");
        }
        return pmfm.getQualitativeValues();

    }

    @Override
    public List<ReferentialVO> getAllPreservingByTaxonGroupId(int taxonGroupId, Date startDate, Date endDate, int locationId) {
        Preconditions.checkNotNull(startDate);

        List<ReferentialVO> result = getEntityManager()
            .createNamedQuery("RoundWeightConversion.preservingByTaxonGroupId", QualitativeValue.class)
            .setParameter("taxonGroupId", taxonGroupId)
            .setParameter("startDate", startDate, TemporalType.DATE)
            // If no end date, use start date
            .setParameter("endDate", endDate != null ? endDate : startDate, TemporalType.DATE)
            .setParameter("locationId", locationId)
            .getResultStream()
            .map(p -> referentialDao.toVO(p))
            .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(result)) {
            return result;
        }

        // Nothing found for this taxon group, so return all dressings
        PmfmVO pmfm = pmfmRepository.get(PmfmEnum.PRESERVATION.getId());
        if (pmfm == null) {
            throw new DataRetrievalFailureException("PMFM for preservation not found");
        }
        return pmfm.getQualitativeValues();
    }

    @Override
    public List<Integer> getAllIdByReferenceTaxonId(int referenceTaxonId, Date startDate, @Nullable Date endDate) {
        Preconditions.checkNotNull(startDate);

        return getEntityManager()
            .createNamedQuery("TaxonGroup2TaxonHierarchy.taxonGroupIdByReferenceTaxonId", Integer.class)
            .setParameter("referenceTaxonId", referenceTaxonId)
            .setParameter("startDate", startDate, TemporalType.DATE)
            // If no end date, use start date
            .setParameter("endDate", endDate != null ? endDate : startDate, TemporalType.DATE)
            .getResultList();
    }

    @Override
    protected Specification<TaxonGroup> toSpecification(IReferentialFilter filter, ReferentialFetchOptions fetchOptions) {
        Preconditions.checkNotNull(filter);
        Integer[] gearIds = filter.getLevelIds();
        filter.setLevelIds(null);

        Specification<TaxonGroup> result = super.toSpecification(filter, fetchOptions)
            .and(hasType(TaxonGroupTypeEnum.METIER_SPECIES.getId()))
            .and(inGearIds(gearIds));

        // restore levelIds
        filter.setLevelIds(gearIds);

        return result;
    }
}
