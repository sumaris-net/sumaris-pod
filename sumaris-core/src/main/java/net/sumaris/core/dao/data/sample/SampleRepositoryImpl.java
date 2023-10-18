package net.sumaris.core.dao.data.sample;

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

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.ImageAttachmentRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.model.referential.pmfm.Matrix;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.ValueObjectFlags;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import net.sumaris.core.vo.filter.SampleFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author peck7 on 01/09/2020.
 */
@Slf4j
public class SampleRepositoryImpl
    extends RootDataRepositoryImpl<Sample, SampleVO, SampleFilterVO, SampleFetchOptions>
    implements SampleSpecifications {

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private MeasurementDao measurementDao;

    @Autowired
    private TaxonNameRepository taxonNameRepository;

    @Autowired
    private ImageAttachmentRepository imageAttachmentRepository;

    private boolean enableHashOptimization;;

    private boolean enableImageAttachments;

    @Autowired
    public SampleRepositoryImpl(EntityManager entityManager, SumarisConfiguration configuration) {
        super(Sample.class, SampleVO.class, entityManager);

        this.enableHashOptimization = configuration.enableSampleHashOptimization();
        this.enableImageAttachments = configuration.enableDataImages();

        // FIXME: Client app: update entity from the save() result
        setCheckUpdateDate(false); // for default save()
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        this.enableHashOptimization = event.getConfiguration().enableSampleHashOptimization();
        this.enableImageAttachments = event.getConfiguration().enableDataImages();
    }

    @Override
    protected Specification<Sample> toSpecification(SampleFilterVO filter, SampleFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
                .and(hasOperationId(filter.getOperationId()))
                .and(hasLandingId(filter.getLandingId()))
                .and(hasObservedLocationId(filter.getObservedLocationId()))
                .and(inObservedLocationIds(filter.getObservedLocationIds()))
                .and(hasTagId(filter.getTagId()))
                .and(withTagId(filter.getWithTagId()))
                .and(addJoinFetch(fetchOptions, true))
                .and(includedIds(filter.getIncludedIds()))
                .and(excludedIds(filter.getExcludedIds()));
    }

    @Override
    public void toVO(Sample source, SampleVO target, SampleFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Matrix
        ReferentialVO matrix = referentialDao.toVO(source.getMatrix());
        target.setMatrix(matrix);

        // Size Unit
        if (source.getSizeUnit() != null && source.getSizeUnit().getId() != UnitEnum.NONE.getId()) {
            target.setSizeUnit(source.getSizeUnit().getLabel());
        }

        // Taxon group
        if (source.getTaxonGroup() != null) {
            target.setTaxonGroup(referentialDao.toVO(source.getTaxonGroup()));
        }

        // Taxon name (from reference)
        if (source.getReferenceTaxon() != null && source.getReferenceTaxon().getId() != null) {
            target.setTaxonName(taxonNameRepository.findReferentByReferenceTaxonId(source.getReferenceTaxon().getId()).orElse(null));
        }

        // Parent sample
        if (source.getParent() != null) {
            target.setParentId(source.getParent().getId());
        }

        // Operation
        if (source.getOperation() != null) {
            target.setOperationId(source.getOperation().getId());
        }

        // Landing
        if (source.getLanding() != null) {
            target.setLandingId(source.getLanding().getId());
        }

        // TODO: Add link to Sale
        //if (source.getSale() != null) {
        //    target.setSaleId(source.getSale().getId());
        //}

        // Batch
        if (source.getBatch() != null) {
            target.setBatchId(source.getBatch().getId());
        }

        // Fetch measurement values
        Integer sampleId = source.getId();
        if (fetchOptions != null && fetchOptions.isWithMeasurementValues() && sampleId != null) {
            target.setMeasurementValues(measurementDao.toMeasurementsMap(source.getMeasurements()));
        }

        // Fetch images
        if (this.enableImageAttachments && fetchOptions != null && fetchOptions.isWithImages() && sampleId != null) {
            List<ImageAttachmentVO> images = imageAttachmentRepository.findAll(ImageAttachmentFilterVO.builder()
                    .objectId(sampleId)
                    .objectTypeId(ObjectTypeEnum.SAMPLE.getId())
                    .build(), ImageAttachmentFetchOptions.MINIMAL);
            target.setImages(images);
        }
    }

    @Override
    public void toEntity(SampleVO source, Sample target, boolean copyIfNull) {
        toEntity(source, target, copyIfNull, target.getId() != null && enableHashOptimization);
    }

    protected boolean toEntity(SampleVO source, Sample target, boolean copyIfNull, boolean allowSkipSameHash) {

        // Copy some fields from owner
        if (source.getOperation() != null) {
            source.setRecorderDepartment(source.getOperation().getRecorderDepartment());
        } else if (source.getLanding() != null) {
            source.setRecorderDepartment(source.getLanding().getRecorderDepartment());
        }

        // Get some parent ids
        Integer parentId = source.getParent() != null ? source.getParent().getId() : source.getParentId();
        Integer opeId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
        Integer landingId = source.getLandingId() != null ? source.getLandingId() : (source.getLanding() != null ? source.getLanding().getId() : null);
        Integer matrixId = source.getMatrixId() != null ? source.getMatrixId() : (source.getMatrix() != null ? source.getMatrix().getId() : null);
        Integer batchId = source.getBatchId() != null ? source.getBatchId() : (source.getBatch() != null ? source.getBatch().getId() : null);

        // Parent sample
        if (copyIfNull || parentId != null) {

            // Check if parent changed
            Sample previousParent = target.getParent();
            if (previousParent != null && !Objects.equals(parentId, previousParent.getId()) && CollectionUtils.isNotEmpty(previousParent.getChildren())) {
                // Remove in the parent children list (to avoid a DELETE CASCADE if the parent is delete later - fix #15)
                previousParent.getChildren().remove(target);
            }

            if (parentId == null) {
                target.setParent(null);
            }
            else {
                Sample parent = getReference(Sample.class, parentId);
                target.setParent(parent);

                // Not need to update the children collection, because mapped by the 'parent' property
                //if (!parent.getChildren().contains(target)) {
                //    parent.getChildren().add(target);
                //}

                // Force operation from parent's operation
                opeId = parent.getOperation().getId();
            }
        }

        // /!\ IMPORTANT: update source's parentId, operationId and landingId, BEFORE calling hashCode()
        source.setParentId(parentId);
        source.setOperationId(opeId);
        source.setLandingId(landingId);
        source.setMatrixId(matrixId);
        source.setBatchId(batchId);
        Integer newHash = source.hashCode();

        // If same hash, then skip (if allow)
        if (allowSkipSameHash && Objects.equals(target.getHash(), newHash)) {
            return true; // Skip
        }

        // Copy properties, and data stuff (program, qualityFlag, recorder, ...)
        super.toEntity(source, target, copyIfNull);

        // Set the new Hash
        target.setHash(newHash);

        // Operation
        if (copyIfNull || (opeId != null)) {
            if (opeId == null) {
                target.setOperation(null);
            } else {
                target.setOperation(getReference(Operation.class, opeId));
            }
        }

        // Landing
        if (copyIfNull || (landingId != null)) {
            if (landingId == null) {
                target.setLanding(null);
            } else {
                target.setLanding(getReference(Landing.class, landingId));
            }
        }

        // Matrix
        if (copyIfNull || source.getMatrix() != null) {
            if (source.getMatrix() == null || source.getMatrix().getId() == null) {
                target.setMatrix(null);
            }
            else {
                target.setMatrix(getReference(Matrix.class, source.getMatrix().getId()));
            }
        }

        // Size Unit
        if (copyIfNull || source.getSizeUnit() != null) {
            if (source.getSizeUnit() == null) {
                target.setSizeUnit(null);
            }
            else {
                ReferentialVO unit = referentialDao.findByUniqueLabel(Unit.class.getSimpleName(), source.getSizeUnit())
                    .orElseThrow(() -> new SumarisTechnicalException(String.format("Invalid 'sample.sizeUnit': unit symbol '%s' not exists", source.getSizeUnit())));
                target.setSizeUnit(getReference(Unit.class, unit.getId()));
            }
        }

        // Taxon group
        if (copyIfNull || source.getTaxonGroup() != null) {
            if (source.getTaxonGroup() == null || source.getTaxonGroup().getId() == null) {
                target.setTaxonGroup(null);
            }
            else {
                target.setTaxonGroup(getReference(TaxonGroup.class, source.getTaxonGroup().getId()));
            }
        }

        // Reference taxon (from taxon name)
        if (copyIfNull || source.getTaxonName() != null) {
            if (source.getTaxonName() == null || source.getTaxonName().getId() == null) {
                target.setReferenceTaxon(null);
            }
            else {
                // Get the taxon name, then set reference taxon
                Integer referenceTaxonId = taxonNameRepository.getReferenceTaxonIdById(source.getTaxonName().getId());
                target.setReferenceTaxon(getReference(ReferenceTaxon.class, referenceTaxonId));
            }
        }

        // Batch
        if (copyIfNull || (batchId != null)) {
            if (batchId == null) {
                target.setBatch(null);
            }
            else {
                target.setBatch(getReference(Batch.class, batchId));
            }
        }

        return false;
    }

    @Override
    protected void onBeforeSaveEntity(SampleVO source, Sample target, boolean isNew) {
        if (!isNew && target.getCreationDate() == null) {
            log.warn(String.format("Updating a sample {id: %s, label: '%s'} without creation_date!", target.getId(), target.getLabel()));
        }
        super.onBeforeSaveEntity(source, target, isNew);
    }

    @Override
    protected void onAfterSaveEntity(SampleVO vo, Sample savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        // Update link to parent
        if (vo.getParentId() == null && savedEntity.getParent() != null) {
            vo.setParentId(savedEntity.getParent().getId());
        }
    }

    @Override
    public List<SampleVO> saveByOperationId(int operationId, List<SampleVO> sources) {

        long debugTime = log.isDebugEnabled() ? System.currentTimeMillis() : 0L;
        if (debugTime != 0L) log.debug(String.format("Saving operation {id:%s} samples... {hash_optimization:%s}", operationId, enableHashOptimization));

        // Load parent entity
        Operation parent = getById(Operation.class, operationId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getTrip().getProgram().getId());

        sources.forEach(sample -> {
            sample.setLandingId(null);
            sample.setOperationId(operationId);
            sample.setProgram(parentProgram);
        });

        // Save all, by parent
        boolean dirty = saveAllByParent(parent, sources);

        if (dirty) {
            getEntityManager().flush();
            getEntityManager().clear();
        }

        if (debugTime != 0L) log.debug("Saving operation {id: {}} samples [OK] in {}", operationId, TimeUtils.printDurationFrom(debugTime));

        return sources;
    }

    @Override
    public List<SampleVO> saveByLandingId(int landingId, List<SampleVO> samples) {

        long debugTime = log.isDebugEnabled() ? System.currentTimeMillis() : 0L;
        if (debugTime != 0L) log.debug(String.format("Saving landing {id:%s} samples... {hash_optimization:%s}", landingId, enableHashOptimization));

        // Load parent entity
        Landing parent = getById(Landing.class, landingId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        // Link to parent
        samples.forEach(sample -> {
            sample.setOperationId(null);
            sample.setLandingId(landingId);
            sample.setProgram(parentProgram);
        });

        // Save all, by parent
        boolean dirty = saveAllByParent(parent, samples);

        if (dirty) {
            getEntityManager().flush();
            getEntityManager().clear();
        }

        if (debugTime != 0L) log.debug(String.format("Saving landing {id:%s} samples [OK] in %s ms", landingId, System.currentTimeMillis() - debugTime));

        return samples;
    }

    protected boolean saveAllByParent(IWithSamplesEntity<Integer, Sample> parent, List<SampleVO> sources) {
        final boolean trace = log.isTraceEnabled();

        // Load existing entities
        final Map<Integer, Sample> sourcesByIds = Beans.splitById(parent.getSamples());
        final Set<Integer> sourcesIdsToSkip = Sets.newHashSet();

        // Get current update date
        Date newUpdateDate = getDatabaseCurrentDate();

        // Save each sources
        long updatesCount = sources.stream().map(source -> {
            Sample target = null;
            if (source.getId() != null) {
                target = sourcesByIds.remove(source.getId());
            }
            // Check can be skipped
            boolean skip = enableHashOptimization && source.getId() != null && sourcesIdsToSkip.contains(source.getId());
            if (!skip) {
                source = optimizedSave(source, target, false, newUpdateDate, enableHashOptimization);
                skip = !Objects.equals(source.getUpdateDate(), newUpdateDate);

                // If not changed, skip all children
                if (skip) {
                    streamRecursiveChildren(source)
                        .map(SampleVO::getId)
                        .forEach(sourcesIdsToSkip::add);
                }
            }
            if (skip && trace) {
                log.trace("Skip save {}", source);
            }
            return !skip;
        })
        // Count updates
        .filter(Boolean::booleanValue).count();

        boolean dirty = updatesCount > 0;

        // Remove unused entities
        if (!sourcesByIds.isEmpty()) {
            sourcesByIds.keySet().forEach(sampleId -> {
                try {
                    this.deleteById(sampleId);
                } catch (EmptyResultDataAccessException e) {
                    // Continue (can occur because of delete cascade)
                }
            });
            dirty = true;
        }

        // Remove parent (use only parentId)
        sources.forEach(sample -> {
            if (sample.getParent() != null) {
                sample.setParentId(sample.getParent().getId());
                sample.setParent(null);
            }
        });

        return dirty;
    }

    protected SampleVO optimizedSave(SampleVO source,
                                     Sample entity,
                                     boolean checkUpdateDate,
                                     Date newUpdateDate,
                                     boolean enableHashOptimization) {
        Preconditions.checkNotNull(source);
        EntityManager em = getEntityManager();

        if (entity == null && source.getId() != null) {
            entity = findById(source.getId()).orElse(null);
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Sample();
            source.setId(null); // Make sure to not re-affect the ID
        }

        if (!isNew && checkUpdateDate) {
            // Check update date
            Daos.checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        onBeforeSaveEntity(source, entity, isNew);

        // VO -> Entity
        boolean skipSave = toEntity(source, entity, true, !isNew && enableHashOptimization);

        // Stop here (without change on the update_date)
        if (skipSave) {
            // Flag as same hash
            source.addFlag(ValueObjectFlags.SAME_HASH);
            return source;
        }

        // Remove same hash flag
        source.removeFlag(ValueObjectFlags.SAME_HASH);

        // Update update_dt
        entity.setUpdateDate(newUpdateDate);
        source.setUpdateDate(newUpdateDate);

        // Save entity
        if (isNew) {
            // Set creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            // Add the new sample
            em.persist(entity);
            source.setId(entity.getId());
            if (log.isTraceEnabled()) log.trace("Adding {}...", entity);
        } else {

            // Workaround, to be sure to have a creation_date
            if (entity.getCreationDate() == null) {
                log.warn("Updating {} without creation_date!", entity);
                entity.setCreationDate(newUpdateDate);
                source.setCreationDate(newUpdateDate);
            }

            // Update existing sample
            if (log.isTraceEnabled()) log.trace("Updating {}...", entity);
            em.merge(entity);
        }

        return source;
    }





    protected Stream<SampleVO> streamRecursiveChildren(SampleVO source) {
        if (CollectionUtils.isEmpty(source.getChildren())) {
            return Stream.empty();
        }
        return source.getChildren().stream().flatMap(c -> Stream.concat(Stream.of(c), streamRecursiveChildren(c)));
    }

}
