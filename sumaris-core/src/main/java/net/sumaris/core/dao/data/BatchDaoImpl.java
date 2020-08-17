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
import com.google.common.collect.Sets;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.Batch;
import net.sumaris.core.model.data.IWithBatchesEntity;
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

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("batchDao")
public class BatchDaoImpl extends BaseDataDaoImpl implements BatchDao {

    /**
     * Logger.
     */
    protected static final Logger logger = LoggerFactory.getLogger(BatchDaoImpl.class);
    private static final boolean trace = logger.isTraceEnabled();

    private boolean enableSaveUsingHash;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonNameRepository taxonNameRepository;

    @Autowired
    private ProductRepository productRepository;

    @PostConstruct
    protected void init() {
        this.enableSaveUsingHash = config.enableBatchHashOptimization();
    }

    @Override
    public List<BatchVO> getAllByOperationId(int operationId) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<Batch> root = query.from(Batch.class);

        query.select(root);

        ParameterExpression<Integer> operationIdParam = cb.parameter(Integer.class);

        query.where(cb.equal(root.get(Batch.Fields.OPERATION).get(Batch.Fields.ID), operationIdParam));

        // Sort by rank order
        query.orderBy(cb.asc(root.get(PmfmStrategy.Fields.RANK_ORDER)));

        return toBatchVOs(getEntityManager().createQuery(query)
            .setParameter(operationIdParam, operationId).getResultStream(), false);
    }

    @Override
    public BatchVO getRootByOperationId(int operationId, boolean withChildren) {

        if (withChildren) {
            // Return all batches as tree form
            return toTree(getAllByOperationId(operationId));
        }

        // create a query returning only root batch
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Batch> query = cb.createQuery(Batch.class);
        Root<Batch> root = query.from(Batch.class);

        query.select(root);

        ParameterExpression<Integer> operationIdParam = cb.parameter(Integer.class);

        query.where(
            cb.equal(root.get(Batch.Fields.OPERATION).get(Batch.Fields.ID), operationIdParam),
            cb.isNull(root.get(Batch.Fields.PARENT))
        );

        try {
            return toBatchVO(getEntityManager().createQuery(query).setParameter(operationIdParam, operationId).getSingleResult(), false);
        } catch (NoResultException e){
            return null;
        }
    }

    @Override
    public BatchVO get(int id) {
        Batch entity = get(Batch.class, id);
        return toBatchVO(entity, false);
    }

    @Override
    public List<BatchVO> saveByOperationId(int operationId, List<BatchVO> sources) {

        long debugTime = logger.isDebugEnabled() ? System.currentTimeMillis() : 0L;
        if (debugTime != 0L) logger.debug(String.format("Saving operation {id:%s} batches... {hash_optimization:%s}", operationId, enableSaveUsingHash));

        // Load parent entity
        Operation parent = get(Operation.class, operationId);

        sources.forEach(source -> source.setOperationId(operationId));

        // Save all by parent
        boolean dirty = saveAllByParent(parent, sources);

        // Flush if need
        if (dirty) {
            entityManager.flush();
            entityManager.clear();
        }

        if (debugTime != 0L) logger.debug(String.format("Saving operation {id:%s} batches [OK] in %s ms", operationId, System.currentTimeMillis() - debugTime));

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
            // FIXME: Client app: update entity from the save() result
            //checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        // Copy some fields from the trip
        copySomeFieldsFromOperation(source);

        // VO -> Entity
        batchVOToEntity(source, entity, true, false);

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
        if (trace) logger.trace(String.format("Deleting batch {id: %s}...", id));
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

    @Override
    public BatchVO toTree(List<BatchVO> sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return null;
        }

        // Assume there is only 1 catch batch
        if (sources.stream().filter(batch -> batch.getParentId() == null).count() > 1) {
            logger.warn("Multiple catch batches in this source list, will return the first one.");
        }

        BatchVO catchBatch = sources.stream().filter(batch -> batch.getParentId() == null).findFirst().orElse(null);
        if (catchBatch == null) {
            return null;
        }

        catchBatch.setChildren(findChildren(catchBatch.getId(), sources));

        return catchBatch;
    }


    /* -- protected methods -- */

    protected boolean saveAllByParent(IWithBatchesEntity<Integer, Batch> parent, List<BatchVO> sources) {

        // Load existing entities
        final Multimap<Integer, Batch> sourcesByHashCode = Beans.splitByNotUniqueProperty(Beans.getList(parent.getBatches()), Batch.Fields.HASH);
        final Multimap<String, Batch> sourcesByLabelMap = Beans.splitByNotUniqueProperty(Beans.getList(parent.getBatches()), Batch.Fields.LABEL);
        final Map<Integer, Batch> sourcesIdsToProcess = Beans.splitById(Beans.getList(parent.getBatches()));
        final Set<Integer> sourcesIdsToSkip = enableSaveUsingHash ? Sets.newHashSet() : null;

        // Save each batches
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        boolean dirty = sources.stream().map(source -> {

            Batch target = null;
            if (source.getId() != null) {
                target = sourcesIdsToProcess.remove(source.getId());
            }
            // Source has no id (e.g. a sampling batch can have no ID sent by SUMARiS app)
            else {
                // Try to get it by hash code
                Collection<Batch> existingBatchs = sourcesByHashCode.get(source.hashCode());
                // Not found by hash code: try by label
                if (CollectionUtils.isEmpty(existingBatchs)) {
                    existingBatchs = sourcesByLabelMap.get(source.getLabel());
                }
                // If one on match => use it
                if (CollectionUtils.size(existingBatchs) == 1) {
                    target = sourcesIdsToProcess.remove(existingBatchs.iterator().next().getId());
                    if (target != null) {
                        source.setId(target.getId());
                    }
                }
            }

            // Check if batch save can be skipped
            boolean skip = source.getId() != null && (enableSaveUsingHash && sourcesIdsToSkip.contains(source.getId()));
            if (!skip) {

                // Save the batch (using a dedicated function)
                source = optimizedSave(source, target, false, newUpdateDate, enableSaveUsingHash);
                skip = !Objects.equals(source.getUpdateDate(), newUpdateDate);

                // If skipped, all children are also skipped
                if (skip) {
                    getAllChildren(source).forEach(b -> sourcesIdsToSkip.add(b.getId()));
                }
            }
            if (skip && trace) {
                logger.trace(String.format("Skip batch {id: %s, label: '%s'}", source.getId(), source.getLabel()));
            }
            return !skip;
        })
            // Count updates
            .filter(Boolean::booleanValue)
            .count() > 0;

        // Remove not processed batches
        if (MapUtils.isNotEmpty(sourcesIdsToProcess)) {
            // Delete linked produces first (ie. Sales of packets)
            productRepository.deleteProductsByBatchIdIn(sourcesIdsToProcess.keySet());
            sourcesIdsToProcess.values().forEach(this::delete);
            dirty = true;
        }

        // Remove parent (use only parentId)
        sources.forEach(batch -> {
            if (batch.getParent() != null) {
                batch.setParentId(batch.getParent().getId());
                batch.setParent(null);
            }
        });

        return dirty;
    }

    /**
     * Save the batch, when saving a full tree (algorithm optimized for this case)
     * /!\ DO NOT USE when updating only one batch, in a existing tree !
     *
     * @param source
     * @param entity
     * @param checkUpdateDate
     * @param newUpdateDate
     * @return
     */
    protected BatchVO optimizedSave(BatchVO source,
                                    Batch entity,
                                    boolean checkUpdateDate,
                                    Timestamp newUpdateDate,
                                    boolean enableBatchHashOptimization) {
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
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        // Copy some fields from the trip
        copySomeFieldsFromOperation(source);

        // VO -> Entity
        boolean skipSave = batchVOToEntity(source, entity, true, !isNew && enableBatchHashOptimization);

        // Stop here (without change on the update_date)
        if (skipSave) return source;

        // Update update_dt
        entity.setUpdateDate(newUpdateDate);
        source.setUpdateDate(newUpdateDate);

        // Save entity
        if (isNew) {
            // Add new batch
            entityManager.persist(entity);
            source.setId(entity.getId());
            if (trace) logger.trace(String.format("Adding batch {id: %s, label: '%s'}...", entity.getId(), entity.getLabel()));
        } else {
            // Update existing batch
            if (trace) logger.trace(String.format("Updating batch {id: %s, label: '%s'}...", entity.getId(), entity.getLabel()));
            entityManager.merge(entity);
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
            TaxonNameVO taxonName = taxonNameRepository.getTaxonNameReferent(source.getReferenceTaxon().getId());
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

        // Quality Flag
        if (source.getQualityFlag() != null) {
            target.setQualityFlagId(source.getQualityFlag().getId());
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
        OperationVO operation = target.getOperation();
        if (operation == null) return;

        target.setRecorderDepartment(operation.getRecorderDepartment());
    }

    protected List<BatchVO> toBatchVOs(List<Batch> source, boolean allFields) {
        return this.toBatchVOs(source.stream(), allFields);
    }

    protected List<BatchVO> toBatchVOs(Stream<Batch> source, boolean allFields) {
        return source.map(s -> this.toBatchVO(s, allFields))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * @param source
     * @param target
     * @param copyIfNull
     * @return true if can skip batch update (only if hash optimization have been enabled)
     */
    protected boolean batchVOToEntity(BatchVO source, Batch target, boolean copyIfNull, boolean allowSkipSameHash) {

        // Get some parent ids
        Integer parentId = (source.getParent() != null ? source.getParent().getId() : source.getParentId());
        Integer opeId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);

        // Parent batch
        if (copyIfNull || (parentId != null)) {

            // Check if parent changed.
            Batch previousParent = target.getParent();
            if (previousParent != null && !Objects.equals(parentId, previousParent.getId())
                && CollectionUtils.isNotEmpty(previousParent.getChildren())) {
                // Remove in the parent children list (to avoid a DELETE CASCADE if the parent is delete later)
                previousParent.getChildren().remove(target);
            }

            if (parentId == null) {
                target.setParent(null);
            } else {
                Batch parent = load(Batch.class, parentId);
                target.setParent(parent);

                // Not need to update the children collection, because mapped by the 'parent' property
                //if (!parent.getChildren().contains(target)) {
                //    parent.getChildren().add(target);
                //}

                // Force same operation as parent (e.g. in case of bad batch tree copy)
                opeId = parent.getOperation().getId();
            }
        }

        // /!\ IMPORTANT: update source's operationId and parentId, BEFORE calling hashCode()
        source.setParentId(parentId);
        source.setOperationId(opeId);
        Integer newHash = source.hashCode();

        // If same hash, then skip (if allow)
        if (allowSkipSameHash && Objects.equals(target.getHash(), newHash)) {
            return true; // Skip
        }

        Beans.copyProperties(source, target);

        // Hash
        target.setHash(newHash);

        // Operation
        if (copyIfNull || (opeId != null)) {
            if (opeId == null) {
                target.setOperation(null);
            } else {
                target.setOperation(load(Operation.class, opeId));
            }
        }

        // Taxon group
        if (copyIfNull || source.getTaxonGroup() != null) {
            if (source.getTaxonGroup() == null || source.getTaxonGroup().getId() == null) {
                target.setTaxonGroup(null);
            } else {
                target.setTaxonGroup(load(TaxonGroup.class, source.getTaxonGroup().getId()));
            }
        }

        // Reference taxon (from taxon name)
        if (copyIfNull || source.getTaxonName() != null) {
            if (source.getTaxonName() == null || source.getTaxonName().getId() == null) {
                target.setReferenceTaxon(null);
            } else {
                if (source.getTaxonName().getReferenceTaxonId() != null) {
                    target.setReferenceTaxon(load(ReferenceTaxon.class, source.getTaxonName().getReferenceTaxonId()));
                } else {
                    // Get the taxon name, then set reference taxon
                    TaxonName taxonname = get(TaxonName.class, source.getTaxonName().getId());
                    if (taxonname != null) {
                        target.setReferenceTaxon(taxonname.getReferenceTaxon());
                    } else {
                        throw new DataIntegrityViolationException(String.format("Invalid batch: unknown taxon name {id:%s}", source.getTaxonName().getId()));
                    }
                }
            }
        }

        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            } else {
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
            } else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        return false;
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

    protected Stream<BatchVO> getAllChildren(BatchVO source) {
        if (CollectionUtils.isEmpty(source.getChildren())) {
            return Stream.empty();
        }
        return source.getChildren().stream().flatMap(c -> Stream.concat(Stream.of(c), getAllChildren(c)));
    }

    protected List<BatchVO> findChildren(int parentId, List<BatchVO> sources) {
        if (CollectionUtils.isEmpty(sources))
            return null;

        List<BatchVO> children = sources.stream().filter(batch -> Objects.equals(batch.getParentId(), parentId)).collect(Collectors.toList());
        children.forEach(batch -> batch.setChildren(findChildren(batch.getId(), sources)));
        return children;
    }

}
