package net.sumaris.server.http.rest;

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


import net.sumaris.core.service.technical.SoftwareService;
import net.sumaris.core.vo.technical.SoftwareVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.vo.node.NodeSummaryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NodeInfoRestController {

    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(NodeInfoRestController.class);

    @Autowired
    private SumarisServerConfiguration config;

    @Autowired
    private SoftwareService softwareService;

    @ResponseBody
    @RequestMapping(value = RestPaths.NODE_INFO_PATH, method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public NodeSummaryVO getNodeSummary() {
        NodeSummaryVO result = new NodeSummaryVO();

        // Set software info
        result.setSoftwareName("sumaris-pod");
        result.setSoftwareVersion(config.getVersionAsString());

        // Set node info
        SoftwareVO software = softwareService.getDefault();
        if (software != null) {
            result.setNodeLabel(software.getLabel());
            result.setNodeName(software.getName());
        }

        return result;
    }
}
