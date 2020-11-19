package net.sumaris.server.http.graphql.technical;

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

import com.google.common.collect.ImmutableList;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.service.technical.TrashService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TrashGraphQLService {

    public static final String JSON_START_SUFFIX = "{";

    private static final Log log = LogFactory.getLog(TrashGraphQLService.class);

    @Autowired
    private TrashService service;

    @GraphQLQuery(name = "trash", description = "Get trash content")
    @IsAdmin
    public List<String> getTrashContent(
        @GraphQLArgument(name = "entityName") String entityName,
        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
        @GraphQLArgument(name = "sortBy", defaultValue = IUpdateDateEntityBean.Fields.UPDATE_DATE) String sort,
        @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction
        ) {
        Pageable pageable = Pageables.create(offset, size, sort, direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null);

        Page<String> page = service.findAll(entityName, pageable, String.class);
        return page.hasContent() ? page.getContent() : ImmutableList.of();
    }

    // TODO: add delete from trash
}
