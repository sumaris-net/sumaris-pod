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

import lombok.NonNull;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.MapUtils;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.filter.MetierFilterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.metier.MetierVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


public class MetierRepositoryImpl
        extends ReferentialRepositoryImpl<Integer, Metier, MetierVO, IReferentialFilter, ReferentialFetchOptions>
        implements MetierSpecifications {

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonGroupRepository taxonGroupRepository;

    public MetierRepositoryImpl(EntityManager entityManager) {
        super(Metier.class, MetierVO.class, entityManager);
    }

    @Override
    public List<MetierVO> findAll(IReferentialFilter filter, Page page, ReferentialFetchOptions fetchOptions) {
        return findByFilter(filter, (int)page.getOffset(), page.getSize(), page.getSortBy(), page.getSortDirection(), fetchOptions);
    }

    @Override
    public List<MetierVO> findByFilter(
            IReferentialFilter filter,
            int offset,
            int size,
            String sortAttribute,
            SortDirection sortDirection,
            ReferentialFetchOptions fetchOptions) {

        final boolean enableSearchOnJoin = (filter.getSearchJoin() != null);

        // Prepare query parameters
        String searchJoinClass = StringUtils.capitalize(filter.getSearchJoin());
        String searchJoinProperty = StringUtils.uncapitalize(filter.getSearchJoin());

        // Compute sort (do NOT sort if searchJoin : will be done later)
        boolean sortingOutsideQuery = enableSearchOnJoin && !ReferentialVO.Fields.ID.equals(sortAttribute);
        Sort.Direction direction = Optional.ofNullable(sortDirection)
                .map(SortDirection::name)
                .map(Sort.Direction::fromString)
                .orElse(Sort.Direction.ASC);
        Sort sort = !sortingOutsideQuery && StringUtils.isNotBlank(sortAttribute)
                ? Sort.by(direction, sortAttribute)
                : Sort.unsorted();

        // Create the query
        TypedQuery<Metier> query = getQuery(toSpecification(filter), Metier.class, sort);

        try (Stream<Metier> stream = query.setFirstResult(offset).setMaxResults(size).getResultStream()) {
            return stream
                    .map(source -> {
                        MetierVO target = this.toVO(source, fetchOptions);

                        if (enableSearchOnJoin) {
                            // Copy join search to label/name
                            Object joinSource = Beans.getProperty(source, searchJoinProperty);
                            if (joinSource instanceof IItemReferentialEntity) {
                                target.setLabel(Beans.getProperty(joinSource, IItemReferentialEntity.Fields.LABEL));
                                target.setName(Beans.getProperty(joinSource, IItemReferentialEntity.Fields.NAME));
                            }

                            if (joinSource instanceof TaxonGroup) {
                                TaxonGroup tg = (TaxonGroup) joinSource;
                                target.getTaxonGroup().setLevelId(tg.getTaxonGroupType().getId());
                            }

                            // Override the entityName, to make sure client cache will NOT mixed Metier and Metier+searchJoin
                            target.setEntityName(target.getEntityName() + searchJoinClass);

                        }

                        //
                        return target;
                    })
                    // If join search: sort using a comparator (sort was skipped in query)
                    .sorted(sortingOutsideQuery ? Beans.naturalComparator(sortAttribute, sortDirection) : Beans.unsortedComparator())
                    .toList();
        }
    }

    @Override
    public void toVO(Metier source, MetierVO target, ReferentialFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Gear
        if (source.getGear() != null) {
            target.setGear(referentialDao.toVO(source.getGear()));
            target.setLevelId(source.getGear().getId());
        }

        // Taxon group
        if (source.getTaxonGroup() != null || copyIfNull) {
            if (source.getTaxonGroup() == null) {
                target.setTaxonGroup(null);
            } else {
                target.setTaxonGroup(taxonGroupRepository.toVO(source.getTaxonGroup()));
            }
        }

        // Properties (e.g. when called from referential graphql service
        if (fetchOptions != null && fetchOptions.isWithProperties()) {
            target.setProperties(MapUtils.of(
                            MetierVO.Fields.GEAR, target.getGear(),
                            MetierVO.Fields.TAXON_GROUP, target.getTaxonGroup()
                        )
            );
        }
    }

    /* -- protected method -- */

    @Override
    protected Specification<Metier> toSpecification(@NonNull IReferentialFilter filter, ReferentialFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
                .and(alreadyPracticedMetier(filter))
                .and(inGearIds(filter))
                .and(inSpatialLocationIds(filter))
                .and(inTaxonGroupTypeIds(filter));
    }

    private Specification<Metier> alreadyPracticedMetier(IReferentialFilter filter) {
        if (!(filter instanceof MetierFilterVO metierFilter)) return null;

        return alreadyPracticedMetier(metierFilter);
    }

    private Specification<Metier> inGearIds(IReferentialFilter filter) {
        if (!(filter instanceof MetierFilterVO metierFilter)) return null;

        return inGearIds(metierFilter.getGearIds());
    }

    private Specification<Metier> inTaxonGroupTypeIds(IReferentialFilter filter) {
        if (!(filter instanceof MetierFilterVO metierFilter)) return null;

        return inTaxonGroupTypeIds(metierFilter.getTaxonGroupTypeIds());
    }
}
