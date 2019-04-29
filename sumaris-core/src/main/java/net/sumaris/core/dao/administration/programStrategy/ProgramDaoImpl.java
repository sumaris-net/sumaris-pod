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
import com.google.common.collect.Maps;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramProperty;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository("programDao")
public class ProgramDaoImpl extends HibernateDaoSupport implements ProgramDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ProgramDaoImpl.class);


    @PostConstruct
    protected void init() {
        Arrays.stream(ProgramEnum.values()).forEach(programEnum -> {
            try {
                ProgramVO program = getByLabel(programEnum.name());
                if (program != null) {
                    programEnum.setId(program.getId());
                } else {
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

        Join<Program, ProgramProperty> upJ = root.join(Program.PROPERTY_PROPERTIES, JoinType.LEFT);

        ParameterExpression<String> withPropertyParam = builder.parameter(String.class);
        ParameterExpression<Collection> statusIdsParam = builder.parameter(Collection.class);
        ParameterExpression<String> searchTextParam = builder.parameter(String.class);

        query.select(root).distinct(true)
                .where(
                        builder.and(
                                // property
                                builder.or(
                                        builder.isNull(withPropertyParam),
                                        builder.equal(upJ.get(ProgramProperty.PROPERTY_LABEL), withPropertyParam)
                                ),
                                // status Ids
                                builder.or(
                                        builder.isNull(statusIdsParam),
                                        root.get(Program.PROPERTY_STATUS).get(IReferentialEntity.PROPERTY_ID).in(statusIdsParam)
                                ),
                                // search text
                                builder.or(
                                        builder.isNull(searchTextParam),
                                        builder.like(builder.upper(root.get(Program.PROPERTY_LABEL)), builder.upper(searchTextParam)),
                                        builder.like(builder.upper(root.get(Program.PROPERTY_NAME)), builder.upper(searchTextParam))
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
                .where(builder.equal(root.get(Program.PROPERTY_LABEL), labelParam));

        TypedQuery<Program> q = getEntityManager().createQuery(query)
                .setParameter(labelParam, label);
        try {
            return toProgramVO(q.getSingleResult());
        } catch(EmptyResultDataAccessException | NoResultException e) {
            return null;
        }
    }

    @Override
    public ProgramVO toProgramVO(Program source) {
        if (source == null) return null;

        ProgramVO target = new ProgramVO();

        Beans.copyProperties(source, target);

        // Status id
        target.setStatusId(source.getStatus().getId());

        // properties
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

        return target;
    }

    /* -- protected methods -- */

}
