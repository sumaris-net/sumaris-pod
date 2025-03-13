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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.util.Images;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.technical.SoftwareVO;
import net.sumaris.server.config.ServerCacheConfiguration;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.http.MediaTypes;
import net.sumaris.server.service.administration.ImageService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.ServletContext;
import java.io.*;

@RestController
@Slf4j
public class ImageRestController implements ResourceLoaderAware {

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private PersonService personService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SumarisServerConfiguration configuration;

    private ResourceLoader resourceLoader;

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @ResponseBody
    @RequestMapping(value = RestPaths.PERSON_AVATAR_PATH, method = RequestMethod.GET,
        produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<?> getPersonAvatar(@PathVariable(name = "pubkey") String pubkey) throws IOException {
        return personService.findAvatarByPubkey(pubkey)
            .map(image -> this.getImageResponse(image, Images.ImageType.THUMBNAIL))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ResponseBody
    @RequestMapping(value = RestPaths.DEPARTMENT_LOGO_PATH, method = RequestMethod.GET,
        produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<?> getDepartmentLogo(@PathVariable(name = "label") String label) throws IOException {
        return departmentService.findLogoByLabel(label)
            .map(image -> this.getImageResponse(image, Images.ImageType.THUMBNAIL))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ResponseBody
    @RequestMapping(value = {RestPaths.IMAGE_PATH, RestPaths.IMAGE_PATH_WITH_FILENAME},
        method = RequestMethod.GET, produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<?> getImage(@PathVariable(name = "id", required = false) Integer id,
                                      @PathVariable(name = "filename", required = false) String filename,
                                      @RequestParam(name = "resolution", required = false) String resolution
    ) {
        try {
            // Convert filename into id
            if (StringUtils.isNotBlank(filename) && StringUtils.isNumeric(filename)) {
                id = Integer.parseInt(filename);
                filename = null;
            }

            Images.ImageType imageType = Images.ImageType.find(resolution).orElse(Images.ImageType.DIAPO);
            if (StringUtils.isNotBlank(filename)) {
                return getImageResponse(filename, imageType);
            }
            ImageAttachmentVO image = imageService.find(id, ImageAttachmentFetchOptions.WITH_CONTENT);
            return getImageResponse(image, imageType);
        } catch (Exception e) {
            log.error("Error while fetching image #{}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @ResponseBody
    @RequestMapping(value = RestPaths.FAVICON, method = RequestMethod.GET,
        produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    @Cacheable(cacheNames = ServerCacheConfiguration.Names.FAVICON)
    public Object getFavicon() throws IOException {

        SoftwareVO software = configurationService.getCurrentSoftware();
        if (software == null) return ResponseEntity.notFound().build();

        String favicon = MapUtils.getString(software.getProperties(), SumarisServerConfigurationOption.SITE_FAVICON.getKey());
        if (StringUtils.isBlank(favicon)) {
            return ResponseEntity.notFound().build();
        }

        // Parse URI like 'image:<ID>'
        if (favicon.startsWith(ImageService.URI_IMAGE_SUFFIX)) {
            String imageId = favicon.substring(ImageService.URI_IMAGE_SUFFIX.length());
            return getImage(Integer.parseInt(imageId), null, Images.ImageType.THUMBNAIL.getSuffix());
        }

        // Redirect to the URL
        if (favicon.startsWith("https://") || favicon.startsWith("http://")) {
            return new RedirectView(favicon);
        }

        // Try to read as a local resource
        try {
            Resource faviconResource = resourceLoader.getResource(favicon);
            InputStream in = faviconResource.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            IOUtils.copy(in, bos);
            bos.close();
            in.close();

            return ResponseEntity.ok()
                .contentLength(faviconResource.contentLength())
                .body(bos.toByteArray());
        } catch (IOException e) {
            // Not a local resource: continue
        }

        // Redirect as a relative URL
        return new RedirectView(configuration.getServerUrl() + (favicon.startsWith("/") ? "" : "/") + favicon);

    }

    protected ResponseEntity<?> getImageResponse(ImageAttachmentVO image, Images.ImageType imageType) {
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        // Read the file
        if (StringUtils.isNotBlank(image.getPath())) {
            return getImageResponse(image.getPath(), imageType);
        }

        if (image.getContent() == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = Base64.decodeBase64(image.getContent());
        return ResponseEntity.ok()
            // Content-Type
            .contentType(MediaType.parseMediaType(image.getContentType()))
            // Content-Length
            .contentLength(bytes.length)
            .body(bytes);
    }

    protected ResponseEntity<?> getImageResponse(@NonNull String filename, Images.ImageType imageType) {

        if (StringUtils.isBlank(filename)) ResponseEntity.notFound().build();

        // Avoid '..' in the path
        if (!RestPaths.isSecuredPath(filename)) {
            log.warn("Reject request to a file {} - Unsecured path", filename);
            return ResponseEntity.badRequest().build();
        }

        // Normalize path (e.g. filename => relative/path/filename.ext
        filename = Images.computePath(filename);

        MediaType mediaType = MediaTypes.getMediaTypeForFileName(this.servletContext, filename)
            .orElse(MediaType.APPLICATION_OCTET_STREAM);

        File file = new File(configuration.getImagesDirectory(), filename);

        if (imageType != null && imageType != Images.ImageType.BASE) {
            file = Images.getImageFile(file, imageType);
        }

        if (!file.exists()) {
            log.warn("Reject request to image {} - File not found, or invalid path", file.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }
        if (!file.canRead()) {
            log.warn("Reject request to image {} - File not readable", file.getAbsolutePath());
            return ResponseEntity.badRequest().build();
        }

        log.debug("Request to image {} of type {}", filename, mediaType);

        // Read the file content
        InputStreamResource resource = null;
        try {
            resource = new InputStreamResource(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            log.error("Cannot read image file: {}", file.getAbsolutePath(), e);
            ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok()
            // Content-Type
            .contentType(mediaType)
            // Content-Length
            .contentLength(file.length())
            .body(resource);
    }

}
