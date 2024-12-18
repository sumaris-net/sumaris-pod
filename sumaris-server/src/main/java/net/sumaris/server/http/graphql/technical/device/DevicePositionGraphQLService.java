package net.sumaris.server.http.graphql.technical.device;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 - 2023 SUMARiS Consortium
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
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.service.technical.device.DevicePositionService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@GraphQLApi
@Transactional
@ConditionalOnWebApplication
public class DevicePositionGraphQLService {

    private final DevicePositionService devicePositionService;

    private final AuthService authService;

    @GraphQLQuery(name = "devicePositions", description = "Find device positions by filter")
    @Transactional(readOnly = true)
    @IsUser
    public List<DevicePositionVO> findAllDevicePositions(
            @NonNull @GraphQLArgument(name = "filter") DevicePositionFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = DevicePositionVO.Fields.ID) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLArgument(name = "fetchOptions") DataFetchOptions fetchOptions,
            @GraphQLEnvironment() ResolutionEnvironment env
    ) {
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);

        // Make sure filter is valid
        sanitizeFilter(filter);

        return devicePositionService.findAll(filter, offset, size, sort, sortDirection, fetchOptions);
    }

    @GraphQLQuery(name = "devicePositionsCount", description = "Get device position count")
    @Transactional(readOnly = true)
    @IsUser
    public long countDevicePositions(@GraphQLArgument(name = "filter") DevicePositionFilterVO filter) {
        Preconditions.checkNotNull(filter, "Missing filter");

        // Make sure filter is valid
        sanitizeFilter(filter);

        return devicePositionService.countByFilter(filter);
    }

    @GraphQLQuery(name = "devicePosition", description = "Find device position by id")
    @IsUser
    public DevicePositionVO findById(
            @GraphQLArgument(name = "id") Integer id,
            @GraphQLArgument(name = "fetchOptions") DataFetchOptions fetchOptions) {
        return devicePositionService.findById(id, fetchOptions).orElse(null);
    }

    @GraphQLMutation(name = "saveDevicePositions", description = "Save many device positions")
    @IsUser
    public List<DevicePositionVO> saveDevicePositions(
        @GraphQLArgument(name = "devicePositions") List<DevicePositionVO> devicePositions) {

        // Sanitize before saving
        Beans.getStream(devicePositions).forEach(this::sanitizeBeforeSave);

        return devicePositionService.saveAll(devicePositions);
    }

    @GraphQLMutation(name = "saveDevicePosition", description = "Save a device position")
    @IsUser
    public DevicePositionVO saveDevicePosition(
            @GraphQLArgument(name = "devicePosition") DevicePositionVO devicePosition) {
        sanitizeBeforeSave(devicePosition);
        return devicePositionService.save(devicePosition);
    }

    @GraphQLMutation(name = "deleteDevicePosition", description = "Delete a device position by id")
    @IsUser
    public void deleteDevicePosition(
            @GraphQLArgument(name = "id") Integer id) {
        devicePositionService.delete(id);
    }

    @GraphQLMutation(name = "deleteDevicePositions", description = "Delete many device positions by id")
    @IsUser
    public void deleteDevicePositions(
        @GraphQLArgument(name = "ids") List<Integer> ids) {
        devicePositionService.deleteAll(ids);
    }

    @GraphQLMutation(name = "deleteDevicePositionsByFilter", description = "Delete many device positions by filter")
    @IsUser
    public void deleteDevicePositionsByFilter(@NonNull @GraphQLArgument(name = "filter") DevicePositionFilterVO filter) {
        Preconditions.checkNotNull(filter.getObjectId());
        Preconditions.checkArgument(filter.getObjectTypeLabel() != null || filter.getObjectTypeId() != null, "Missing required 'filter.objectTypeId' or 'filter.objectTypeLabel");
        sanitizeFilter(filter);
        devicePositionService.deleteByFilter(filter);
    }

    /* -- internal functions -- */

    private void sanitizeBeforeSave(DevicePositionVO source) {
        // Fore recorder person to self, when not an admin
        if (!authService.isAdmin()) {
            PersonVO user = authService.getAuthenticatedUser().orElseThrow(UnauthorizedException::new);
            source.setRecorderPersonId(user.getId());
            source.setRecorderDepartmentId(user.getDepartment().getId());
        }
    }

    private void sanitizeFilter(DevicePositionFilterVO filter) {
        // Only admin can show not self position
        if (!authService.isAdmin()) {
            Integer userId = authService.getAuthenticatedUserId().orElseThrow(UnauthorizedException::new);
            filter.setRecorderPersonId(userId);
            filter.setRecorderDepartmentId(null); // Not need (already filter on user)
        }
    }
}
