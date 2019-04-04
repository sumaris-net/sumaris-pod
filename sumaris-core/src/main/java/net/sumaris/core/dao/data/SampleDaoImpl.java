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
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonNameDao;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.data.Batch;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Sample;
import net.sumaris.core.model.referential.pmfm.Matrix;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.SampleVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("sampleDao")
public class SampleDaoImpl extends BaseDataDaoImpl implements SampleDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(SampleDaoImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonNameDao taxonNameDao;

    @Autowired
    private PersonDao personDao;

    @Override
    @SuppressWarnings("unchecked")
    public List<SampleVO> getAllByOperationId(int operationId) {

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Sample> query = cb.createQuery(Sample.class);
        Root<Sample> root = query.from(Sample.class);

        query.select(root);

        ParameterExpression<Integer> tripIdParam = cb.parameter(Integer.class);

        query.where(cb.equal(root.get(Sample.PROPERTY_OPERATION).get(Sample.PROPERTY_ID), tripIdParam));

        // Sort by rank order
        query.orderBy(cb.asc(root.get(PmfmStrategy.PROPERTY_RANK_ORDER)));

        return toSampleVOs(getEntityManager().createQuery(query)
                .setParameter(tripIdParam, operationId).getResultList(), false);
    }


    @Override
    public SampleVO get(int id) {
        Sample entity = get(Sample.class, id);
        return toSampleVO(entity, false);
    }

    @Override
    public List<SampleVO> saveByOperationId(int operationId, List<SampleVO> sources) {

        // Load parent entity
        Operation parent = get(Operation.class, operationId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getTrip().getProgram().getId());

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getSamples()));

        // Save each gears
        List<SampleVO> result = sources.stream().map(source -> {
            source.setOperationId(operationId);
            source.setProgram(parentProgram);
            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::delete);
        }

        return result;
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
            // TODO: check why SUMARiS app did not refresh sample's updateDate after a first save
            //checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        // Copy some fields from the trip
        copySomeFieldsFromOperation(source);

        // VO -> Entity
        sampleVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            // Force creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        entityManager.flush();
        entityManager.clear();

        return source;
    }

    @Override
    public void delete(int id) {

        log.debug(String.format("Deleting sample {id=%s}...", id));
        delete(Sample.class, id);
    }

    @Override
    public SampleVO toSampleVO(Sample source) {
        return toSampleVO(source, true);
    }


    /* -- protected methods -- */

    protected SampleVO toSampleVO(Sample source, boolean allFields) {

        if (source == null) return null;

        SampleVO target = new SampleVO();

        Beans.copyProperties(source, target);

        // Matrix
        ReferentialVO matrix = referentialDao.toReferentialVO(source.getMatrix());
        target.setMatrix(matrix);

        // Taxon group
        if (source.getTaxonGroup() != null) {
            ReferentialVO taxonGroup = referentialDao.toReferentialVO(source.getTaxonGroup());
            target.setTaxonGroup(taxonGroup);
        }

        // Taxon name (from reference)
        if (source.getReferenceTaxon() != null) {
            ReferentialVO taxonName = taxonNameDao.getTaxonNameReferent(source.getReferenceTaxon().getId());
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
        // Batch
        if (source.getBatch() != null) {
            target.setBatchId(source.getBatch().getId());
        }

        // If full export
        if (allFields) {
            // Recorder department
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class);
            target.setRecorderDepartment(recorderDepartment);

            // Recorder person
            if (source.getRecorderPerson() != null) {
                PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
                target.setRecorderPerson(recorderPerson);
            }
        }

        return target;
    }

    protected void copySomeFieldsFromOperation(SampleVO target) {
        OperationVO source = target.getOperation();
        if (source == null) return;

        target.setRecorderDepartment(source.getRecorderDepartment());

    }

    protected List<SampleVO> toSampleVOs(List<Sample> source, boolean allFields) {
        return this.toSampleVOs(source.stream(), allFields);
    }

    protected List<SampleVO> toSampleVOs(Stream<Sample> source, boolean allFields) {
        return source.map(s -> this.toSampleVO(s, allFields))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void sampleVOToEntity(SampleVO source, Sample target, boolean copyIfNull) {

        copyRootDataProperties(source, target, copyIfNull);

        // Matrix
        if (copyIfNull || source.getMatrix() != null) {
            if (source.getMatrix() == null || source.getMatrix().getId() == null) {
                target.setMatrix(null);
            }
            else {
                target.setMatrix(load(Matrix.class, source.getMatrix().getId()));
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

        Integer parentId = source.getParentId() != null ? source.getParentId() : (source.getParent() != null ? source.getParent().getId() : null);
        Integer opeId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);

        // Parent sample
        if (copyIfNull || (parentId != null)) {
            if (parentId == null) {
                target.setParent(null);
            }
            else {
                Sample parent = load(Sample.class, parentId);
                target.setParent(parent);

                // Force operation from parent's operation
                opeId = parent.getOperation().getId();
            }
        }

        // Operation
        if (copyIfNull || (opeId != null)) {
            if (opeId == null) {
                target.setOperation(null);
            } else {
                Operation operation = load(Operation.class, opeId);
                target.setOperation(operation);
            }
        }

        // Batch
        Integer batchId = source.getBatchId() != null ? source.getBatchId() : (source.getBatch() != null ? source.getBatch().getId() : null);
        if (copyIfNull || (batchId != null)) {
            if (batchId == null) {
                target.setBatch(null);
            }
            else {
                target.setBatch(load(Batch.class, batchId));
            }
        }
    }
}
