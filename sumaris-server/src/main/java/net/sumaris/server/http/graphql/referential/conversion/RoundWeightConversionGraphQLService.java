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

package net.sumaris.server.http.graphql.referential.conversion;

import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.conversion.RoundWeightConversionService;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLUtils;
import net.sumaris.server.http.security.IsAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@Service
@GraphQLApi
@Transactional
public class RoundWeightConversionGraphQLService {

    @Resource
    private RoundWeightConversionService service;

    @Resource
    private LocationService locationService;

    @Resource
    private TaxonGroupService taxonGroupService;

    @Resource
    private ReferentialService referentialService;

    /* -- Referential queries -- */

    @GraphQLQuery(name = "roundWeightConversions", description = "Search in round weight conversions")
    @Transactional(readOnly = true)
    public List<RoundWeightConversionVO> findByFilter(
            @GraphQLArgument(name = "filter") RoundWeightConversionFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = RoundWeightConversionVO.Fields.ID) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLEnvironment ResolutionEnvironment env) {

        Page page = Page.builder()
            .offset(offset != null ? offset : 0)
            .size(size != null ? size : 1000)
            .sortBy(sort == null ? RoundWeightConversionVO.Fields.ID : sort)
            .sortDirection(SortDirection.fromString(direction, SortDirection.ASC))
            .build();
        RoundWeightConversionFetchOptions fetchOptions = getFetchOptions(GraphQLUtils.fields(env));

        return service.findByFilter(filter, page, fetchOptions);
    }

    @GraphQLQuery(name = "roundWeightConversionsCount", description = "Search in round weight conversions")
    @Transactional(readOnly = true)
    public Long countByFilter(@GraphQLArgument(name = "filter") RoundWeightConversionFilterVO filter) {

        return service.countByFilter(filter);
    }

    @GraphQLMutation(name = "saveRoundWeightConversions", description = "Save many round weight conversions")
    @IsAdmin
    public List<RoundWeightConversionVO> saveAll(
        @GraphQLArgument(name = "data") List<RoundWeightConversionVO> sources) {
        return service.saveAll(sources);
    }

    @GraphQLMutation(name = "deleteRoundWeightConversions", description = "Delete many round weight conversions")
    @IsAdmin
    public void deleteAllById(
        @GraphQLArgument(name = "ids") List<Integer> ids) {
        service.deleteAllById(ids);
    }

    @GraphQLQuery(name = "taxonGroup", description = "Get round weight conversion's taxon group")
    public TaxonGroupVO getTaxonGroup(@GraphQLContext RoundWeightConversionVO source) {
        if (source.getTaxonGroup() != null) return source.getTaxonGroup();
        if (source.getTaxonGroupId() == null) return null;
        return taxonGroupService.get(source.getTaxonGroupId());
    }

    @GraphQLQuery(name = "location", description = "Get round weight conversion's location")
    public LocationVO getLocation(@GraphQLContext RoundWeightConversionVO source) {
        if (source.getLocation() != null) return source.getLocation();
        if (source.getLocationId() == null) return null;
        return locationService.get(source.getLocationId());
    }

    @GraphQLQuery(name = "dressing", description = "Get round weight conversion's dressing")
    public ReferentialVO getDressing(@GraphQLContext RoundWeightConversionVO source) {
        if (source.getDressing() != null) return source.getDressing();
        if (source.getDressingId() == null) return null;
        return referentialService.get(QualitativeValue.class, source.getDressingId());
    }

    @GraphQLQuery(name = "preserving", description = "Get round weight conversion's preserving")
    public ReferentialVO getPreserving(@GraphQLContext RoundWeightConversionVO source) {
        if (source.getPreserving() != null) return source.getPreserving();
        if (source.getPreservingId() == null) return null;
        return referentialService.get(QualitativeValue.class, source.getPreservingId());
    }

    private RoundWeightConversionFetchOptions getFetchOptions(Set<String> fields) {
        return RoundWeightConversionFetchOptions.builder()
            .withLocation(fields.contains(WeightLengthConversionVO.Fields.LOCATION))
            .build();
    }
}
