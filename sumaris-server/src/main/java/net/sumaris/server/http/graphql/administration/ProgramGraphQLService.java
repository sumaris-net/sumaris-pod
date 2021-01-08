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
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.referential.gear.GearClassification;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsSupervisor;
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
            @GraphQLArgument(name = "sortBy", defaultValue = ProgramVO.Fields.LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        if (filter == null) {
            return programService.getAll();
        }
        return programService.findByFilter(filter, offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "strategy", description = "Get a strategy")
    @Transactional(readOnly = true)
    public StrategyVO getStrategy(
            @GraphQLArgument(name = "label") String label,
            @GraphQLArgument(name = "id") Integer id,
            @GraphQLArgument(name = "expandedPmfmStrategy", defaultValue = "false") Boolean expandedPmfmStrategy
    ) {
        Preconditions.checkArgument(id != null || StringUtils.isNotBlank(label));
        if (id != null) {
            return strategyService.get(id, getFetchOptions(expandedPmfmStrategy));
        }
        return strategyService.getByLabel(label, getFetchOptions(expandedPmfmStrategy));
    }

    @GraphQLQuery(name = "strategies", description = "Search in strategies")
    @Transactional(readOnly = true)
    public List<StrategyVO> findStrategiesByFilter(
            @GraphQLArgument(name = "filter") StrategyFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = StrategyVO.Fields.LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        if (filter == null) {
            return strategyService.getAll();
        }
        return strategyService.findByFilter(filter, offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));
    }

    @GraphQLQuery(name = "taxonGroupType", description = "Get program's taxon group type")
    public ReferentialVO getProgramTaxonGroupType(@GraphQLContext ProgramVO program) {
        if (program.getTaxonGroupTypeId() != null && program.getTaxonGroupType() == null) {
            return referentialService.get(TaxonGroupType.class, program.getTaxonGroupTypeId());
        }
        return program.getTaxonGroupType();
    }

    @GraphQLQuery(name = "gearClassification", description = "Get program's gear classification")
    public ReferentialVO getProgramGearClassification(@GraphQLContext ProgramVO program) {
        if (program.getGearClassificationId() != null && program.getGearClassification() == null) {
            return referentialService.get(GearClassification.class, program.getGearClassificationId());
        }
        return program.getGearClassification();
    }

    @GraphQLQuery(name = "strategies", description = "Get program's strategies")
    public List<StrategyVO> getStrategiesByProgram(@GraphQLContext ProgramVO program,
                                                   @GraphQLEnvironment() Set<String> fields) {
        if (program.getStrategies() != null) {
            return program.getStrategies();
        }
        return strategyService.findByProgram(program.getId(), getFetchOptions(fields));
    }

    @GraphQLQuery(name = "pmfmStrategies", description = "Get strategy's pmfms")
    public List<PmfmStrategyVO> getPmfmStrategiesByStrategy(@GraphQLContext StrategyVO strategy,
                                                   @GraphQLEnvironment() Set<String> fields) {
        if (strategy.getPmfmStrategies() != null) {
            return strategy.getPmfmStrategies();
        }
        return strategyService.findPmfmStrategiesByStrategy(strategy.getId(), getFetchOptions(fields));
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

    @GraphQLQuery(name = "strategyNextLabel", description = "Get next label for strategy")
    public String findNextLabelByProgramId(
            @GraphQLArgument(name = "programId") int programId,
            @GraphQLArgument(name = "labelPrefix", defaultValue = "") String labelPrefix,
            @GraphQLArgument(name = "nbDigit", defaultValue = "0") Integer nbDigit) {
        return strategyService.findNextLabelByProgramId(programId,
                labelPrefix == null ? "" : labelPrefix,
                nbDigit == null ? 0 : nbDigit);
    }

    /* -- Mutations -- */

    @GraphQLMutation(name = "saveProgram", description = "Save a program (with strategies)")
    @IsSupervisor
    public ProgramVO saveProgram(
            @GraphQLArgument(name = "program") ProgramVO program) {
        ProgramVO result = programService.save(program);
        return result;
    }

    @GraphQLMutation(name = "deleteProgram", description = "Delete a program")
    @IsAdmin
    public void deleteProgram(@GraphQLArgument(name = "id") int id) {
        programService.delete(id);
    }

    @GraphQLMutation(name = "saveStrategy", description = "Save a strategy")
    @IsSupervisor
    public StrategyVO saveStrategy(
            @GraphQLArgument(name = "strategy") StrategyVO strategy) {
        StrategyVO result = strategyService.save(strategy);
        return result;
    }

    @GraphQLMutation(name = "deleteStrategy", description = "Delete a strategy")
    @IsAdmin
    public void deleteStrategy(@GraphQLArgument(name = "id") int id) {
        strategyService.delete(id);
    }

    /* -- Protected methods -- */

    protected ProgramFetchOptions getProgramFetchOptions(Set<String> fields) {
        return ProgramFetchOptions.builder()
                .withLocations(
                        fields.contains(StringUtils.slashing(ProgramVO.Fields.LOCATIONS, ReferentialVO.Fields.ID))
                        || fields.contains(StringUtils.slashing(ProgramVO.Fields.LOCATION_CLASSIFICATIONS, ReferentialVO.Fields.ID))
                        || fields.contains(ProgramVO.Fields.LOCATION_CLASSIFICATION_IDS)
                )
                .withProperties(
                        fields.contains(ProgramVO.Fields.PROPERTIES)
                )
                .build();
    }

    protected StrategyFetchOptions getFetchOptions(Set<String> fields) {
        return StrategyFetchOptions.builder()
                .withPmfmStrategyInheritance(
                        fields.contains(StringUtils.slashing(Strategy.Fields.PMFM_STRATEGIES, PmfmStrategyVO.Fields.LABEL))
                                && !fields.contains(StringUtils.slashing(Strategy.Fields.PMFM_STRATEGIES, PmfmStrategyVO.Fields.PMFM))
                )
                .build();
    }

    protected StrategyFetchOptions getFetchOptions(Boolean expandedPmfmStrategy) {
        return StrategyFetchOptions.builder()
                .withPmfmStrategyExpanded(expandedPmfmStrategy)
                .build();
    }
}
