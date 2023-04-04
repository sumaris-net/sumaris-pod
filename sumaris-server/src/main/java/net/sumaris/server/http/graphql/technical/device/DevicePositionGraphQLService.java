package net.sumaris.server.http.graphql.technical.device;

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.technical.device.DevicePositionService;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.IsUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@RequiredArgsConstructor
@GraphQLApi
@Transactional
@ConditionalOnWebApplication
public class DevicePositionGraphQLService {

    @Resource
    private final DevicePositionService devicePositionService;
    @GraphQLQuery(name = "devicePositions", description = "Find DevicePositions by filter")
    //  TODO  @IsUser
    public List<DevicePositionVO> findAllDevicePositions(
            @GraphQLArgument(name = "filter") DevicePositionFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ObservedLocationVO.Fields.ID) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLArgument(name = "fetchOptions") DataFetchOptions fetchOptions,
            @GraphQLEnvironment() ResolutionEnvironment env
    ) {
        // TODO if is not admin modifiy filter to only fetch current user postion
        Preconditions.checkNotNull(filter, "Missing filter");
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);
        return devicePositionService.findAll(filter, offset, size, sort, sortDirection, fetchOptions);
    }

    @GraphQLQuery(name = "devicePositionsCount", description = "Get DevicePostion count")
    @Transactional(readOnly = true)
    //  TODO  @IsUser
    public long countDevicePositions(@GraphQLArgument(name = "filter") DevicePositionFilterVO filter) {
        // TODO if is not admin modifiy filter to only fetch current user postion
        Preconditions.checkNotNull(filter, "Missing filter");
        return devicePositionService.countByFilter(filter);
    }

    @GraphQLQuery(name = "devicePosition", description = "Find DevicePosition by id")
    public DevicePositionVO findById(
            @GraphQLArgument(name = "id") Integer id,
            @GraphQLArgument(name = "fetchOptions") DataFetchOptions fetchOptions) {
        return devicePositionService.findById(id, fetchOptions).orElse(null);
    }

    @GraphQLMutation(name = "saveDevicePosition", description = "Save a DevicePosition")
    public DevicePositionVO saveDevicePosition(
            @GraphQLArgument(name = "devicePosition") DevicePositionVO devicePosition) {
        return devicePositionService.save(devicePosition);
    }

    @GraphQLMutation(name = "deleteDevicePosition", description = "Delete a DevicePosition by id")
    public void deleteDevicePosition(
            @GraphQLArgument(name = "id") Integer id) {
        devicePositionService.delete(id);
    }
}
