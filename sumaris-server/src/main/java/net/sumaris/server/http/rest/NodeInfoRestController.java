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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.Programs;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.technical.SoftwareVO;
import net.sumaris.server.config.ServerCacheConfiguration;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.service.administration.DataAccessControlService;
import net.sumaris.server.util.node.NodeFeatureVO;
import net.sumaris.server.util.node.NodeSummaryVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NodeInfoRestController {

    private final SumarisServerConfiguration configuration;

    private final ConfigurationService configurationService;

    private final ProgramService programService;

    private final DataAccessControlService accessControlService;

    @ResponseBody
    @GetMapping(value = RestPaths.NODE_INFO_PATH,
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_UTF8_VALUE
        })
    @Cacheable(cacheNames = ServerCacheConfiguration.Names.NODE_INFO)
    public NodeSummaryVO getNodeSummary() {
        NodeSummaryVO result = new NodeSummaryVO();

        // Set software info
        result.setSoftwareName("sumaris-pod");

        String version = configuration.getVersionAsString();
        if (StringUtils.isBlank(version)) {
            version = "0.0.1"; // Should never occur
        }
        result.setSoftwareVersion(version);

        // Set node info
        SoftwareVO software = configurationService.getCurrentSoftware();
        if (software != null) {
            result.setNodeLabel(software.getLabel());
            result.setNodeName(software.getName());
        }

        // Add features
        List<NodeFeatureVO> features = getFeatures();
        if (features != null) {
            result.setFeatures(features);
        }

        return result;
    }


    private List<NodeFeatureVO> getFeatures() {

        Integer[] authorizedProgramIds = this.accessControlService.getAuthorizedProgramIds()
                .map(list -> list.toArray(new Integer[0]))
                .orElse(new Integer[0]);

        List<ProgramVO> programsVO = this.programService.findAll(ProgramFilterVO.builder()
                .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
                .includedIds(authorizedProgramIds)
                .build());

        // Use programs as features
        return
                programsVO.stream()
                        .map(authorizedProgram -> NodeFeatureVO.builder()
                                .id(authorizedProgram.getId())
                                .name(authorizedProgram.getName())
                                .label(authorizedProgram.getLabel())
                                .description(authorizedProgram.getDescription())
                                .updateDate(authorizedProgram.getUpdateDate())
                                .creationDate(authorizedProgram.getCreationDate())
                                .statusId(authorizedProgram.getStatusId())
                                .logo(Programs.getProperty(authorizedProgram, ProgramPropertyEnum.PROGRAM_LOGO))
                                .build()
                        ).toList();
    }
}
