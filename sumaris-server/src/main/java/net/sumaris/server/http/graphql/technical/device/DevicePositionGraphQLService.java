package net.sumaris.server.http.graphql.technical.device;

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.technical.device.DevicePositionService;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@RequiredArgsConstructor
@GraphQLApi
@ConditionalOnWebApplication
public class DevicePositionGraphQLService {

    @Resource
    private final DevicePositionService devicePositionService;

    @GraphQLQuery(name = "devicePositions", description = "Find DevicePositions by filter")
    public List<DevicePositionVO> findAllDevicePositions(
            @GraphQLArgument(name = "filter") DevicePositionFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ObservedLocationVO.Fields.ID) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLArgument(name = "fetchOptions") DataFetchOptions fetchOptions) {
        Preconditions.checkNotNull(filter, "Missing filter");
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);
        return devicePositionService.findAll(filter, offset, size, sort, sortDirection, fetchOptions);
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
