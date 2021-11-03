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

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.referential.ReferentialExternalService;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@GraphQLApi
public class ReferentialExternalGraphQLService {

    @Autowired
    private ReferentialExternalService referentialExternalService;

    /* -- Referential queries -- */

    @GraphQLQuery(name = "analyticReferences", description = "Search in analytic references")
    @Transactional(readOnly = true)
    public List<? extends ReferentialVO> findAnalyticReferencesByFilter(
            @GraphQLArgument(name = "filter") ReferentialFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        return referentialExternalService.findAnalyticReferencesByFilter(
                filter != null ? filter : new ReferentialFilterVO(),
                offset == null ? 0 : offset,
                size == null ? 1000 : size,
                sort == null ? ReferentialVO.Fields.LABEL : sort,
                direction == null ? SortDirection.ASC : SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "analyticReferencesCount", description = "Get analytic references count")
    @Transactional(readOnly = true)
    public Long getAnalyticReferencesCount(@GraphQLArgument(name = "filter") ReferentialFilterVO filter) {
        return referentialExternalService.countAnalyticReferencesByFilter(
                filter != null ? filter : new ReferentialFilterVO()
        );
    }

}
