package net.sumaris.core.dao.administration.programStrategy;

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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Repository("programDao")
public class ProgramDaoImpl extends HibernateDaoSupport implements ProgramDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ProgramDaoImpl.class);

    @Autowired
    private TaxonGroupRepository taxonGroupRepository;

    @Autowired
    private ReferentialDao referentialDao;

    @PostConstruct
    protected void init() {
        Arrays.stream(ProgramEnum.values()).forEach(programEnum -> {
            try {
                ProgramVO program = getByLabel(programEnum.name());
                if (program != null) {
                    programEnum.setId(program.getId());
                } else {
                    // TODO query by id and show program code/name
                    log.warn("Missing program with label=" + programEnum.name());
                }
            } catch(Throwable t) {
                log.error(String.format("Could not initialized enumeration for program {%s}: %s", programEnum.name(), t.getMessage()), t);
            }
        });
    }

    @Override
    public List<ProgramVO> getAll() {
        CriteriaQuery<Program> query = entityManager.getCriteriaBuilder().createQuery(Program.class);
        Root<Program> root = query.from(Program.class);

        query.select(root);

        return getEntityManager()
                .createQuery(query)
                .getResultStream()
                .map(this::toProgramVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProgramVO> findByFilter(ProgramFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Program> query = builder.createQuery(Program.class);
        Root<Program> root = query.from(Program.class);

        Join<Program, ProgramProperty> upJ = root.join(Program.Fields.PROPERTIES, JoinType.LEFT);

        ParameterExpression<String> withPropertyParam = builder.parameter(String.class);
        ParameterExpression<Collection> statusIdsParam = builder.parameter(Collection.class);
        ParameterExpression<String> searchTextParam = builder.parameter(String.class);

        query.select(root).distinct(true)
                .where(
                        builder.and(
                                // property
                                builder.or(
                                        builder.isNull(withPropertyParam),
                                        builder.equal(upJ.get(ProgramProperty.Fields.LABEL), withPropertyParam)
                                ),
                                // status Ids
                                builder.or(
                                        builder.isNull(statusIdsParam),
                                        root.get(Program.Fields.STATUS).get(IReferentialEntity.Fields.ID).in(statusIdsParam)
                                ),
                                // search text
                                builder.or(
                                        builder.isNull(searchTextParam),
                                        builder.like(builder.upper(root.get(Program.Fields.LABEL)), builder.upper(searchTextParam)),
                                        builder.like(builder.upper(root.get(Program.Fields.NAME)), builder.upper(searchTextParam))
                                )
                        ));

        if (StringUtils.isNotBlank(sortAttribute)) {
            if (sortDirection == SortDirection.ASC) {
                query.orderBy(builder.asc(root.get(sortAttribute)));
            } else {
                query.orderBy(builder.desc(root.get(sortAttribute)));
            }
        }

        String searchText = StringUtils.trimToNull(filter.getSearchText());
        String searchTextAnyMatch = null;
        if (StringUtils.isNotBlank(searchText)) {
            searchTextAnyMatch = ("*" + searchText + "*"); // add trailing escape char
            searchTextAnyMatch = searchTextAnyMatch.replaceAll("[*]+", "*"); // group escape chars
            searchTextAnyMatch = searchTextAnyMatch.replaceAll("[%]", "\\%"); // protected '%' chars
            searchTextAnyMatch = searchTextAnyMatch.replaceAll("[*]", "%"); // replace asterix
        }

        return entityManager.createQuery(query)
                .setParameter(withPropertyParam, filter.getWithProperty())
                .setParameter(statusIdsParam, filter.getStatusIds())
                .setParameter(searchTextParam, searchTextAnyMatch)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(this::toProgramVO)
                .collect(Collectors.toList());
    }

    @Override
    public ProgramVO get(final int id) {
        return toProgramVO(get(Program.class, id));
    }

    @Override
    public ProgramVO getByLabel(String label) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Program> query = builder.createQuery(Program.class);
        Root<Program> root = query.from(Program.class);

        ParameterExpression<String> labelParam = builder.parameter(String.class);

        query.select(root)
                .where(builder.equal(root.get(Program.Fields.LABEL), labelParam));

        TypedQuery<Program> q = getEntityManager().createQuery(query)
                .setParameter(labelParam, label);
        try {
            return toProgramVO(q.getSingleResult());
        } catch(EmptyResultDataAccessException | NoResultException e) {
            return null;
        }
    }


    @Override
    public ProgramVO save(ProgramVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel(), "Missing 'label'");
        Preconditions.checkNotNull(source.getName(), "Missing 'name'");
        Preconditions.checkNotNull(source.getStatusId(), "Missing 'statusId'");

        EntityManager entityManager = getEntityManager();
        Program entity = null;
        if (source.getId() != null) {
            entity = get(Program.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Program();
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

        programVOToEntity(source, entity, true);

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
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        // Save properties
        saveProperties(source.getProperties(), entity, newUpdateDate);

        getEntityManager().flush();
        getEntityManager().clear();

        // Emit event to listeners
        //emitSaveEvent(source);

        return source;
    }

    @Override
    public List<ReferentialVO> getGears(int programId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Gear> query = builder.createQuery(Gear.class);
        Root<Gear> root = query.from(Gear.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Gear, Strategy> gearInnerJoin = root.joinList(Gear.Fields.STRATEGIES, JoinType.INNER);

        query.select(root)
                .where(
                        builder.and(
                                // program
                                builder.equal(gearInnerJoin.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(Gear.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(Gear.Fields.LABEL)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(referentialDao::toReferentialVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaxonGroupVO> getTaxonGroups(int programId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TaxonGroup> query = builder.createQuery(TaxonGroup.class);
        Root<TaxonGroup> root = query.from(TaxonGroup.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<TaxonGroup, TaxonGroupStrategy> innerJoinTGS = root.joinList(TaxonGroup.Fields.STRATEGIES, JoinType.INNER);
        Join<TaxonGroupStrategy, Strategy> innerJoinS = innerJoinTGS.join(TaxonGroupStrategy.Fields.STRATEGY, JoinType.INNER);


        query.select(root)
                .where(
                        builder.and(
                                // program
                                builder.equal(innerJoinS.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(TaxonGroup.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(TaxonGroup.Fields.LABEL)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(taxonGroupRepository::toTaxonGroupVO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(int id) {
        log.debug(String.format("Deleting program {id=%s}...", id));
        delete(Program.class, id);
    }


    @Override
    public ProgramVO toProgramVO(Program source) {
        return toProgramVO(source, ProgramFetchOptions.builder()
                .withProperties(true)
                .build());
    }

    @Override
    public ProgramVO toProgramVO(Program source, ProgramFetchOptions fetchOptions) {
        if (source == null) return null;

        ProgramVO target = new ProgramVO();

        Beans.copyProperties(source, target);

        // Status id
        target.setStatusId(source.getStatus().getId());

        // properties
        if (fetchOptions.isWithProperties()) {
            Map<String, String> properties = Maps.newHashMap();
            Beans.getStream(source.getProperties())
                    .filter(prop -> Objects.nonNull(prop)
                            && Objects.nonNull(prop.getLabel())
                            && Objects.nonNull(prop.getName())
                    )
                    .forEach(prop -> {
                        if (properties.containsKey(prop.getLabel())) {
                            logger.warn(String.format("Duplicate program property with label {%s}. Overriding existing value with {%s}", prop.getLabel(), prop.getName()));
                        }
                        properties.put(prop.getLabel(), prop.getName());
                    });
            target.setProperties(properties);
        }

        return target;
    }

    /* -- protected methods -- */




    protected void programVOToEntity(ProgramVO source, Program target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Status
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            }
            else {
                target.setStatus(load(Status.class, source.getStatusId()));
            }
        }

    }

    protected void saveProperties(Map<String, String> source, Program parent, Timestamp updateDate) {
        final EntityManager em = getEntityManager();
        if (MapUtils.isEmpty(source)) {
            if (parent.getProperties() != null) {
                List<ProgramProperty> toRemove = ImmutableList.copyOf(parent.getProperties());
                parent.getProperties().clear();
                toRemove.forEach(em::remove);
            }
        }
        else {
            Map<String, ProgramProperty> existingProperties = Beans.splitByProperty(
                    Beans.getList(parent.getProperties()),
                    ProgramProperty.Fields.LABEL);
            final Status enableStatus = em.getReference(Status.class, StatusEnum.ENABLE.getId());
            if (parent.getProperties() == null) {
                parent.setProperties(Lists.newArrayList());
            }
            final List<ProgramProperty> targetProperties = parent.getProperties();

            // Transform each entry into ProgramProperty
            source.entrySet().stream()
                    .filter(e -> Objects.nonNull(e.getKey())
                            && Objects.nonNull(e.getValue())
                    )
                    .map(e -> {
                        ProgramProperty prop = existingProperties.remove(e.getKey());
                        boolean isNew = (prop == null);
                        if (isNew) {
                            prop = new ProgramProperty();
                            prop.setLabel(e.getKey());
                            prop.setProgram(parent);
                            prop.setCreationDate(updateDate);
                        }
                        prop.setName(e.getValue());
                        prop.setStatus(enableStatus);
                        prop.setUpdateDate(updateDate);
                        if (isNew) {
                            em.persist(prop);
                        }
                        else {
                            em.merge(prop);
                        }
                        return prop;
                    })
                    .forEach(targetProperties::add);

            // Remove old properties
            if (MapUtils.isNotEmpty(existingProperties)) {
                parent.getProperties().removeAll(existingProperties.values());
                existingProperties.values().forEach(em::remove);
            }

        }
    }
}
