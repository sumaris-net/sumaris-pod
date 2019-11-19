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
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

import static net.sumaris.core.dao.referential.metier.MetierSpecifications.*;

public class MetierRepositoryImpl
        extends SumarisJpaRepositoryImpl<Metier, Integer>
        implements MetierRepositoryExtend {


    private static final Logger log =
            LoggerFactory.getLogger(MetierRepositoryImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonGroupRepository taxonGroupRepository;

    public MetierRepositoryImpl(EntityManager entityManager) {
        super(Metier.class, entityManager);
    }

    @Override
    public List<ReferentialVO> findByFilter(
                                               ReferentialFilterVO filter,
                                               int offset,
                                               int size,
                                               String sortAttribute,
                                               SortDirection sortDirection) {

        Preconditions.checkNotNull(filter);

        // Prepare query parameters
        Integer[] levelIds = (filter.getLevelId() != null) ? new Integer[]{filter.getLevelId()} :
                filter.getLevelIds();
        String searchJoinProperty = filter.getSearchJoin() != null ? StringUtils.uncapitalize(filter.getSearchJoin()) : null;
        String searchText = StringUtils.isBlank(filter.getSearchText()) ? null : (filter.getSearchText() + "*") // add trailing wildcard
                .replaceAll("[*]+", "*") // group escape chars
                .replaceAll("[%]", "\\%") // protected '%' chars
                .replaceAll("[*]", "%"); // replace asterix

        // With join property
        Specification<Metier> searchTextSpecification;
        if (searchJoinProperty != null) {
            searchTextSpecification = joinSearchText(
                    searchJoinProperty,
                    filter.getSearchAttribute(), "searchText");
        }
        else {
            searchTextSpecification = searchText(filter.getSearchAttribute(), "searchText");
        }

        Specification<Metier> specification = Specification.where(searchTextSpecification)
                .and(inStatusIds(filter.getStatusIds()))
                .and(inGearIds(levelIds));


        Pageable page = getPageable(offset, size, sortAttribute, sortDirection);
        TypedQuery<Metier> query = getQuery(specification, Metier.class, page);

        Parameter<String> searchTextParam = query.getParameter("searchText", String.class);
        if (searchTextParam != null) {
            query.setParameter(searchTextParam, searchText);
        }

        return query
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultStream()
                    .distinct()
                    .map(source -> {
                        MetierVO target = this.toMetierVO(source);

                        // Copy join search to label/name
                        if (searchJoinProperty != null) {
                            Object joinSource = Beans.getProperty(source, searchJoinProperty);
                            if (joinSource != null && joinSource instanceof IItemReferentialEntity) {
                                target.setLabel(Beans.getProperty(joinSource, IItemReferentialEntity.Fields.LABEL));
                                target.setName(Beans.getProperty(joinSource, IItemReferentialEntity.Fields.NAME));
                            }
                        }
                        return target;
                    })
                    .collect(Collectors.toList());
    }


    @Override
    public MetierVO toMetierVO(@Nullable Metier source) {
        if (source == null) return null;

        MetierVO target = new MetierVO();

        Beans.copyProperties(source, target);

        // StatusId
        target.setStatusId(source.getStatus().getId());

        // Gear
        if (source.getGear() != null) {
            target.setGear(referentialDao.toReferentialVO(source.getGear()));
            target.setLevelId(source.getGear().getId());
        }

        // Taxon group
        if (source.getTaxonGroup() != null) {
            target.setTaxonGroup(taxonGroupRepository.toTaxonGroupVO(source.getTaxonGroup()));
        }

        return target;
    }

    @Override
    public MetierVO getById(int id) {
        return toMetierVO(getOne(id));
    }
}
