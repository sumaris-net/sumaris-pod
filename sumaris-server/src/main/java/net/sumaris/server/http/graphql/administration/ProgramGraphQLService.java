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
import io.reactivex.rxjava3.core.BackpressureStrategy;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.referential.gear.Gear;
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
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.*;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLHelper;
import net.sumaris.server.http.graphql.GraphQLUtils;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsSupervisor;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.administration.DataAccessControlService;
import net.sumaris.server.service.technical.EntityWatchService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.nuiton.util.TimeLog;
import org.reactivestreams.Publisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@GraphQLApi
@RequiredArgsConstructor
@Slf4j
public class ProgramGraphQLService {

    static final Integer MIN_WATCH_INTERVAL_IN_SECONDS = 30;

    private final SumarisServerConfiguration configuration;

    private final ProgramService programService;

    private final StrategyService strategyService;

    private final ReferentialService referentialService;

    private final PmfmService pmfmService;

    private final TaxonNameService taxonNameService;

    private final AuthService authService;

    private final EntityWatchService entityWatchService;

    private final DataAccessControlService dataAccessControlService;

    private final TimeLog timeLog = new TimeLog(ProgramGraphQLService.class);

    /* -- Program / Strategy-- */

    @GraphQLQuery(name = "program", description = "Get a program")
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

        // Add access restriction
        filter = restrictProgramFilter(filter);

        return programService.findByFilter(filter, offset, size, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "programsCount", description = "Get programs count")
    @Transactional(readOnly = true)
    public Long getProgramCount(@GraphQLArgument(name = "filter") ProgramFilterVO filter) {
        // Add access restriction
        filter = restrictProgramFilter(filter);

        // Count
        return programService.countByFilter(filter);
    }

    @GraphQLQuery(name = "strategy", description = "Get a strategy")
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

    @GraphQLQuery(name = "privileges", description = "Get current user program's privileges")
    public List<String> getProgramUserPrivileges(@GraphQLContext ProgramVO program) {
        // TODO add department privileges
        return authService.getAuthenticatedUser()
                .map(user -> programService.getAllPrivilegesByUserId(program.getId(), user.getId())
                    .stream().map(ProgramPrivilegeEnum::name)
                    .toList()
                )
                .orElseGet(ArrayList::new);
    }

    @GraphQLQuery(name = "strategies", description = "Get program's strategies")
    @Transactional(readOnly = true)
    public List<StrategyVO> getStrategiesByProgram(@GraphQLContext ProgramVO program,
                                                   @GraphQLArgument(name = "filter") StrategyFilterVO filter,
                                                   @GraphQLEnvironment ResolutionEnvironment env) {
        if (program.getStrategies() != null) return program.getStrategies();
        filter = StrategyFilterVO.nullToEmpty(filter);

        // Force parent program
        filter.setProgramIds(new Integer[]{program.getId()});

        // Limit on user department, if enable in programs
        filter = fillStrategyFilter(filter);

        if (ArrayUtils.isEmpty(filter.getAcquisitionLevels())) {
            log.warn("Fetching program -> strategies without 'filter.acquisitionLevels'. Not recommended in production!");
        }

        long now = TimeLog.getTime();
        try {
            return strategyService.findByFilter(filter, null, getStrategyFetchOptions(GraphQLUtils.fields(env)));
        }
        finally {
            timeLog.log(now, "getStrategiesByProgram");
        }
    }


    @GraphQLQuery(name = "acquisitionLevels", description = "Get program's acquisition levels")
    public List<ReferentialVO> getProgramAcquisitionLevels(@GraphQLContext ProgramVO program) {
        if (program.getAcquisitionLevels() != null) return program.getAcquisitionLevels();
        if (program.getId() == null) return null;
        return programService.getAcquisitionLevelsById(program.getId());
    }

    @GraphQLQuery(name = "acquisitionLevelLabels", description = "Get program acquisition level's labels")
    public List<String> getProgramAcquisitionLevelLabels(@GraphQLContext ProgramVO program) {
        if (program.getAcquisitionLevelLabels() != null) return program.getAcquisitionLevelLabels();
        return Beans.getStream(getProgramAcquisitionLevels(program))
            .map(ReferentialVO::getLabel)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    @GraphQLQuery(name = "gears", description = "Get strategy's gears")
    public List<ReferentialVO> getStrategyGears(@GraphQLContext StrategyVO strategy) {
        if (strategy.getGears() != null) return strategy.getGears();

        if (CollectionUtils.isEmpty(strategy.getPmfms())) return null;

        Integer[] gearIds = Beans.getStream(strategy.getPmfms())
            .flatMap(ps -> Beans.getStream(ps.getGearIds()))
            .collect(Collectors.toSet())
            .toArray(Integer[]::new);
        if (ArrayUtils.isEmpty(gearIds)) return null;

        return referentialService.findByFilter(Gear.ENTITY_NAME, ReferentialFilterVO.builder().includedIds(gearIds).build(), 0, gearIds.length);
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
                .withGears(fields.contains(DenormalizedPmfmStrategyVO.Fields.GEARS))
                .build());
    }

    @GraphQLQuery(name = "pmfm", description = "Get strategy pmfm")
    public PmfmVO getPmfmStrategyPmfm(@GraphQLContext PmfmStrategyVO pmfmStrategy) {
        if (pmfmStrategy.getPmfm() != null) {
            return pmfmStrategy.getPmfm();
        } else if (pmfmStrategy.getPmfmId() != null) {
            return pmfmService.get(pmfmStrategy.getPmfmId(), PmfmFetchOptions.builder()
                    .withInheritance(false)
                    .withQualitativeValue(true)
                    .build());
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
        if (pmfmStrategy.getMethod() != null) return pmfmStrategy.getMethod();
        if (pmfmStrategy.getMethodId() != null) {
            return referentialService.get(Method.class, pmfmStrategy.getMethodId());
        }
        return null;
    }

    @GraphQLQuery(name = "taxonNames", description = "Get taxon group's taxons")
    public List<TaxonNameVO> getTaxonGroupTaxonNames(@GraphQLContext TaxonGroupVO taxonGroup) {
        if (taxonGroup.getTaxonNames() != null) return taxonGroup.getTaxonNames();
        if (taxonGroup.getId() == null) return null;
        return taxonNameService.findAllByTaxonGroupId(taxonGroup.getId());
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
                                              @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") Integer intervalInSeconds,
                                              @GraphQLEnvironment ResolutionEnvironment env) {
        ProgramFetchOptions fetchOptions = getProgramFetchOptions(GraphQLUtils.fields(env));

        if (intervalInSeconds != null && intervalInSeconds < MIN_WATCH_INTERVAL_IN_SECONDS) {
            intervalInSeconds = MIN_WATCH_INTERVAL_IN_SECONDS;
        }

        log.info("Checking changes Program#{}, every {}s", id, intervalInSeconds);

        return entityWatchService.watchEntity(updateDate -> {
                // Get actual program
                if (updateDate == null) {
                    return Optional.of(programService.get(id, fetchOptions));
                }
                // Get if newer
                return programService.findNewerById(id, updateDate, fetchOptions);
            }, intervalInSeconds, true)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLSubscription(name = "lastStrategiesUpdateDate", description = "Subscribe to last strategies update date")
    @IsUser
    public Publisher<Date> lastStrategiesUpdateDate(@GraphQLNonNull @GraphQLArgument(name = "filter") StrategyFilterVO filter,
                                                   @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to check, in seconds.") Integer intervalInSeconds
    ) {
        Preconditions.checkArgument(filter != null && ArrayUtils.isNotEmpty(filter.getProgramIds()),
            String.format("Required 'filter.programIds' to listen for strategies changes"));

        if (intervalInSeconds != null && intervalInSeconds < MIN_WATCH_INTERVAL_IN_SECONDS) {
            intervalInSeconds = MIN_WATCH_INTERVAL_IN_SECONDS;
        }

        log.debug("Checking strategies max update date, on Program#{} every {}s", filter.getProgramIds(), intervalInSeconds);

        AtomicReference<Date> lastUpdateDate = new AtomicReference<>(null);

        return entityWatchService.watchByLoader(() -> {
                Date current = strategyService.maxUpdateDateByFilter(filter);
                Date previous = lastUpdateDate.get();
                if (previous == null || (current != null && current.after(previous))) {
                    lastUpdateDate.set(current);
                    return Optional.of(current);
                }

                return Optional.empty();
            }, intervalInSeconds, false /*only changes, but not actual value*/)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLSubscription(name = "updateProgramStrategies", description = "Subscribe to changes on program's strategies")
    @IsUser
    public Publisher<List<StrategyVO>> updateProgramStrategies(@GraphQLNonNull @GraphQLArgument(name = "programId") final int programId,
                                                               @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") Integer intervalInSeconds,
                                                               @GraphQLEnvironment ResolutionEnvironment env) {

        Set<String> fields = GraphQLUtils.fields(env);
        StrategyFetchOptions fetchOptions = getStrategyFetchOptions(fields);
        if (intervalInSeconds != null && intervalInSeconds < MIN_WATCH_INTERVAL_IN_SECONDS) {
            intervalInSeconds = MIN_WATCH_INTERVAL_IN_SECONDS;
        }

        Preconditions.checkArgument(programId >= 0, "Invalid programId");

        log.info("Checking strategies changes on Program#{}, every {}s", programId, intervalInSeconds);

        return entityWatchService.watchEntities((lastUpdateDate) -> {
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

        return entityWatchService.watchEntities(Program.class,
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
                    log.debug("Fetching {} programs for Person#{}...", programIds.size(), personId);
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
            .withAcquisitionLevels(
                fields.contains(StringUtils.slashing(ProgramVO.Fields.ACQUISITION_LEVELS, ReferentialVO.Fields.ID))
                || fields.contains(ProgramVO.Fields.ACQUISITION_LEVEL_LABELS)
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
            .withAppliedStrategies(
                    fields.contains(StringUtils.slashing(StrategyVO.Fields.APPLIED_STRATEGIES, AppliedStrategyVO.Fields.ID))
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
                    fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.SIGNIF_FIGURES_NUMBER)) ||
                    fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.DETECTION_THRESHOLD)) ||
                    fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.PRECISION))
            )

            // Retrieve how to fetch Pmfms
            .pmfmsFetchOptions(
                PmfmStrategyFetchOptions.builder()
                    .withCompleteName(fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.COMPLETE_NAME)))
                    .withGears(fields.contains(StringUtils.slashing(StrategyVO.Fields.DENORMALIZED_PMFMS, DenormalizedPmfmStrategyVO.Fields.GEARS)))
                    // Full pmfm load
                    .withPmfms(fields.contains(StringUtils.slashing(StrategyVO.Fields.PMFMS, ReferentialVO.Fields.ID)))
                    .build()
            )

            .build();
    }

    protected void checkCanEditProgram(Integer programId) {

        // New program
        if (programId == null) {
            // Only admin can create a program
            dataAccessControlService.checkIsAdmin("Cannot create a program. Not an admin.");
        }

        // Edit an existing program
        else {

            // Admin can edit
            if (authService.isAdmin()) return; // OK

            PersonVO user = authService.getAuthenticatedUser().orElseThrow(() -> new AccessDeniedException("Forbidden"));

            // Manager can edit
            boolean isManager = programService.hasUserPrivilege(programId, user.getId(), ProgramPrivilegeEnum.MANAGER)
                || programService.hasDepartmentPrivilege(programId, user.getDepartment().getId(), ProgramPrivilegeEnum.MANAGER);
            if (!isManager) throw new AccessDeniedException("Forbidden");
        }
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

    protected StrategyFilterVO fillStrategyFilter(StrategyFilterVO filter) {
        filter = StrategyFilterVO.nullToEmpty(filter);

        // Is program filtered ?
        boolean noProgramFilter = ArrayUtils.isEmpty(filter.getProgramIds()) && ArrayUtils.isEmpty(filter.getProgramLabels());

        // Enable limit access from StrategyDepartment, when:
        // - no program (avoid to read all strategies) - should never happen
        // - If right by StrategyDepartment is enabled used (in program properties)
        boolean enableStrategyDepartment = noProgramFilter
            || Beans.getStream(filter.getProgramIds())
                .anyMatch(programId -> programService.hasPropertyValueByProgramId(programId, ProgramPropertyEnum.PROGRAM_STRATEGY_DEPARTMENT_ENABLE, Boolean.TRUE.toString()))
            || Beans.getStream(filter.getProgramLabels())
                .anyMatch(programLabel -> programService.hasPropertyValueByProgramLabel(programLabel, ProgramPropertyEnum.PROGRAM_STRATEGY_DEPARTMENT_ENABLE, Boolean.TRUE.toString()));
        return fillStrategyFilter(filter, enableStrategyDepartment);
    }

    /**
     * Restrict to self department data
     *
     * @param filter
     */
    protected StrategyFilterVO fillStrategyFilter(StrategyFilterVO filter, boolean enableStrategyDepartment) {
        filter = StrategyFilterVO.nullToEmpty(filter);

        // Restrict to self department data
        // (No restriction if admin)
        if (enableStrategyDepartment && !authService.isAdmin()) {
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

    protected ProgramFilterVO restrictProgramFilter(@NonNull ProgramFilterVO filter) {
        filter = ProgramFilterVO.nullToEmpty(filter);

        Integer[] programIds = filter.getId() != null ? new Integer[]{filter.getId()} : filter.getIncludedIds();

        // Limit to authorized ids
        Integer[] authorizedProgramIds = dataAccessControlService.getAuthorizedProgramIds(programIds)
            .orElse(DataAccessControlService.NO_ACCESS_FAKE_IDS);

        // Reset id, as it has been deprecated
        if (filter.getId() != null) {
            filter.setId(null);
            GraphQLHelper.logDeprecatedUse(authService, "ProgramFilterVO.id", "1.24.0");
        }

        // Apply limitations
        filter.setIncludedIds(authorizedProgramIds);

        return filter;
    }
}
