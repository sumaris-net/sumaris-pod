package net.sumaris.server.http.graphql.technical.namedFilter;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import net.sumaris.core.util.Beans;
import net.sumaris.server.http.graphql.GraphQLHelper;
import net.sumaris.server.http.security.AuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.service.technical.namedFilter.NamedFilterService;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFetchOptions;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFilterVO;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterSaveOptions;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.IsUser;

@Service
@RequiredArgsConstructor
@GraphQLApi
@Transactional
@ConditionalOnWebApplication
public class NamedFilterGraphQLService {

    private final NamedFilterService namedFilterService;
    private final AuthService authService;

    @GraphQLQuery(name = "namedFilter", description = "Find NamedFilter by id")
    @IsUser
    public NamedFilterVO findById(
            @GraphQLArgument(name = "id") Integer id,
            @GraphQLEnvironment ResolutionEnvironment env) {
        Set<String> fields = GraphQLHelper.fields(env);
        NamedFilterFetchOptions fetchOptions = getFetchOption(fields);
        return namedFilterService.findById(id, fetchOptions).orElse(null);
    }

    @GraphQLQuery(name = "namedFilters", description = "Find NamedFilter by filter")
    @Transactional(readOnly = true)
    @IsUser
    public List<NamedFilterVO> findAllNamedFilter(
            @NonNull @GraphQLArgument(name = "filter") NamedFilterFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = NamedFilterVO.Fields.ID) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLEnvironment ResolutionEnvironment env
            ) {
        Set<String> fields = GraphQLHelper.fields(env);
        NamedFilterFetchOptions fetchOptions = getFetchOption(fields);
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);
        // Make sure filter is valid
        sanitizeFilter(filter);
        return namedFilterService.findAll(filter, offset, size, sort, sortDirection, fetchOptions);
    }

    @GraphQLMutation(name = "saveNamedFilter", description = "Save a NamedFilter")
    @IsUser
    public NamedFilterVO saveNamedFilter(
        @NonNull @GraphQLArgument(name = "namedFilter") NamedFilterVO namedFilter,
        @GraphQLArgument(name = "saveOptions") NamedFilterSaveOptions saveOptions)
    {
        sanitizeBeforeSave(namedFilter, saveOptions);
        return namedFilterService.save(namedFilter, saveOptions);
    }

    @GraphQLMutation(name = "saveNamedFilters", description = "Save many NamedFilter")
    @IsUser
    public List<NamedFilterVO> saveNamedFilters(
        @NonNull @GraphQLArgument(name = "namedFilters") List<NamedFilterVO> namedFilters,
        @GraphQLArgument(name = "saveOptions") NamedFilterSaveOptions saveOptions)
    {

        // Sanitize before saving
        Beans.getStream(namedFilters).forEach(entity -> sanitizeBeforeSave(entity, saveOptions));

        return namedFilterService.saveAll(namedFilters, saveOptions);
    }

    @GraphQLMutation(name = "deleteNamedFilter", description = "Delete a NamedFilter by id")
    @IsUser
    public void deleteNamedFilter(
            @GraphQLArgument(name = "id") Integer id)
    {
        namedFilterService.delete(id);
    }

    @GraphQLMutation(name = "deleteNamedFilter", description = "Delete many NamedFilters by ids")
    @IsUser
    public void deleteNamedFilter(
        @GraphQLArgument(name = "ids") List<Integer> ids) {
        namedFilterService.deleteAll(ids);
    }

    /* -- internal functions -- */

    private void sanitizeBeforeSave(NamedFilterVO source, @Nullable NamedFilterSaveOptions options) {
        // TODO Save departement filter if option isDepartementFilter is set
        PersonVO user = authService.getAuthenticatedUser().orElseThrow(UnauthorizedException::new);
        source.setRecorderPerson(user);
        source.setRecorderPersonId(user.getId());
    }

    private void sanitizeFilter(NamedFilterFilterVO filter) {
        Integer userId = authService.getAuthenticatedUserId().orElseThrow(UnauthorizedException::new);
        filter.setRecorderPersonId(userId);
        filter.setRecorderDepartmentId(null); // TODO
    }

    private NamedFilterFetchOptions getFetchOption(Set<String> fields) {
        return NamedFilterFetchOptions.builder().withContent(fields.contains(NamedFilterVO.Fields.CONTENT)).build();
    }
}
