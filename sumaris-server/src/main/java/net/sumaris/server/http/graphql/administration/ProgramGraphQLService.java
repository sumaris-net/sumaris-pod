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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.reactivex.BackpressureStrategy;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.model.referential.gear.GearClassification;
import net.sumaris.core.model.referential.location.LocationClassification;
import net.sumaris.core.model.referential.pmfm.Fraction;
import net.sumaris.core.model.referential.pmfm.Matrix;
import net.sumaris.core.model.referential.pmfm.Method;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLUtils;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsSupervisor;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.administration.DataAccessControlService;
import net.sumaris.server.service.technical.EntityEventService;
import org.apache.commons.collections4.CollectionUtils;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

@Service
@Transactional
@GraphQLApi
@Slf4j
public class ProgramGraphQLService {

    @Autowired
    private SumarisServerConfiguration configuration;

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
    private AuthService authService;

    @Autowired
    private EntityEventService entityEventService;

    @Autowired
    private DataAccessControlService dataAccessControlService;

    @Autowired
    public ProgramGraphQLService() {
        super();
    }


    /* -- Program / Strategy-- */

    @GraphQLQuery(name = "program", description = "Get a program")
    @Transactional(readOnly = true)
    public ProgramVO getProgram(
        @GraphQLArgument(name = "label") String label,
        @GraphQLArgument(name = "id") Integer id,
        @GraphQLEnvironment ResolutionEnvironment env
    ) {
        Preconditions.checkArgument(id != null || StringUtils.isNotBlank(label));
        ProgramFetchOptions fetchOptions = getProgramFetchOptions(GraphQLUtils.fields(env));
        if (id != null) {
            return programService.get(id, fetchOptions);
        }
        return programService.getByLabel(label, fetchOptions);
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
    public Long getProgramCount(@GraphQLArgument(name = "filter") ProgramFilterVO filter) {
        return referentialService.countByFilter(Program.class.getSimpleName(), filter);
    }

    @GraphQLQuery(name = "strategy", description = "Get a strategy")
    @Transactional(readOnly = true)
    public StrategyVO getStrategy(@GraphQLNonNull @GraphQLArgument(name = "id") @NonNull Integer id,
                                  @GraphQLEnvironment ResolutionEnvironment env) {

        return strategyService.get(id, getStrategyFetchOptions(GraphQLUtils.fields(env)));
    }

    @GraphQLQuery(name = "strategies", description = "Search in strategies")
    @Transactional(readOnly = true)
    public List<StrategyVO> findStrategiesByFilter(
        @GraphQLNonNull @GraphQLArgument(name = "filter") @NonNull StrategyFilterVO filter,
        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
        @GraphQLArgument(name = "sortBy", defaultValue = StrategyVO.Fields.LABEL) String sort,
        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
        @GraphQLEnvironment ResolutionEnvironment env) {
        filter = fillStrategyFilter(filter);
        return strategyService.findByFilter(filter,
            Page.builder()
                .offset(offset)
                .size(size)
                .sortBy(sort)
                .sortDirection(SortDirection.fromString(direction))
                .build(),
            getStrategyFetchOptions(GraphQLUtils.fields(env)));
    }

    @GraphQLQuery(name = "strategiesCount", description = "Get strategies count")
    @Transactional(readOnly = true)
    public Long getStrategyCount(@GraphQLArgument(name = "filter") StrategyFilterVO filter) {
        filter = fillStrategyFilter(filter);
        return strategyService.countByFilter(filter);
    }

    @GraphQLQuery(name = "taxonGroupType", description = "Get program's taxon group type")
    public ReferentialVO getProgramTaxonGroupType(@GraphQLContext ProgramVO program) {
        if (program.getTaxonGroupType() != null) return program.getTaxonGroupType();
        if (program.getTaxonGroupTypeId() == null) return null;
        return referentialService.get(TaxonGroupType.class, program.getTaxonGroupTypeId());
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
        if (CollectionUtils.isNotEmpty(program.getLocationClassificationIds()) && CollectionUtils.isEmpty(program.getLocationClassifications())) {
            Integer[] locationClassificationIds = program.getLocationClassificationIds().toArray(new Integer[0]);
            return referentialService.findByFilter(LocationClassification.class.getSimpleName(),
                ReferentialFilterVO.builder()
                    .includedIds(locationClassificationIds)
                    .build(), 0, locationClassificationIds.length);
        }
        return program.getLocationClassifications();
    }

    @GraphQLQuery(name = "strategies", description = "Get program's strategies")
    public List<StrategyVO> getStrategiesByProgram(@GraphQLContext ProgramVO program,
                                                   @GraphQLEnvironment ResolutionEnvironment env) {
        if (program.getStrategies() != null) return program.getStrategies();
        return strategyService.findByProgram(program.getId(), getStrategyFetchOptions(GraphQLUtils.fields(env)));
    }

    @GraphQLQuery(name = "pmfms", description = "Get strategy's pmfms")
    public List<PmfmStrategyVO> getPmfmsByStrategy(@GraphQLContext StrategyVO strategy) {
        if (strategy.getPmfms() != null) return strategy.getPmfms();
        return strategyService.findPmfmsByFilter(PmfmStrategyFilterVO.builder()
                .strategyId(strategy.getId()).build(),
            PmfmStrategyFetchOptions.DEFAULT);
    }

    @GraphQLQuery(name = "denormalizedPmfms", description = "Get strategy's denormalized pmfms")
    public List<DenormalizedPmfmStrategyVO> getDenormalizedPmfmByStrategy(@GraphQLContext StrategyVO strategy,
                                                                          @GraphQLEnvironment ResolutionEnvironment env) {
        if (strategy.getDenormalizedPmfms() != null) return strategy.getDenormalizedPmfms();
        Set<String> fields = GraphQLUtils.fields(env);
        return strategyService.findDenormalizedPmfmsByFilter(PmfmStrategyFilterVO.builder().strategyId(strategy.getId()).build(),
            PmfmStrategyFetchOptions.builder()
                .uniqueByPmfmId(true)
                .withCompleteName(fields.contains(DenormalizedPmfmStrategyVO.Fields.COMPLETE_NAME))
                .build());
    }

    @GraphQLQuery(name = "pmfm", description = "Get strategy pmfm")
    public PmfmVO getPmfmStrategyPmfm(@GraphQLContext PmfmStrategyVO pmfmStrategy) {
        if (pmfmStrategy.getPmfm() != null) {
            return pmfmStrategy.getPmfm();
        } else if (pmfmStrategy.getPmfmId() != null) {
            return pmfmService.get(pmfmStrategy.getPmfmId());
        }
        return null;
    }

    @GraphQLQuery(name = "parameter", description = "Get strategy parameter")
    public ReferentialVO getPmfmStrategyParameter(@GraphQLContext PmfmStrategyVO pmfmStrategy) {
        if (pmfmStrategy.getParameter() != null) {
            return pmfmStrategy.getParameter();
        } else if (pmfmStrategy.getParameterId() != null) {
            return referentialService.get(Parameter.class, pmfmStrategy.getParameterId());
        }
        return null;
    }

    @GraphQLQuery(name = "matrix", description = "Get strategy matrix")
    public ReferentialVO getPmfmStrategyMatrix(@GraphQLContext PmfmStrategyVO pmfmStrategy) {
        if (pmfmStrategy.getMatrix() != null) {
            return pmfmStrategy.getMatrix();
        } else if (pmfmStrategy.getMatrixId() != null) {
            return referentialService.get(Matrix.class, pmfmStrategy.getMatrixId());
        }
        return null;
    }

    @GraphQLQuery(name = "fraction", description = "Get strategy fraction")
    public ReferentialVO getPmfmStrategyFraction(@GraphQLContext PmfmStrategyVO pmfmStrategy) {
        if (pmfmStrategy.getFraction() != null) {
            return pmfmStrategy.getFraction();
        } else if (pmfmStrategy.getFractionId() != null) {
            return referentialService.get(Fraction.class, pmfmStrategy.getFractionId());
        }
        return null;
    }

    @GraphQLQuery(name = "method", description = "Get strategy method")
    public ReferentialVO getPmfmStrategyMethod(@GraphQLContext PmfmStrategyVO pmfmStrategy) {
        if (pmfmStrategy.getMethod() != null) {
            return pmfmStrategy.getMethod();
        } else if (pmfmStrategy.getMethodId() != null) {
            return referentialService.get(Method.class, pmfmStrategy.getMethodId());
        }
        return null;
    }

    @GraphQLQuery(name = "taxonNames", description = "Get taxon group's taxons")
    public List<TaxonNameVO> getTaxonGroupTaxonNames(@GraphQLContext TaxonGroupVO taxonGroup) {
        if (taxonGroup.getId() != null) {
            return taxonNameService.findAllByTaxonGroupId(taxonGroup.getId());
        }
        return null;
    }

    @GraphQLQuery(name = "strategyNextLabel", description = "Get next label for strategy")
    @IsUser
    public String findNextLabelByProgramId(
        @GraphQLArgument(name = "programId") int programId,
        @GraphQLArgument(name = "labelPrefix", defaultValue = "") String labelPrefix,
        @GraphQLArgument(name = "nbDigit", defaultValue = "0") Integer nbDigit) {
        checkCanEditProgram(programId);

        return strategyService.computeNextLabelByProgramId(programId,
            labelPrefix == null ? "" : labelPrefix,
            nbDigit == null ? 0 : nbDigit);
    }

    @GraphQLQuery(name = "strategyNextSampleLabel", description = "Get next sample label for strategy")
    @IsUser
    public String findNextSampleLabelByStrategy(
        @GraphQLNonNull @GraphQLArgument(name = "strategyLabel") @NonNull String strategyLabel,
        @GraphQLArgument(name = "labelSeparator", defaultValue = "") String labelSeparator,
        @GraphQLArgument(name = "nbDigit", defaultValue = "0") Integer nbDigit) {
        return strategyService.computeNextSampleLabelByStrategy(strategyLabel,
            labelSeparator == null ? "" : labelSeparator,
            nbDigit == null ? 0 : nbDigit);
    }

    /* -- Mutations -- */

    @GraphQLMutation(name = "saveProgram", description = "Save a program (with strategies)")
    @IsSupervisor
    public ProgramVO saveProgram(
        @GraphQLNonNull @GraphQLArgument(name = "program") @NonNull ProgramVO program,
        @GraphQLArgument(name = "options") ProgramSaveOptions options) {
        checkCanEditProgram(program.getId());
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
        Preconditions.checkNotNull(strategy.getProgramId(), "Missing 'strategy.programId'");
        checkCanEditStrategy(strategy.getProgramId(), strategy.getId());
        return strategyService.save(strategy);
    }

    @GraphQLMutation(name = "deleteStrategy", description = "Delete a strategy")
    @IsSupervisor
    public void deleteStrategy(@GraphQLNonNull @GraphQLArgument(name = "id") int id) {
        StrategyVO strategy = strategyService.get(id, null);
        checkCanDeleteStrategy(strategy.getProgramId(), id);
        strategyService.delete(id);
    }

    /* -- Subscriptions -- */



    @GraphQLSubscription(name = "updateProgram", description = "Subscribe to changes on a program")
    @IsUser
    public Publisher<ProgramVO> updateProgram(@GraphQLArgument(name = "id") final int id,
                                              @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer minIntervalInSecond,
                                              @GraphQLEnvironment ResolutionEnvironment env) {
        ProgramFetchOptions fetchOptions = getProgramFetchOptions(GraphQLUtils.fields(env));

        log.info("Checking changes Program#{}, every {} sec", id, minIntervalInSecond);

        return entityEventService.watchEntity(updateDate -> {
                // Get actual program
                if (updateDate == null) {
                    return Optional.of(programService.get(id, fetchOptions));
                }
                // Get if newer
                return programService.findNewerById(id, updateDate, fetchOptions);
            }, minIntervalInSecond, true)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLSubscription(name = "updateProgramStrategies", description = "Subscribe to changes on program's strategies")
    @IsUser
    public Publisher<List<StrategyVO>> updateProgramStrategies(@GraphQLNonNull @GraphQLArgument(name = "programId") final int programId,
                                                               @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer intervalInSeconds,
                                                               @GraphQLEnvironment ResolutionEnvironment env) {

        Set<String> fields = GraphQLUtils.fields(env);
        StrategyFetchOptions fetchOptions = getStrategyFetchOptions(fields);

        Preconditions.checkArgument(programId >= 0, "Invalid programId");

        log.info("Checking strategies changes on Program#{}, every {} sec", programId, intervalInSeconds);

        return entityEventService.watchEntities((lastUpdateDate) -> {
                // Get actual values
                if (lastUpdateDate == null) {
                    return Optional.of(strategyService.findByProgram(programId, fetchOptions));
                }

                // Get newer strategies
                List<StrategyVO> updatedStrategies = strategyService.findNewerByProgramId(programId, lastUpdateDate, fetchOptions);
                return CollectionUtils.isEmpty(updatedStrategies) ? Optional.empty() : Optional.of(updatedStrategies);
            }, intervalInSeconds, false /*only changes, but not actual list*/)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLSubscription(name = "authorizedPrograms", description = "Subscribe to user's authorized programs")
    @IsUser
    @Transactional(readOnly = true)
    public Publisher<List<ProgramVO>> getAuthorizedPrograms(
        @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer intervalInSeconds,
        @GraphQLArgument(name = "startWithActualValue") Boolean startWithActualValue,
        @GraphQLEnvironment ResolutionEnvironment env
    ) {

        final Integer personId = this.authService.getAuthenticatedUserId().orElse(null);
        final ProgramFetchOptions fetchOptions = getProgramFetchOptions(GraphQLUtils.fields(env));
        startWithActualValue = startWithActualValue != null ? startWithActualValue : Boolean.FALSE;

        log.info("Watching programs for Person#{} every {}s", personId, intervalInSeconds);

        // Define a loader, decorate to return only when changes
        Callable<Optional<List<Integer>>> programIdsLoader = Observables.distinctUntilChanged(() -> {
            log.debug("Checking programs for Person#{}...", personId);
            List<Integer> programIds = dataAccessControlService.getAuthorizedProgramIdsByUserId(personId);
            return Optional.of(programIds);
        });

        return entityEventService.watchEntities(Program.class,
            // Call program ids loader
            () -> programIdsLoader.call()
                // Then convert to VO
                .map(programIds -> {
                    // User has no programs:
                    if (CollectionUtils.isEmpty(programIds)) {
                        // return an empty list (because findByFilter will return full list)
                        return ImmutableList.of();
                    }

                    // Fetch VO, by ids
                    log.debug("Loading programs for Person#{}...", personId);
                    return programService.findByFilter(
                        ProgramFilterVO.builder()
                            .includedIds(programIds.toArray(new Integer[0]))
                            .build(),
                        Page.builder()
                            .sortBy(IEntity.Fields.ID).sortDirection(SortDirection.ASC)
                            .build(),
                        fetchOptions);
                }),
                intervalInSeconds,
                startWithActualValue)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    /* -- Protected methods -- */

    protected ProgramFetchOptions getProgramFetchOptions(Set<String> fields) {
        return ProgramFetchOptions.builder()
            .withLocations(
                fields.contains(StringUtils.slashing(ProgramVO.Fields.LOCATIONS, ReferentialVO.Fields.ID))
                    || fields.contains(ProgramVO.Fields.LOCATION_IDS)
            )
            .withLocationClassifications(
                fields.contains(StringUtils.slashing(ProgramVO.Fields.LOCATION_CLASSIFICATIONS, ReferentialVO.Fields.ID))
                    || fields.contains(ProgramVO.Fields.LOCATION_CLASSIFICATION_IDS)
            )
            .withProperties(
                fields.contains(ProgramVO.Fields.PROPERTIES)
            )
            .withDepartments(
                fields.contains(StringUtils.slashing(ProgramVO.Fields.DEPARTMENTS, ReferentialVO.Fields.ID))
            )
            .withPersons(
                fields.contains(StringUtils.slashing(ProgramVO.Fields.PERSONS, ReferentialVO.Fields.ID))
            )
            .build();
    }

    protected StrategyFetchOptions getStrategyFetchOptions(Set<String> fields) {
        return StrategyFetchOptions.builder()
            .withTaxonNames(
                fields.contains(StringUtils.slashing(StrategyVO.Fields.TAXON_NAMES, TaxonNameStrategyVO.Fields.PRIORITY_LEVEL))
                    || fields.contains(StringUtils.slashing(StrategyVO.Fields.TAXON_NAMES, TaxonNameStrategyVO.Fields.TAXON_NAME, TaxonNameVO.Fields.REFERENCE_TAXON_ID))
            )
            .withTaxonGroups(
                fields.contains(StringUtils.slashing(StrategyVO.Fields.TAXON_GROUPS, TaxonGroupStrategyVO.Fields.PRIORITY_LEVEL))
                    || fields.contains(StringUtils.slashing(StrategyVO.Fields.TAXON_GROUPS, TaxonGroupStrategyVO.Fields.TAXON_GROUP, IEntity.Fields.ID))
            )
            .withDepartments(
                fields.contains(StringUtils.slashing(StrategyVO.Fields.DEPARTMENTS, StrategyDepartmentVO.Fields.ID))
                    || fields.contains(StringUtils.slashing(StrategyVO.Fields.DEPARTMENTS, StrategyDepartmentVO.Fields.DEPARTMENT, IEntity.Fields.ID))
            )
            .withGears(
                fields.contains(StringUtils.slashing(StrategyVO.Fields.GEARS, ReferentialVO.Fields.ID))
                    || fields.contains(StringUtils.slashing(StrategyVO.Fields.GEARS, ReferentialVO.Fields.LABEL))
            )
            // Test if should include Pmfms
            .withPmfms(
                fields.contains(StringUtils.slashing(StrategyVO.Fields.PMFMS, PmfmStrategyVO.Fields.ID))
            )
            // Test if should include DenormalizedPmfms
            .withDenormalizedPmfms(
                fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.LABEL)) ||
                    fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.TYPE)) ||
                    fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.UNIT_LABEL)) ||
                    fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.MAXIMUM_NUMBER_DECIMALS)) ||
                    fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.SIGNIF_FIGURES_NUMBER))
            )

            // Retrieve how to fetch Pmfms
            .pmfmsFetchOptions(
                PmfmStrategyFetchOptions.builder()
                    .withCompleteName(fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.COMPLETE_NAME)))
                    .build()
            )
            .build();
    }

    protected void checkCanEditProgram(Integer programId) {

        if (programId == null) {
            checkIsAdmin("Cannot create a program. Not an admin.");
            return;
        }

        // Admin can create a program
        if (authService.isAdmin()) return; // OK

        PersonVO user = authService.getAuthenticatedUser().orElseThrow(() -> new AccessDeniedException("Forbidden"));

        boolean isManager = programService.hasUserPrivilege(programId, user.getId(), ProgramPrivilegeEnum.MANAGER)
            || programService.hasDepartmentPrivilege(programId, user.getDepartment().getId(), ProgramPrivilegeEnum.MANAGER);
        if (!isManager) throw new AccessDeniedException("Forbidden");
    }

    protected void checkCanEditStrategy(int programId, Integer strategyId) {
        // Is new strategy: must have right on program
        if (strategyId == null) {
            checkCanEditProgram(programId);
            return;
        }

        // Admin can edit strategy
        if (authService.isAdmin()) return; // OK

        PersonVO user = authService.getAuthenticatedUser().orElseThrow(() -> new AccessDeniedException("Forbidden"));
        boolean isManager =
            // Program manager
            programService.hasUserPrivilege(programId, user.getId(), ProgramPrivilegeEnum.MANAGER)
                || programService.hasDepartmentPrivilege(programId, user.getId(), ProgramPrivilegeEnum.MANAGER)
                // Strategy manager
                || strategyService.hasUserPrivilege(strategyId, user.getId(), ProgramPrivilegeEnum.MANAGER)
                || strategyService.hasDepartmentPrivilege(strategyId, user.getDepartment().getId(), ProgramPrivilegeEnum.MANAGER);
        if (!isManager) {
            throw new AccessDeniedException("Forbidden");
        }
    }

    protected void checkCanDeleteStrategy(int programId, Integer strategyId) {
        checkCanEditStrategy(programId, strategyId);
    }

    /**
     * Check user is admin
     */
    protected void checkIsAdmin(String message) {
        if (!authService.isAdmin()) throw new AccessDeniedException(message != null ? message : "Access forbidden");
    }

    /**
     * Restrict to self department data
     *
     * @param filter
     */
    protected StrategyFilterVO fillStrategyFilter(StrategyFilterVO filter) {

        if (authService.isAdmin()) {
            // No restriction on department (= show all)
        }
        else {
            // Restrict to self department data
            PersonVO user = authService.getAuthenticatedUser().orElse(null);
            if (user != null) {
                Integer depId = user.getDepartment().getId();
                if (!canDepartmentAccessNotSelfData(depId)) {
                    // Limit data access to user's department
                    filter.setDepartmentIds(new Integer[]{depId});
                }
            } else {
                // Hide all. Should never occur
                filter.setDepartmentIds(DataAccessControlService.NO_ACCESS_FAKE_IDS);
            }
        }

        return filter;
    }

    protected boolean canDepartmentAccessNotSelfData(@NonNull Integer actualDepartmentId) {
        List<Integer> expectedDepartmentIds = configuration.getAccessNotSelfDataDepartmentIds();
        return CollectionUtils.isEmpty(expectedDepartmentIds) || expectedDepartmentIds.contains(actualDepartmentId);
    }
}
