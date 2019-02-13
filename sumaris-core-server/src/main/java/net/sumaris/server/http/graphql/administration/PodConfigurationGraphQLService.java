package net.sumaris.server.http.graphql.administration;

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
import net.sumaris.core.service.administration.PodConfigService;
import net.sumaris.core.vo.technical.PodConfigurationVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PodConfigurationGraphQLService {

    private static final Log log = LogFactory.getLog(PodConfigurationGraphQLService.class);

    @Autowired
    private PodConfigService podConfigService ;


    @Autowired
    public PodConfigurationGraphQLService(SumarisServerConfiguration config) {
        super();

    }

    @GraphQLQuery(name = "configuration", description = "All Pod's Configurations")
    @Transactional(readOnly = true)
    public PodConfigurationVO findConfs(){
        PodConfigurationVO res = new PodConfigurationVO();
        res.setName("POD's Configuration");
        res.setLabel("Sumaris Flavour ");
        res.setBackGroundImages(podConfigService.listBackgrounds());
        res.setProperties(podConfigService.propertiesVO("ADAP"));


        return res;
    }



}
