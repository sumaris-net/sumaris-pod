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
import com.google.common.collect.Sets;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.pmfm.Matrix;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.SampleVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("sampleDao")
public class SampleDaoImpl extends BaseDataDaoImpl implements SampleDao {

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(SampleDaoImpl.class);
    private static final boolean trace = logger.isTraceEnabled();

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonNameRepository taxonNameRepository;

    @Autowired
    private PersonRepository personRepository;

    private int unitIdNone;

    private boolean enableSaveUsingHash;

    @PostConstruct
    protected void init() {
        this.unitIdNone = config.getUnitIdNone();
        this.enableSaveUsingHash = config.enableSampleHashOptimization();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SampleVO> getAllByOperationId(int operationId) {

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Sample> query = cb.createQuery(Sample.class);
        Root<Sample> root = query.from(Sample.class);

        query.select(root);

        ParameterExpression<Integer> tripIdParam = cb.parameter(Integer.class);

        query.where(cb.equal(root.get(Sample.Fields.OPERATION).get(Operation.Fields.ID), tripIdParam));

        // Sort by rank order
        query.orderBy(cb.asc(root.get(PmfmStrategy.Fields.RANK_ORDER)));

        return toSampleVOs(getEntityManager().createQuery(query)
                .setParameter(tripIdParam, operationId).getResultList(), false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SampleVO> getAllByLandingId(int landingId) {

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Sample> query = cb.createQuery(Sample.class);
        Root<Sample> root = query.from(Sample.class);

        query.select(root);

        ParameterExpression<Integer> idParam = cb.parameter(Integer.class);

        query.where(cb.equal(root.get(Sample.Fields.LANDING).get(Landing.Fields.ID), idParam));

        // Sort by rank order
        query.orderBy(cb.asc(root.get(PmfmStrategy.Fields.RANK_ORDER)));

        return toSampleVOs(getEntityManager().createQuery(query)
                .setParameter(idParam, landingId).getResultStream(), false);
    }


    @Override
    public SampleVO get(int id) {
        Sample entity = get(Sample.class, id);
        return toSampleVO(entity, false);
    }

    @Override
    public List<SampleVO> saveByOperationId(int operationId, List<SampleVO> sources) {

        long debugTime = logger.isDebugEnabled() ? System.currentTimeMillis() : 0L;
        if (debugTime != 0L) logger.debug(String.format("Saving operation {id:%s} samples... {hash_optimization:%s}", operationId, enableSaveUsingHash));

        // Load parent entity
        Operation parent = get(Operation.class, operationId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getTrip().getProgram().getId());

        sources.forEach(source -> {
            source.setOperationId(operationId);
            source.setProgram(parentProgram);
        });

        // Save all, by parent
        boolean dirty = saveAllByParent(parent, sources);

        if (dirty) {
            entityManager.flush();
            entityManager.clear();
        }

        if (debugTime != 0L) logger.debug(String.format("Saving operation {id:%s} samples [OK] in %s ms", operationId, System.currentTimeMillis() - debugTime));

        return sources;
    }

    @Override
    public List<SampleVO> saveByLandingId(int landingId, List<SampleVO> sources) {
        long debugTime = logger.isDebugEnabled() ? System.currentTimeMillis() : 0L;
        if (debugTime != 0L) logger.debug(String.format("Saving landing {id:%s} samples... {hash_optimization:%s}", landingId, enableSaveUsingHash));

        // Load parent entity
        Landing parent = get(Landing.class, landingId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        // Save each entities
        sources.forEach(source -> {
            source.setLandingId(landingId);
            source.setProgram(parentProgram);
        });

        // Save all, by parent
        boolean dirty = saveAllByParent(parent, sources);

        if (dirty) {
            entityManager.flush();
            entityManager.clear();
        }

        if (debugTime != 0L) logger.debug(String.format("Saving landing {id:%s} samples [OK] in %s ms", landingId, System.currentTimeMillis() - debugTime));

        return sources;
    }

    @Override
    public SampleVO save(SampleVO source) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        Sample entity = null;
        if (source.getId() != null) {
            entity = get(Sample.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Sample();
        }

        if (!isNew) {
            // Check update date
            // FIXME: Client app: update entity from the save() result
            //checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        // Copy some fields from the trip
        copySomeFieldsFromParent(source);

        // VO -> Entity
        sampleVOToEntity(source, entity, true, false);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entity
        if (isNew) {
            // Force creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            if (entity.getCreationDate() == null) {
                logger.warn(String.format("Updating a sample {id: %s, label: '%s'} without creation_date!", entity.getId(), entity.getLabel()));
                entity.setCreationDate(newUpdateDate);
                source.setCreationDate(newUpdateDate);
            }
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        // Update link to parent
        if (source.getParentId() == null && entity.getParent() != null) {
            source.setParentId(entity.getParent().getId());
        }

        entityManager.flush();
        entityManager.clear();

        return source;
    }

    @Override
    public void delete(int id) {
        if (trace) logger.trace(String.format("Deleting sample {id: %s}...", id));
        delete(Sample.class, id);
    }

    @Override
    public SampleVO toSampleVO(Sample source) {
        return toSampleVO(source, true);
    }


    /* -- protected methods -- */

    protected boolean saveAllByParent(IWithSamplesEntity<Integer, Sample> parent,
                                             List<SampleVO> sources) {

        // Load existing entities
        final Map<Integer, Sample> sourcesIdsToProcess = Beans.splitById(Beans.getList(parent.getSamples()));
        final Set<Integer> sourcesIdsToSkip = enableSaveUsingHash ? Sets.newHashSet() : null;

        // Save each samples
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        boolean dirty = sources.stream().map(source -> {
            Sample target = null;
            if (source.getId() != null) {
                target = sourcesIdsToProcess.remove(source.getId());
            }
            // Check if batch save can be skipped
            boolean skip = source.getId() != null && (enableSaveUsingHash && sourcesIdsToSkip.contains(source.getId()));
            if (!skip) {
                source = optimizedSave(source, target, false, newUpdateDate, enableSaveUsingHash);
                skip = !Objects.equals(source.getUpdateDate(), newUpdateDate);

                // If not changed, add children to the skip list
                if (skip) {
                    getAllChildren(source).forEach(b -> sourcesIdsToSkip.add(b.getId()));
                }
            }
            if (skip && trace) {
                logger.trace(String.format("Skip sample {id: %s, label: '%s'}", source.getId(), source.getLabel()));
            }
            return !skip;
        })
                // Count updates
                .filter(Boolean::booleanValue)
                .count() > 0;

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesIdsToProcess)) {
            sourcesIdsToProcess.values().forEach(this::delete);
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
                                     Timestamp newUpdateDate,
                                     boolean enableHashOptimization) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        if (entity == null && source.getId() != null) {
            entity = get(Sample.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Sample();
        }

        if (!isNew && checkUpdateDate) {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        // Copy some fields from the trip
        copySomeFieldsFromParent(source);

        // VO -> Entity
        boolean skipSave = sampleVOToEntity(source, entity, true, !isNew && enableHashOptimization);

        // Stop here (without change on the update_date)
        if (skipSave) return source;

        // Update update_dt
        entity.setUpdateDate(newUpdateDate);
        source.setUpdateDate(newUpdateDate);

        // Save entity
        if (isNew) {
            // Set creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            // Add the new sample
            entityManager.persist(entity);
            source.setId(entity.getId());
            if (trace) logger.trace(String.format("Adding sample {id: %s, label: '%s'}...", entity.getId(), entity.getLabel()));
        } else {

            // Workaround, to be sure to have a creation_date
            if (entity.getCreationDate() == null) {
                logger.warn(String.format("Updating a sample {id: %s, label: '%s'} without creation_date!", entity.getId(), entity.getLabel()));
                entity.setCreationDate(newUpdateDate);
                source.setCreationDate(newUpdateDate);
            }

            // Update existing sample
            if (trace) logger.trace(String.format("Updating sample {id: %s, label: '%s'}...", entity.getId(), entity.getLabel()));
            entityManager.merge(entity);
        }

        return source;
    }

    protected SampleVO toSampleVO(Sample source, boolean allFields) {

        if (source == null) return null;

        SampleVO target = new SampleVO();

        Beans.copyProperties(source, target);

        // Matrix
        ReferentialVO matrix = referentialDao.toReferentialVO(source.getMatrix());
        target.setMatrix(matrix);

        // Size Unit
        if (source.getSizeUnit() != null && source.getSizeUnit().getId().intValue() != unitIdNone) {
            target.setSizeUnit(source.getSizeUnit().getLabel());
        }

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

        // Quality Flag
        if (source.getQualityFlag() != null) {
            target.setQualityFlagId(source.getQualityFlag().getId());
        }

        // If full export
        if (allFields) {
            // Recorder department
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
            target.setRecorderDepartment(recorderDepartment);

            // Recorder person
            if (source.getRecorderPerson() != null) {
                PersonVO recorderPerson = personRepository.toVO(source.getRecorderPerson());
                target.setRecorderPerson(recorderPerson);
            }
        }

        return target;
    }

    protected void copySomeFieldsFromParent(SampleVO target) {
        OperationVO operation = target.getOperation();
        if (operation != null) {
            target.setRecorderDepartment(operation.getRecorderDepartment());
            return;
        }
        LandingVO landing = target.getLanding();
        if (landing != null) {
            target.setRecorderDepartment(landing.getRecorderDepartment());
            return;
        }
    }

    protected List<SampleVO> toSampleVOs(List<Sample> source, boolean allFields) {
        return this.toSampleVOs(source.stream(), allFields);
    }

    protected List<SampleVO> toSampleVOs(Stream<Sample> source, boolean allFields) {
        return source.map(s -> this.toSampleVO(s, allFields))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected boolean sampleVOToEntity(SampleVO source, Sample target, boolean copyIfNull, boolean allowSkipSameHash) {

        // Get some parent ids
        Integer parentId = source.getParent() != null ? source.getParent().getId() : source.getParentId();
        Integer opeId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
        Integer landingId = source.getLandingId() != null ? source.getLandingId() : (source.getLanding() != null ? source.getLanding().getId() : null);
        Integer matrixId = source.getMatrixId() != null ? source.getMatrixId() : (source.getMatrix() != null ? source.getMatrix().getId() : null);
        Integer batchId = source.getBatchId() != null ? source.getBatchId() : (source.getBatch() != null ? source.getBatch().getId() : null);

        // Parent sample
        if (copyIfNull || (parentId != null)) {

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
                Sample parent = load(Sample.class, parentId);
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
        copyRootDataProperties(source, target, copyIfNull);

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

        // Landing
        if (copyIfNull || (landingId != null)) {
            if (landingId == null) {
                target.setLanding(null);
            } else {
                target.setLanding(load(Landing.class, landingId));
            }
        }

        // Matrix
        if (copyIfNull || source.getMatrix() != null) {
            if (source.getMatrix() == null || source.getMatrix().getId() == null) {
                target.setMatrix(null);
            }
            else {
                target.setMatrix(load(Matrix.class, source.getMatrix().getId()));
            }
        }

        // Size Unit
        if (copyIfNull || source.getSizeUnit() != null) {
            if (source.getSizeUnit() == null) {
                target.setSizeUnit(null);
            }
            else {
                ReferentialVO unit = referentialDao.findByUniqueLabel(Unit.class.getSimpleName(), source.getSizeUnit());
                Preconditions.checkNotNull(unit, String.format("Invalid 'sample.sizeUnit': unit symbol '%s' not exists", source.getSizeUnit()));
                target.setSizeUnit(load(Unit.class, unit.getId()));
            }
        }

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
                // Get the taxon name, then set reference taxon
                TaxonName taxonname = get(TaxonName.class, source.getTaxonName().getId());
                target.setReferenceTaxon(taxonname.getReferenceTaxon());
            }
        }

        // Batch
        if (copyIfNull || (batchId != null)) {
            if (batchId == null) {
                target.setBatch(null);
            }
            else {
                target.setBatch(load(Batch.class, batchId));
            }
        }

        return false;
    }

    protected Stream<SampleVO> getAllChildren(SampleVO source) {
        if (CollectionUtils.isEmpty(source.getChildren())) {
            return Stream.empty();
        }
        return source.getChildren().stream().flatMap(c -> Stream.concat(Stream.of(c), getAllChildren(c)));
    }
}
