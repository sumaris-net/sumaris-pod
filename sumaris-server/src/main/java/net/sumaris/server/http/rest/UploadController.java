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

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.exception.FileUploadException;
import net.sumaris.server.security.IDownloadController;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


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
    public FileUploadResponse uploadFile(@RequestParam("file") MultipartFile file,
                              @RequestParam("objectType") String objectType,
                              @RequestParam("objectId") Integer objectId) throws IOException {

        File uploadedFile = storeFile(file, objectType, objectId);

        String fileDownloadUri = downloadController.registerFile(uploadedFile, false);

        return FileUploadResponse.builder()
            .fileName(uploadedFile.getName())
            .fileDownloadUri(fileDownloadUri)
            .fileType(file.getContentType())
            .size(file.getSize())
            .build();
    }


    public File storeFile(MultipartFile file, @NonNull String objectType, @NonNull Integer objectId) {

        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        RestPaths.checkSecuredPath(originalFileName);

        String username = getAuthenticatedUsername();
        RestPaths.checkSecuredPath(username);

        String fileName = "";
        try {

            String fileExtension = "";
            try {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            } catch (Exception e) {
                fileExtension = "";
            }
            fileName = username + "_" + objectType + "_" + objectId + fileExtension;
            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = uploadDirectory.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // TODO: store

            return targetLocation.toFile();
        } catch (IOException ex) {
            throw new FileUploadException(String.format("Could not store file {}. Please try again !", fileName), ex);
        }
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
}
