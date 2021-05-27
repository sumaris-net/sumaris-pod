package net.sumaris.server.http.graphql.referential;

/*-
 * #%L
 * SUMARiS:: Server
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
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.*;
import net.sumaris.server.http.security.IsSupervisor;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.technical.ChangesPublisherService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class TaxonNameGraphQLService {

    @Autowired
    private ReferentialService referentialService;

    @Autowired
    private TaxonNameService taxonNameService;

    @Autowired
    private ChangesPublisherService changesPublisherService;

    @Autowired
    public TaxonNameGraphQLService() {
        super();
    }


    /* -- Taxon Name -- */

    @GraphQLQuery(name = "taxonName", description = "Get a Taxon Name")
    @Transactional(readOnly = true)
    public TaxonNameVO getTaxonName(
            @GraphQLArgument(name = "label") String label,
            @GraphQLArgument(name = "id") Integer id
    ) {
        if (id != null) {
            return taxonNameService.get(id);
        } else {
            return taxonNameService.getByLabel(label);
        }
    }

    @GraphQLQuery(name = "taxonNames", description = "Search in taxon names")
    @Transactional(readOnly = true)
    public List<TaxonNameVO> findTaxonNamesByFilter(
            @GraphQLArgument(name = "filter") TaxonNameFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = IReferentialVO.Fields.LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        if (filter == null) {
            return taxonNameService.getAll(true);
        }
        return taxonNameService.findByFilter(filter, offset, size, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "taxonNameCount", description = "Get taxon name count")
    @Transactional(readOnly = true)
    public Long getTaxonNameCount(@GraphQLArgument(name = "filter") TaxonNameFilterVO filter) {
        return referentialService.countByFilter(Program.class.getSimpleName(), filter);
    }

    @GraphQLSubscription(name = "updateTaxonName", description = "Subscribe to changes on a taxon name")
    @IsUser
    public Publisher<TaxonNameVO> updateTaxonName(@GraphQLArgument(name = "id") final Integer id,
                                                  @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer minIntervalInSecond,
                                                  @GraphQLEnvironment ResolutionEnvironment env) {

        Preconditions.checkArgument(id >= 0, "Invalid 'id' argument");
        return changesPublisherService.getPublisher(TaxonName.class, TaxonNameVO.class, id, minIntervalInSecond, true);
    }

    /* -- Mutations -- */

    @GraphQLMutation(name = "saveTaxonName", description = "Save a Taxon name")
    @IsSupervisor
    public TaxonNameVO saveTaxonName(@GraphQLNonNull @GraphQLArgument(name = "taxonName") @NonNull TaxonNameVO taxonName) {
        return taxonNameService.save(taxonName);
    }

}
