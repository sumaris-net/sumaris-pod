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
import net.sumaris.core.dao.administration.programStrategy.ProgramDao;
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.exception.DataLockedException;
import net.sumaris.core.model.QualityFlagEnum;
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.core.vo.filter.IRootDataFilter;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import java.sql.Timestamp;

public class RootDataRepositoryImpl<
    E extends IRootDataEntity<ID>,
    ID extends Integer,
    V extends IRootDataVO<ID>,
    F extends IRootDataFilter
    >
    extends DataRepositoryImpl<E, ID, V, F>
    implements RootDataRepository<E, ID, V, F> {

    /**
     * Logger.
     */
    private static final Logger log =
        LoggerFactory.getLogger(RootDataRepositoryImpl.class);

    @Autowired
    private PersonDao personDao;

    @Autowired
    private ProgramDao programDao;

    public RootDataRepositoryImpl(Class<E> domainClass,
                                  EntityManager entityManager) {
        super(domainClass, entityManager);
        setCopyExcludeProperties(
                IRootDataEntity.Fields.UPDATE_DATE,
                IRootDataEntity.Fields.CREATION_DATE);

        this.setEnableForUpdate(true);
    }

    @Override
    public <S extends E> S save(S entity) {
        // When new entity: set the creation date
        if (entity.getId() == null || entity.getCreationDate() == null) {
            entity.setCreationDate(entity.getUpdateDate());
        }
        return super.save(entity);
    }

    @Override
    public void toEntity(V source, E target, boolean copyIfNull) {
        DataDaos.copyRootDataProperties(getEntityManager(), source, target, copyIfNull, getCopyExcludeProperties());
    }

    @Override
    public void toVO(E source, V target, DataFetchOptions fetchOptions, boolean copyIfNull) {

        super.toVO(source, target, fetchOptions, copyIfNull);

        // Program
        if (source.getProgram() != null) {
            target.setProgram(programDao.toProgramVO(source.getProgram(),
                ProgramFetchOptions.builder().withProperties(false).build()));
        }

        // Recorder person
        if ((fetchOptions == null || fetchOptions.isWithRecorderPerson()) && source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
        }

    }

    @Override
    public V validate(V source) {
        Preconditions.checkNotNull(source);

        E entity = getOne(source.getId());
        if (entity == null) {
            throw new DataRetrievalFailureException(String.format("Entity {%s} not found", source.getId()));
        }

        // Check update date
        if (isCheckUpdateDate()) Daos.checkUpdateDateForUpdate(source, entity);

        // Lock entityName
        if (isLockForUpdateEnable()) lockForUpdate(entity);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // TODO VALIDATION PROCESS HERE
        entity.setValidationDate(newUpdateDate);

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        source.setValidationDate(newUpdateDate);
        source.setUpdateDate(newUpdateDate);

        return source;
    }

    @Override
    public V unvalidate(V vo) {
        Preconditions.checkNotNull(vo);

        E entity = getOne(vo.getId());
        if (entity == null) {
            throw new DataRetrievalFailureException(String.format("Entity{%s} not found", vo.getId()));
        }

        // Check update date
        if (isCheckUpdateDate()) Daos.checkUpdateDateForUpdate(vo, entity);

        // Lock entityName
        if (isLockForUpdateEnable()) lockForUpdate(entity);

        // TODO UNVALIDATION PROCESS HERE
        entity.setValidationDate(null);
        entity.setQualificationDate(null);
        entity.setQualityFlag(load(QualityFlag.class, QualityFlagEnum.NOT_QUALIFED.getId()));

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        vo.setValidationDate(null);
        vo.setQualificationDate(null);
        vo.setQualityFlagId(QualityFlagEnum.NOT_QUALIFED.getId());
        vo.setUpdateDate(newUpdateDate);

        return vo;
    }

    /* -- protected method -- */

    @Override
    protected void onAfterSaveEntity(V vo, E savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        if (isNew) {
            vo.setCreationDate(savedEntity.getCreationDate());
        }
    }

}
