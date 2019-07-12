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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupHistoricalRecord;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.technical.optimization.taxon.TaxonGroup2TaxonHierarchy;
import net.sumaris.core.model.technical.optimization.taxon.TaxonGroupHierarchy;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TaxonGroupRepositoryImpl
        extends SumarisJpaRepositoryImpl<TaxonGroup, Integer>
        implements TaxonGroupRepositoryExtend {

    private static final Logger log =
            LoggerFactory.getLogger(TaxonGroupRepositoryImpl.class);

    @Autowired
    private TaxonNameDao taxonNameDao;

    public TaxonGroupRepositoryImpl(EntityManager entityManager) {
        super(TaxonGroup.class, entityManager);
    }

    @Override
    public TaxonGroupVO toTaxonGroupVO(TaxonGroup source) {
        TaxonGroupVO target = new TaxonGroupVO();

        Beans.copyProperties(source, target);

        return target;
    }

    @Override
    public long countTaxonGroupHierarchy() {
        return (Long)getEntityManager().createQuery("select count(*) from TaxonGroupHierarchy").getSingleResult();
    }

    @Override
    public void updateTaxonGroupHierarchies() {
        if (log.isInfoEnabled()) {
            log.info("Updating technical tables {TAXON_GROUP_HIERARCHY} and {TAXON_GROUP2TAXON_HIERARCHY}...");
        }
        updateTaxonGroupHierarchy();
        updateTaxonGroup2TaxonHierarchy();
    }

    @Override
    public void updateTaxonGroupHierarchy() {
        final EntityManager em = getEntityManager();

        // Get existing hierarchy
        final Multimap<Integer, Integer> existingLinkToRemove = ArrayListMultimap.create();
        CriteriaQuery<TaxonGroupHierarchy> query = em.getCriteriaBuilder().createQuery(TaxonGroupHierarchy.class);
        query.from(TaxonGroupHierarchy.class);
        em.createQuery(query).getResultStream()
                .forEach(th -> existingLinkToRemove.put(th.getParentTaxonGroup().getId(), th.getChildTaxonGroup().getId()));

        final MutableInt insertCounter = new MutableInt();

        // Get all taxon group
        findAll(Sort.by(TaxonGroup.PROPERTY_ID))
                .stream()
                .forEach(tg -> {
                    Integer childId = tg.getId();
                    // Link to himself
                    if (!existingLinkToRemove.remove(childId, childId)) {
                        TaxonGroupHierarchy tgh = new TaxonGroupHierarchy();
                        tgh.setParentTaxonGroup(load(TaxonGroup.class, childId));
                        tgh.setChildTaxonGroup(load(TaxonGroup.class, childId));
                        em.persist(tgh);
                        insertCounter.increment();
                    }

                    TaxonGroup parent = tg.getParentTaxonGroup();
                    while (parent != null) {
                        if (!existingLinkToRemove.remove(parent.getId(), childId)) {
                            TaxonGroupHierarchy tgh = new TaxonGroupHierarchy();
                            tgh.setParentTaxonGroup(load(TaxonGroup.class, parent.getId()));
                            tgh.setChildTaxonGroup(load(TaxonGroup.class, childId));
                            em.persist(tgh);
                            insertCounter.increment();
                        }
                        parent = parent.getParentTaxonGroup();
                    }
                });

        em.flush();
        em.clear();

        // Remove unused values
        final MutableInt deleteCounter = new MutableInt();
        if (!existingLinkToRemove.isEmpty()) {
            existingLinkToRemove.entries().forEach(entry -> {
                int deletedRowCount = em.createQuery("delete from TaxonGroupHierarchy where parentTaxonGroup.id=:parentId and childTaxonGroup.id=:childId")
                        .setParameter("parentId", entry.getKey())
                        .setParameter("childId", entry.getValue())
                        .executeUpdate();
                deleteCounter.add(deletedRowCount);
            });
        }

        log.info(String.format("Technical table TAXON_GROUP_HISTORY successfully updated. (inserts: %s, deletes: %s)",
                insertCounter.getValue(), deleteCounter.getValue()));
    }

    @Override
    public void updateTaxonGroup2TaxonHierarchy() {
        final EntityManager em = getEntityManager();

        // Get existing hierarchy
        final Multimap<Integer, Integer> existingLinkToRemove = ArrayListMultimap.create();
        {
            CriteriaQuery<TaxonGroup2TaxonHierarchy> query = em.getCriteriaBuilder().createQuery(TaxonGroup2TaxonHierarchy.class);
            query.from(TaxonGroup2TaxonHierarchy.class);
            em.createQuery(query).getResultStream()
                    .forEach(th -> existingLinkToRemove.put(th.getParentTaxonGroup().getId(), th.getChildReferenceTaxon().getId()));
        }

        // Get all taxon group
        final MutableInt insertCounter = new MutableInt();
        {
            CriteriaQuery<TaxonGroupHistoricalRecord> query = em.getCriteriaBuilder().createQuery(TaxonGroupHistoricalRecord.class);
            query.from(TaxonGroupHistoricalRecord.class);
            em.createQuery(query).getResultStream()
                    .forEach(history -> {
                        Integer parentId = history.getTaxonGroup().getId();
                        Integer childId = history.getReferenceTaxon().getId();
                        // Direct link
                        if (!existingLinkToRemove.remove(parentId, childId)) {
                            TaxonGroup2TaxonHierarchy hierarchy = new TaxonGroup2TaxonHierarchy();
                            hierarchy.setParentTaxonGroup(history.getTaxonGroup());
                            hierarchy.setChildReferenceTaxon(history.getReferenceTaxon());
                            hierarchy.setStartDate(history.getStartDate());
                            hierarchy.setEndDate(history.getEndDate());
                            hierarchy.setIsInherited(false);
                            em.persist(hierarchy);
                            insertCounter.increment();
                        }

                        TaxonNameVO parent = taxonNameDao.getTaxonNameReferent(childId);
                        List<TaxonName> children = taxonNameDao.getAllTaxonNameByParentIds(ImmutableList.of(parent.getId()));
                        while (CollectionUtils.isNotEmpty(children)) {
                            children.forEach(child -> {
                                Integer inheritedChildId = child.getReferenceTaxon().getId();
                                if (!existingLinkToRemove.remove(parentId, inheritedChildId)) {
                                    TaxonGroup2TaxonHierarchy hierarchy = new TaxonGroup2TaxonHierarchy();
                                    hierarchy.setParentTaxonGroup(history.getTaxonGroup());
                                    hierarchy.setChildReferenceTaxon(child.getReferenceTaxon());
                                    hierarchy.setStartDate(history.getStartDate());
                                    hierarchy.setEndDate(history.getEndDate());
                                    hierarchy.setIsInherited(true); // Mark has inherited
                                    em.persist(hierarchy);
                                    insertCounter.increment();
                                }
                            });
                            children = taxonNameDao.getAllTaxonNameByParentIds(
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
        if (!existingLinkToRemove.isEmpty()) {
            existingLinkToRemove.entries().forEach(entry -> {
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
}
