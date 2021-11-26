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

package net.sumaris.server.http.rest;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.exception.InvalidPathException;
import net.sumaris.server.http.MediaTypes;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.security.IDownloadController;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Controller
@Slf4j
public class DownloadController implements IDownloadController {

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private SumarisServerConfiguration configuration;

    @RequestMapping({RestPaths.DOWNLOAD_PATH + "/{username}/{filename}", RestPaths.DOWNLOAD_PATH + "/{filename}"})
    public ResponseEntity<InputStreamResource> downloadFileAsPath(
            @PathVariable(name="username", required = false) String username,
            @PathVariable(name="filename") String filename
    ) throws IOException {
        if (StringUtils.isNotBlank(username)) {
            return doDownloadFile(username + "/" + filename);
        }
        else {
            return doDownloadFile(filename);
        }
    }

    @RequestMapping(RestPaths.DOWNLOAD_PATH)
    public ResponseEntity<InputStreamResource> downloadFileAsQuery(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "filename") String filename
    ) throws IOException{
        if (StringUtils.isNotBlank(username)) {
            return doDownloadFile(username + "/" + filename);
        }
        else {
            return doDownloadFile(filename);
        }
    }

    public String registerFile(File sourceFile, boolean moveSourceFile) throws IOException {

        String username = getAuthenticatedUsername();

        // Make sure the username can be used as a path (fail if '../' injection in the username token)
        String userPath =  asSecuredPath(username);
        if (!username.equals(userPath)) {
            throw new AuthenticationCredentialsNotFoundException("Bad authentication token");
        }

        File userDirectory = new File(configuration.getDownloadDirectory(), userPath);
        FileUtils.forceMkdir(userDirectory);
        File targetFile = new File(userDirectory, sourceFile.getName());

        if (targetFile.exists()) {
            int counter = 1;
            String baseName = Files.getNameWithoutExtension(sourceFile);
            String extension = Files.getExtension(sourceFile)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid argument 'sourcePath': missing file extension"));
            do {
                targetFile = new File(userDirectory, String.format("%s-%s.%s",
                        baseName,
                        counter++,
                        extension));
            } while (targetFile.exists());
        }

        if (moveSourceFile) {
            FileUtils.moveFile(sourceFile, targetFile);
        }
        else {
            FileUtils.copyFile(sourceFile, targetFile);
        }

        return Joiner.on('/').join(
                configuration.getServerUrl() + RestPaths.DOWNLOAD_PATH,
                userPath,
                targetFile.getName());
    }

    /* -- protected method -- */

    protected ResponseEntity<InputStreamResource> doDownloadFile(String filename) throws IOException {
        if (StringUtils.isBlank(filename)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        // Avoid '..' in the filename
        if (!RestPaths.isSecuredPath(filename)) {
            log.warn(String.format("Reject download request: invalid filename {}", filename));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .build();
        }

        MediaType mediaType = MediaTypes.getMediaTypeForFileName(this.servletContext, filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        File file = new File(configuration.getDownloadDirectory(), filename);
        if (!file.exists()) {
            log.warn(String.format("Reject download request: file {%s} not found, or invalid path", filename));
            return ResponseEntity.notFound().build();
        }
        if (!file.canRead()) {
            log.warn(String.format("Reject download request: file {%s} not readable", filename));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        log.debug(String.format("Download request to file {%s} of type {%s}", filename, mediaType));
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                // Content-Disposition
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                // Content-Type
                .contentType(mediaType)
                // Content-Length
                .contentLength(file.length())
                .body(resource);
    }

    protected String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            }
        }
        return null;
    }

    protected String asSecuredPath(String path) {
        Preconditions.checkNotNull(path);
        Preconditions.checkArgument(path.trim().length() > 0);
        // Avoid '../' in the filename
        return path.trim().replaceAll("[.][.]/?", "");
    }
}
