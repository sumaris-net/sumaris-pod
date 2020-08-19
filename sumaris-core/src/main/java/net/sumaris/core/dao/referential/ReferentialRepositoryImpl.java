package net.sumaris.core.dao.referential;

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
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.referential.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * Base Repository class for Referential entities
 *
 * @author peck7 on 03/04/2020.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class ReferentialRepositoryImpl<E extends IItemReferentialEntity, V extends IReferentialVO, F extends ReferentialFilterVO>
    extends SumarisJpaRepositoryImpl<E, Integer, V>
    implements ReferentialRepository<E, V, F>, ReferentialSpecifications<E> {

    private static final Logger LOG = LoggerFactory.getLogger(ReferentialRepositoryImpl.class);

    private boolean checkUpdateDate = true;

    public ReferentialRepositoryImpl(Class<E> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
    }

    public boolean isCheckUpdateDate() {
        return checkUpdateDate;
    }

    public void setCheckUpdateDate(boolean checkUpdateDate) {
        this.checkUpdateDate = checkUpdateDate;
    }

    @Override
    public List<V> findAll(F filter) {
        return findAll(toSpecification(filter)).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Page<V> findAll(F filter, Pageable pageable) {
        return findAll(toSpecification(filter), pageable)
            .map(this::toVO);
    }

    @Override
    public List<V> findAll(F filter, DataFetchOptions fetchOptions) {
        return findAll(toSpecification(filter)).stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAll(F filter, Pageable pageable, DataFetchOptions fetchOptions) {
        return findAll(toSpecification(filter), pageable)
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public Page<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions) {
        return findAll(PageRequest.of(offset / size, size, Sort.Direction.fromString(sortDirection.toString()), sortAttribute))
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public Page<V> findAll(F filter, int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions) {
        return findAll(toSpecification(filter), Pageables.create(offset, size, sortAttribute, sortDirection))
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public List<V> findAllAsVO(Specification<E> spec) {
        return super.findAll(spec).stream()
            .map(this::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAllAsVO(Specification<E> spec, Pageable pageable) {
        return super.findAll(spec, pageable)
            .map(this::toVO);
    }

    @Override
    public List<V> findAllAsVO(Specification<E> spec, DataFetchOptions fetchOptions) {
        return super.findAll(spec).stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAllAsVO(Specification<E> spec, Pageable pageable, DataFetchOptions fetchOptions) {
        return super.findAll(spec, pageable)
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public long count(F filter) {
        return count(toSpecification(filter));
    }

    @Override
    public V get(int id) {
        return toVO(this.getOne(id));
    }

    @Override
    public V get(int id, DataFetchOptions fetchOptions) {
        return toVO(this.getOne(id), fetchOptions);
    }

    @Override
    public V getByLabel(String label) {
        V result = findByLabel(label);
        if (result == null)
            throw new EntityNotFoundException();
        return result;
    }

    @Override
    public V findByLabel(String label) {

        @SuppressWarnings("unchecked") List<V> result = findAll((F) ReferentialFilterVO.builder().label(label).build());
        if (CollectionUtils.isEmpty(result)) {
            return null;
        } else {
            if (result.size() > 1) {
                LOG.warn("findByLabel returns more than 1 result. Returning the first one");
            }
            return result.get(0);
        }

    }

    @Override
    public V save(V vo) {
        E entity = toEntity(vo);

        boolean isNew = entity.getId() == null;
        if (isNew) {
            entity.setCreationDate(new Date());
        }

        if (checkUpdateDate) {
            // Check update date
            Daos.checkUpdateDateForUpdate(vo, entity);
        }

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        E savedEntity = save(entity);

        // Update VO
        onAfterSaveEntity(vo, savedEntity, isNew);

        return vo;
    }

    @Override
    protected void onAfterSaveEntity(V vo, E savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
        vo.setUpdateDate(savedEntity.getUpdateDate());
        if (isNew)
            vo.setCreationDate(savedEntity.getCreationDate());
    }

    @Override
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

    @Override
    public void toEntity(V source, E target, boolean copyIfNull) {
        // copy properties
        super.toEntity(source, target, copyIfNull);

        // Status
        if (copyIfNull || source.getStatusId() != null) {
            Daos.setEntityProperty(getEntityManager(), target, IWithStatusEntity.Fields.STATUS, Status.class, source.getStatusId());
        }

        // Validity status
        if (target instanceof IWithValidityStatusEntity) {
            Integer validityStatusId = Beans.getProperty(source, "validityStatusId");
            if (copyIfNull || validityStatusId != null) {
                Daos.setEntityProperty(getEntityManager(), target, IWithValidityStatusEntity.Fields.VALIDITY_STATUS, ValidityStatus.class, validityStatusId);
            }
        }

    }

    @Override
    public V toVO(E source) {
        return toVO(source, null);
    }

    protected V toVO(E source, DataFetchOptions fetchOptions) {
        V target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    protected void toVO(E source, V target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        Beans.copyProperties(source, target);
    }

    @Override
    public V createVO() {
        try {
            return getVOClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Class<V> getVOClass() {
        throw new NotImplementedException("Not implemented yet. Should be override by subclass");
    }

    @Override
    public Specification<E> toSpecification(F filter) {
        // default specification
        return BindableSpecification
            .where(inStatusIds(filter))
            .and(hasLabel(filter.getLabel()))
            .and(searchOrJoinSearchText(filter));
    }

}
