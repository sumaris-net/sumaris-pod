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
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.vo.filter.MetierFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import net.sumaris.server.http.security.IsAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ReferentialGraphQLService {

    @Autowired
    private ReferentialService referentialService;

    @Autowired
    private TaxonNameService taxonNameService;

    @Autowired
    private TaxonGroupService taxonGroupService;

    @Autowired
    private MetierRepository metierRepository;

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
                    filter != null ? filter : new ReferentialFilterVO(),
                    offset, size, sort,
                    SortDirection.valueOf(direction.toUpperCase()));
        }

        return referentialService.findByFilter(entityName, filter,
                offset == null ? 0 : offset,
                size == null ? 1000 : size,
                sort == null ? ReferentialVO.Fields.LABEL : sort,
                direction == null ? SortDirection.ASC : SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "metiers", description = "Search in metiers")
    @Transactional(readOnly = true)
    public List<MetierVO> findMetiersByFilter(
            @GraphQLArgument(name = "filter") MetierFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.NAME) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        if (filter == null)
            filter = new MetierFilterVO();

//        return metierRepository.findAll(filter, offset, size, sort, SortDirection.valueOf(direction.toUpperCase()), null).getContent();

        return metierRepository.findByFilter(filter, offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));

    }

    @GraphQLQuery(name = "metier", description = "Get a metier by id")
    @Transactional(readOnly = true)
    public MetierVO getMetierById(@GraphQLArgument(name = "id") int id) {
        return metierRepository.get(id);
    }

    @GraphQLQuery(name = "referentialsCount", description = "Get referentials count")
    @Transactional(readOnly = true)
    public Long getReferentialsCount(@GraphQLArgument(name = "entityName") String entityName,
                                     @GraphQLArgument(name = "filter") ReferentialFilterVO filter) {
        return referentialService.countByFilter(entityName, filter);
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
        List<TaxonNameVO> result = taxonNameService.findByFilter(filter, offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));



        return result;
    }

    @GraphQLQuery(name = "taxonGroupIds", description = "Get taxon groups from a taxon name")
    public List<Integer> getTaxonGroupIdsByTaxonName(@GraphQLContext TaxonNameVO taxonNameVO) {
        if (taxonNameVO.getReferenceTaxonId() != null) {
            return taxonGroupService.getAllIdByReferenceTaxonId(taxonNameVO.getReferenceTaxonId(), new Date(), null);
        }
        // Should never occur !
        return null;
    }

}
