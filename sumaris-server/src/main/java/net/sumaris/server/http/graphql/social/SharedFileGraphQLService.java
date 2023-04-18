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

package net.sumaris.server.http.graphql.social;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.rest.RestPaths;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.security.IFileController;
import net.sumaris.server.security.ISecurityContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@GraphQLApi
@ConditionalOnWebApplication
@Slf4j
public class SharedFileGraphQLService {

//    private final ISecurityContext<PersonVO> securityContext;
    private final SumarisServerConfiguration configuration;
    private final IFileController fileController;

    @GraphQLMutation(name = "shareFile", description = "Share an existing file")
    @IsUser
    public String shareFile(@GraphQLArgument(name = "fileName") String fileName) {
        Preconditions.checkNotNull(fileName, "Argument 'fileName' must not be null.");

//        if (!securityContext.isUser()) throw new UnauthorizedException();

        File sourceFile = fileController.getUserUploadFile(fileName);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new SumarisTechnicalException("File not found, or invalid");
        }

        File targetFile;
        try {
            targetFile = new File(fileController.registerPulbicFile(sourceFile, true));
        } catch (IOException ioe) {
            throw new SumarisTechnicalException("Could not create the directory where the downloaded files will be stored.", ioe);
        }

        return Joiner.on('/').join(
                configuration.getServerUrl() + RestPaths.DOWNLOAD_PATH,
                fileController.PUBLIC_DIRECTORY,
                targetFile.getName());
    }
}
