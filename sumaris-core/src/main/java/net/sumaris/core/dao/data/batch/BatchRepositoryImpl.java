/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.dao.data.batch;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.product.ProductRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.data.Batch;
import net.sumaris.core.model.data.IWithBatchesEntity;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.batch.BatchFetchOptions;
import net.sumaris.core.vo.data.batch.BatchFilterVO;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BatchRepositoryImpl
        extends DataRepositoryImpl<Batch, BatchVO, BatchFilterVO, BatchFetchOptions>
        implements BatchSpecifications {

    private boolean enableSaveUsingHash;

    private final ReferentialDao referentialDao;
    private final TaxonNameRepository taxonNameRepository;
    private final MeasurementDao measurementDao;
    private final ProductRepository productRepository;

    protected BatchRepositoryImpl(EntityManager entityManager,
                                  ReferentialDao referentialDao,
                                  TaxonNameRepository taxonNameRepository,
                                  MeasurementDao measurementDao,
                                  ProductRepository productRepository) {
        super(Batch.class, BatchVO.class, entityManager);
        this.referentialDao = referentialDao;
        this.taxonNameRepository = taxonNameRepository;
        this.measurementDao = measurementDao;
        this.productRepository = productRepository;
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableSaveUsingHash = getConfig().enableBatchHashOptimization();
    }

    @Override
    public BatchVO getCatchBatchByOperationId(int operationId, BatchFetchOptions fetchOptions) {

        if (fetchOptions.isWithChildrenEntities()) {
            // Return all batches as tree form
            return toTree(
                    findAllVO(
                        BindableSpecification.where(hasOperationId(operationId)),
                        BatchFetchOptions.builder()
                            .withMeasurementValues(fetchOptions.isWithMeasurementValues())
                            .withRecorderDepartment(fetchOptions.isWithRecorderDepartment())
                            .withChildrenEntities(false) // Children not need (function toTree() will linked parent/children)
                            .build()
                    ));
        }

        // Return the root batch only
        try {
            return findOne(hasNoParent()
                    .and(hasOperationId(operationId))
                    .and(addJoinFetch(fetchOptions, false))
                )
                .map(source -> toVO(source, fetchOptions))
                .orElse(null);
        } catch (NoResultException e){
            return null;
        }
    }

    @Override
    public BatchVO getCatchBatchBySaleId(int saleId, BatchFetchOptions fetchOptions) {
        if (fetchOptions.isWithChildrenEntities()) {
            // Return all batches as tree form
            return toTree(
                findAllVO(
                    BindableSpecification.where(hasSaleId(saleId)),
                    BatchFetchOptions.builder()
                        .withMeasurementValues(fetchOptions.isWithMeasurementValues())
                        .withRecorderDepartment(fetchOptions.isWithRecorderDepartment())
                        .withChildrenEntities(false) // Children not need (function toTree() will linked parent/children)
                        .build()
                ));
        }

        // Return the root batch only
        try {
            return findOne(
                BindableSpecification
                    .where(hasNoParent())
                    .and(hasSaleId(saleId))
                    .and(addJoinFetch(fetchOptions, false/*find one*/))
            )
            .map(source -> toVO(source, fetchOptions))
            .orElse(null);
        } catch (NoResultException e){
            return null;
        }
    }

    @Override
    public List<BatchVO> findAllVO(Specification<Batch> spec, BatchFetchOptions fetchOptions) {
        // Standard load
        if (!fetchOptions.isWithMeasurementValues()) {
            return super.findAllVO(spec, fetchOptions);
        }

        // Load using and optimized way
        List<BatchVO> result = super.findAllVO(spec,  BatchFetchOptions.builder()
                .withMeasurementValues(false) // Will be just later, in an optimize way
                .withRecorderDepartment(fetchOptions.isWithRecorderDepartment())
                .withChildrenEntities(fetchOptions.isWithChildrenEntities())
                .build());

        // Optimize measurement load
        Collection<Integer> batchIds = Beans.collectIds(result);

        Map<Integer, Map<Integer, String>> sm = measurementDao.getBatchesSortingMeasurementsMap(batchIds);
        Map<Integer, Map<Integer, String>> qm = measurementDao.getBatchesQuantificationMeasurementsMap(batchIds);

        result.forEach(b -> {
            int batchId = b.getId();
            b.setMeasurementValues(Beans.mergeMap(sm.get(batchId), qm.get(batchId)));
        });

        return result;
    }


    @Override
    public List<BatchVO> saveByOperationId(int operationId, List<BatchVO> sources) {

        long startTime = System.currentTimeMillis();
        log.debug("Saving operation {id: {}} batches... {hash_optimization: {}}", operationId, enableSaveUsingHash);

        // Load parent entity
        Operation parent = getById(Operation.class, operationId);

        sources.forEach(source -> source.setOperationId(operationId));

        // Save all by parent
        boolean dirty = saveAllByParent(parent, sources);

        // Flush if need
        if (dirty) {
            EntityManager entityManager = getEntityManager();
            entityManager.flush();
            entityManager.clear();
        }

        log.debug("Saving operation {id: {}} batches [OK] in {}", operationId, TimeUtils.printDurationFrom(startTime));

        return sources;
    }

    @Override
    public List<BatchVO> saveBySaleId(int saleId, List<BatchVO> sources) {
        long startTime = System.currentTimeMillis();
        log.debug("Saving sale {id: {}} batches... {hash_optimization: {}}", saleId, enableSaveUsingHash);

        // Load parent entity
        Sale parent = getById(Sale.class, saleId);

        sources.forEach(source -> source.setSaleId(saleId));

        // Save all by parent
        boolean dirty = saveAllByParent(parent, sources);

        // Flush if need
        if (dirty) {
            EntityManager entityManager = getEntityManager();
            entityManager.flush();
            entityManager.clear();
        }

        log.debug("Saving sale {id: {}} batches [OK] in {}", saleId, TimeUtils.printDurationFrom(startTime));

        return sources;
    }

    @Override
    protected void onBeforeSaveEntity(BatchVO source, Batch target, boolean isNew) {
        // Copy some fields from the trip
        copySomeFieldsFromOperation(source);

        super.onBeforeSaveEntity(source, target, isNew);
    }

    @Override
    public List<BatchVO> toFlatList(final BatchVO source) {
        List<BatchVO> result = Lists.newArrayList();
        fillListFromTree(result, source);
        return result;
    }

    @Override
    public BatchVO toTree(List<BatchVO> sources) {
        if (CollectionUtils.isEmpty(sources)) return null;

        List<BatchVO> roots = sources.stream()
                // Find the root catch
                .filter(batch -> batch.getParentId() == null)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(roots)) {
            log.warn("No catch batches found in this source list. Will return null.");
            return null;
        }

        // Assume there is only 1 catch batch
        if (CollectionUtils.size(roots) > 1) {
            log.warn("Multiple catch batches in this source list, will return the first one.");
        }

        // Get root
        BatchVO rootBatch = roots.get(0);

        // Fill children
        fillRecursiveChildren(rootBatch, sources);

        return rootBatch;
    }

    /* -- protected methods -- */

    protected Specification<Batch> toSpecification(BatchFilterVO filter, BatchFetchOptions fetchOptions) {
        // default specification
        return super.toSpecification(filter, fetchOptions)
                .and(hasOperationId(filter.getOperationId()))
                .and(hasSaleId(filter.getSaleId()))
                .and(addJoinFetch(fetchOptions, true))
                ;
    }

    protected boolean saveAllByParent(IWithBatchesEntity<Integer, Batch> parent, List<BatchVO> sources) {

        // Load existing entities
        final List<Batch> nonNullBatches = Beans.getList(parent.getBatches());
        final Multimap<Integer, Batch> sourcesByHashCode = Beans.splitByNotUniqueProperty(nonNullBatches, Batch.Fields.HASH, 0);
        final Multimap<String, Batch> sourcesByLabelMap = Beans.splitByNotUniqueProperty(nonNullBatches, Batch.Fields.LABEL, "!!MISSING_LABEL!!");
        final Map<Integer, Batch> sourcesIdsToProcess = Beans.splitById(nonNullBatches);
        final Set<Integer> sourcesIdsToSkip = enableSaveUsingHash ? Sets.newHashSet() : null;

        // Save each batches
        final boolean trace = log.isTraceEnabled();
        Date newUpdateDate = getDatabaseCurrentDate();
        long updatesCount = sources.stream().map(source -> {

            Batch target = null;
            if (source.getId() != null) {
                target = sourcesIdsToProcess.remove(source.getId());
            }
            // Source has no id (e.g. a sampling batch can have no ID sent by SUMARiS app)
            else {
                // Try to find it by hash code
                Collection<Batch> existingBatchs = sourcesByHashCode.get(source.hashCode());
                // Not found by hash code: try by label
                if (CollectionUtils.isEmpty(existingBatchs) && source.getLabel() != null) {
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
            boolean skip = enableSaveUsingHash && source.getId() != null && sourcesIdsToSkip.contains(source.getId());
            if (!skip) {

                // Save the batch (using a dedicated function)
                source = optimizedSave(source, target, false, newUpdateDate, enableSaveUsingHash);
                skip = !Objects.equals(source.getUpdateDate(), newUpdateDate);

                // If skipped, all children are also skipped
                if (skip) {
                    streamRecursiveChildren(source)
                            .map(BatchVO::getId)
                            .forEach(sourcesIdsToSkip::add);
                }
            }
            if (skip && trace) {
                log.trace("Skip batch {id: {}, label: '{}'}", source.getId(), source.getLabel());
            }
            return !skip;
        })
            // Count updates
            .filter(Boolean::booleanValue)
            .count();

        boolean dirty = updatesCount > 0;

        // Remove not processed batches
        if (MapUtils.isNotEmpty(sourcesIdsToProcess)) {
            // Delete linked produces first (ie. Sales of packets)
            productRepository.deleteProductsByBatchIdIn(sourcesIdsToProcess.keySet());
            this.deleteAll(sourcesIdsToProcess.values());
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
                                    Date newUpdateDate,
                                    boolean enableBatchHashOptimization) {
        Preconditions.checkNotNull(source);

        if (entity == null && source.getId() != null) {
            // do NOT use get, to allow batch tree to be saved, event if using bad ID
            entity = find(Batch.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Batch();
        }

        if (!isNew) {
            // Check update date
            if (checkUpdateDate) Daos.checkUpdateDateForUpdate(source, entity);

            // Always disabled, for optimized save
            // if (isLockForUpdate()) lockForUpdate(entity);

        }

        onBeforeSaveEntity(source, entity, isNew);

        // VO -> Entity
        boolean skipSave = toEntity(source, entity, true, !isNew && enableBatchHashOptimization);

        // Stop here (without change on the update_date)
        if (skipSave) return source;

        // Update update_dt
        entity.setUpdateDate(newUpdateDate);

        // Save entity
        Batch savedEntity = save(entity);

        onAfterSaveEntity(source, savedEntity, isNew);

        return source;
    }


    @Override
    public void toVO(Batch source,  BatchVO target, BatchFetchOptions fetchOptions, boolean copyIfNull) {
        Beans.copyProperties(source, target);

        // Taxon group
        if (source.getTaxonGroup() != null) {
            ReferentialVO taxonGroup = referentialDao.toVO(source.getTaxonGroup());
            target.setTaxonGroup(taxonGroup);
        }

        // Taxon name (from reference)
        if (source.getReferenceTaxon() != null && source.getReferenceTaxon().getId() != null) {
            target.setTaxonName(taxonNameRepository.findReferentByReferenceTaxonId(source.getReferenceTaxon().getId()).orElse(null));
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

        // Recorder department
        if (fetchOptions.isWithRecorderDepartment()) {
            // Recorder department
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
            target.setRecorderDepartment(recorderDepartment);
        }

        // Measurement values (as map)
        if (fetchOptions.isWithMeasurementValues() && source.getId() != null) {
            target.setMeasurementValues(Beans.mergeMap(
                    measurementDao.toMeasurementsMap(source.getSortingMeasurements()),
                    measurementDao.toMeasurementsMap(source.getQuantificationMeasurements())
            ));
        }
    }

    protected void copySomeFieldsFromOperation(BatchVO target) {
        OperationVO operation = target.getOperation();
        if (operation == null) return;

        target.setRecorderDepartment(operation.getRecorderDepartment());
    }

    /**
     * @param source
     * @param target
     * @param copyIfNull
     * @return true if can skip batch update (only if hash optimization have been enabled)
     */
    @Override
    public void toEntity(BatchVO source, Batch target, boolean copyIfNull) {
        toEntity(source, target, copyIfNull, target.getId() != null && enableSaveUsingHash);
    }

    protected boolean toEntity(BatchVO source, Batch target, boolean copyIfNull, boolean allowSkipSameHash) {
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
                Batch parent = getReference(Batch.class, parentId);
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

        // Copy properties, and data stuff (program, qualityFlag, recorder, ...)
        super.toEntity(source, target, copyIfNull);

        // Set the new hash
        target.setHash(newHash);

        // Operation
        if (copyIfNull || (opeId != null)) {
            if (opeId == null) {
                target.setOperation(null);
            } else {
                target.setOperation(getReference(Operation.class, opeId));
            }
        }

        // Taxon group
        if (copyIfNull || source.getTaxonGroup() != null) {
            if (source.getTaxonGroup() == null || source.getTaxonGroup().getId() == null) {
                target.setTaxonGroup(null);
            } else {
                target.setTaxonGroup(getReference(TaxonGroup.class, source.getTaxonGroup().getId()));
            }
        }

        // Reference taxon (from taxon name)
        if (copyIfNull || source.getTaxonName() != null) {
            if (source.getTaxonName() == null || source.getTaxonName().getId() == null) {
                target.setReferenceTaxon(null);
            } else {
                if (source.getTaxonName().getReferenceTaxonId() != null) {
                    target.setReferenceTaxon(getReference(ReferenceTaxon.class, source.getTaxonName().getReferenceTaxonId()));
                } else {
                    // Get the taxon name, then set reference taxon
                    Integer referenceTaxonId = taxonNameRepository.getReferenceTaxonIdById(source.getTaxonName().getId());
                    if (referenceTaxonId != null) {
                        target.setReferenceTaxon(getReference(ReferenceTaxon.class, referenceTaxonId));
                    } else {
                        throw new DataIntegrityViolationException(String.format("Invalid batch: unknown taxon name {id:%s}", source.getTaxonName().getId()));
                    }
                }
            }
        }

        // Reference taxon (from taxon name)
        if (copyIfNull || source.getTaxonName() != null) {
            if (source.getTaxonName() == null || source.getTaxonName().getId() == null) {
                target.setReferenceTaxon(null);
            }
            else {
                if (source.getTaxonName().getReferenceTaxonId() != null) {
                    target.setReferenceTaxon(getReference(ReferenceTaxon.class, source.getTaxonName().getReferenceTaxonId()));
                } else {
                    // Get the taxon name, then set reference taxon
                    Integer referenceTaxonId = taxonNameRepository.getReferenceTaxonIdById(source.getTaxonName().getId());
                    if (referenceTaxonId != null) {
                        target.setReferenceTaxon(getReference(ReferenceTaxon.class, referenceTaxonId));
                    } else {
                        throw new DataIntegrityViolationException(String.format("Invalid batch: unknown taxon name {id:%s}", source.getTaxonName().getId()));
                    }
                }
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

    protected Stream<BatchVO> streamRecursiveChildren(BatchVO source) {
        if (CollectionUtils.isEmpty(source.getChildren())) {
            return Stream.empty();
        }
        return source.getChildren().stream().flatMap(c -> Stream.concat(Stream.of(c), streamRecursiveChildren(c)));
    }


    protected void fillRecursiveChildren(BatchVO source, List<BatchVO> sources) {
        source.setChildren(fillRecursiveChildren(source.getId(), sources));
    }

    protected List<BatchVO> fillRecursiveChildren(int parentId, List<BatchVO> sources) {
        if (CollectionUtils.isEmpty(sources)) return null;

        List<BatchVO> children = sources.stream()
                .filter(batch -> Objects.equals(batch.getParentId(), parentId))
                .collect(Collectors.toList());
        children.forEach(batch -> batch.setChildren(fillRecursiveChildren(batch.getId(), sources)));
        return children;
    }

}
