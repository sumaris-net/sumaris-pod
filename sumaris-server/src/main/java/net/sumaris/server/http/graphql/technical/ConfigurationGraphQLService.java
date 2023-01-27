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

package net.sumaris.server.http.graphql.technical;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.service.technical.SoftwareService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.technical.ConfigurationVO;
import net.sumaris.core.vo.technical.SoftwareVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.service.administration.ImageService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@GraphQLApi
@Slf4j


public class ConfigurationGraphQLService {

    public static final String JSON_START_SUFFIX = "{";

    @Autowired
    private SumarisServerConfiguration configuration;

    @Autowired
    private SoftwareService softwareService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImageService imageService;

    @Autowired
    private AuthService authService;

    @GraphQLQuery(name = "configuration", description = "Load pod configuration")
    public ConfigurationVO getConfiguration(
        @GraphQLArgument(name = "id") Integer id, // /!\ Deprecated !
        @GraphQLArgument(name = "label") String label, // /!\ Deprecated !
        @GraphQLEnvironment Set<String> fields
    ) {
        if (id != null || label != null) {
            log.warn("Deprecated used of GraphQL 'configuration' query. Since version 1.8.0, arguments 'id' and 'label' have been deprecated, and will be ignored.");
        }

        SoftwareVO software = configurationService.getCurrentSoftware();

        // Transform to configuration (fill images, etc.)
        ConfigurationVO configuration = toConfiguration(software, fields);

        if (authService.isAdmin()) return configuration;

        // Sanitize, if not admin
        return sanitizeConfiguration(configuration);
    }

    @GraphQLMutation(name = "saveConfiguration", description = "Save pod configuration")
    @IsAdmin
    public ConfigurationVO saveConfiguration(
        @GraphQLArgument(name = "config") ConfigurationVO configuration,
        @GraphQLEnvironment Set<String> fields) {

        SoftwareVO software = softwareService.save(configuration);

        return toConfiguration(software, fields);
    }

    /* -- protected methods -- */

    protected ConfigurationVO toConfiguration(SoftwareVO software, Set<String> fields) {
        if (software == null) return null;
        ConfigurationVO result = new ConfigurationVO(software);

        // Fill partners departments
        if (fields.contains(ConfigurationVO.Fields.PARTNERS)) {
            this.fillPartners(result);
        }

        // Fill background images URLs
        if (fields.contains(ConfigurationVO.Fields.BACKGROUND_IMAGES)) {
            this.fillBackgroundImages(result);
        }

        // Fill logo URL
        String logoUri = getProperty(result, SumarisServerConfigurationOption.SITE_LOGO_SMALL.getKey());
        if (StringUtils.isNotBlank(logoUri)) {
            String logoUrl = imageService.getImageUrlByUri(logoUri);
            result.getProperties().put(
                SumarisServerConfigurationOption.SITE_LOGO_SMALL.getKey(),
                logoUrl);
            result.setSmallLogo(logoUrl);
        }

        // Fill large logo
        String logoLargeUri = getProperty(result, SumarisServerConfigurationOption.LOGO_LARGE.getKey());
        if (StringUtils.isNotBlank(logoLargeUri)) {
            String logoLargeUrl = imageService.getImageUrlByUri(logoLargeUri);
            result.getProperties().put(
                SumarisServerConfigurationOption.LOGO_LARGE.getKey(),
                logoLargeUrl);
            result.setLargeLogo(logoLargeUrl);
        }

        // Replace favicon ID by an URL
        String faviconUri = getProperty(result, SumarisServerConfigurationOption.SITE_FAVICON.getKey());
        if (StringUtils.isNotBlank(faviconUri)) {
            String faviconUrl = imageService.getImageUrlByUri(faviconUri);
            result.getProperties().put(SumarisServerConfigurationOption.SITE_FAVICON.getKey(), faviconUrl);
        }

        // Add expected auth token
        result.getProperties().put(
            SumarisServerConfigurationOption.AUTH_TOKEN_TYPE.getKey(),
            configuration.getAuthTokenType().getLabel());

        // Publish some option, need by App
        {
            // Add DB timezone (e.g. used by SFA instance)
            String dbTimeZone = configuration.getApplicationConfig().getOption(SumarisConfigurationOption.DB_TIMEZONE.getKey());
            if (StringUtils.isNotBlank(dbTimeZone)) {
                result.getProperties().put(
                    SumarisConfigurationOption.DB_TIMEZONE.getKey(),
                    dbTimeZone);
            }

            // Trash enable ?
            result.getProperties().computeIfAbsent(
                SumarisConfigurationOption.ENABLE_ENTITY_TRASH.getKey(),
                (key) -> Boolean.toString(configuration.enableEntityTrash()));
        }

        return result;
    }

    protected String getProperty(ConfigurationVO config, String propertyName) {
        return MapUtils.getString(config.getProperties(), propertyName);
    }

    protected String[] getPropertyAsArray(ConfigurationVO config, String propertyName) {
        String value = getProperty(config, propertyName);

        if (StringUtils.isBlank(value)) return null;

        try {
            return objectMapper.readValue(value, String[].class);
        } catch (IOException e) {
            log.warn(String.format("Unable to deserialize array value for option {%s}: %s", propertyName, value));
            return value.split(",");
        }
    }

    protected void fillPartners(ConfigurationVO result) {
        String[] values = getPropertyAsArray(result, SumarisServerConfigurationOption.SITE_PARTNER_DEPARTMENTS.getKey());

        if (ArrayUtils.isNotEmpty(values)) {

            // Get department from IDs
            int[] ids = Stream.of(values)
                .map(String::trim)
                .mapToInt(uri -> {
                    if (uri.startsWith(DepartmentService.URI_DEPARTMENT_SUFFIX)) {
                        return Integer.parseInt(uri.substring(DepartmentService.URI_DEPARTMENT_SUFFIX.length()));
                    }
                    return -1;
                })
                .filter(id -> id >= 0).toArray();
            List<DepartmentVO> departments = departmentService.getByIds(ids);

            // Get department from JSON
            List<DepartmentVO> deserializeDepartments = Stream.of(values)
                .map(String::trim)
                .map(jsonStr -> {
                    if (jsonStr.startsWith(JSON_START_SUFFIX)) {
                        try {
                            return objectMapper.readValue(jsonStr, DepartmentVO.class);
                        } catch (IOException e) {
                            log.warn(String.format("Unable to deserialize a value for option {%s}: %s", SumarisServerConfigurationOption.SITE_PARTNER_DEPARTMENTS.getKey(), jsonStr), e);
                            return null;
                        }
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toList());

            departments = Stream.concat(departments.stream(), deserializeDepartments.stream())
                .collect(Collectors.toList());
            departments.forEach(imageService::fillLogo);
            result.setPartners(departments);
        }
    }

    protected void fillBackgroundImages(ConfigurationVO result) {
        String[] values = getPropertyAsArray(result, SumarisServerConfigurationOption.SITE_BACKGROUND_IMAGES.getKey());

        if (ArrayUtils.isNotEmpty(values)) {

            List<String> urls = Stream.of(values)
                .map(imageService::getImageUrlByUri)
                .collect(Collectors.toList());
            result.setBackgroundImages(urls);
        }
    }

    /**
     * Clean configuraiton properties for NON admin users
     * @param configuration
     * @return
     */
    protected ConfigurationVO sanitizeConfiguration(ConfigurationVO configuration) {

        // Remove all transient keys (but keep some, like DB Timezone...)
        // TODO

        return configuration;
    }
}
