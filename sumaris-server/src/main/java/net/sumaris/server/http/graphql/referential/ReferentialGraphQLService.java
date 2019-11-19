package net.sumaris.server.http.graphql.referential;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import net.sumaris.server.http.security.IsAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReferentialGraphQLService {

    private static final Logger log = LoggerFactory.getLogger(ReferentialGraphQLService.class);

    @Autowired
    private ReferentialService referentialService;

    @Autowired
    private TaxonNameService taxonNameService;

    @Autowired
    private TaxonGroupService taxonGroupService;

    @Autowired
    private MetierRepository metierRepository;

    /* -- Referential queries -- */

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
            @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.NAME) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {


        // TODO: not used in app: remove
        //if ("TargetSpecies".equals(entityName)) {
        //    return taxonGroupService.findTargetSpeciesByFilter(
        //            filter != null ? filter : new ReferentialFilterVO(),
        //            offset, size, sort,
        //            SortDirection.valueOf(direction.toUpperCase()));
        //}

        // Special case
        if ("Metier".equals(entityName)) {
            return metierRepository.findByFilter(
                    filter != null ? filter : new ReferentialFilterVO(),
                    offset, size, sort,
                    SortDirection.valueOf(direction.toUpperCase()));
        }

        return referentialService.findByFilter(entityName, filter, offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "referentialsCount", description = "Get referentials count")
    @Transactional(readOnly = true)
    public Long getReferentialsCount(@GraphQLArgument(name = "entityName") String entityName) {
        return referentialService.count(entityName);
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

    @GraphQLQuery(name = "taxonNames", description = "Search in taxon names")
    @Transactional(readOnly = true)
    public List<TaxonNameVO> findTaxonNames(
            @GraphQLArgument(name = "filter") TaxonNameFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.NAME) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        filter = filter != null ? filter : new TaxonNameFilterVO();
        return taxonNameService.findByFilter(filter, offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));
    }

}
