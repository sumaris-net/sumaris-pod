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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.referential.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Base Repository class for Referential entities
 *
 * @author peck7 on 03/04/2020.
 */

@Slf4j
@NoRepositoryBean
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public abstract class ReferentialRepositoryImpl<
    ID extends Serializable,
    E extends IItemReferentialEntity<ID>,
    V extends IReferentialVO<ID>,
    F extends IReferentialFilter,
    O extends IFetchOptions>
    extends SumarisJpaRepositoryImpl<E, ID, V>
    implements ReferentialRepository<ID, E, V, F, O>, ReferentialSpecifications<ID, E> {

    public ReferentialRepositoryImpl(Class<E> domainClass, Class<V> voClass, EntityManager entityManager) {
        super(domainClass, voClass, entityManager);
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
    public List<V> findAll(F filter, O fetchOptions) {
        return findAll(toSpecification(filter)).stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public List<V> findAll(F filter, net.sumaris.core.dao.technical.Page page, O fetchOptions) {
        Specification<E> spec = filter != null ? toSpecification(filter, fetchOptions) : null;
        TypedQuery<E> query = getQuery(spec, page, getDomainClass());

        // Add hints
        configureQuery(query, page, fetchOptions);

        return streamQuery(query)
            .map(entity -> toVO(entity, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAll(F filter, Pageable pageable, O fetchOptions) {
        return findAll(toSpecification(filter), pageable)
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public Page<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, O fetchOptions) {
        return findAll(Pageables.create(offset, size, sortAttribute, sortDirection))
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public Page<V> findAll(F filter, int offset, int size, String sortAttribute, SortDirection sortDirection, O fetchOptions) {
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
    public List<V> findAllAsVO(Specification<E> spec, O fetchOptions) {
        return super.findAll(spec).stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAllAsVO(Specification<E> spec, Pageable pageable, O fetchOptions) {
        return super.findAll(spec, pageable)
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public long count(F filter) {
        return count(toSpecification(filter));
    }

    @Override
    public V get(ID id) {
        return toVO(this.getById(id));
    }

    @Override
    public V get(ID id, O fetchOptions) {
        return toVO(this.getById(id), fetchOptions);
    }

    @Override
    public V getByLabel(String label) {
        return getByLabel(label, null);
    }

    @Override
    public V getByLabel(String label, O fetchOption) {
        return findByLabel(label, fetchOption).orElseThrow(() ->
            new EntityNotFoundException(String.format("%s entity with label '%s' not found", getDomainClass().getSimpleName(), label))
        );
    }

    @Override
    public Optional<V> findVOById(ID id) {
        return findVOById(id, null);
    }

    @Override
    public Optional<V> findVOById(ID id, O fetchOptions) {
        return super.findById(id).map(entity -> toVO(entity, fetchOptions));
    }

    @Override
    public Optional<V> findByLabel(String label) {
        return findByLabel(label, null);
    }

    @Override
    public Optional<V> findByLabel(String label, O fetchOptions) {
        List<E> result = findAll(BindableSpecification.where(hasLabel(label)));
        if (CollectionUtils.isEmpty(result)) {
            return Optional.empty();
        } else {
            if (result.size() > 1) {
                log.warn(String.format("%s entity with label '%s' -> more than 1 occurrence (%s found). Returning the first one",
                    getDomainClass().getSimpleName(), label, result.size()));
            }
            return Optional.of(result.get(0)).map(e -> toVO(e, fetchOptions));
        }
    }

    @Override
    public void toEntity(V source, E target, boolean copyIfNull) {
        // copy properties
        super.toEntity(source, target, copyIfNull);

        // Creation date
        if (target.getId() == null || target.getCreationDate() == null) {
            target.setCreationDate(new Date());
        }

        // Status
        if (copyIfNull || source.getStatusId() != null) {
            Daos.setEntityProperty(getEntityManager(), target, IWithStatusEntity.Fields.STATUS, Status.class, source.getStatusId());
        }

        // Validity status
        if (target instanceof IWithValidityStatusEntity) {
            Integer validityStatusId = Beans.getProperty(source, ReferentialVO.Fields.VALIDITY_STATUS_ID);
            if (copyIfNull || validityStatusId != null) {
                Daos.setEntityProperty(getEntityManager(), target, IWithValidityStatusEntity.Fields.VALIDITY_STATUS, ValidityStatus.class, validityStatusId);
            }
        }

    }

    @Override
    protected void onAfterSaveEntity(V vo, E savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        if (isNew) {
            // recopy creation date
            vo.setCreationDate(savedEntity.getCreationDate());
        }
    }

    @Override
    public V toVO(E source) {
        return toVO(source, null);
    }

    protected V toVO(E source, O fetchOptions) {
        if (source == null) return null;
        V target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    protected void toVO(E source, V target, O fetchOptions, boolean copyIfNull) {
        Beans.copyProperties(source, target);
        target.setStatusId(source.getStatus().getId());
    }

    @Override
    public V createVO() {
        try {
            return getVOClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected final Specification<E> toSpecification(F filter) {
        return toSpecification(filter, null);
    }

    protected Specification<E> toSpecification(@NonNull F filter, O fetchOptions) {
        // Special case when filtering by ID:
        if (filter.getId() != null) {
            return BindableSpecification.where(hasId(filter.getId()));
        }
        Class<E> clazz = getDomainClass();

        // default specification
        return BindableSpecification
            .where(inStatusIds(filter.getStatusIds()))
            .and(hasLabel(filter.getLabel()))
            .and(inLevelIds(clazz, filter.getLevelId() != null ? new Integer[]{filter.getLevelId()} : filter.getLevelIds()))
            .and(inLevelLabels(clazz, filter.getLevelLabel() != null ? new String[]{filter.getLevelLabel()} : filter.getLevelLabels()))
            .and(searchOrJoinSearchText(filter))
            .and(inSearchJoinLevelIds(filter.getSearchJoin(), filter.getSearchJoinLevelIds()))
            .and(includedIds(filter.getIncludedIds()))
            .and(excludedIds(filter.getExcludedIds()));
    }

    protected void configureQuery(TypedQuery<E> query, net.sumaris.core.dao.technical.Page page, @Nullable O fetchOptions) {
        // Can be override by subclasses
    }
}
