package net.sumaris.server.http.graphql.technical.device;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.service.technical.device.DevicePositionService;
import net.sumaris.core.vo.technical.device.DevicePositionFetchOptions;
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

    @GraphQLQuery(name = "findByFilter", description = "Find DevicePositions by filter")
    public List<DevicePositionVO> findByFilter(
            @GraphQLArgument(name = "filter") DevicePositionFilterVO filter,
            @GraphQLArgument(name = "page") Page page,
            @GraphQLArgument(name = "fetchOptions") DevicePositionFetchOptions fetchOptions) {
        return devicePositionService.findByFilter(filter, page, fetchOptions);
    }

    @GraphQLQuery(name = "findById", description = "Find DevicePosition by id")
    public DevicePositionVO findById(
            @GraphQLArgument(name = "id") Integer id,
            @GraphQLArgument(name = "fetchOptions") DevicePositionFetchOptions fetchOptions) {
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
