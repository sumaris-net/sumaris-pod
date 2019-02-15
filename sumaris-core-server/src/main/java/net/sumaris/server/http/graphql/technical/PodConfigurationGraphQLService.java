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

import io.leangen.graphql.annotations.*;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.service.technical.SoftwareService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.technical.PodConfigurationVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.http.graphql.administration.AdministrationGraphQLService;
import net.sumaris.server.http.rest.RestPaths;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class PodConfigurationGraphQLService {

    private static final Log log = LogFactory.getLog(PodConfigurationGraphQLService.class);

    @Autowired
    private SoftwareService service;

    @Autowired
    private AdministrationGraphQLService administrationGraphQLService;

    @Autowired
    private DepartmentService departmentService;

    private String imageUrl;

    @Autowired
    public PodConfigurationGraphQLService(SumarisServerConfiguration config) {
        super();

        // Prepare URL for String formatter
        imageUrl = config.getServerUrl() + RestPaths.IMAGE_PATH;
    }

    @GraphQLQuery(name = "configuration", description = "All Pod's Configurations")
    @Transactional(readOnly = true)
    public PodConfigurationVO getConfiguration(
            @GraphQLEnvironment() Set<String> fields
    ){
        PodConfigurationVO result  = service.getDefault();

        // Fill partners departments
        if (result != null && fields.contains(PodConfigurationVO.PROPERTY_PARTNERS)) {
            this.fillPartners(result);
        }

        // Fill background images URLs
        if (result != null && fields.contains(PodConfigurationVO.PROPERTY_BACKGROUND_IMAGES)) {
            this.fillBackgroundImages(result);
        }

        // Fill default program
        result.setDefaultProgram(getProperty(result, SumarisServerConfigurationOption.DEFAULT_PROGRAM.getKey()));

        // Fill logo
        String logoId = getProperty(result, SumarisServerConfigurationOption.SITE_LOGO_ID.getKey());
        result.setLogo(getImageUrl(logoId));

        // Fill favicon
        result.getProperties().put("favicon",  getImageUrl(getProperty(result, "favicon") ));

        return result ;
    }

    @GraphQLMutation(name = "saveConfiguration", description = "Save the pod configuration")
    @Transactional
    public void save(@GraphQLArgument(name = "app") PodConfigurationVO configuration){
        service.save(configuration);
    }

    /* -- protected methods -- */

    protected String getProperty(PodConfigurationVO config, String propertyName) {
        return MapUtils.getString(config.getProperties(), propertyName);
    }

    protected void fillPartners(PodConfigurationVO result) {
        String depIds = getProperty(result, SumarisServerConfigurationOption.SITE_PARTNERS_DEPARTMENT_IDS.getKey());

        if (StringUtils.isNotBlank(depIds)) {
            int[] ids = Stream.of(depIds.split(",")).mapToInt(Integer::parseInt).toArray();
            List<DepartmentVO> departments = departmentService.getByIds(ids);
            departments.stream().forEach(administrationGraphQLService::fillLogo);
            result.setPartners(departments);
        }
    }

    protected void fillBackgroundImages(PodConfigurationVO result) {
        String imageIds = getProperty(result, SumarisServerConfigurationOption.SITE_BACKGROUND_IMAGE_IDS.getKey());

        if (StringUtils.isNotBlank(imageIds)) {
            List<String> urls = Stream.of(imageIds.split(","))
                    .map(this::getImageUrl)
                    .collect(Collectors.toList());
            result.setBackgroundImages(urls);
        }
    }

    protected String getImageUrl(String id) {
        if (StringUtils.isBlank(id)) return null;
        return imageUrl.replace("{id}", id);
    }

}
