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
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SoftwareDao;
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
import net.sumaris.core.util.crypto.MD5Util;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.technical.SoftwareVO;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private ReferentialDao referentialDao;

    @Autowired
    private SoftwareDao softwareDao;

    @Autowired
    private ApplicationEventPublisher publisher;

    Map<String, String> userProfileEnumNameByLabel = new HashMap<>();
    Map<String, String> userProfileLabelByEnumName = new HashMap<>();

    protected PersonRepositoryImpl(EntityManager entityManager) {
        super(Person.class, PersonVO.class, entityManager);
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        initUserProfileConversionMaps(event.getConfiguration());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PERSON_BY_ID, key = "#id", unless="#result==null")
    public Optional<PersonVO> findById(int id) {
        return super.findById(id).map(this::toVO);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PERSON_BY_PUBKEY, key = "#pubkey", unless="#result==null")
    public Optional<PersonVO> findByPubkey(@NonNull String pubkey) {
        return findAll(hasPubkey(pubkey)).stream().findFirst().map(this::toVO);
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
            .and(searchText(new String[]{
                    Person.Fields.PUBKEY,
                    Person.Fields.EMAIL,
                    Person.Fields.FIRST_NAME,
                    Person.Fields.LAST_NAME
                },
                Daos.getEscapedSearchText(filter.getSearchText(), true)))
            .and(excludedIds(filter.getExcludedIds()))
            ;
    }

    @Override
    public void toVO(Person source, PersonVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        // Department
        if (source.getDepartment() == null || source.getDepartment().getId() == null) {
            if (copyIfNull) {
                target.setDepartment(null);
            }
        }
        else {
            DepartmentVO department = departmentRepository.get(source.getDepartment().getId());
            target.setDepartment(department);
        }

        // Status
        target.setStatusId(source.getStatus().getId());

        // Profiles (keep only label)
        if (CollectionUtils.isNotEmpty(source.getUserProfiles())) {
            List<String> profiles = source.getUserProfiles().stream()
                .map(profile -> userProfileEnumNameByLabel.getOrDefault(profile.getLabel(), profile.getLabel()))
                .collect(Collectors.toList());
            target.setProfiles(profiles);
        }

        // Has avatar
        target.setHasAvatar(source.getAvatar() != null);

    }

    @Override
    @Caching(put = {
        @CachePut(cacheNames= CacheNames.PERSON_BY_ID, key="#vo.id", condition = "#vo != null && #vo.id != null"),
        @CachePut(cacheNames= CacheNames.PERSON_BY_PUBKEY, key="#vo.pubkey", condition = "#vo != null && #vo.id != null && #vo.pubkey != null")
    })
    public PersonVO save(PersonVO vo) {
        Preconditions.checkNotNull(vo);
        Preconditions.checkNotNull(vo.getEmail(), "Missing 'email'");
        Preconditions.checkNotNull(vo.getStatusId(), "Missing 'statusId'");
        Preconditions.checkNotNull(vo.getDepartment(), "Missing 'department'");
        Preconditions.checkNotNull(vo.getDepartment().getId(), "Missing 'department.id'");

        Person entity = toEntity(vo);

        boolean isNew = entity.getId() == null;
        if (isNew) {
            entity.setCreationDate(new Date());
        }

        // If new
        if (isNew) {
            // Set default status to Temporary
            if (vo.getStatusId() == null) {
                vo.setStatusId(StatusEnum.TEMPORARY.getId());
            }
        }
        // If update
        else {

            // Check update date
            Daos.checkUpdateDateForUpdate(vo, entity);

            // Lock entityName
            lockForUpdate(entity);
        }

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        Person savedEntity = save(entity);

        // Update VO
        onAfterSaveEntity(vo, savedEntity, isNew);

        return vo;

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
                target.setDepartment(load(Department.class, source.getDepartment().getId()));
            }
        }

        // Status
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            } else {
                target.setStatus(load(Status.class, source.getStatusId()));
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
                        // translate the user profile label
                        String translatedLabel = userProfileLabelByEnumName.getOrDefault(profile, profile);
                        if (StringUtils.isNotBlank(translatedLabel)) {
                            Optional<ReferentialVO> userProfileVO = referentialDao.findByUniqueLabel(UserProfile.class.getSimpleName(), translatedLabel);
                            if (userProfileVO.isPresent()) {
                                UserProfile up = load(
                                    UserProfile.class,
                                    userProfileVO.get().getId()
                                );
                                target.getUserProfiles().add(up);
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PERSON_BY_ID, key = "#id"),
        @CacheEvict(cacheNames = CacheNames.PERSON_BY_PUBKEY, allEntries = true)
    })
    public void deleteById(Integer id) {
        log.debug(String.format("Deleting person {id=%s}...", id));

        super.deleteById(id);

        // Emit delete person event
        publisher.publishEvent(new EntityDeleteEvent(id, Person.class.getSimpleName(), null));
    }

    /**
     * Create maps to convert UserProfileEnum.label into UserProfile.label
     * map from software properties 'sumaris.userProfile.<ENUM>.label' (<ENUM> is one of the UserProfileEnum name)
     */
    private void initUserProfileConversionMaps(SumarisConfiguration configuration) {
        Map<String, String> userProfileEnumNameByLabel = new HashMap<>();
        Map<String, String> userProfileLabelByEnumName = new HashMap<>();

        Pattern userProfilePropertyPattern = Pattern.compile("sumaris.userProfile.(\\w+).label");

        SoftwareVO software = this.softwareDao.findByLabel(configuration.getAppName()).orElse(null);
        if (software != null && MapUtils.isNotEmpty(software.getProperties())) {

            // Check if there is overrided user profile labels
            software.getProperties().forEach((key, value) -> {
                Matcher matcher = userProfilePropertyPattern.matcher(key);
                if (StringUtils.isNotBlank(value) && matcher.find()) {
                    String enumName = matcher.group(1);
                    userProfileEnumNameByLabel.put(value, enumName);
                    userProfileLabelByEnumName.put(enumName, value);
                }
            });
        }

        this.userProfileEnumNameByLabel = userProfileEnumNameByLabel;
        this.userProfileLabelByEnumName = userProfileLabelByEnumName;
    }

}
