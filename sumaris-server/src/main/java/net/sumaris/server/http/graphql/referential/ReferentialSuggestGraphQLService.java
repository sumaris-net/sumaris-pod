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

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.location.LocationClassificationEnum;
import net.sumaris.core.service.referential.ReferentialSuggestService;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReferentialSuggestGraphQLService {

    @Autowired
    private ReferentialSuggestService referentialSuggestService;

    /* -- Referential queries -- */

    @GraphQLQuery(name = "suggestedReferentials", description = "Get already filled values from entityName")
    public List<? extends ReferentialVO> findSuggestedReferentialsFromStrategy(
            @GraphQLArgument(name = "entityName") String entityName,
            @GraphQLArgument(name = "programId") int programId,
            @GraphQLArgument(name = "locationClassification", description = "only for Location entities") LocationClassificationEnum locationClassification,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        return referentialSuggestService.findFromStrategy(entityName, programId, locationClassification,
                offset == null ? 0 : offset,
                size == null ? 1000 : size,
                sort == null ? ReferentialVO.Fields.LABEL : sort,
                direction == null ? SortDirection.ASC : SortDirection.valueOf(direction.toUpperCase()));
    }

}
