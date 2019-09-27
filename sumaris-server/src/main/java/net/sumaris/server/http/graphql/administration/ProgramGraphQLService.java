package net.sumaris.server.http.graphql.administration;

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

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.*;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.data.IWithObserversEntity;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.service.referential.PmfmService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsSupervisor;
import net.sumaris.server.http.security.IsUser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProgramGraphQLService {

    private static final Logger log = LoggerFactory.getLogger(ProgramGraphQLService.class);

    @Autowired
    private ProgramService programService;

    @Autowired
    private StrategyService strategyService;

    @Autowired
    private ReferentialService referentialService;

    @Autowired
    private PmfmService pmfmService;

    @Autowired
    private TaxonNameService taxonNameService;

    @Autowired
    public ProgramGraphQLService() {
        super();
    }


    /* -- Program / Strategy-- */

    @GraphQLQuery(name = "program", description = "Get a program")
    @Transactional(readOnly = true)
    public ProgramVO getProgram(
            @GraphQLArgument(name = "label") String label,
            @GraphQLArgument(name = "id") Integer id
    ) {
        Preconditions.checkArgument(id != null || StringUtils.isNotBlank(label));
        if (id != null) {
            return programService.get(id);
        }
        return programService.getByLabel(label);
    }

    @GraphQLQuery(name = "programs", description = "Search in programs")
    @Transactional(readOnly = true)
    public List<ProgramVO> findProgramsByFilter(
            @GraphQLArgument(name = "filter") ProgramFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = ProgramVO.PROPERTY_LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        if (filter == null) {
            return programService.getAll();
        }
        return programService.findByFilter(filter, offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "strategies", description = "Get program's strategie")
    public List<StrategyVO> getStrategiesByProgram(@GraphQLContext ProgramVO program,
                                                   @GraphQLEnvironment() Set<String> fields) {
        return strategyService.findByProgram(program.getId(), getFetchOptions(fields));
    }

    @GraphQLQuery(name = "pmfm", description = "Get strategy pmfm")
    public PmfmVO getPmfmStrategyPmfm(@GraphQLContext PmfmStrategyVO pmfmStrategy) {
        if (pmfmStrategy.getPmfm() != null) {
            return pmfmStrategy.getPmfm();
        }
        else if (pmfmStrategy.getPmfmId() != null) {
            return pmfmService.get(pmfmStrategy.getPmfmId());
        }
        return null;
    }

    @GraphQLQuery(name = "taxonNames", description = "Get taxon group's taxons")
    public List<TaxonNameVO> getTaxonGroupTaxonNames(@GraphQLContext TaxonGroupVO taxonGroup) {
        if (taxonGroup.getId() != null) {
            return taxonNameService.getAllByTaxonGroup(taxonGroup.getId());
        }
        return null;
    }

    /* -- Mutations -- */

    @GraphQLMutation(name = "saveProgram", description = "Save a program (with strategies)")
    @IsSupervisor
    public ProgramVO saveProgram(
            @GraphQLArgument(name = "program") ProgramVO program) {
        return programService.save(program);
    }

    @GraphQLMutation(name = "deleteProgram", description = "Delete a program")
    @IsAdmin
    public void deleteProgram(@GraphQLArgument(name = "id") int id) {
        programService.delete(id);
    }

    /* -- Protected methods -- */
    protected StrategyFetchOptions getFetchOptions(Set<String> fields) {
        return StrategyFetchOptions.builder()
                .withPmfmStrategyInheritance(
                        fields.contains(Strategy.PROPERTY_PMFM_STRATEGIES + "/" + PmfmStrategyVO.PROPERTY_LABEL)
                        && !fields.contains(Strategy.PROPERTY_PMFM_STRATEGIES + "/" + PmfmStrategyVO.PROPERTY_PMFM)
                )
                .build();
    }
}
