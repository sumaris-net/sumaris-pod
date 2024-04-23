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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.ForbiddenException;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeUtils;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.security.AuthService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("dataAccessControlService")
@RequiredArgsConstructor
public class DataAccessControlServiceImpl implements DataAccessControlService {



    /**
     * By default (configuration not loaded: no access)
     */
    private ImmutableList<Integer> authorizedProgramIds = ImmutableList.of(NO_ACCESS_FAKE_ID);
    private ImmutableList<Integer> accessNotSelfDataDepartmentIds = ImmutableList.of(NO_ACCESS_FAKE_ID);

    private ImmutableList<Integer> writeProgramPrivilegeIds = ProgramPrivilegeUtils.getWriteIds();

    private String accessNotSelfDataMinRole = "ROLE_ADMIN";

    protected final SumarisServerConfiguration configuration;

    protected final ProgramRepository programRepository;

    protected final AuthService authService;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        authorizedProgramIds = toListOrNull(configuration.getAuthorizedProgramIds());
        accessNotSelfDataDepartmentIds = toListOrNull(configuration.getAccessNotSelfDataDepartmentIds());
        accessNotSelfDataMinRole = StringUtils.trimToNull(configuration.getAccessNotSelfDataMinRole());
        writeProgramPrivilegeIds = ProgramPrivilegeUtils.getWriteIds(); // Refresh this list, because enums can has changed
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
//        PersonVO user = authService.getAuthenticatedUser().orElse(null);
//        if (user == null) return;;
//        int userDepartmentId = user.getDepartment().getId();
//        if (userDepartmentId !== recorderDepartmentId && accessNotSelfDataMinRole.);

    }

    @Override
    public void checkCanWrite(IRootDataVO data) {
        Preconditions.checkNotNull(data.getProgram());
        Preconditions.checkNotNull(data.getProgram().getId());

        boolean authorized = getAuthorizedProgramIds(new Integer[]{data.getProgram().getId()}, writeProgramPrivilegeIds)
            .map(ArrayUtils::isNotEmpty)
            .orElse(false);

        // TODO check program location ?

        // User has no rights: no access
        if (!authorized) throw new UnauthorizedException();

    }

    @Override
    public <T extends IRootDataVO> void checkCanWriteAll(Collection<T> data) {
        Integer[] programIds = data.stream().map(IRootDataVO::getProgram)
            .map(ProgramVO::getId)
            .collect(Collectors.toSet())
            .toArray(Integer[]::new);
        boolean authorized = getAuthorizedProgramIds(programIds)
            .map(authorizedProgramIds -> authorizedProgramIds.length == programIds.length)
            .orElse(false);

        // User has no rights: no access
        if (!authorized) throw new UnauthorizedException();
    }

    @Override
    public Optional<Integer[]> getAuthorizedProgramIdsByUserId(int userId, Integer[] programIds) {
        return getAuthorizedProgramIdsByUserId(userId, programIds, null);
    }

    //@Override
    public Optional<Integer[]> getAuthorizedProgramIds(Integer[] programIds, List<Integer> programPrivilegeIds) {

        // Admin
        if (authService.isAdmin()) return getAllAuthorizedProgramIds(programIds);

        // Other user
        return authService.getAuthenticatedUserId()
            .flatMap(userId -> getAuthorizedProgramIdsByUserId(userId, programIds, programPrivilegeIds));
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
    public Optional<Integer[]> getAuthorizedLocationIds(Integer[] programIds, Integer[] locationIds) {
        // Admin
        if (authService.isAdmin()) return Optional.of(locationIds);

        // Other user
        return authService.getAuthenticatedUserId()
            .flatMap(userId -> getAuthorizedLocationIdsByUserId(userId, programIds, locationIds));
    }

    @Override
    public Optional<Integer[]> getAuthorizedLocationIdsByUserId(int userId, Integer[] programIds, Integer[] locationIds) {
        List<Integer> userLocationIds = programRepository.getProgramLocationIdsByUserId(userId, programIds);

        // User cannot access any locations
        if (CollectionUtils.isEmpty(userLocationIds)) return Optional.empty(); // No access

        // User can access to all locations
        if (CollectionUtils.containsAny(userLocationIds, (Integer)null)) {
            // Should return a NOT empty value
            return Optional.of(locationIds != null ? locationIds : new Integer[0]);
        }

        // Intersection between expected and authorized
        Collection<Integer> intersection = CollectionUtils.intersection(
            ImmutableList.copyOf(locationIds),
            userLocationIds
        );

        return CollectionUtils.isEmpty(intersection)
            ? Optional.empty() // No access, if intersection is empty
            : Optional.of(intersection.toArray(Integer[]::new));
    }

    @Override
    public Optional<Integer[]> getAllAuthorizedProgramIds(Integer[] programIds) {
        return getAllAuthorizedProgramIdsAsCollection(programIds)
            .map(this::toArray);
    }

    @Override
    public List<Integer> getAuthorizedProgramIdsByUserId(int userId) {
        return programRepository.getProgramIdsByUserIdAndPrivilegeIds(userId, null /*= ALL */);
    }

    /* -- protected functions -- */

    protected Optional<Collection<Integer>> getAllAuthorizedProgramIdsAsCollection(Integer[] programIds) {
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

    protected Optional<Integer[]> getAuthorizedProgramIdsByUserId(int userId, Integer[] programIds, List<Integer> programPrivilegeIds) {
        // To get allowed program ids, we made intersection with not empty lists.

        // User has no rights: no access
        final Collection<Integer> userProgramIds = programRepository.getProgramIdsByUserIdAndPrivilegeIds(userId, programPrivilegeIds);
        if (CollectionUtils.isEmpty(userProgramIds)) return Optional.empty();

        return getAllAuthorizedProgramIdsAsCollection(programIds)
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

    protected Integer[] toArray(@NonNull Collection<Integer> items) {
        return items.toArray(Integer[]::new);
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
