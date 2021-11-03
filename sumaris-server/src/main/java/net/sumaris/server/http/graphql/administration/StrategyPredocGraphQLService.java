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

package net.sumaris.server.http.graphql.administration;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.location.LocationClassificationEnum;
import net.sumaris.core.service.administration.programStrategy.StrategyPredocService;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@GraphQLApi
public class StrategyPredocGraphQLService {

    @Autowired
    private StrategyPredocService strategyPredocService;

    /* -- Referential queries -- */

    @GraphQLQuery(name = "strategiesReferentials", description = "Get already filled values from entityName")
    public List<? extends ReferentialVO> findStrategiesReferentials(
            @GraphQLArgument(name = "entityName") String entityName,
            @GraphQLArgument(name = "programId") int programId,
            @GraphQLArgument(name = "locationClassification", description = "only for Location entities") LocationClassificationEnum locationClassification,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "100") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        return strategyPredocService.findStrategiesReferentials(entityName, programId, locationClassification,
                offset == null ? 0 : offset,
                size == null ? 100 : size,
                sort == null ? ReferentialVO.Fields.LABEL : sort,
                direction == null ? SortDirection.ASC : SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "strategiesAnalyticReferences", description = "Get already filled analytic references")
    public List<String> findStrategiesAnalyticReferences(
            @GraphQLArgument(name = "programId") int programId) {
        return strategyPredocService.findStrategiesAnalyticReferences(programId);
    }

    @GraphQLQuery(name = "strategiesDepartments", description = "Get already filled departments")
    public List<Integer> findStrategiesDepartments(
            @GraphQLArgument(name = "programId") int programId) {
        return strategyPredocService.findStrategiesDepartments(programId);
    }

    @GraphQLQuery(name = "strategiesLocations", description = "Get already filled locations")
    public List<Integer> findStrategiesLocations(
            @GraphQLArgument(name = "programId") int programId,
            @GraphQLArgument(name = "locationClassification") LocationClassificationEnum locationClassification) {
        return strategyPredocService.findStrategiesLocations(programId, locationClassification);
    }

    @GraphQLQuery(name = "strategiesTaxonNames", description = "Get already filled taxon names")
    public List<Integer> findStrategiesTaxonNames(
            @GraphQLArgument(name = "programId") int programId) {
        return strategyPredocService.findStrategiesTaxonNames(programId);
    }

    @GraphQLQuery(name = "strategiesPmfms", description = "Get already filled PMFM or one of parameter, matrix, fraction, method")
    public List<Integer> findStrategiesPmfms(
            @GraphQLArgument(name = "programId") int programId,
            @GraphQLArgument(name = "referenceTaxonId") Integer referenceTaxonId,
            @GraphQLArgument(name = "field", defaultValue = PmfmStrategy.Fields.PMFM) String field) {
        return strategyPredocService.findStrategiesPmfms(programId, referenceTaxonId,
                field == null ? PmfmStrategy.Fields.PMFM : field);
    }

}
