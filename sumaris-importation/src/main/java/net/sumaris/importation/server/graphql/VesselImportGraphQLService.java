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
import net.sumaris.core.model.technical.job.JobTypeEnum;
import net.sumaris.core.service.technical.JobExecutionService;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.job.JobVO;
import net.sumaris.importation.core.service.vessel.SiopVesselsImportService;
import net.sumaris.importation.core.service.vessel.vo.SiopVesselImportContextVO;
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
public class VesselImportGraphQLService {

    private final SiopVesselsImportService siopVesselsImportService;
    private final Optional<JobExecutionService> jobExecutionService;
    private final ISecurityContext<PersonVO> securityContext;
    private final IFileController fileController;


    @PostConstruct
    protected void init() {
        if (jobExecutionService.isEmpty()) {
            log.warn("Cannot starts vessel import service, because job service has been disabled");
        }
    }

    @GraphQLQuery(name = "importSiopVessels", description = "Import vessels from a SIOP file")
    public JobVO importSiopVessels(@GraphQLArgument(name = "fileName") String fileName) {
        Preconditions.checkNotNull(fileName, "Argument 'fileName' must not be null.");

        if (!securityContext.isAdmin()) throw new UnauthorizedException();

        JobExecutionService jobExecutionService = this.jobExecutionService
                .orElseThrow(() -> new SumarisTechnicalException("Unable to import vessels: job service has been disabled"));


        File inputFile = fileController.getUserUploadFile(fileName);
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new SumarisTechnicalException("File not found, or invalid");
        }

        PersonVO user = securityContext.getAuthenticatedUser().get();
        JobVO job = JobVO.builder()
                .type(JobTypeEnum.SIOP_VESSELS_IMPORTATION.name())
                .name(t("sumaris.import.vessel.siop.job.name", fileName))
                .issuer(user.getPubkey())
                .build();
        SiopVesselImportContextVO context = SiopVesselImportContextVO.builder()
                .recorderPersonId(user.getId())
                .processingFile(inputFile)
                .build();

        // Execute importJob by JobService (async)
        return jobExecutionService.run(job,
                () -> context,
                (progression) -> siopVesselsImportService.asyncImportFromFile(context, progression));
    }
}
