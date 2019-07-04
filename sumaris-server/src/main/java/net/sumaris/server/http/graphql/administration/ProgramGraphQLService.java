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
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.service.referential.PmfmService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.server.http.security.IsAdmin;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @GraphQLQuery(name = "programPmfms", description = "Get program's pmfm")
    @Transactional(readOnly = true)
    public List<PmfmStrategyVO> getProgramPmfms(
            @GraphQLArgument(name = "program", description = "A valid program code") String programLabel,
            @GraphQLArgument(name = "acquisitionLevel", description = "A valid acquisition level (e.g. 'TRIP', 'OPERATION', 'PHYSICAL_GEAR')") String acquisitionLevel
            ) {
        Preconditions.checkNotNull(programLabel, "Missing program");
        ProgramVO program = programService.getByLabel(programLabel);

        if (program == null) throw new SumarisTechnicalException(String.format("Program {%s} not found", programLabel));

        // ALl pmfm from the program
        if (StringUtils.isBlank(acquisitionLevel)) {
            List<PmfmStrategyVO> res = strategyService.getPmfmStrategies(program.getId());
            return res;
        }

        ReferentialVO acquisitionLevelVO = referentialService.findByUniqueLabel(AcquisitionLevel.class.getSimpleName(), acquisitionLevel);
        return strategyService.getPmfmStrategiesByAcquisitionLevel(program.getId(), acquisitionLevelVO.getId());

    }

    @GraphQLQuery(name = "programGears", description = "Get program's gears")
    @Transactional(readOnly = true)
    public List<ReferentialVO> getProgramGears(
            @GraphQLArgument(name = "program", description = "A valid program code") String programLabel) {
        Preconditions.checkNotNull(programLabel, "Missing program");
        ProgramVO program = programService.getByLabel(programLabel);

        if (program == null) throw new SumarisTechnicalException(String.format("Program {%s} not found", programLabel));

        return strategyService.getGears(program.getId());

    }

    @GraphQLQuery(name = "programTaxonGroups", description = "Get program's taxon groups")
    @Transactional(readOnly = true)
    public List<ReferentialVO> getProgramTaxonGroups(
            @GraphQLArgument(name = "program", description = "A valid program code") String programLabel) {
        Preconditions.checkNotNull(programLabel, "Missing program");
        ProgramVO program = programService.getByLabel(programLabel);

        if (program == null) throw new SumarisTechnicalException(String.format("Program {%s} not found", programLabel));

        return strategyService.getTaxonGroups(program.getId());

    }

    @GraphQLQuery(name = "strategies", description = "Get program's strategie")
    public List<StrategyVO> getStrategiesByProgram(@GraphQLContext ProgramVO program) {
        return strategyService.findByProgram(program.getId());
    }

    @GraphQLQuery(name = "pmfm", description = "Get strategy pmfm")
    public PmfmVO getPmfmStrategyPmfm(@GraphQLContext PmfmStrategyVO pmfmStrategy) {
        if (pmfmStrategy.getPmfm() != null) {
            return pmfmStrategy.getPmfm();
        }
        if (pmfmStrategy.getPmfm() == null && pmfmStrategy.getPmfmId() != null) {
            return pmfmService.get(pmfmStrategy.getPmfmId());
        }
        return null;
    }

    /* -- Mutations -- */

    @GraphQLMutation(name = "saveProgram", description = "Save a program (with strategies)")
    @IsAdmin
    public ProgramVO saveProgram(
            @GraphQLArgument(name = "program") ProgramVO program) {
        return programService.save(program);
    }

    /* -- Protected methods -- */

}
