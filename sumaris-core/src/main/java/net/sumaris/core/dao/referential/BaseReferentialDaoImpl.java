package net.sumaris.core.dao.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.DataDaos;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.*;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ParameterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public abstract class BaseReferentialDaoImpl<
        E extends IReferentialEntity,
        V extends IReferentialVO
       > extends HibernateDaoSupport {

    protected abstract V toVO(E source);

    protected abstract E createEntity();

    protected abstract Class<E> getDomainClass();

    public V get(final int id) {
        return toVO(getOne(id));
    }

    public V save(V vo) {

        E entity = toEntity(vo);

        if (entity.getId() == null) {
            entity.setCreationDate(new Date());
        }

        // Check update date
        Daos.checkUpdateDateForUpdate(vo, entity);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entity
        E savedEntity = save(entity);

        // Update VO
        vo.setId(savedEntity.getId());
        vo.setCreationDate(savedEntity.getCreationDate());
        vo.setUpdateDate(newUpdateDate);

        return vo;
    }


    public E toEntity(V vo) {
        Preconditions.checkNotNull(vo);
        E entity;
        if (vo.getId() != null) {
            entity = getOne(vo.getId());
        } else {
            entity = createEntity();
        }

        // Remember the entity's update date
        Date entityUpdateDate = entity.getUpdateDate();

        toEntity(vo, entity, true);

        // Restore the update date (can be override by Beans.copyProperties())
        entity.setUpdateDate(entityUpdateDate);

        return entity;
    }

    public E getOne(int id) {
        return entityManager.getReference(getDomainClass(), id);
    }

    public E save(E entity) {
        // Save entity
        if (entity.getId() == null) {
            entityManager.persist(entity);
        } else {
            entityManager.merge(entity);
        }

        return entity;
    }

    /* -- protected methods -- */

    protected void toEntity(V source, E target, boolean copyIfNull) {
        Beans.copyProperties(source, target);

        // Status
        if (copyIfNull || source.getStatusId() != null) {
            Daos.setEntityProperty(entityManager, target,
                    IWithStatusEntity.Fields.STATUS, Status.class, source.getStatusId());
        }

        // Validity status
        if (target instanceof IWithValidityStatusEntity) {
            Integer validityStatusId = Beans.getProperty(source, "validityStatusId");
            if (copyIfNull || validityStatusId != null) {
                Daos.setEntityProperty(entityManager, target,
                        IWithValidityStatusEntity.Fields.VALIDITY_STATUS, ValidityStatus.class, validityStatusId);
            }
        }
    }

}
