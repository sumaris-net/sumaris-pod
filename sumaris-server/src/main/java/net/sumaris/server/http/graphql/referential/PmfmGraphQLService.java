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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.pmfm.Fraction;
import net.sumaris.core.model.referential.pmfm.Matrix;
import net.sumaris.core.model.referential.pmfm.Method;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.pmfm.ParameterService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.pmfm.ParameterVO;
import net.sumaris.core.vo.referential.pmfm.PmfmFetchOptions;
import net.sumaris.core.vo.referential.pmfm.PmfmVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLUtils;
import net.sumaris.server.http.security.IsAdmin;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@GraphQLApi
@Transactional
public class PmfmGraphQLService {

    @Autowired
    private PmfmService pmfmService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private ReferentialService referentialService;

    /* -- Pmfm -- */

    @GraphQLQuery(name = "pmfms", description = "Search in PMFM")
    @Transactional(readOnly = true)
    public List<PmfmVO> findPmfmsByFilter(
            @GraphQLArgument(name = "filter") ReferentialFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLEnvironment ResolutionEnvironment env
    ) {

        PmfmFetchOptions fetchOptions = getPmfmFetchOptions(GraphQLUtils.fields(env));

        List<PmfmVO> res = pmfmService.findByFilter(
                ReferentialFilterVO.nullToEmpty(filter),
                offset, size, sort, SortDirection.fromString(direction, SortDirection.ASC),
                fetchOptions);

        return res;
    }

    @GraphQLQuery(name = "pmfm", description = "Get a PMFM")
    @Transactional(readOnly = true)
    public PmfmVO getPmfm(
            @GraphQLArgument(name = "label") String label,
            @GraphQLArgument(name = "id") Integer id,
            @GraphQLEnvironment ResolutionEnvironment env
            ) {
        Preconditions.checkArgument(id != null || StringUtils.isNotBlank(label), "Required 'id' or 'label' to get a pmfm");

        PmfmFetchOptions fetchOptions = getPmfmFetchOptions(GraphQLUtils.fields(env));
        fetchOptions.setWithInheritance(false);

        if (id != null) {
            return pmfmService.get(id, fetchOptions);
        }
        return pmfmService.getByLabel(label, fetchOptions);
    }

    @GraphQLMutation(name = "savePmfm", description = "Create or update a pmfm")
    @IsAdmin
    public PmfmVO savePmfm(
            @GraphQLArgument(name = "pmfm") PmfmVO source) {
        return pmfmService.save(source);
    }

    @GraphQLQuery(name = "completeName", description = "Get PMFM's complete name")
    public String getPmfmCompleteName(@GraphQLContext PmfmVO pmfm) {
        if (pmfm.getCompleteName() != null) return pmfm.getCompleteName();
        return pmfmService.computeCompleteName(pmfm.getId());
    }

    @GraphQLQuery(name = "parameter", description = "Get PMFM's parameter")
    public ParameterVO getPmfmParameter(@GraphQLContext PmfmVO pmfm) {
        if (pmfm.getParameterId() == null) return null;
        return getParameter(null, pmfm.getParameterId());
    }

    @GraphQLQuery(name = "matrix", description = "Get PMFM's matrix")
    public ReferentialVO getPmfmMatrix(@GraphQLContext PmfmVO pmfm) {
        if (pmfm.getMatrixId() == null) return null;
        return referentialService.get(Matrix.class, pmfm.getMatrixId());
    }

    @GraphQLQuery(name = "fraction", description = "Get PMFM's fraction")
    public ReferentialVO getPmfmFraction(@GraphQLContext PmfmVO pmfm) {
        if (pmfm.getFractionId() == null) return null;
        return referentialService.get(Fraction.class, pmfm.getFractionId());
    }

    @GraphQLQuery(name = "method", description = "Get PMFM's method")
    public ReferentialVO getPmfmMethod(@GraphQLContext PmfmVO pmfm) {
        if (pmfm.getMethodId() == null) return null;
        return referentialService.get(Method.class, pmfm.getMethodId());
    }

    @GraphQLQuery(name = "unit", description = "Get PMFM's unit")
    public ReferentialVO getPmfmUnit(@GraphQLContext PmfmVO pmfm) {
        if (pmfm.getUnitId() == null) return null;
        return referentialService.get(Unit.class, pmfm.getUnitId());
    }


    /* -- Parameter -- */

    @GraphQLQuery(name = "parameter", description = "Get a parameter")
    @Transactional(readOnly = true)
    public ParameterVO getParameter(
            @GraphQLArgument(name = "label") String label,
            @GraphQLArgument(name = "id") Integer id
    ) {
        Preconditions.checkArgument(id != null || StringUtils.isNotBlank(label));
        if (id != null) {
            return parameterService.get(id);
        }
        return parameterService.getByLabel(label);
    }

    @GraphQLMutation(name = "saveParameter", description = "Create or update a parameter")
    @IsAdmin
    public ParameterVO saveParameter(
            @GraphQLArgument(name = "parameter") ParameterVO source) {
        return parameterService.save(source);
    }


    protected PmfmFetchOptions getPmfmFetchOptions(Set<String> fields) {
        return PmfmFetchOptions.builder()
                .withQualitativeValue(
                        fields.contains(net.sumaris.core.util.StringUtils.slashing(PmfmVO.Fields.QUALITATIVE_VALUES, ReferentialVO.Fields.ID))
                )
                .build();
    }
}
