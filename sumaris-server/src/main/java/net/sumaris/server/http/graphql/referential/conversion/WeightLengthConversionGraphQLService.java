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
import net.sumaris.core.service.referential.conversion.WeightLengthConversionService;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.vo.referential.*;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
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
public class WeightLengthConversionGraphQLService {


    @Resource
    private WeightLengthConversionService service;

    @Resource
    private LocationService locationService;

    @Resource
    private TaxonNameService taxonNameService;

    @Resource
    private ReferentialService referentialService;

    /* -- Referential queries -- */

    @GraphQLQuery(name = "weightLengthConversions", description = "Search in weight length conversions")
    @Transactional(readOnly = true)
    public List<WeightLengthConversionVO> findWeightLengthConversionsByFilter(
            @GraphQLArgument(name = "filter") WeightLengthConversionFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = WeightLengthConversionVO.Fields.ID) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLEnvironment ResolutionEnvironment env) {

        Page page = Page.builder()
            .offset(offset != null ? offset : 0)
            .size(size != null ? size : 1000)
            .sortBy(sort == null ? WeightLengthConversionVO.Fields.ID : sort)
            .sortDirection(SortDirection.fromString(direction, SortDirection.ASC))
            .build();
        WeightLengthConversionFetchOptions fetchOptions = getFetchOptions(GraphQLUtils.fields(env));

        return service.findByFilter(filter, page, fetchOptions);
    }

    @GraphQLQuery(name = "weightLengthConversionsCount", description = "Search in weight length conversions")
    @Transactional(readOnly = true)
    public Long count(
        @GraphQLArgument(name = "filter") WeightLengthConversionFilterVO filter) {

        return service.countByFilter(filter);
    }

    @GraphQLMutation(name = "saveWeightLengthConversions", description = "Save many weight length conversions")
    @IsAdmin
    public List<WeightLengthConversionVO> saveAll(
        @GraphQLArgument(name = "data") List<WeightLengthConversionVO> sources) {
        return service.saveAll(sources);
    }

    @GraphQLMutation(name = "deleteWeightLengthConversions", description = "Delete many weight length conversions")
    @IsAdmin
    public void deleteAllById(
        @GraphQLArgument(name = "ids") List<Integer> ids) {
        service.deleteAllById(ids);
    }

    @GraphQLQuery(name = "taxonName", description = "Get round weight conversion's taxon group")
    public TaxonNameVO getTaxonName(@GraphQLContext WeightLengthConversionVO source) {
        if (source.getTaxonName() != null) return source.getTaxonName();
        if (source.getReferenceTaxonId() == null) return null;
        return taxonNameService.get(source.getReferenceTaxonId(), null);
    }

    @GraphQLQuery(name = "location", description = "Get weight length conversion's location")
    public LocationVO getLocation(@GraphQLContext WeightLengthConversionVO source) {
        if (source.getLocation() != null) return source.getLocation();
        if (source.getLocationId() == null) return null;
        return locationService.get(source.getLocationId());
    }

    @GraphQLQuery(name = "sex", description = "Get weight length conversion's sex")
    public ReferentialVO getSex(@GraphQLContext WeightLengthConversionVO source) {
        if (source.getSex() != null) return source.getSex();
        if (source.getSexId() == null) return null;
        return referentialService.get(QualitativeValue.class, source.getSexId());
    }

    @GraphQLQuery(name = "lengthParameter", description = "Get weight length conversion's length parameter")
    public ReferentialVO getLengthParameter(@GraphQLContext WeightLengthConversionVO source) {
        if (source.getLengthParameter() != null) return source.getLengthParameter();
        if (source.getLengthParameterId() == null) return null;
        return referentialService.get(Parameter.class, source.getLengthUnitId());
    }

    @GraphQLQuery(name = "lengthUnit", description = "Get weight length conversion's length unit")
    public ReferentialVO getLengthUnit(@GraphQLContext WeightLengthConversionVO source) {
        if (source.getLengthUnit() != null) return source.getLengthUnit();
        if (source.getLengthUnitId() == null) return null;
        return referentialService.get(Unit.class, source.getLengthUnitId());
    }

    private WeightLengthConversionFetchOptions getFetchOptions(Set<String> fields) {
        return WeightLengthConversionFetchOptions.builder()
            .withLocation(fields.contains(WeightLengthConversionVO.Fields.LOCATION))
            .withLengthPmfmIds(fields.contains(WeightLengthConversionVO.Fields.LENGTH_PMFM_IDS))
            .withRectangleLabels(fields.contains(WeightLengthConversionVO.Fields.RECTANGLE_LABELS))
            .build();
    }
}
