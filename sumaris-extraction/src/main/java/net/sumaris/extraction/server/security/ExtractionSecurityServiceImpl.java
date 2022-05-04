package net.sumaris.extraction.server.security;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.extraction.core.service.AggregationService;
import net.sumaris.extraction.core.vo.AggregationTypeVO;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.extraction.core.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.extraction.server.config.ExtractionWebConfigurationOption;
import net.sumaris.server.security.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * @author benoit.lavenier@e-is.pro
 */
@Slf4j
@Service("extractionSecurityService")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
public class ExtractionSecurityServiceImpl implements ExtractionSecurityService {

    @Autowired
    private SumarisConfiguration configuration;

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private IAuthService<PersonVO> authService;

    private String accessNotSelfExtractionMinRole;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        this.accessNotSelfExtractionMinRole = configuration.getApplicationConfig().getOption(ExtractionWebConfigurationOption.ACCESS_NOT_SELF_EXTRACTION_MIN_ROLE.getKey());
    }

    @Override
    public boolean canReadAll() {
        return (StringUtils.isNotBlank(accessNotSelfExtractionMinRole)
                && authService.hasAuthority(accessNotSelfExtractionMinRole))
                || authService.isAdmin();
    }

    @Override
    public boolean canRead(@NonNull IExtractionFormat format) {

        // User can read all extraction
        if (canReadAll()) return true; // OK if can read all

        if (format.getCategory() != null && format.getCategory() == ExtractionCategoryEnum.LIVE) {
            return false; // KO: Live extraction not allowed, when cannot read all data
        }

        ExtractionTypeVO type = aggregationService.getTypeByFormat(format);

        if (type.isPublic()) return true; // OK if public

        // Get extraction type recorder
        PersonVO user = getAuthenticatedUser().orElse(null);
        if (user == null) throw new UnauthorizedException("User not login");

        Integer recorderPersonId = type.getRecorderPerson() != null ? type.getRecorderPerson().getId() : null;
        boolean isSameRecorder = Objects.equals(user.getId(), recorderPersonId);
        if (isSameRecorder) return true; // OK if same

        return false; // KO: not same recorder
    }

    @Override
    public boolean canWrite() {
        return authService.isSupervisor();
    }

    @Override
    public boolean canWriteAll() {
        return authService.isAdmin();
    }

    @Override
    public boolean canWrite(@NonNull ExtractionTypeVO type) {

        if (type.getCategory() != null && type.getCategory() == ExtractionCategoryEnum.LIVE) {
            return false; // KO: only products are writable
        }

        // User can read all extraction
        if (canWriteAll()) return true; // OK if can read all

        // Get extraction type recorder
        PersonVO user = getAuthenticatedUser().orElse(null);
        if (user == null) throw new UnauthorizedException("User not login");

        Integer recorderPersonId = type.getRecorderPerson() != null ? type.getRecorderPerson().getId() : null;
        boolean isSameRecorder = user != null && Objects.equals(user.getId(), recorderPersonId);
        if (isSameRecorder) return true; // OK if same issuer

        return false; // KO
    }

    @Override
    public boolean canRead(int productId) {
       AggregationTypeVO type = aggregationService.getTypeById(productId, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
       return canRead(type);
    }

    @Override
    public boolean canWrite(int productId) throws UnauthorizedException {
        AggregationTypeVO type = aggregationService.getTypeById(productId, ExtractionProductFetchOptions.builder()
            .withRecorderDepartment(true)
            .withRecorderPerson(true)
        .build());
        return canWrite(type);
    }

    @Override
    public Optional<PersonVO> getAuthenticatedUser() {
        return authService.getAuthenticatedUser();
    }

    @Override
    public ExtractionTypeFilterVO sanitizeFilter(ExtractionTypeFilterVO filter) {
        ExtractionTypeFilterVO result = filter != null ? filter : new ExtractionTypeFilterVO();

        if (canReadAll()) return result;

        return getAuthenticatedUser()

            // Known user: restrict to self data - issue #199
            .map(user -> {
                result.setRecorderPersonId(user.getId());
                result.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
                return result;
            })

            // Anonymous user: limit to public extraction
            .orElseGet(() -> {
                result.setCategory(ExtractionCategoryEnum.PRODUCT.name());
                result.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId()});
                return result;
            });
    }
}
