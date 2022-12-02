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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.MediaTypes;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.security.IFileController;
import net.sumaris.server.security.ISecurityContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;

@Controller
@Slf4j
public class FileController implements IFileController {

    private final Path downloadDirectory;
    private final Path uploadDirectory;
    private final ServletContext servletContext;

    private final SumarisServerConfiguration configuration;

    private final ISecurityContext<PersonVO> securityContext;


    public FileController(ServletContext servletContext,
                          SumarisServerConfiguration configuration,
                          ISecurityContext<PersonVO> securityContext) {
        this.configuration = configuration;
        this.servletContext = servletContext;
        this.securityContext = securityContext;

        try {
            this.downloadDirectory = Paths.get(configuration.getDownloadDirectory().getAbsolutePath())
                .toAbsolutePath().normalize();
            Files.createDirectories(this.downloadDirectory);
        } catch (Exception ex) {
            throw new SumarisTechnicalException("Could not create the directory where the downloaded files will be stored.", ex);
        }

        try {
            this.uploadDirectory = Paths.get(configuration.getUploadDirectory().getAbsolutePath())
                .toAbsolutePath().normalize();
            Files.createDirectories(this.uploadDirectory);
        } catch (Exception ex) {
            throw new SumarisTechnicalException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @RequestMapping({
            RestPaths.DOWNLOAD_PATH + "/{username}/{filename:[a-zA-Z0-9-_$.]+}",
            RestPaths.DOWNLOAD_PATH + "/{filename:[a-zA-Z0-9-_$.]+}"
    })
    public ResponseEntity<InputStreamResource> downloadFileAsPath(
            @PathVariable(name="username", required = false) String username,
            @PathVariable(name="filename") String filename
    ) throws IOException {
        if (StringUtils.isNotBlank(username)) {
            return getDownloadFileResponse(username + File.separator + filename);
        }
        else {
            return getDownloadFileResponse(filename);
        }
    }

    @RequestMapping(RestPaths.DOWNLOAD_PATH)
    public ResponseEntity<InputStreamResource> downloadFileAsQuery(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "filename") String filename
    ) throws IOException{
        if (StringUtils.isNotBlank(username)) {
            return getDownloadFileResponse(username + File.separator + filename);
        }
        else {
            return getDownloadFileResponse(filename);
        }
    }

    @PostMapping(RestPaths.UPLOAD_PATH)
    @IsUser
    public ResponseEntity<UploadFileResponse>  uploadFile(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("resourceType") String resourceType,
                                                          @RequestParam("resourceId") String resourceId,
                                                          @RequestParam(value = "action", required = false, defaultValue = "none") String action,
                                                          @RequestParam(value = "replace", defaultValue = "false", required = false) String replaceStr) throws IOException {

        boolean replace = Boolean.parseBoolean(replaceStr);
        Path targetFile = getUploadPath(file.getOriginalFilename(), resourceType, resourceId, replace);
        if (targetFile == null) {
            log.warn(String.format("Reject upload request: invalid resource type {%s}", resourceType));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            writeMultipartFile(file, targetFile, replace);
        } catch (IOException ioe) {
            log.error(ioe.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(UploadFileResponse.builder().message(ioe.toString()).build());
        }

        String userPath = targetFile.getParent().getFileName().toString();

        String fileUrl = Joiner.on('/').join(
            configuration.getServerUrl() + RestPaths.UPLOAD_PATH,
            userPath,
            targetFile.getFileName().toString());

        return ResponseEntity.ok()
            .body(UploadFileResponse.builder().message("OK")
                .fileName(targetFile.getFileName().toString())
                .fileUri(fileUrl)
                .fileType(file.getContentType())
                .size(file.getSize())
                .build());
    }

    @Override
    public String registerFile(File sourceFile, boolean moveSourceFile) throws IOException {

        String username = securityContext.getAuthenticatedUsername().orElseThrow(UnauthorizedException::new);

        // Make sure the username can be used as a path (fail if '../' injection in the username token)
        String userPath =  asSecuredPath(username);
        if (!username.equals(userPath)) {
            throw new AuthenticationCredentialsNotFoundException("Bad authentication token");
        }

        Path userDirectory = this.downloadDirectory.resolve(userPath);
        Files.createDirectories(userDirectory);
        Path targetFile = userDirectory.resolve(sourceFile.getName());

        if (Files.exists(targetFile)) {
            int counter = 1;
            String baseName = Files.getNameWithoutExtension(sourceFile);
            String extension = Files.getExtension(sourceFile).orElse("");
            do {
                String fileName = String.format("%s-%s.%s",
                    baseName,
                    counter++);
                fileName += extension;
                targetFile = userDirectory.resolve(fileName);
            } while (Files.exists(targetFile));
        }

        if (moveSourceFile) {
            Files.moveFile(sourceFile, targetFile.toFile());
        }
        else {
            Files.copyFile(sourceFile, targetFile.toFile());
        }

        return Joiner.on('/').join(
                configuration.getServerUrl() + RestPaths.DOWNLOAD_PATH,
                userPath,
                targetFile.getFileName().toString());
    }

    public ResponseEntity<InputStreamResource> getDownloadFileResponse(@NonNull Path baseDirectory, String filename) throws IOException {
        if (StringUtils.isBlank(filename)) return ResponseEntity.badRequest().build();

        // Avoid '..' in the path
        if (!RestPaths.isSecuredPath(filename)) {
            log.warn("Reject request to a file {} - Unsecured path", filename);
            return ResponseEntity.badRequest().build();
        }

        MediaType mediaType = MediaTypes.getMediaTypeForFileName(this.servletContext, filename)
            .orElse(MediaType.APPLICATION_OCTET_STREAM);

        File file = baseDirectory.resolve(filename).toFile();
        if (!file.exists()) {
            log.warn("Reject request to file {} - File not found, or invalid path", file.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }
        if (!file.canRead()) {
            log.warn("Reject request to file {} - File not readable", file.getAbsolutePath());
            return ResponseEntity.badRequest().build();
        }

        log.debug("Request to file {} of type {}", filename, mediaType);
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

    @Override
    public File getUserUploadFile(String filename) {

        String username = securityContext.getAuthenticatedUsername().orElseThrow(UnauthorizedException::new);
        username = asSecuredPath(username);

        Path userPath = uploadDirectory.resolve(username);

        Path targetPath = userPath.resolve(asSecuredPath(filename));

        return targetPath.toFile();
    }

    /* -- protected method -- */

    protected ResponseEntity<InputStreamResource> getDownloadFileResponse(String filename) throws IOException {
        return getDownloadFileResponse(downloadDirectory, filename);
    }

    protected String asSecuredPath(String path) {
        Preconditions.checkNotNull(path);
        Preconditions.checkArgument(path.trim().length() > 0);
        // Avoid '../' in the filename
        return path.trim().replaceAll("[.][.]/?", "");
    }

    public Path getUploadPath(String originalFileName, @NonNull String resourceType, @NonNull String resourceId, boolean replace) {

        // Normalize file name
        originalFileName = StringUtils.cleanPath(originalFileName);
        RestPaths.checkSecuredPath(originalFileName);

        String username = securityContext.getAuthenticatedUsername().orElseThrow(UnauthorizedException::new);
        RestPaths.checkSecuredPath(username);

        Path targetDirectory = uploadDirectory.resolve(username);

        // Append the original file extension
        String extension = net.sumaris.core.util.Files.getExtension(originalFileName)
            .orElse(null);

        // Copy file to the target location (Replacing existing file with the same name)
        Path targetLocation;
        int counter = 1;
        String fileName;
        do {
            fileName = Joiner.on("-").join(resourceType, resourceId).trim();
            if (counter > 1) fileName += "-" + counter;
            if (extension != null) fileName += "." + extension;
            targetLocation = targetDirectory.resolve(fileName);
            counter++;
        } while (!replace && Files.exists(targetLocation));

        return targetLocation;

    }

    protected void writeMultipartFile(MultipartFile source, Path target, boolean replace) throws IOException {
        if (replace) {
            Files.deleteQuietly(target);
        }
        Files.createDirectories(target.getParent());
        Files.copyStream(source.getInputStream(), Files.newOutputStream(target));
    }
}
