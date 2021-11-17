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
import net.sumaris.core.dao.referential.ReferentialEntities;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.MetierFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.*;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.technical.ChangesPublisherService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@GraphQLApi
@Transactional
public class ReferentialGraphQLService {

    @Autowired
    private ReferentialService referentialService;

    @Autowired
    private TaxonGroupService taxonGroupService;

    @Autowired
    private MetierRepository metierRepository;

    @Autowired
    private ChangesPublisherService changesPublisherService;

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
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        // Special case
        if (Metier.class.getSimpleName().equals(entityName)) {
            return metierRepository.findByFilter(
                    ReferentialFilterVO.nullToEmpty(filter),
                    offset, size, sort,
                    SortDirection.valueOf(direction.toUpperCase()));
        }

        return referentialService.findByFilter(entityName,
                ReferentialFilterVO.nullToEmpty(filter),
                offset == null ? 0 : offset,
                size == null ? 1000 : size,
                sort == null ? ReferentialVO.Fields.LABEL : sort,
                SortDirection.fromString(direction, SortDirection.ASC));
    }

    @GraphQLQuery(name = "referentialsCount", description = "Get referentials count")
    @Transactional(readOnly = true)
    public Long getReferentialsCount(@GraphQLArgument(name = "entityName") String entityName,
                                     @GraphQLArgument(name = "filter") ReferentialFilterVO filter) {
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

    @GraphQLQuery(name = "level", description = "Get the level from a referential entity")
    @Transactional(readOnly = true)
    public ReferentialVO getReferentialLevel(
            @GraphQLContext ReferentialVO referential) {
        return referentialService.getLevelById(referential.getEntityName(), referential.getLevelId());
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

        return changesPublisherService.getPublisher(
                ReferentialEntities.getEntityClass(entityName),
                ReferentialVO.class, id, minIntervalInSecond, true);
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

    /* -- taxon -- */

    @GraphQLQuery(name = "taxonGroupIds", description = "Get taxon groups from a taxon name")
    public List<Integer> getTaxonGroupIdsByTaxonName(@GraphQLContext TaxonNameVO taxonNameVO) {
        if (taxonNameVO.getReferenceTaxonId() != null) {
            return taxonGroupService.getAllIdByReferenceTaxonId(taxonNameVO.getReferenceTaxonId(), new Date(), null);
        }
        // Should never occur !
        return null;
    }

    @GraphQLQuery(name = "taxonGroups", description = "Get taxon groups from a taxon name")
    public List<TaxonGroupVO> getTaxonGroupByFilter  (@GraphQLArgument(name = "filter") ReferentialFilterVO filter,
                                                      @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                      @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                      @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.NAME) String sort,
                                                      @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        return taxonGroupService.findTargetSpeciesByFilter(
                ReferentialFilterVO.nullToEmpty(filter),
                offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "taxonGroupsCount", description = "Count taxonGroups")
    @Transactional(readOnly = true)
    public Long countTaxonGroups(@GraphQLArgument(name = "filter") ReferentialFilterVO filter) {
        return referentialService.countByFilter("TaxonGroup", filter);
    }

    /* -- protected functions -- */

    protected TaxonNameFetchOptions getFetchOptions(Set<String> fields) {
        return TaxonNameFetchOptions.builder()
            .withParentTaxonName(fields.contains(StringUtils.slashing(TaxonNameVO.Fields.PARENT_TAXON_NAME, IEntity.Fields.ID)))
            .withTaxonomicLevel(fields.contains(StringUtils.slashing(TaxonNameVO.Fields.TAXONOMIC_LEVEL, IEntity.Fields.ID)))
            .build();
    }
}
