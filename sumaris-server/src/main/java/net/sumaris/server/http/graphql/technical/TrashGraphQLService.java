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

package net.sumaris.server.http.graphql.technical;

import com.google.common.collect.ImmutableList;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.service.technical.TrashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@GraphQLApi
@Slf4j
public class TrashGraphQLService {

    @Autowired
    private TrashService service;

    @GraphQLQuery(name = "trashEntities", description = "Get trash content")
    @IsAdmin
    public List<String> findAll(
        @GraphQLArgument(name = "entityName") String entityName,
        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
        @GraphQLArgument(name = "sortBy", defaultValue = IUpdateDateEntityBean.Fields.UPDATE_DATE) String sort,
        @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction
        ) {
        Pageable pageable = Pageables.create(offset, size, sort, direction != null ? SortDirection.fromString(direction) : null);

        Page<String> page = service.findAll(entityName, pageable, String.class);
        return page.hasContent() ? page.getContent() : ImmutableList.of();
    }

    @GraphQLQuery(name = "trashEntity", description = "Get trash file content")
    @IsAdmin
    public String get(
            @GraphQLArgument(name = "entityName") String entityName,
            @GraphQLArgument(name = "id") String id
    ) {

        return service.getById(entityName, id, String.class);
    }

    @GraphQLMutation(name = "deleteTrashEntity", description = "Delete an entity from the trash")
    @IsAdmin
    public void delete(@GraphQLArgument(name = "entityName") String entityName,
                       @GraphQLArgument(name = "id") String id) {
        service.delete(entityName, id);
    }
}
