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
import lombok.NonNull;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.referential.gear.GearClassification;
import net.sumaris.core.model.referential.location.LocationClassification;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.util.Beans;
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
import java.util.stream.Collectors;

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
        return programService.findByFilter(filter, offset, size, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "programsCount", description = "Get programs count")
    @Transactional(readOnly = true)
    public Long getProgramsCount(@GraphQLArgument(name = "filter") ProgramFilterVO filter) {
        return referentialService.countByFilter(Program.class.getSimpleName(), filter);
    }

    @GraphQLQuery(name = "strategy", description = "Get a strategy")
    @Transactional(readOnly = true)
    public StrategyVO getStrategy(@GraphQLNonNull @GraphQLArgument(name = "id") @NonNull Integer id,
                                  @GraphQLEnvironment() Set<String> fields) {

        return strategyService.get(id, getStrategyFetchOptions(fields));
    }

    @GraphQLQuery(name = "strategies", description = "Search in strategies")
    @Transactional(readOnly = true)
    public List<StrategyVO> findStrategiesByFilter(
            @GraphQLNonNull @GraphQLArgument(name = "filter") @NonNull StrategyFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = StrategyVO.Fields.LABEL) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLEnvironment() Set<String> fields) {

        return strategyService.findByFilter(filter,
                Pageables.create(offset, size, sort, SortDirection.fromString(direction)),
                getStrategyFetchOptions(fields));
    }

    @GraphQLQuery(name = "strategiesCount", description = "Get strategies count")
    @Transactional(readOnly = true)
    public Long getStrategiesCount(@GraphQLArgument(name = "filter") StrategyFilterVO filter) {
        return referentialService.countByFilter(Strategy.class.getSimpleName(), filter);
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
            return strategyService.get(id, getStrategyFetchOptions(expandedPmfmStrategy));
        }
        return strategyService.getByLabel(label, getStrategyFetchOptions(expandedPmfmStrategy));
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

    @GraphQLQuery(name = "locationClassifications", description = "Get program's location classifications")
    public List<ReferentialVO> getProgramLocationClassifications(@GraphQLContext ProgramVO program) {
        if (program.getLocationClassificationIds() != null && program.getLocationClassifications() == null) {
            return program.getLocationClassificationIds().stream()
                    .map(id -> referentialService.get(LocationClassification.class, id))
                    .collect(Collectors.toList());
        }
        return program.getLocationClassifications();
    }

    @GraphQLQuery(name = "strategies", description = "Get program's strategies")
    public List<StrategyVO> getStrategiesByProgram(@GraphQLContext ProgramVO program,
                                                   @GraphQLEnvironment() Set<String> fields) {
        if (program.getStrategies() != null) {
            return program.getStrategies();
        }
        return strategyService.findByProgram(program.getId(), getStrategyFetchOptions(fields));
    }

    @GraphQLQuery(name = "pmfmStrategies", description = "Get strategy's pmfms")
    public List<PmfmStrategyVO> getPmfmStrategiesByStrategy(@GraphQLContext StrategyVO strategy,
                                                   @GraphQLEnvironment() Set<String> fields) {
        if (strategy.getPmfmStrategies() != null) {
            return strategy.getPmfmStrategies();
        }
        return strategyService.findPmfmStrategiesByStrategy(strategy.getId(), getStrategyFetchOptions(fields));
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
            return taxonNameService.getAllByTaxonGroupId(taxonGroup.getId());
        }
        return null;
    }

    // TODO BLA rename ?
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
            @GraphQLNonNull @GraphQLArgument(name = "program") ProgramVO program,
            @GraphQLArgument(name = "options") ProgramSaveOptions options) {
        checkCanEditProgram(program);
        return programService.save(program, options);
    }

    @GraphQLMutation(name = "deleteProgram", description = "Delete a program")
    @IsAdmin
    public void deleteProgram(@GraphQLNonNull @GraphQLArgument(name = "id") int id) {
        programService.delete(id);
    }

    @GraphQLMutation(name = "saveStrategy", description = "Save a strategy")
    @IsSupervisor
    public StrategyVO saveStrategy(
            @GraphQLNonNull @GraphQLArgument(name = "strategy") StrategyVO strategy) {
        checkCanEditStrategy(strategy);
        return strategyService.save(strategy);
    }

    @GraphQLMutation(name = "deleteStrategy", description = "Delete a strategy")
    @IsSupervisor
    public void deleteStrategy(@GraphQLNonNull @GraphQLArgument(name = "id") int id) {
        checkCanDeleteStrategy(id);
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

    protected StrategyFetchOptions getStrategyFetchOptions(Set<String> fields) {
        return StrategyFetchOptions.builder()
                .withPmfmStrategyInheritance(
                        fields.contains(StringUtils.slashing(Strategy.Fields.PMFM_STRATEGIES, PmfmStrategyVO.Fields.LABEL))
                                && !fields.contains(StringUtils.slashing(Strategy.Fields.PMFM_STRATEGIES, PmfmStrategyVO.Fields.PMFM))
                )
                .build();
    }

    // TODO BLA: voir si on ne pas deduire la valeur de exapanded, par la grappe demandée ?
    protected StrategyFetchOptions getStrategyFetchOptions(Boolean expandedPmfmStrategy) {
        return StrategyFetchOptions.builder()
                .withPmfmStrategyExpanded(expandedPmfmStrategy)
                .build();
    }

    protected void checkCanEditProgram(ProgramVO program) {
        // TODO: check if user is a program manager
    }

    protected void checkCanEditStrategy(StrategyVO strategy) {
        // TODO: check if user is a strategy manager
    }

    protected void checkCanDeleteStrategy(int id) {
        // TODO: check if user is a strategy manager ?
    }
}
