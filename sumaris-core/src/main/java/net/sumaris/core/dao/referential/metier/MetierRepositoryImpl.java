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
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.filter.MetierFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

        // Prepare query parameters
        String searchJoinClass = filter.getSearchJoin();
        String searchJoinProperty = searchJoinClass != null ? StringUtils.uncapitalize(searchJoinClass) : null;
        final boolean enableSearchOnJoin = (searchJoinProperty != null);

        // Create page (do NOT sort if searchJoin : will be done later)
        boolean sortingOutsideQuery = enableSearchOnJoin && !ReferentialVO.Fields.ID.equals(sortAttribute);
        Pageable page = Pageables.create(offset, size, !sortingOutsideQuery ? sortAttribute : null, !sortingOutsideQuery ? sortDirection : null);

        // Create the query
        TypedQuery<Metier> query = createQueryByFilter(filter, page);

        return query
            .setFirstResult(offset)
            .setMaxResults(size)
            .getResultStream()
            .distinct()
            .map(source -> {
                MetierVO target = this.toVO(source);

                if (enableSearchOnJoin) {
                    // Copy join search to label/name
                    Object joinSource = Beans.getProperty(source, searchJoinProperty);
                    if (joinSource instanceof IItemReferentialEntity) {
                        target.setLabel(Beans.getProperty(joinSource, IItemReferentialEntity.Fields.LABEL));
                        target.setName(Beans.getProperty(joinSource, IItemReferentialEntity.Fields.NAME));
                    }

                    // Override the entityName, to make sure client cache will NOT mixed Metier and Metier+searchJoin
                    target.setEntityName(target.getEntityName() + searchJoinClass);

                }
                return target;
            })
            // If join search: sort using a comparator (sort was skipped in query)
            .sorted(sortingOutsideQuery ? Beans.naturalComparator(sortAttribute, sortDirection) : Beans.unsortedComparator())
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
                .and(inStatusIds(filter.getStatusIds()))
                .and(searchOrJoinSearchText(filter))
                .and(alreadyPracticedMetier(filter));
    }

    /* -- protected method -- */


    private Specification<Metier> alreadyPracticedMetier(ReferentialFilterVO filter) {
        if (!(filter instanceof MetierFilterVO)) return null;
        MetierFilterVO metierFilter = (MetierFilterVO) filter;

        return alreadyPraticedMetier(metierFilter.getVesselId());
    }

    private TypedQuery<Metier> createQueryByFilter(ReferentialFilterVO filter, Pageable pageable) {
        Preconditions.checkNotNull(filter);

        TypedQuery<Metier> query = getQuery(toSpecification(filter), Metier.class, pageable);

        // Bind search text parameter
        setParameterIfExists(query, SEARCH_TEXT_PARAMETER, Daos.getEscapedSearchText(filter.getSearchText()));

        // Bind metiers parameters
        if (filter instanceof MetierFilterVO){
            MetierFilterVO metierFilter = (MetierFilterVO)filter;

            // Calculate dates
            final Date endDate = metierFilter.getDate() != null ? metierFilter.getDate() : new Date();
            final Date startDate = Dates.removeMonth(endDate, 12); // TODO: get it from a config option

            setParameterIfExists(query, START_DATE_PARAMETER, startDate);
            setParameterIfExists(query, END_DATE_PARAMETER, endDate);
            setParameterIfExists(query, PROGRAM_LABEL_PARAMETER, metierFilter.getProgramLabel());
            setParameterIfExists(query, TRIP_ID_PARAMETER, metierFilter.getTripId());
        }

        return query;
    }

}
