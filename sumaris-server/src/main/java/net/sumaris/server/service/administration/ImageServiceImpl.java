package net.sumaris.server.service.administration;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.service.data.ImageAttachmentService;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.Images;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.crypto.MD5Util;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import net.sumaris.server.config.ServerCacheConfiguration;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.http.rest.RestPaths;
import org.apache.commons.io.FilenameUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("imageService")
@Slf4j
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private String personAvatarUrl;
    private String departmentLogoUrl;
    private String gravatarUrl;
    private String imageUrl;
    private boolean enableImagesDirectory;

    private final SumarisServerConfiguration configuration;
    private final ImageAttachmentService imageAttachmentService;


    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {

        // Prepare URL for String formatter
        personAvatarUrl = configuration.getServerUrl() + RestPaths.PERSON_AVATAR_PATH;
        departmentLogoUrl = configuration.getServerUrl() + RestPaths.DEPARTMENT_LOGO_PATH;

        // Get and check the gravatar URL
        gravatarUrl = getAndCheckGravatarUrl(configuration);

        // Prepare URL for String formatter
        imageUrl = StringUtils.removeTrailingSlash(configuration.getServerUrl())
            + RestPaths.IMAGE_PATH.replaceAll("\\{(\\w+):[^}]+\\}", "{$1}");

        enableImagesDirectory = configuration.enableDataImagesDirectory() && Files.exists(configuration.getImagesDirectory());

        // Override image size enumerations
        Images.ImageSize.SMALL.setMaxSize(configuration.getImageSizeSmall());
        Images.ImageSize.MEDIUM.setMaxSize(configuration.getImageSizeMedium());
        Images.ImageSize.LARGE.setMaxSize(configuration.getImageSizeLarge());

    }

    @Override
    public List<ImageAttachmentVO> getImagesForObject(int objectId, ObjectTypeEnum objectType) {
        List<ImageAttachmentVO> images = imageAttachmentService.findAllByObject(objectId, objectType, null);

        images.forEach(this::fillUrl);
        images.forEach(this::cleanDataUrl); // Should not be sent to App, if URL has been set

        return images;
    }

    @Override
    public List<ImageAttachmentVO> findAllByFilter(@NonNull ImageAttachmentFilterVO filter, @NonNull Page page, ImageAttachmentFetchOptions fetchOptions) {
        return imageAttachmentService.findAllByFilter(filter, page, fetchOptions);
    }

    @Override
    @Cacheable(cacheNames = ServerCacheConfiguration.Names.IMAGE_BY_ID, unless = "#result==null")
    public ImageAttachmentVO find(int id, ImageAttachmentFetchOptions fetchOptions) {
        return imageAttachmentService.find(id, fetchOptions);
    }

    public void fillAvatar(PersonVO person) {
        if (person == null || personAvatarUrl == null) return;
        if (person.getHasAvatar() != null && person.getHasAvatar() && org.apache.commons.lang3.StringUtils.isNotBlank(person.getPubkey())) {
            person.setAvatar(personAvatarUrl.replace("{pubkey}", person.getPubkey()));
        }
        // Use gravatar URL
        else if (gravatarUrl != null && StringUtils.isNotBlank(person.getEmail())) {
            person.setAvatar(gravatarUrl.replace("{md5}", MD5Util.md5Hex(person.getEmail())));
        }
    }

    public void fillLogo(DepartmentVO department) {
        if (department == null || departmentLogoUrl == null) return;
        if (department.getHasLogo() != null && department.getHasLogo() && StringUtils.isNotBlank(department.getLabel())) {
            department.setLogo(departmentLogoUrl.replace("{label}", department.getLabel()));
        }
    }

    public void fillUrl(ImageAttachmentVO image) {
        if (image == null) return;
        image.setUrl(getImageUrl(image));
    }

    public void cleanDataUrl(ImageAttachmentVO image) {
        image.setDataUrl(null);
    }

    @Override
    public String getImageUrl(ImageAttachmentVO image) {
        if (image == null) return null;
        if (image.getUrl() != null) return image.getUrl(); // Already fill

        // when possible, use the filename to create an URL
        // It is better because we not need to load the ImageAttachement to get the image path (it can be computed)
        if (enableImagesDirectory && StringUtils.isNotBlank(image.getPath())) {
            return getImageUrlByPath(image.getPath());
        }

        if (image.getId() == null) return null;

        // By default, use the id
        // Can be slower, because need to load the ImageAttachement to get the image path or content
        return getImageUrlById(image.getId());
    }

    @Override
    public String getImageUrlByUri(String imageUri) {
        if (StringUtils.isBlank(imageUri) || imageUrl == null) return null;

        // Resolve URI like 'image:<ID>'
        if (imageUri.startsWith(ImageService.URI_IMAGE_SUFFIX)) {
            return imageUrl.replace("{id}", imageUri.substring(ImageService.URI_IMAGE_SUFFIX.length()));
        }
        // should be a URL, so return it
        return imageUri;
    }

    @Override
    public String getImageUrlById(int id) {
        return imageUrl.replace("{id}", String.valueOf(id));
    }

    @Override
    public String getImageUrlByPath(String filename) {
        // Normalize the path (will check if well formatted)
        String path = Images.computePath(filename);
        filename = FilenameUtils.getName(path);

        return imageUrl.replace("{id}", filename);
    }


    /* -- protected methods -- */

    protected String getAndCheckGravatarUrl(SumarisServerConfiguration config) {
        if (!config.enableGravatarFallback()) return null; // Skip if disable

        String gravatarUrl = config.gravatarUrl();
        if (StringUtils.isBlank(gravatarUrl)) {
            log.error("Invalid option '{}': must be a valid URL, with the sequence '{md5}'. Skipping option", SumarisServerConfigurationOption.GRAVATAR_URL.getKey());
            return null;
        }

        // Check 'md5' exists in the URL, to be able to replace by MD5(email)
        if (!gravatarUrl.contains("{md5}")) {
            log.error("Invalid option '{}': the sequence '{md5}' is missing. Skipping option", SumarisServerConfigurationOption.GRAVATAR_URL.getKey());
            return null;
        }

        // OK
        return gravatarUrl;
    }
}
