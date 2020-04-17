package net.sumaris.core.dao.referential.metier;

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
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.filter.MetierFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static net.sumaris.core.dao.referential.metier.MetierSpecifications.*;

public class MetierRepositoryImpl
    extends ReferentialRepositoryImpl<Metier, MetierVO, ReferentialFilterVO>
    implements MetierRepositoryExtend {

    private static final Logger log = LoggerFactory.getLogger(MetierRepositoryImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonGroupRepository taxonGroupRepository;

    public MetierRepositoryImpl(EntityManager entityManager) {
        super(Metier.class, entityManager);
    }

    @Override
    public List<MetierVO> findByFilter(
        ReferentialFilterVO filter,
        int offset,
        int size,
        String sortAttribute,
        SortDirection sortDirection) {

        Preconditions.checkNotNull(filter);

        // Switch to specific search if a date and a vessel id is provided
        if (filter instanceof MetierFilterVO) {
            MetierFilterVO metierFilter = (MetierFilterVO) filter;
            if (metierFilter.getDate() != null && metierFilter.getVesselId() == null)
                return findByDateAndVesselId(metierFilter, offset, size, sortAttribute, sortDirection);
        }

        // Prepare query parameters
        String searchJoinProperty = filter.getSearchJoin() != null ? StringUtils.uncapitalize(filter.getSearchJoin()) : null;
        String searchText = Daos.getEscapedSearchText(filter.getSearchText());

        // With join property
        Specification<Metier> searchTextSpecification;
        if (searchJoinProperty != null) {
            searchTextSpecification = joinSearchText(
                searchJoinProperty,
                filter.getSearchAttribute(), SEARCH_TEXT_PARAMETER);
        } else {
            searchTextSpecification = searchText(filter.getSearchAttribute(), SEARCH_TEXT_PARAMETER);
        }

        Specification<Metier> specification = toSpecification(filter).and(searchTextSpecification);

        Pageable page = getPageable(offset, size, sortAttribute, sortDirection);
        TypedQuery<Metier> query = getQuery(specification, Metier.class, page);

        Parameter<String> searchTextParam = query.getParameter(SEARCH_TEXT_PARAMETER, String.class);
        if (searchTextParam != null) {
            query.setParameter(searchTextParam, searchText);
        }

        return query
            .setFirstResult(offset)
            .setMaxResults(size)
            .getResultStream()
            .distinct()
            .map(source -> {
                MetierVO target = this.toVO(source);

                // Copy join search to label/name
                if (searchJoinProperty != null) {
                    Object joinSource = Beans.getProperty(source, searchJoinProperty);
                    if (joinSource instanceof IItemReferentialEntity) {
                        target.setLabel(Beans.getProperty(joinSource, IItemReferentialEntity.Fields.LABEL));
                        target.setName(Beans.getProperty(joinSource, IItemReferentialEntity.Fields.NAME));
                    }
                }
                return target;
            })
            .collect(Collectors.toList());
    }

    private List<MetierVO> findByDateAndVesselId(MetierFilterVO filter, Integer offset, Integer size, String sort, SortDirection sortDirection) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(filter.getDate());
        Preconditions.checkNotNull(filter.getVesselId());

        // Calculate dates
        Date endDate = filter.getDate();
        Date startDate = Dates.removeMonth(endDate, 12); // TODO get this predocumentation length from configuration

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Metier> criteriaQuery = cb.createQuery(Metier.class);
        Root<Metier> metiers = criteriaQuery.from(Metier.class);
        Root<Operation> operations = criteriaQuery.from(Operation.class);
        Join<Operation, Trip> trips = operations.join(Operation.Fields.TRIP, JoinType.INNER);
        ParameterExpression<Integer> tripIdParameter = cb.parameter(Integer.class);

        String searchText = Daos.getEscapedSearchText(filter.getSearchText());
        Specification<Metier> searchTextSpecification = searchText(filter.getSearchAttribute(), SEARCH_TEXT_PARAMETER);
        Specification<Metier> specification = toSpecification(filter).and(searchTextSpecification);

        criteriaQuery.where(cb.and(
            cb.equal(operations.get(Operation.Fields.METIER), metiers.get(Metier.Fields.ID)),
            cb.equal(trips.get(Trip.Fields.VESSEL).get(Vessel.Fields.ID), filter.getVesselId()),
            // TODO add program filter
            cb.not(
                cb.or(
                    cb.greaterThan(operations.get(Operation.Fields.START_DATE_TIME), endDate),
                    cb.lessThan(cb.coalesce(operations.get(Operation.Fields.END_DATE_TIME), startDate), startDate)
                )
            ),
            cb.or(
                cb.isNull(tripIdParameter),
                cb.notEqual(trips.get(Trip.Fields.ID), tripIdParameter)
            ),
            specification.toPredicate(metiers, criteriaQuery, cb)
        ));

        criteriaQuery.select(metiers);

        TypedQuery<Metier> query = getEntityManager().createQuery(criteriaQuery);

        Parameter<String> searchTextParam = query.getParameter(SEARCH_TEXT_PARAMETER, String.class);
        if (searchTextParam != null) {
            query.setParameter(searchTextParam, searchText);
        }
        query.setParameter(tripIdParameter, filter.getTripId());

        return query
            .setFirstResult(offset)
            .setMaxResults(size)
            .getResultStream()
            .distinct()
            .map(this::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public void toVO(Metier source, MetierVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // StatusId
        target.setStatusId(source.getStatus().getId());

        // Gear
        if (source.getGear() != null) {
            target.setGear(referentialDao.toReferentialVO(source.getGear()));
            target.setLevelId(source.getGear().getId());
        }

        // Taxon group
        if (source.getTaxonGroup() != null) {
            target.setTaxonGroup(taxonGroupRepository.toVO(source.getTaxonGroup()));
        }

    }

    @Override
    public Class<MetierVO> getVOClass() {
        return MetierVO.class;
    }

    @Override
    public Specification<Metier> toSpecification(ReferentialFilterVO filter) {

        Integer[] levelIds = (filter.getLevelId() != null) ? new Integer[]{filter.getLevelId()} : filter.getLevelIds();

        return Specification
            .where(inGearIds(levelIds))
            .and(inStatusIds(filter.getStatusIds()));
    }
}
