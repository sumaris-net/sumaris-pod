package net.sumaris.core.dao.administration.user;

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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.UserProfile;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.util.crypto.MD5Util;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonFetchOptions;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author peck7 on 20/08/2020.
 */
@Slf4j
public class PersonRepositoryImpl
    extends SumarisJpaRepositoryImpl<Person, Integer, PersonVO>
    implements PersonSpecifications {

    @Autowired
    protected DepartmentRepository departmentRepository;

    @Autowired
    private ApplicationEventPublisher publisher;

    protected PersonRepositoryImpl(EntityManager entityManager) {
        super(Person.class, PersonVO.class, entityManager);
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        // Force clear cache, because UserProfileEnum can have changed, to VO profiles can changed also
        clearCache();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_PUBKEY, allEntries = true)
    })
    public void clearCache() {
        log.debug("Cleaning Person's cache...");
    }

    @Override
    public PersonVO get(int id) {
        return findById(id).orElseThrow(() -> new DataRetrievalFailureException("Cannot load person with id=" + id));
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PERSON_BY_ID, key = "#id", unless="#result==null")
    public Optional<PersonVO> findById(int id) {
        return super.findById(id).map(this::toVO);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PERSON_BY_PUBKEY, key = "#pubkey", unless="#result==null")
    public Optional<PersonVO> findByPubkey(@NonNull String pubkey) {
        return findAll(hasPubkey(pubkey)).stream().findFirst().map(this::toVO);
    }

    @Override
    public Optional<PersonVO> findByUsername(String username) {
        return findAll(hasUsername(username)).stream().filter(p -> StatusEnum.ENABLE.getId().equals(p.getStatus().getId())).findFirst().map(this::toVO);
    }

    @Override
    public List<PersonVO> findByFilter(PersonFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        return findAll(toSpecification(filter), Pageables.create(offset, size, sortAttribute, sortDirection))
            .stream()
            .map(this::toVO)
            .collect(Collectors.toList());

    }

    @Override
    public long countByFilter(PersonFilterVO filter) {
        return count(toSpecification(filter));
    }

    @Override
    public List<String> getEmailsByProfiles(List<Integer> userProfileIds, List<Integer> statusIds) {

        // Build filter
        PersonFilterVO filter = PersonFilterVO.builder()
                .userProfileIds(userProfileIds != null ? userProfileIds.toArray(new Integer[0]) : null)
                .statusIds(statusIds != null ? statusIds.toArray(new Integer[0]) : null)
                .build();

        return findAll(toSpecification(filter)).stream()
            .map(Person::getEmail)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // not used
    protected List<PersonVO> findByFilter(PersonFilterVO filter, Pageable pageable) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(pageable);

        TypedQuery<Person> query = getQuery(toSpecification(filter), pageable);

        return query.getResultStream()
            .map(this::toVO)
            .collect(Collectors.toList());

    }

    protected Specification<Person> toSpecification(PersonFilterVO filter) {

        return BindableSpecification
            .where(inStatusIds(filter))
            .and(hasUserProfileIds(filter))
            .and(hasPubkey(filter.getPubkey()))
            .and(hasEmail(filter.getEmail()))
            .and(hasFirstName(filter.getFirstName()))
            .and(hasLastName(filter.getLastName()))
            .and(searchText(filter))
            .and(includedIds(filter.getIncludedIds()))
            .and(excludedIds(filter.getExcludedIds()));
    }

    @Override
    public PersonVO toVO(Person source, PersonFetchOptions fetchOptions) {
        PersonVO target = createVO();
        toVO(source, target, PersonFetchOptions.nullToDefault(fetchOptions), true);
        return target;
    }

    @Override
    public void toVO(Person source, PersonVO target, boolean copyIfNull) {
        toVO(source, target, PersonFetchOptions.DEFAULT, copyIfNull);
    }

    @Override
    @Caching(put = {
        @CachePut(cacheNames= CacheConfiguration.Names.PERSON_BY_ID, key="#source.id", condition = "#source != null && #source.id != null"),
        @CachePut(cacheNames= CacheConfiguration.Names.PERSON_BY_PUBKEY, key="#source.pubkey", condition = "#source != null && #source.id != null && #source.pubkey != null")
    })
    public PersonVO save(PersonVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getEmail(), "Missing 'email'");
        Preconditions.checkNotNull(source.getStatusId(), "Missing 'statusId'");
        Preconditions.checkNotNull(source.getDepartment(), "Missing 'department'");
        Preconditions.checkNotNull(source.getDepartment().getId(), "Missing 'department.id'");

        Person entity = toEntity(source);

        boolean isNew = entity.getId() == null;
        if (isNew) {
            entity.setCreationDate(new Date());
        }

        // If new
        if (isNew) {
            // Set default status to Temporary
            if (source.getStatusId() == null) {
                source.setStatusId(StatusEnum.TEMPORARY.getId());
            }
        }
        // If update
        else {

            // Check update date
            Daos.checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity);
        }

        // Update update_dt
        Date newUpdateDate = getDatabaseCurrentDate();
        entity.setUpdateDate(newUpdateDate);

        Person savedEntity = save(entity);

        // Update VO
        onAfterSaveEntity(source, savedEntity, isNew);

        return source;

    }

    @Override
    protected void onAfterSaveEntity(PersonVO vo, Person savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
        if (isNew) {
            vo.setCreationDate(savedEntity.getCreationDate());
        }

        // Publish event
        if (isNew) {
            publisher.publishEvent(new EntityInsertEvent(vo.getId(), Person.class.getSimpleName(), vo));
        } else {
            publisher.publishEvent(new EntityUpdateEvent(vo.getId(), Person.class.getSimpleName(), vo));
        }
    }

    @Override
    public void toEntity(PersonVO source, Person target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Email
        if (StringUtils.isNotBlank(source.getEmail())) {
            target.setEmailMD5(MD5Util.md5Hex(source.getEmail()));
        }

        // Department
        if (copyIfNull || source.getDepartment() != null) {
            if (source.getDepartment() == null) {
                target.setDepartment(null);
            } else {
                target.setDepartment(getReference(Department.class, source.getDepartment().getId()));
            }
        }

        // Status
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            } else {
                target.setStatus(getReference(Status.class, source.getStatusId()));
            }
        }

        // User profiles
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getProfiles())) {
            if (CollectionUtils.isEmpty(source.getProfiles())) {
                target.getUserProfiles().clear();
            } else {
                target.getUserProfiles().clear();
                for (String profile : source.getProfiles()) {
                    if (StringUtils.isNotBlank(profile)) {
                        UserProfileEnum.findByName(profile).ifPresent(userProfileEnum -> {
                            UserProfile up = getReference(UserProfile.class, userProfileEnum.getId());
                            target.getUserProfiles().add(up);
                        });
                    }
                }
            }
        }

    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_ID, key = "#id"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_PUBKEY, allEntries = true)
    })
    public void deleteById(Integer id) {
        log.debug(String.format("Deleting person {id=%s}...", id));

        // Load first (need by event), and mark as deleted
        PersonVO deletedPerson = get(id);
        deletedPerson.setStatusId(StatusEnum.DELETED.getId());

        super.deleteById(id);

        // Emit delete person event
        publisher.publishEvent(new EntityDeleteEvent(id, Person.class.getSimpleName(), deletedPerson));
    }

    protected void toVO(Person source, PersonVO target, PersonFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        // Department
        if (fetchOptions.isWithDepartment()) {
            if (source.getDepartment() == null || source.getDepartment().getId() == null) {
                if (copyIfNull) {
                    target.setDepartment(null);
                }
            } else {
                DepartmentVO department = departmentRepository.get(source.getDepartment().getId());
                target.setDepartment(department);
            }
        }

        // Status
        target.setStatusId(source.getStatus().getId());

        // Profiles (keep only label)
        if (fetchOptions.isWithUserProfiles()) {
            if (CollectionUtils.isNotEmpty(source.getUserProfiles())) {
                List<String> profiles = source.getUserProfiles().stream()
                    .map(UserProfile::getLabel)
                    // Convert DB label into name of UserProfileEnum
                    .map(label -> UserProfileEnum.findByLabel(label).orElse(null))
                    .filter(Objects::nonNull)
                    .map(Enum::name)
                    .collect(Collectors.toList());
                target.setProfiles(profiles);
            }
        }

        // Has avatar
        target.setHasAvatar(source.getAvatar() != null);

    }
}
