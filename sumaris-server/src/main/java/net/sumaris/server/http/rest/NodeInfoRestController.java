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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.vo.technical.SoftwareVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.util.node.NodeSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class NodeInfoRestController {

    @Autowired
    private SumarisServerConfiguration configuration;

    @Autowired
    private ConfigurationService configurationService;

    @ResponseBody
    @GetMapping(value = RestPaths.NODE_INFO_PATH,
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_UTF8_VALUE
        })
    public NodeSummaryVO getNodeSummary() {
        NodeSummaryVO result = new NodeSummaryVO();

        // Set software info
        result.setSoftwareName("sumaris-pod");
        result.setSoftwareVersion(configuration.getVersionAsString());

        // Set node info
        SoftwareVO software = configurationService.getCurrentSoftware();
        if (software != null) {
            result.setNodeLabel(software.getLabel());
            result.setNodeName(software.getName());
        }

        return result;
    }
}
