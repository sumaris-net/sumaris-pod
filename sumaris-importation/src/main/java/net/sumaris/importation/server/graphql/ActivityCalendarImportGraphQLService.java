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

package net.sumaris.importation.server.graphql;

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.model.technical.job.JobTypeEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.technical.JobExecutionService;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.job.JobVO;
import net.sumaris.importation.core.service.activitycalendar.ActivityCalendarImportService;
import net.sumaris.importation.core.service.activitycalendar.vo.ActivityCalendarImportContextVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.security.IFileController;
import net.sumaris.server.security.ISecurityContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Optional;

import static org.nuiton.i18n.I18n.t;
@Service
@RequiredArgsConstructor
@GraphQLApi
@ConditionalOnWebApplication
@Slf4j
public class ActivityCalendarImportGraphQLService {

    private final ActivityCalendarImportService activityCalendarImportService;
    private final Optional<JobExecutionService> jobExecutionService;
    private final ProgramService programService;
    private final ISecurityContext<PersonVO> securityContext;
    private final IFileController fileController;


    @PostConstruct
    protected void init() {
        if (jobExecutionService.isEmpty()) {
            log.warn("Cannot starts activity calendar import service, because job service has been disabled");
        }
    }


    @GraphQLQuery(name = "importActivityCalendars", description = "Import a list of activity calendar from a CSV file")
    public JobVO importActivityCalendars(@GraphQLArgument(name = "fileName") String fileName) {
        Preconditions.checkNotNull(fileName, "Argument 'fileName' must not be null.");

        // Check user is authenticated
        PersonVO user = securityContext.getAuthenticatedUser().orElseThrow(UnauthorizedException::new);

        // Check user is an admin or program manager
        if (!securityContext.isAdmin()) {
            boolean isProgramManager = programService.getAllPrivilegesByUserId(ProgramEnum.SIH_ACTIFLOT.getId(), user.getId())
                .stream().anyMatch(privilege -> privilege == ProgramPrivilegeEnum.MANAGER);
            if (!isProgramManager) throw new UnauthorizedException();
        }

        JobExecutionService jobExecutionService = this.jobExecutionService
                .orElseThrow(() -> new SumarisTechnicalException("Unable to import activity calendar: job service has been disabled"));

        File inputFile = fileController.getUserUploadFile(fileName);
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new SumarisTechnicalException("File not found, or invalid");
        }

        JobVO job = JobVO.builder()
                .type(JobTypeEnum.ACTIVITY_CALENDARS_IMPORTATION.name())
                .name(t("sumaris.import.activityCalendar.job.name", fileName))
                .issuer(user.getPubkey())
                .build();

        ActivityCalendarImportContextVO context = ActivityCalendarImportContextVO.builder()
                .recorderPersonId(user.getId())
                .processingFile(inputFile)
                .build();

        // Execute importJob by JobService (async)
        return jobExecutionService.run(job,
                () -> context,
                (progression) -> activityCalendarImportService.asyncImportFromFile(context, progression));
    }

}
