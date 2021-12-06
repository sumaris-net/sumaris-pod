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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.IDataFetchOptions;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.core.vo.filter.IRootDataFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.EntityManager;
import java.util.Date;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@NoRepositoryBean
@Slf4j
public abstract class RootDataRepositoryImpl<
        E extends IRootDataEntity<Integer>,
        V extends IRootDataVO<Integer>,
        F extends IRootDataFilter,
        O extends IDataFetchOptions
        >
        extends DataRepositoryImpl<E, V, F, O>
        implements RootDataRepository<E, V, F, O> {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ProgramRepository programRepository;

    protected RootDataRepositoryImpl(Class<E> domainClass, Class<V> voClass, EntityManager entityManager) {
        super(domainClass, voClass, entityManager);
        setCopyExcludeProperties(
                IRootDataEntity.Fields.UPDATE_DATE,
                IRootDataEntity.Fields.CREATION_DATE);

        this.setLockForUpdate(true);
    }

    @Override
    public void toEntity(V source, E target, boolean copyIfNull) {
        DataDaos.copyRootDataProperties(getEntityManager(), source, target, copyIfNull, getCopyExcludeProperties());
    }

    @Override
    protected void onBeforeSaveEntity(V vo, E entity, boolean isNew) {
        super.onBeforeSaveEntity(vo, entity, isNew);

        // When new entity: set the creation date
        if (isNew || entity.getCreationDate() == null) {
            entity.setCreationDate(entity.getUpdateDate());
        }
    }

    @Override
    protected void onAfterSaveEntity(V vo, E savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        if (isNew) {
            vo.setCreationDate(savedEntity.getCreationDate());
        }
    }

    @Override
    public void toVO(E source, V target, O fetchOptions, boolean copyIfNull) {

        super.toVO(source, target, fetchOptions, copyIfNull);

        // Program
        if (source.getProgram() != null) {
            target.setProgram(programRepository.toVO(source.getProgram(),
                    ProgramFetchOptions.builder().withProperties(false).build()));
        }

        // Recorder person
        if ((fetchOptions == null || fetchOptions.isWithRecorderPerson()) && source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personRepository.toVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
        }

    }

    public V validate(V vo) {
        Preconditions.checkNotNull(vo);
        E entity = getById(vo.getId());

        // Check update date
        if (isCheckUpdateDate()) Daos.checkUpdateDateForUpdate(vo, entity);

        // Lock entityName
        if (isLockForUpdate()) lockForUpdate(entity);

        // Update update_dt
        Date newUpdateDate = getDatabaseCurrentDate();
        entity.setUpdateDate(newUpdateDate);

        // TODO VALIDATION PROCESS HERE
        entity.setValidationDate(newUpdateDate);

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        vo.setValidationDate(newUpdateDate);
        vo.setUpdateDate(newUpdateDate);

        return vo;
    }

    @Override
    public V unValidate(V vo) {
        return unvalidate(vo, true);
    }


    private V unvalidate(V vo, boolean save) {
        Preconditions.checkNotNull(vo);
        E entity = getById(vo.getId());

        // Check update date
        if (isCheckUpdateDate()) Daos.checkUpdateDateForUpdate(vo, entity);

        // Lock entityName
        if (save && isLockForUpdate()) lockForUpdate(entity);

        // TODO UNVALIDATION PROCESS HERE
        entity.setValidationDate(null);
        entity.setQualificationDate(null);
        entity.setQualityFlag(getReference(QualityFlag.class, QualityFlagEnum.NOT_QUALIFIED.getId()));

        // Update update_dt
        Date newUpdateDate = getDatabaseCurrentDate();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (save)
            getEntityManager().merge(entity);

        // Update source
        vo.setValidationDate(null);
        vo.setQualificationDate(null);
        vo.setQualityFlagId(QualityFlagEnum.NOT_QUALIFIED.getId());
        vo.setUpdateDate(newUpdateDate);

        return vo;
    }

    /* -- protected method -- */

    @Override
    protected Specification<E> toSpecification(F filter, O fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
                .and(hasRecorderPersonId(filter.getRecorderPersonId()))
                .and(hasProgramLabel(filter.getProgramLabel()))
                .and(hasProgramIds(filter.getProgramIds()));
    }
}
