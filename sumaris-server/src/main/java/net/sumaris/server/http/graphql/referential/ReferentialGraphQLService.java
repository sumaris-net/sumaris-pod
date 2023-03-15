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

package net.sumaris.server.http.graphql.referential;

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.ReferentialEntities;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.vo.filter.MetierFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.*;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLHelper;
import net.sumaris.server.http.graphql.GraphQLUtils;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.administration.DataAccessControlService;
import net.sumaris.server.service.technical.EntityWatchService;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@GraphQLApi
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReferentialGraphQLService {

    private final ReferentialService referentialService;

    private final TaxonGroupService taxonGroupService;

    private final MetierRepository metierRepository;

    private final EntityWatchService entityWatchService;

    private final DataAccessControlService dataAccessControlService;

    private final AuthService authService;

    /* -- Referential queries -- */

    @GraphQLQuery(name = "lastUpdateDate", description = "Get last update date of all referential")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Date getLastUpdateDate() {
        return referentialService.getLastUpdateDate();
    }

    @GraphQLQuery(name = "referentialTypes", description = "Get all types of referential")
    @Transactional(readOnly = true)
    public List<ReferentialTypeVO> getAllReferentialTypes() {
        return referentialService.getAllTypes();
    }

    @GraphQLQuery(name = "referentials", description = "Search in referentials")
    @Transactional(readOnly = true)
    public List<? extends ReferentialVO> findReferentialsByFilter(
            @GraphQLArgument(name = "entityName") String entityName,
            @GraphQLArgument(name = "filter") ReferentialFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLEnvironment() ResolutionEnvironment env) {



        // Metier: special case to be able to sort on join attribute (e.g. taxonGroup)
        if (Metier.class.getSimpleName().equalsIgnoreCase(entityName)) {
            return metierRepository.findByFilter(
                    MetierFilterVO.nullToEmpty(filter),
                    offset, size, sort,
                    SortDirection.valueOf(direction.toUpperCase()));
        }

        // Restrict access to program
        if (Program.class.getSimpleName().equalsIgnoreCase(entityName)) {
            restrictProgramFilter(entityName, filter);
        }

        Set<String> fields = GraphQLUtils.fields(env);

        return referentialService.findByFilter(entityName,
                ReferentialFilterVO.nullToEmpty(filter),
                offset == null ? 0 : offset,
                size == null ? 1000 : size,
                sort == null ? ReferentialVO.Fields.LABEL : sort,
                SortDirection.fromString(direction, SortDirection.ASC),
                ReferentialFetchOptions.builder()
                    .withProperties(fields.contains(ReferentialVO.Fields.PROPERTIES))
                    .build()
            );
    }

    @GraphQLQuery(name = "referentialsCount", description = "Get referentials count")
    @Transactional(readOnly = true)
    public Long getReferentialsCount(@GraphQLArgument(name = "entityName") String entityName,
                                     @GraphQLArgument(name = "filter") ReferentialFilterVO filter) {
        // Restrict access to program
        restrictProgramFilter(entityName, filter);

        return referentialService.countByFilter(entityName, filter);
    }


    @GraphQLQuery(name = "metiers", description = "Search in metiers")
    @Transactional(readOnly = true)
    public List<MetierVO> findMetiersByFilter(
            @GraphQLArgument(name = "filter") MetierFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.NAME) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        return metierRepository.findByFilter(
                MetierFilterVO.nullToEmpty(filter),
                offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "metiersCount", description = "Count metiers")
    @Transactional(readOnly = true)
    public Long countMetiers(@GraphQLArgument(name = "filter") MetierFilterVO filter) {
        return metierRepository.count(filter);
    }

    @GraphQLQuery(name = "metier", description = "Get a metier by id")
    @Transactional(readOnly = true)
    public MetierVO getMetierById(@GraphQLArgument(name = "id") int id) {
        return metierRepository.get(id);
    }


    @GraphQLQuery(name = "referentialLevels", description = "Get all levels from entityName")
    @Transactional(readOnly = true)
    public List<ReferentialVO> getAllReferentialLevels(
            @GraphQLArgument(name = "entityName") String entityName) {
        return referentialService.getAllLevels(entityName);
    }


    @GraphQLMutation(name = "saveReferential", description = "Create or update a referential")
    @IsAdmin
    public ReferentialVO saveReferential(
            @GraphQLArgument(name = "referential") ReferentialVO referential) {
        return referentialService.save(referential);
    }

    @GraphQLSubscription(name = "updateReferential", description = "Subscribe to changes on a referential")
    @IsUser
    public Publisher<ReferentialVO> updateReferential(@GraphQLNonNull @GraphQLArgument(name = "entityName") final String entityName,
                                                      @GraphQLNonNull @GraphQLArgument(name = "id") final int id,
                                                      @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer minIntervalInSecond) {
        Preconditions.checkNotNull(entityName, "Missing 'entityName'");
        Preconditions.checkArgument(id >= 0, "Invalid 'id'");

        return entityWatchService.watchEntity(
                ReferentialEntities.getEntityClass(entityName),
                ReferentialVO.class, id, minIntervalInSecond, true)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLMutation(name = "saveReferentials", description = "Create or update many referential")
    @IsAdmin
    public List<ReferentialVO> saveReferentials(
            @GraphQLArgument(name = "referentials") List<ReferentialVO> referential) {
        return referentialService.save(referential);
    }

    @GraphQLMutation(name = "deleteReferential", description = "Delete a referential (by id)")
    @IsAdmin
    public void deleteReferential(
            @GraphQLArgument(name = "entityName") String entityName,
            @GraphQLArgument(name = "id") int id) {
        referentialService.delete(entityName, id);
    }

    @GraphQLMutation(name = "deleteReferentials", description = "Delete many referential (by ids)")
    @IsAdmin
    public void deleteReferentials(
            @GraphQLArgument(name = "entityName") String entityName,
            @GraphQLArgument(name = "ids") List<Integer> ids) {
        referentialService.delete(entityName, ids);
    }

    /* -- Fetch sub properties (level, parent) -- */

    @GraphQLQuery(name = "level", description = "Get the level from a referential entity")
    @Transactional(readOnly = true)
    public ReferentialVO getReferentialLevel(@GraphQLContext ReferentialVO referential) {
        return referentialService.getLevelById(referential.getEntityName(), referential.getLevelId());
    }

    @GraphQLQuery(name = "parent", description = "Get referential's parent")
    public ReferentialVO getReferentialParent(@GraphQLContext ReferentialVO entity) {
        if (entity.getParent() != null) return entity.getParent();
        if (entity.getParentId() == null || entity.getEntityName() == null) return null;
        return referentialService.get(entity.getEntityName(), entity.getParentId());
    }

    /* -- taxon -- */

    @GraphQLQuery(name = "taxonGroups", description = "Search in taxon groups")
    public List<TaxonGroupVO> getTaxonGroupByFilter(@GraphQLArgument(name = "filter") ReferentialFilterVO filter,
                                                    @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                    @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                    @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.NAME) String sort,
                                                    @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        return taxonGroupService.findAllByFilter(
                ReferentialFilterVO.nullToEmpty(filter),
                offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "taxonGroupsCount", description = "Count taxon groups")
    @Transactional(readOnly = true)
    public Long countTaxonGroups(@GraphQLArgument(name = "filter") ReferentialFilterVO filter) {
        return referentialService.countByFilter("TaxonGroup", filter);
    }

    /* -- protected functions -- */

    protected void restrictProgramFilter(@NonNull String entityName, @NonNull ReferentialFilterVO filter) {

        // Program
        if (Program.class.getSimpleName().equalsIgnoreCase(entityName)) {

            Integer[] programIds = filter.getId() != null ? new Integer[]{filter.getId()} : filter.getIncludedIds();

            // Limit to authorized ids
            Integer[] authorizedProgramIds = dataAccessControlService.getAuthorizedProgramIds(programIds)
                .orElse(DataAccessControlService.NO_ACCESS_FAKE_IDS);

            // Reset id, as it has been deprecated
            if (filter.getId() != null) {
                filter.setId(null);
                GraphQLHelper.logDeprecatedUse(authService, "ReferentialFilterVO.id", "1.24.0");
            }

            // Apply limitations
            filter.setIncludedIds(authorizedProgramIds);
        }

        // TODO: other entities ? e.g. 'Location' ?

    }

}
