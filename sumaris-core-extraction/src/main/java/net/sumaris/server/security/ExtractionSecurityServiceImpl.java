package net.sumaris.server.security;

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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.extraction.service.AggregationService;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.util.*;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.server.config.ExtractionWebConfigurationOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author benoit.lavenier@e-is.pro
 */
@Service("extractionSecurityService")
@ConditionalOnBean({IAuthService.class})
public class ExtractionSecurityServiceImpl implements ExtractionSecurityService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionSecurityServiceImpl.class);

    @Autowired
    protected SumarisConfiguration configuration;

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    protected IAuthService<PersonVO> authService;


    private String minRoleForNotSelfDataAccess;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        this.minRoleForNotSelfDataAccess = configuration.getApplicationConfig().getOption(ExtractionWebConfigurationOption.AUTH_ROLE_NOT_SELF_EXTRACTION_ACCESS.getKey());
    }


    @Override
    public boolean canReadAll() {
        return StringUtils.isBlank(minRoleForNotSelfDataAccess) || authService.hasAuthority(minRoleForNotSelfDataAccess);
    }

    @Override
    public boolean canRead(@NonNull ExtractionTypeVO type) {

        // User can read all extraction
        if (canReadAll()) return true; // OK if can read all

        if (type.getCategory() != null && type.getCategory() == ExtractionCategoryEnum.LIVE) {
            return false; // KO: Live extraction not allowed, when cannot read all data
        }

        if (type.isPublic()) return true; // OK if public

        // Get extraction type recorder
        PersonVO user = getAuthenticatedUser().orElse(null);
        if (user == null) throw new UnauthorizedException("User not login");

        Integer recorderPersonId = type.getRecorderPerson() != null ? type.getRecorderPerson().getId() : null;
        boolean isSameRecorder = user != null && Objects.equals(user.getId(), recorderPersonId);
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
       AggregationTypeVO type = aggregationService.get(productId, ExtractionProductFetchOptions.MINIMAL);
       return canRead(type);
    }

    @Override
    public boolean canWrite(int productId) throws UnauthorizedException {
        AggregationTypeVO type = aggregationService.get(productId, ExtractionProductFetchOptions.MINIMAL);
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

        return getAuthenticatedUser().filter(Objects::nonNull)

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
