package net.sumaris.core.dao.data;

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
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonNameDao;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.Batch;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.BatchVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("batchDao")
public class BatchDaoImpl extends BaseDataDaoImpl implements BatchDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(BatchDaoImpl.class);

    private boolean debug;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonNameDao taxonNameDao;

    public BatchDaoImpl() {
        super();
        debug = log.isDebugEnabled();
    }

    @Override
    public List<BatchVO> getAllByOperationId(int operationId) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<Batch> root = query.from(Batch.class);

        query.select(root);

        ParameterExpression<Integer> tripIdParam = cb.parameter(Integer.class);

        query.where(cb.equal(root.get(Batch.Fields.OPERATION).get(Batch.Fields.ID), tripIdParam));

        // Sort by rank order
        query.orderBy(cb.asc(root.get(PmfmStrategy.Fields.RANK_ORDER)));

        return toBatchVOs(getEntityManager().createQuery(query)
                .setParameter(tripIdParam, operationId).getResultList(), false);
    }

    @Override
    public BatchVO get(int id) {
        Batch entity = get(Batch.class, id);
        return toBatchVO(entity, false);
    }

    @Override
    public List<BatchVO> saveByOperationId(int operationId, List<BatchVO> sources) {

        // Load parent entity
        Operation parent = get(Operation.class, operationId);
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();

        // Remember existing entities

        final Multimap<Integer, Batch> sourcesByHashCode = Beans.splitByNotUniqueProperty(Beans.getList(parent.getBatches()), Batch.Fields.HASH);
        final Multimap<String, Batch> sourcesByLabelMap = Beans.splitByNotUniqueProperty(Beans.getList(parent.getBatches()), Batch.Fields.LABEL);
        final Map<Integer, Batch> sourcesIdsToRemove = Beans.splitById(Beans.getList(parent.getBatches()));

        // Save each batches
        sources.forEach(source -> {
            source.setOperationId(operationId);

            Batch existingBatch = null;
            if (source.getId() != null) {
                existingBatch = sourcesIdsToRemove.remove(source.getId());
            }
            // Id not exists: try by hash or label
            if (source.getId() != null && existingBatch == null) {
                // Try to get iit by hash code
                Collection<Batch> existingBatchs = sourcesByHashCode.get(source.hashCode());
                // Not found by hash code: try by label
                if (CollectionUtils.isEmpty(existingBatchs)) {
                    existingBatchs = sourcesByLabelMap.get(source.getLabel());
                }
                // If one on match => use it
                if (CollectionUtils.size(existingBatchs) == 1) {
                    existingBatch = existingBatchs.iterator().next();
                    sourcesIdsToRemove.remove(existingBatch.getId());
                    source.setId(existingBatch.getId());
                }
            }

            // Save the batch, in an optimized way
            optimizedSave(source, existingBatch, false, newUpdateDate);
        });

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.values().forEach(this::delete);
        }

        // Remove parent (use only parentId)
        sources.forEach(batch -> {
            if (batch.getParent() != null) {
                batch.setParentId(batch.getParent().getId());
                batch.setParent(null);
            }
        });

        entityManager.flush();
        entityManager.clear();

        return sources;
    }


    @Override
    public BatchVO save(BatchVO source) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        Batch entity = null;
        if (source.getId() != null) {
            entity = get(Batch.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Batch();
        }

        if (!isNew) {
            // Check update date
            // FIXME: Cliant app: update entity from the save() result
            //checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        // Copy some fields from the trip
        copySomeFieldsFromOperation(source);

        // VO -> Entity
        batchVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            entityManager.merge(entity);
        }

        // Update date
        source.setUpdateDate(newUpdateDate);

        entityManager.flush();
        entityManager.clear();

        return source;
    }

    @Override
    public void delete(int id) {

        log.debug(String.format("Deleting batch {id=%s}...", id));
        delete(Batch.class, id);
    }

    @Override
    public BatchVO toBatchVO(Batch source) {
        return toBatchVO(source, true);
    }

    @Override
    public List<BatchVO> toFlatList(final BatchVO source) {
        List<BatchVO> result = Lists.newArrayList();
        fillListFromTree(result, source);
        return result;
    }

    /* -- protected methods -- */

    /**
     * Save the batch, when saving a full tree (algorithm optimized for this case)
     * /!\ DO NOT USE when updating only one batch, in a existing tree !
     *
     * @param source
     * @param entity
     * @param checkUpdateDate
     * @param newUpdateDate
     * @param flush
     * @return
     */
    protected BatchVO optimizedSave(BatchVO source,
                             Batch entity,
                             boolean checkUpdateDate,
                             Timestamp newUpdateDate) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        if (entity == null && source.getId() != null) {
            entity = get(Batch.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Batch();
        }

        if (!isNew && checkUpdateDate) {
            // Check update date
            // FIXME: Client app: update entity from the save() result
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        // Copy some fields from the trip
        copySomeFieldsFromOperation(source);

        // VO -> Entity
        Integer previousHash = entity.getHash();
        batchVOToEntity(source, entity, true);

        // Update update_dt
        entity.setUpdateDate(newUpdateDate);
        source.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            boolean needUpdate = !Objects.equals(previousHash, entity.getHash());

            // Check when (hash code has changed)
            if (needUpdate) {
                if (debug) logger.debug(String.format("Bach {%s} updated (hash changed)", entity.getLabel()));
                entityManager.merge(entity);
            } else {
                if (debug) logger.debug(String.format("Bach {%s} is unchanged. TODO: avoid to call entityManager.merge() ?", entity.getLabel()));
                // TODO: remove next call to merge() after MANY tests + Unit tests
                entityManager.merge(entity);
            }
        }

        return source;
    }

    protected BatchVO toBatchVO(Batch source, boolean allFields) {

        if (source == null) return null;

        BatchVO target = new BatchVO();

        Beans.copyProperties(source, target);

        // Taxon group
        if (source.getTaxonGroup() != null) {
            ReferentialVO taxonGroup = referentialDao.toReferentialVO(source.getTaxonGroup());
            target.setTaxonGroup(taxonGroup);
        }

        // Taxon name (from reference)
        if (source.getReferenceTaxon() != null) {
            TaxonNameVO taxonName = taxonNameDao.getTaxonNameReferent(source.getReferenceTaxon().getId());
            target.setTaxonName(taxonName);
        }

        // Parent batch
        if (source.getParent() != null) {
            target.setParentId(source.getParent().getId());
        }

        // Operation
        if (source.getOperation() != null) {
            target.setOperationId(source.getOperation().getId());
        }

        // If full export
        if (allFields) {
            // Recorder department
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
            target.setRecorderDepartment(recorderDepartment);
        }

        return target;
    }

    protected void copySomeFieldsFromOperation(BatchVO target) {
        OperationVO source = target.getOperation();
        if (source == null) return;

        target.setRecorderDepartment(source.getRecorderDepartment());
    }

    protected List<BatchVO> toBatchVOs(List<Batch> source, boolean allFields) {
        return this.toBatchVOs(source.stream(), allFields);
    }

    protected List<BatchVO> toBatchVOs(Stream<Batch> source, boolean allFields) {
        return source.map(s -> this.toBatchVO(s, allFields))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void batchVOToEntity(BatchVO source, Batch target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Taxon group
        if (copyIfNull || source.getTaxonGroup() != null) {
            if (source.getTaxonGroup() == null || source.getTaxonGroup().getId() == null) {
                target.setTaxonGroup(null);
            }
            else {
                target.setTaxonGroup(load(TaxonGroup.class, source.getTaxonGroup().getId()));
            }
        }

        // Reference taxon (from taxon name)
        if (copyIfNull || source.getTaxonName() != null) {
            if (source.getTaxonName() == null || source.getTaxonName().getId() == null) {
                target.setReferenceTaxon(null);
            }
            else {
                if (source.getTaxonName().getReferenceTaxonId() != null) {
                    target.setReferenceTaxon(load(ReferenceTaxon.class, source.getTaxonName().getReferenceTaxonId()));
                }
                else {
                    // Get the taxon name, then set reference taxon
                    TaxonName taxonname = get(TaxonName.class, source.getTaxonName().getId());
                    if (taxonname != null) {
                        target.setReferenceTaxon(taxonname.getReferenceTaxon());
                    }
                    else {
                        throw new DataIntegrityViolationException("Invalid batch: unknown taxon name with id "+ source.getTaxonName().getId());
                    }
                }
            }
        }

        Integer parentId = (source.getParent() != null ? source.getParent().getId() : source.getParentId());
        Integer opeId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);

        // Parent batch
        if (copyIfNull || (parentId != null)) {
            if (parentId == null) {
                target.setParent(null);
            }
            else {
                // Detect the previous parent. If changed, remove the batch from tha parent children list - fix #15
                Batch oldParent = target.getParent();
                if (oldParent != null && parentId != oldParent.getId() && CollectionUtils.isNotEmpty(oldParent.getChildren())) {
                    oldParent.getChildren().remove(target);
                }

                // Set parent
                Batch parent = load(Batch.class, parentId);
                target.setParent(parent);

                // This is need in an optimized save
                //if (!parent.getChildren().contains(target)) {
                //    parent.getChildren().add(target);
                //}

                // Force same operation as parent (e.g. in case of bad batch tree copy)
                opeId = parent.getOperation().getId();
            }
        }

        // Operation
        if (copyIfNull || (opeId != null)) {
            if (opeId == null) {
                target.setOperation(null);
            }
            else {
                target.setOperation(load(Operation.class, opeId));
            }
        }

        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            }
            else {
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
            }
            else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        // Store hash code
        target.setHash(source.hashCode());
    }

    protected void fillListFromTree(final List<BatchVO> result, final BatchVO source) {

        result.add(source);

        if (CollectionUtils.isNotEmpty(source.getChildren())) {
            source.getChildren().forEach(child -> {
                child.setParentId(source.getId());
                fillListFromTree(result, child);
            });
        }

        // Not need anymore
        source.setParent(null);
        source.setChildren(null);
    }
}
