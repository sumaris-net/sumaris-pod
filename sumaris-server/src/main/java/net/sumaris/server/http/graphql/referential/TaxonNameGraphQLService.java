/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.server.http.graphql.referential;

import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.taxon.ReferenceTaxonRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameFetchOptions;
import net.sumaris.core.vo.referential.TaxonNameVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLUtils;
import net.sumaris.server.http.security.IsSupervisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@GraphQLApi
@Transactional
@Slf4j
public class TaxonNameGraphQLService {


    @Autowired
    private TaxonNameService taxonNameService;

    @Autowired
    private TaxonGroupService taxonGroupService;

    @Autowired
    private ReferenceTaxonRepository referenceTaxonRepository;

    @Autowired
    public TaxonNameGraphQLService() {
        super();
    }

    /* -- Taxon Name -- */

    @GraphQLQuery(name = "taxonName", description = "Get a Taxon Name")
    @Transactional(readOnly = true)
    public TaxonNameVO getTaxonName(
            @GraphQLArgument(name = "label") String label,
            @GraphQLArgument(name = "id") Integer id,
            @GraphQLEnvironment ResolutionEnvironment env
    ) {
        TaxonNameFetchOptions fetchOptions = getFetchOptions(GraphQLUtils.fields(env));
        if (id != null) {
            return taxonNameService.get(id, fetchOptions);
        }
        // By label
        return taxonNameService.getByLabel(label, fetchOptions);
    }

    @GraphQLQuery(name = "taxonNames", description = "Search in taxon names")
    @Transactional(readOnly = true)
    public List<TaxonNameVO> findTaxonNames(
            @GraphQLArgument(name = "filter") TaxonNameFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = IReferentialVO.Fields.NAME) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLEnvironment ResolutionEnvironment env) {

        Page page = Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction))
            .build();
        TaxonNameFetchOptions fetchOptions = getFetchOptions(GraphQLUtils.fields(env));

        if (filter == null) {
            return taxonNameService.findAllSpeciesAndSubSpecies(true, page, fetchOptions);
        }
        return taxonNameService.findByFilter(filter, page, fetchOptions);
    }

    @GraphQLQuery(name = "taxonNameCount", description = "Get taxon name count")
    @Transactional(readOnly = true)
    public Long getTaxonNameCount(@GraphQLArgument(name = "filter") TaxonNameFilterVO filter) {
        return taxonNameService.countByFilter(filter);
    }

    @GraphQLQuery(name = "taxonGroupIds", description = "Get taxon group's ids of a taxon name")
    public List<Integer> getTaxonGroupIds(@GraphQLContext TaxonNameVO taxonNameVO) {
        if (taxonNameVO.getTaxonGroupIds() != null) return taxonNameVO.getTaxonGroupIds();
        if (taxonNameVO.getReferenceTaxonId() != null) {
            return taxonGroupService.getAllIdByReferenceTaxonId(taxonNameVO.getReferenceTaxonId(), new Date(), null);
        }
        // Should never occur !
        return null;
    }

    /* -- Mutations -- */

    @GraphQLMutation(name = "saveTaxonName", description = "Save a Taxon name")
    @IsSupervisor
    public TaxonNameVO saveTaxonName(@GraphQLNonNull @GraphQLArgument(name = "taxonName") @NonNull TaxonNameVO taxonName) {
        return taxonNameService.save(taxonName);
    }

    /* -- Reference Taxon -- */

    @GraphQLQuery(name = "referenceTaxonExists", description = "Search in referenceTaxons")
    @Transactional(readOnly = true)
    public Boolean referenceTaxonExists(@GraphQLArgument(name = "id") final Integer id) {
        Optional<ReferenceTaxon> referenceTaxon = referenceTaxonRepository.findById(id);
        return referenceTaxon.isPresent();
    }

    /* -- protected functions -- */

    protected TaxonNameFetchOptions getFetchOptions(Set<String> fields) {
        return TaxonNameFetchOptions.builder()
            .withParentTaxonName(fields.contains(StringUtils.slashing(TaxonNameVO.Fields.PARENT_TAXON_NAME, IEntity.Fields.ID)))
            .withTaxonomicLevel(fields.contains(StringUtils.slashing(TaxonNameVO.Fields.TAXONOMIC_LEVEL, IEntity.Fields.ID)))
            .build();
    }
}
