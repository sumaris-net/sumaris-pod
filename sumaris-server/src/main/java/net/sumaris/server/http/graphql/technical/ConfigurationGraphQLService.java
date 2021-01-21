package net.sumaris.server.http.graphql.technical;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.service.technical.CacheStatistics;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.service.technical.SoftwareService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.technical.ConfigurationVO;
import net.sumaris.core.vo.technical.SoftwareVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.http.graphql.administration.AdministrationGraphQLService;
import net.sumaris.server.http.ontology.RestPaths;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.service.administration.ImageService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ConfigurationGraphQLService {

    public static final String JSON_START_SUFFIX = "{";

    private static final Log log = LogFactory.getLog(ConfigurationGraphQLService.class);

    @Autowired
    private SoftwareService service;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AdministrationGraphQLService administrationGraphQLService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheStatistics cacheStatistics;

    @Autowired
    private CacheManager cacheManager;

    private String imageUrl;

    @Autowired
    public ConfigurationGraphQLService(SumarisServerConfiguration config) {
        super();

        // Prepare URL for String formatter
        imageUrl = config.getServerUrl() + RestPaths.IMAGE_PATH;
    }

    @GraphQLQuery(name = "configuration", description = "A software configuration")
    public ConfigurationVO getConfiguration(
        @GraphQLArgument(name = "id") Integer id,
        @GraphQLArgument(name = "label") String label,
        @GraphQLEnvironment() Set<String> fields
    ) {
        SoftwareVO software = getSoftware(id, label);
        return toConfiguration(software, fields);
    }

    @GraphQLMutation(name = "saveConfiguration", description = "Save a configuration")
    @IsAdmin
    public ConfigurationVO saveConfiguration(
        @GraphQLArgument(name = "config") ConfigurationVO configuration,
        @GraphQLEnvironment() Set<String> fields) {

        SoftwareVO software = service.save(configuration);

        // Applying default

        return toConfiguration(software, fields);
    }

    @GraphQLQuery(name = "software", description = "A software config")
    @IsAdmin
    public SoftwareVO getSoftware(
        @GraphQLArgument(name = "id") Integer id,
        @GraphQLArgument(name = "label") String label
    ) {
        if (id != null) {
            return service.get(id);
        }
        if (label == null) {
            return configurationService.getCurrentSoftware();
        }
        return service.getByLabel(label);
    }

    @GraphQLMutation(name = "saveSoftware", description = "Save a software configuration")
    @IsAdmin
    public SoftwareVO saveSoftware(@GraphQLArgument(name = "software") SoftwareVO software) {
        return service.save(software);
    }

    @GraphQLQuery(name = "cacheStatistics", description = "Get cache statistics")
    @IsAdmin
    public Map<String, Map<String, Long>> getCacheStats() {
        return cacheStatistics.getCacheDetails();
    }

    @GraphQLQuery(name = "clearCache", description = "Clear a single cache or all caches")
    @IsAdmin
    public boolean clearCache(
        @GraphQLArgument(name = "name") String name
    ) {
        try {
            if (StringUtils.isBlank(name)) {
                log.info("Clearing caches...");
                cacheManager.clearAll();

            } else {
                log.info(String.format("Clearing cache (%s)...", name));
                Ehcache cache = cacheManager.getEhcache(name);
                if (cache != null)
                    cache.removeAll();
            }
        } catch (IllegalStateException | CacheException e) {
            log.error("Error while clearing caches", e);
            return false;
        }
        log.info("Caches cleared.");
        return true;
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
            String logoUrl = getImageUrl(logoUri);
            result.getProperties().put(
                SumarisServerConfigurationOption.SITE_LOGO_SMALL.getKey(),
                logoUrl);
            result.setSmallLogo(logoUrl);
        }

        // Fill large logo
        String logoLargeUri = getProperty(result, SumarisServerConfigurationOption.LOGO_LARGE.getKey());
        if (StringUtils.isNotBlank(logoLargeUri)) {
            String logoLargeUrl = getImageUrl(logoLargeUri);
            result.getProperties().put(
                SumarisServerConfigurationOption.LOGO_LARGE.getKey(),
                logoLargeUrl);
            result.setLargeLogo(logoLargeUrl);
        }

        // Replace favicon ID by an URL
        String faviconUri = getProperty(result, SumarisServerConfigurationOption.SITE_FAVICON.getKey());
        if (StringUtils.isNotBlank(faviconUri)) {
            String faviconUrl = getImageUrl(faviconUri);
            result.getProperties().put(SumarisServerConfigurationOption.SITE_FAVICON.getKey(), faviconUrl);
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
                .map(administrationGraphQLService::fillLogo)
                .collect(Collectors.toList());
            result.setPartners(departments);
        }
    }

    protected void fillBackgroundImages(ConfigurationVO result) {
        String[] values = getPropertyAsArray(result, SumarisServerConfigurationOption.SITE_BACKGROUND_IMAGES.getKey());

        if (ArrayUtils.isNotEmpty(values)) {

            List<String> urls = Stream.of(values)
                .map(this::getImageUrl)
                .collect(Collectors.toList());
            result.setBackgroundImages(urls);
        }
    }

    protected String getImageUrl(String imageUri) {
        if (StringUtils.isBlank(imageUri)) return null;

        // Resolve URI like 'image:<ID>'
        if (imageUri.startsWith(ImageService.URI_IMAGE_SUFFIX)) {
            return imageUrl.replace("{id}", imageUri.substring(ImageService.URI_IMAGE_SUFFIX.length()));
        }
        // should be a URL, so return it
        return imageUri;
    }

}
