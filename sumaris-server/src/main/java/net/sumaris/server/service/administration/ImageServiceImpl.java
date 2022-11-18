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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.service.data.ImageAttachmentService;
import net.sumaris.core.util.crypto.MD5Util;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.server.config.ServerCacheConfiguration;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.http.rest.RestPaths;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("imageService")
@Slf4j
public class ImageServiceImpl implements ImageService {

    private String personAvatarUrl;
    private String departmentLogoUrl;
    private String gravatarUrl;
    private String imageUrl;

    @Resource
    private SumarisServerConfiguration configuration;

    @Resource
    private ImageAttachmentService imageAttachmentService;


    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {

        // Prepare URL for String formatter
        personAvatarUrl = configuration.getServerUrl() + RestPaths.PERSON_AVATAR_PATH;
        departmentLogoUrl = configuration.getServerUrl() + RestPaths.DEPARTMENT_LOGO_PATH;

        // Get and check the gravatar URL
        gravatarUrl = getAndCheckGravatarUrl(configuration);

        // Prepare URL for String formatter
        imageUrl = configuration.getServerUrl() + RestPaths.IMAGE_PATH;
    }

    @Override
    @Cacheable(cacheNames = ServerCacheConfiguration.Names.IMAGE_BY_ID, unless = "#result==null")
    public ImageAttachmentVO find(int id) {
        return imageAttachmentService.find(id);
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
