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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.security.IDownloadController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
@Slf4j
public class UploadController {

    private final Path uploadDirectory;

    private final SumarisServerConfiguration configuration;

    private final IDownloadController downloadController;

    @Autowired
    public UploadController(SumarisServerConfiguration configuration,
                            IDownloadController downloadController) {
        this.configuration = configuration;
        this.downloadController = downloadController;
        this.uploadDirectory = Paths.get(configuration.getUploadDirectory().getAbsolutePath())
            .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (Exception ex) {
            throw new SumarisTechnicalException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @PostMapping(RestPaths.UPLOAD_PATH)
    @IsUser
    public ResponseEntity<UploadResponse>  uploadFile(@RequestParam("file") MultipartFile file,
                                                      @RequestParam("resourceType") String resourceType,
                                                      @RequestParam("resourceId") String resourceId,
                                                      @RequestParam(value = "replace", defaultValue = "false", required = false) String replaceStr) throws IOException {

        boolean replace = Boolean.parseBoolean(replaceStr);
        Path targetFile = getResourcePath(file, resourceType, resourceId, replace);
        if (targetFile == null) {
            log.warn(String.format("Reject upload request: invalid resource type {%s}", resourceType));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            copyResource(file, targetFile, replace);
        } catch (IOException ioe) {
            log.error(ioe.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(UploadResponse.builder().message(ioe.toString()).build());
        }

        String userPath = targetFile.getParent().getFileName().toString();

        String fileUrl = Joiner.on('/').join(
            configuration.getServerUrl() + RestPaths.UPLOAD_PATH,
            userPath,
            targetFile.getFileName().toString());

        return ResponseEntity.ok()
            .body(UploadResponse.builder().message("OK")
                .fileName(targetFile.getFileName().toString())
                .fileDownloadUri(fileUrl)
                .fileType(file.getContentType())
                .size(file.getSize())
                .build());
    }

    public Path getResourcePath(MultipartFile file, @NonNull String resourceType, @NonNull String resourceId, boolean replace) {

        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        RestPaths.checkSecuredPath(originalFileName);

        String username = getAuthenticatedUsername();
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

    public Resource loadFileAsResource(String fileName) throws Exception {

        try {
            Path filePath = this.uploadDirectory.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileNotFoundException("File not found " + fileName);
        }
    }

    /* -- protected method -- */

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

    protected void copyResource(MultipartFile source, Path target, boolean replace) throws IOException {
        if (replace) {
            Files.deleteQuietly(target);
        }
        Files.createDirectories(target.getParent());
        Files.copyStream(source.getInputStream(), Files.newOutputStream(target));
    }

    protected void deleteResource(Path resource) {
        Files.deleteQuietly(resource);
    }

}
