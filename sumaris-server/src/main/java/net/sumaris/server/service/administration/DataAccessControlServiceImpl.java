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

package net.sumaris.server.service.administration;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.ForbiddenException;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.security.AuthService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service("dataAccessControlService")
public class DataAccessControlServiceImpl implements DataAccessControlService {


    /**
     * By default (configuration not loaded: no access)
     */
    private ImmutableList<Integer> authorizedProgramIds = ImmutableList.of(NO_ACCESS_FAKE_ID);
    private ImmutableList<Integer> accessNotSelfDataDepartmentIds = ImmutableList.of(NO_ACCESS_FAKE_ID);
    private String accessNotSelfDataMinRole = "ROLE_ADMIN";

    @Autowired
    protected SumarisServerConfiguration configuration;

    @Autowired
    protected ProgramRepository programRepository;

    @Autowired
    protected AuthService authService;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        authorizedProgramIds = toListOrNull(configuration.getAuthorizedProgramIds());
        accessNotSelfDataDepartmentIds = toListOrNull(configuration.getAccessNotSelfDataDepartmentIds());
        accessNotSelfDataMinRole = StringUtils.trimToNull(configuration.getAccessNotSelfDataMinRole());
    }

    @Override
    public boolean canUserAccessNotSelfData() {
        return accessNotSelfDataMinRole == null || authService.hasAuthority(accessNotSelfDataMinRole);
    }

    @Override
    public boolean canDepartmentAccessNotSelfData(@NonNull Integer actualDepartmentId) {
        return accessNotSelfDataDepartmentIds == null || accessNotSelfDataDepartmentIds.contains(actualDepartmentId);
    }

    /**
     * Check user is admin
     */
    @Override
    public void checkIsAdmin(String message) {
        if (!authService.isAdmin()) throw new ForbiddenException(message != null ? message : "Access forbidden");
    }

    @Override
    public void checkCanRead(IRootDataVO data) {

        // TODO
        // Restrict to self department data
        /*PersonVO user = authService.getAuthenticatedUser().orElse(null);
        int userDepartmentId = user.getDepartment().getId();
        if (userDepartmentId !== recorderDepartmentId && accessNotSelfDataMinRole.)*/

    }

    @Override
    public void checkCanWrite(IRootDataVO data) {
        // TODO
    }

    @Override
    public Optional<Integer[]> getAuthorizedProgramIds(Integer[] programIds) {

        // Admin
        if (authService.isAdmin()) return getAllAuthorizedProgramIds(programIds);

        // Other user
        return authService.getAuthenticatedUserId()
            .flatMap(userId -> getAuthorizedProgramIdsByUserId(userId, programIds));
    }

    @Override
    public Optional<Integer[]> getAllAuthorizedProgramIds(Integer[] programIds) {
        return getAuthorizedProgramIdsAsCollection(programIds)
            .map(this::toArray);
    }

    @Override
    public List<Integer> getAuthorizedProgramIdsByUserId(int userId) {
        return programRepository.getProgramIdsByUserId(userId);
    }

    @Override
    public Optional<Integer[]> getAuthorizedProgramIdsByUserId(int userId, Integer[] programIds) {
        // To get allowed program ids, we made intersection with not empty lists.

        // User has no rights: no access
        final Collection<Integer> userProgramIds = programRepository.getProgramIdsByUserId(userId);
        if (CollectionUtils.isEmpty(userProgramIds)) return Optional.empty();

        return getAuthorizedProgramIdsAsCollection(programIds)
            .flatMap(authorizedProgramIds -> {

                // No restriction: only user's programs
                if (CollectionUtils.isEmpty(authorizedProgramIds)) return Optional.of(toNotNullList(userProgramIds));

                // Intersect authorized (by config) and user programs
                Collection<Integer> intersection = CollectionUtils.intersection(
                    authorizedProgramIds,
                    userProgramIds
                );

                return CollectionUtils.isEmpty(intersection)
                    ? Optional.empty() // No access
                    : Optional.of(intersection);
            })
            .map(this::toArray);
    }

    /* -- protected functions -- */

    protected Optional<Collection<Integer>> getAuthorizedProgramIdsAsCollection(Integer[] programIds) {
        // Nothing limited by config: all requested programs are authorized
        if (authorizedProgramIds == null) return Optional.of(toNotNullList(programIds));

        // Nothing requested: all authorized program
        if (ArrayUtils.isEmpty(programIds)) return Optional.of(authorizedProgramIds);

        // Intersection between expected and authorized
        Collection<Integer> intersection = CollectionUtils.intersection(
            ImmutableList.copyOf(programIds),
            authorizedProgramIds
        );

        return CollectionUtils.isEmpty(intersection)
            ? Optional.empty() // No access, if no intersection
            : Optional.of(intersection);
    }

    protected Collection<Integer> toNotEmptyCollection(Collection<Integer> items) {
        return CollectionUtils.isNotEmpty(items) ? items : ImmutableList.of(NO_ACCESS_FAKE_ID);
    }

    protected Integer[] toArray(@NonNull Collection<Integer> items) {
        return items.toArray(new Integer[items.size()]);
    }

    protected ImmutableList<Integer> toNotNullList(Integer[] items) {
        return ArrayUtils.isEmpty(items) ? ImmutableList.of() : ImmutableList.copyOf(items);
    }

    protected ImmutableList<Integer> toNotNullList(Collection<Integer> items) {
        return CollectionUtils.isEmpty(items) ? ImmutableList.of() : ImmutableList.copyOf(items);
    }

    protected ImmutableList<Integer> toListOrNull(Collection<Integer> items) {
        return CollectionUtils.isEmpty(items) ? null : ImmutableList.copyOf(items);
    }
}
