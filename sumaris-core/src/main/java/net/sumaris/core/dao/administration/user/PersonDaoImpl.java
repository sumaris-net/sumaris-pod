package net.sumaris.core.dao.administration.user;

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
import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.data.ImageAttachmentDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.SoftwareDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.UserProfile;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.crypto.MD5Util;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Repository("personDao")
public class PersonDaoImpl extends HibernateDaoSupport implements PersonDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(PersonDaoImpl.class);

    private List<Listener> listeners = new CopyOnWriteArrayList<>();

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private ImageAttachmentDao imageAttachmentDao;

    @Autowired
    private SoftwareDao softwareDao;

    @Autowired
    private ReferentialDao referentialDao;

    @Override
    @SuppressWarnings("unchecked")
    public List<PersonVO> findByFilter(PersonFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Person> query = builder.createQuery(Person.class);
        Root<Person> root = query.from(Person.class);
        Join<Person, UserProfile> upJ = root.join(Person.Fields.USER_PROFILES, JoinType.LEFT);

        query.select(root).distinct(true);

        addSorting(query, builder, root, sortAttribute, sortDirection);

        return toPersonVOs(createPersonQuery(builder,query, root, upJ, filter)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultList());
    }


    @Override
    public Long countByFilter(PersonFilterVO filter) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<Person> root = query.from(Person.class);
        Join<Person, UserProfile> upJ = root.join(Person.Fields.USER_PROFILES, JoinType.LEFT);

        query.select(builder.count(root));

        return createPersonQuery(builder, query, root, upJ, filter)
                .getSingleResult();
    }

    @Override
    public PersonVO getByPubkeyOrNull(String pubkey) {
        return toPersonVO(getEntityByPubkeyOrNull(pubkey));
    }

    @Override
    public ImageAttachmentVO getAvatarByPubkey(String pubkey) {

        Person person = getEntityByPubkeyOrNull(pubkey);
        if (person == null || person.getAvatar() == null) {
            throw new DataRetrievalFailureException(I18n.t("sumaris.error.person.avatar.notFound"));
        }

        return imageAttachmentDao.get(person.getAvatar().getId());
    }

    @Override
    public List<String> getEmailsByProfiles(List<Integer> userProfiles, List<Integer> statusIds) {
        Preconditions.checkNotNull(userProfiles);
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(userProfiles));
        Preconditions.checkNotNull(statusIds);
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(statusIds));

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Person> query = builder.createQuery(Person.class);
        Root<Person> root = query.from(Person.class);

        Join<Person, UserProfile> upJ = root.join(Person.Fields.USER_PROFILES, JoinType.INNER);

        ParameterExpression<Collection> userProfileIdParam = builder.parameter(Collection.class);
        ParameterExpression<Collection> statusIdsParam = builder.parameter(Collection.class);

        query.select(root/*.get(Person.PROPERTY_EMAIL)*/).distinct(true)
                .where(
                        builder.and(
                                // user profile Ids
                                upJ.get(UserProfile.Fields.ID).in(userProfileIdParam),
                                // status Ids
                                root.get(Person.Fields.STATUS).get(Status.Fields.ID).in(statusIdsParam)
                        ));

        // TODO: select email column only
        return entityManager.createQuery(query)
                .setParameter(userProfileIdParam, userProfiles)
                .setParameter(statusIdsParam, ImmutableList.copyOf(statusIds))
                .setMaxResults(100)
                .getResultList()
                .stream()
                .map(Person::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isExistsByEmailHash(final String hash) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<Person> root = query.from(Person.class);

        ParameterExpression<String> hashParam = builder.parameter(String.class);

        query.select(builder.count(root.get(Person.Fields.ID)))
             .where(builder.equal(root.get(Person.Fields.EMAIL_M_D5), hashParam));

        return getEntityManager().createQuery(query)
                .setParameter(hashParam, hash)
                .getSingleResult() > 0;
    }

    @Override
    public PersonVO get(int id) {
        return toPersonVO(get(Person.class, id));
    }

    @Override
    public PersonVO save(PersonVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getEmail(), "Missing 'email'");
        Preconditions.checkNotNull(source.getStatusId(), "Missing 'statusId'");
        Preconditions.checkNotNull(source.getDepartment(), "Missing 'department'");
        Preconditions.checkNotNull(source.getDepartment().getId(), "Missing 'department.id'");

        EntityManager entityManager = getEntityManager();
        Person entity = null;
        if (source.getId() != null) {
            entity = get(Person.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Person();
        }

        // If new
        if (isNew) {
            // Set default status to Temporary
            if (source.getStatusId() == null) {
                source.setStatusId(config.getStatusIdTemporary());
            }
        }
        // If update
        else {

            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity, LockModeType.PESSIMISTIC_WRITE);
        }

        personVOToEntity(source, entity, true);

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

        getEntityManager().flush();
        getEntityManager().clear();

        // Emit event to listeners
        emitSaveEvent(source);

        return source;
    }

    @Override
    public void delete(int id) {
        log.debug(String.format("Deleting person {id=%s}...", id));
        delete(Person.class, id);

        // Emit to listener
        emitDeleteEvent(id);
    }

    @Override
    public PersonVO toPersonVO(Person source) {
        if (source == null) return null;
        PersonVO target = new PersonVO();

        Beans.copyProperties(source, target);

        // Department
        DepartmentVO department = departmentDao.get(source.getDepartment().getId());
        target.setDepartment(department);

        // Status
        target.setStatusId(source.getStatus().getId());

        // Profiles (keep only label)
        if (CollectionUtils.isNotEmpty(source.getUserProfiles())) {
            List<String> profiles = source.getUserProfiles().stream()
                    .map(profile -> getUserProfileLabelTranslationMap(true).getOrDefault(profile.getLabel(), profile.getLabel()))
                    .collect(Collectors.toList());
            target.setProfiles(profiles);
        }

        // Has avatar
        target.setHasAvatar(source.getAvatar() != null);

        return target;
    }

    @Override
    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }


    /* -- protected methods -- */


    private <R> TypedQuery<R> createPersonQuery(CriteriaBuilder builder,
                                                CriteriaQuery<R> query,
                                                Root<Person> root,
                                                Join<Person, UserProfile> upJ,
                                                PersonFilterVO filter) {
        EntityManager entityManager = getEntityManager();

        ParameterExpression<Boolean> hasUserProfileIdsParam = builder.parameter(Boolean.class);
        ParameterExpression<Collection> userProfileIdsParam = builder.parameter(Collection.class);
        ParameterExpression<Boolean> hasStatusIdsParam = builder.parameter(Boolean.class);
        ParameterExpression<Collection> statusIdsParam = builder.parameter(Collection.class);
        ParameterExpression<String> pubkeyParam = builder.parameter(String.class);
        ParameterExpression<String> firstNameParam = builder.parameter(String.class);
        ParameterExpression<String> lastNameParam = builder.parameter(String.class);
        ParameterExpression<String> emailParam = builder.parameter(String.class);
        ParameterExpression<String> searchTextParam = builder.parameter(String.class);

        // Prepare status ids
        Collection<Integer> statusIds = ArrayUtils.isEmpty(filter.getStatusIds()) ?
                null : ImmutableList.copyOf(filter.getStatusIds());

        // Prepare user profile ids
        Collection<Integer> userProfileIds;
        if (ArrayUtils.isNotEmpty(filter.getUserProfiles())) {
            userProfileIds = Arrays.stream(filter.getUserProfiles())
                    .map(UserProfileEnum::valueOf)
                    .map(profile -> profile.id)
                    .collect(Collectors.toList());
        }
        else if (ArrayUtils.isNotEmpty(filter.getUserProfileIds())) {
            userProfileIds = ImmutableList.copyOf(filter.getUserProfileIds());
        }
        else if (filter.getUserProfileId() != null) {
            userProfileIds = ImmutableList.of(filter.getUserProfileId());
        }
        else {
            userProfileIds = null;
        }

        query.where(
                builder.and(
                        // user profile Ids
                        builder.or(
                                builder.isFalse(hasUserProfileIdsParam),
                                upJ.get(UserProfile.Fields.ID).in(userProfileIdsParam)
                        ),
                        // status Ids
                        builder.or(
                                builder.isFalse(hasStatusIdsParam),
                                root.get(Person.Fields.STATUS).get(Status.Fields.ID).in(statusIdsParam)
                        ),
                        // pubkey
                        builder.or(
                                builder.isNull(pubkeyParam),
                                builder.equal(root.get(Person.Fields.PUBKEY), pubkeyParam)
                        ),
                        // email
                        builder.or(
                                builder.isNull(emailParam),
                                builder.equal(root.get(Person.Fields.EMAIL), emailParam)
                        ),
                        // firstName
                        builder.or(
                                builder.isNull(firstNameParam),
                                builder.equal(builder.upper(root.get(Person.Fields.FIRST_NAME)), builder.upper(firstNameParam))
                        ),
                        // lastName
                        builder.or(
                                builder.isNull(lastNameParam),
                                builder.equal(builder.upper(root.get(Person.Fields.LAST_NAME)), builder.upper(lastNameParam))
                        ),
                        // search text
                        builder.or(
                                builder.isNull(searchTextParam),
                                builder.like(builder.upper(root.get(Person.Fields.PUBKEY)), builder.upper(searchTextParam)),
                                builder.like(builder.upper(root.get(Person.Fields.EMAIL)), builder.upper(searchTextParam)),
                                builder.like(builder.upper(root.get(Person.Fields.FIRST_NAME)), builder.upper(searchTextParam)),
                                builder.like(builder.upper(root.get(Person.Fields.LAST_NAME)), builder.upper(searchTextParam))
                        )
                ));

        String searchText = StringUtils.trimToNull(filter.getSearchText());
        String searchTextAnyMatch = null;
        if (StringUtils.isNotBlank(searchText)) {
            searchTextAnyMatch = ("*" + searchText + "*"); // add trailing escape char
            searchTextAnyMatch = searchTextAnyMatch.replaceAll("[*]+", "*"); // group escape chars
            searchTextAnyMatch = searchTextAnyMatch.replaceAll("[%]", "\\%"); // protected '%' chars
            searchTextAnyMatch = searchTextAnyMatch.replaceAll("[*]", "%"); // replace asterix
        }


        return entityManager.createQuery(query)
                .setParameter(hasUserProfileIdsParam, CollectionUtils.isNotEmpty(userProfileIds))
                .setParameter(userProfileIdsParam,  userProfileIds)
                .setParameter(hasStatusIdsParam, CollectionUtils.isNotEmpty(statusIds))
                .setParameter(statusIdsParam, statusIds)
                .setParameter(pubkeyParam, filter.getPubkey())
                .setParameter(emailParam, filter.getEmail())
                .setParameter(firstNameParam, filter.getFirstName())
                .setParameter(lastNameParam, filter.getLastName())
                .setParameter(searchTextParam, searchTextAnyMatch);
    }

    protected List<PersonVO> toPersonVOs(List<Person> source) {
        return source.stream()
                .map(this::toPersonVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void personVOToEntity(PersonVO source, Person target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Email
        if (StringUtils.isNotBlank(source.getEmail())) {
            target.setEmailMD5(MD5Util.md5Hex(source.getEmail()));
        }

        // Department
        if (copyIfNull || source.getDepartment() != null) {
            if (source.getDepartment() == null) {
                target.setDepartment(null);
            }
            else {
                target.setDepartment(load(Department.class, source.getDepartment().getId()));
            }
        }

        // Status
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            }
            else {
                target.setStatus(load(Status.class, source.getStatusId()));
            }
        }

        // User profiles
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getProfiles())) {
            if (CollectionUtils.isEmpty(source.getProfiles())) {
                target.getUserProfiles().clear();
            }
            else {
                target.getUserProfiles().clear();
                for (String profile: source.getProfiles()) {
                    if (StringUtils.isNotBlank(profile)) {
                        // translate the user profile label
                        String translatedLabel = getUserProfileLabelTranslationMap(false).getOrDefault(profile, profile);
                        if (StringUtils.isNotBlank(translatedLabel)) {
                            ReferentialVO userProfileVO = referentialDao.findByUniqueLabel("UserProfile", translatedLabel);
                            UserProfile up = load(UserProfile.class, userProfileVO.getId());
                            target.getUserProfiles().add(up);
                        }
                    }
                }
            }
        }

    }


    protected Person getEntityByPubkeyOrNull(String pubkey) {

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Person> query = builder.createQuery(Person.class);
        Root<Person> root = query.from(Person.class);

        ParameterExpression<String> pubkeyParam = builder.parameter(String.class);

        query.select(root)
                .where(builder.equal(root.get(PersonVO.Fields.PUBKEY), pubkeyParam));

        try {
            return entityManager.createQuery(query)
                    .setParameter(pubkeyParam, pubkey)
                    .getSingleResult();
        } catch (EmptyResultDataAccessException | NoResultException e) {
            return null;
        }
    }

    protected void emitSaveEvent(final PersonVO person) {
        listeners.forEach(l -> {
            try {
                l.onSave(person);
            } catch(Throwable t) {
                log.error("Person listener (onSave) error: " + t.getMessage(), t);
                // Continue, to avoid transaction cancellation
            }
        });
    }

    protected void emitDeleteEvent(final int id) {
        listeners.forEach(l -> {
            try {
                l.onDelete(id);
            } catch(Throwable t) {
                log.error("Person listener (onDelete) error: " + t.getMessage(), t);
                // Continue, to avoid transaction cancellation
            }
        });
    }

    /**
     * Create a translate map from software properties 'sumaris.userProfile.<ENUM>.label' (<ENUM> is one of the UserProfileEnum name)
     *
     * @param toVO if true, the map key is the translated label, the value is the enum label; if false, it's inverted
     * @return the translation map
     */
    private Map<String, String> getUserProfileLabelTranslationMap(boolean toVO) {
        Map<String, String> translateMap = new HashMap<>();
        Pattern pattern = Pattern.compile("sumaris.userProfile.(\\w+).label");
        softwareDao.get(config.getAppName()).getProperties().forEach((key, value) -> {
            Matcher matcher = pattern.matcher(key);
            if (value != null && matcher.find()) {
                if (toVO)
                    translateMap.put(value, matcher.group(1));
                else
                    translateMap.put(matcher.group(1), value);
            }
        });
        return translateMap;
    }
}
