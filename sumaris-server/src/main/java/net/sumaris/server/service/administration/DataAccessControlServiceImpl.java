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
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.ForbiddenException;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.PersonVO;
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

@Service("dataAccessControlService")
public class DataAccessControlServiceImpl implements DataAccessControlService {

    public static final Integer FAKE_ID = -999;

    /**
     * By default (configuration not loaded: no access)
     */
    private List<Integer> authorizedProgramIds = ImmutableList.of(FAKE_ID);
    private List<Integer> accessNotSelfDataDepartmentIds = ImmutableList.of(FAKE_ID);
    private String accessNotSelfDataMinRole = "ROLE_ADMIN";

    @Autowired
    protected SumarisServerConfiguration configuration;

    @Autowired
    protected ProgramRepository programRepository;

    @Autowired
    protected AuthService authService;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        authorizedProgramIds = configuration.getAuthorizedProgramIds();
        accessNotSelfDataDepartmentIds = configuration.getAccessNotSelfDataDepartmentIds();
        accessNotSelfDataMinRole = configuration.getAccessNotSelfDataMinRole();
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
    public Integer[] getAuthorizedProgramIds(Integer[] programIds) {

        // Admin
        if (authService.isAdmin()) return getAllAuthorizedProgramIds(programIds);

        // Other user
        return authService.getAuthenticatedUserId()
            .map(userId -> getAuthorizedProgramIdsByUserId(userId, programIds))
            // Guest: can see all programs
            // /!\ This case can only occur on referential access (see ReferentialGraphQLService)
            // Data graphQL access should be protected by @IsUser(), to required a logged in user
            .orElseGet(() -> getAllAuthorizedProgramIds(programIds));
    }

    @Override
    public Integer[] getAllAuthorizedProgramIds(Integer[] programIds) {
        return toArrayOrNull(getAuthorizedProgramIdsAsCollection(programIds));
    }

    @Override
    public Integer[] getAuthorizedProgramIdsByUserId(int userId, Integer[] programIds) {
        // To get allowed program ids, we made intersection with not empty lists.

        // User has no rights: no access
        Collection<Integer> userProgramIds = programRepository.getProgramIdsByUserId(userId);
        if (CollectionUtils.isEmpty(userProgramIds)) return new Integer[]{FAKE_ID};

        // All programs authorized: return requested programs
        Collection<Integer> authorizedProgramIds = getAuthorizedProgramIdsAsCollection(programIds);
        if (CollectionUtils.isEmpty(authorizedProgramIds)) return userProgramIds.toArray(new Integer[0]);

        // Intersect authorized (by config) and user programs
        Collection<Integer> userAuthorizedProgramIds = Beans.intersection(
            authorizedProgramIds,
            userProgramIds
        );

        // If intersection is empty (=no access): return a FAKE program id
        return toNotEmptyArray(userAuthorizedProgramIds);
    }

    /* -- protected functions -- */

    protected Collection<Integer> getAuthorizedProgramIdsAsCollection(Integer[] programIds) {
        // Nothing limited by config: all requested programs are authorized
        if (CollectionUtils.isEmpty(authorizedProgramIds)) return toCollectionOrNull(programIds);

        // Nothing requested: all authorized program
        if (ArrayUtils.isEmpty(programIds)) return authorizedProgramIds;

        // Intersection between expected and authorized
        Collection<Integer> intersectProgramIds = Beans.intersection(
            ImmutableList.copyOf(programIds),
            authorizedProgramIds
        );

        // If intersection is empty (=no access): return a FAKE program id
        return toNotEmptyCollection(intersectProgramIds);
    }

    protected Integer[] toNotEmptyArray(Collection<Integer> items) {
        return CollectionUtils.isNotEmpty(items) ? items.toArray(new Integer[0]) : new Integer[]{FAKE_ID};
    }

    protected Collection<Integer> toNotEmptyCollection(Collection<Integer> items) {
        return CollectionUtils.isNotEmpty(items) ? items : ImmutableList.of(FAKE_ID);
    }

    protected Integer[] toArrayOrNull(Collection<Integer> items) {
        return CollectionUtils.isEmpty(items) ? null : items.toArray(new Integer[items.size()]);
    }

    protected Collection<Integer> toCollectionOrNull(Integer[] items) {
        return ArrayUtils.isEmpty(items) ? null : ImmutableList.copyOf(items);
    }
}
