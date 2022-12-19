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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.service.social.UserEventService;
import net.sumaris.core.service.technical.JobService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.service.ExtractionProductService;
import net.sumaris.extraction.core.service.ExtractionTypeService;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.core.vo.technical.extraction.ExtractionTypeFilterVO;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.extraction.server.config.ExtractionWebConfigurationOption;
import net.sumaris.server.security.ISecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.Serializable;
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

    private final SumarisConfiguration configuration;

    private final ExtractionProductService productService;
    private final ExtractionTypeService extractionTypeService;
    private final ISecurityContext<PersonVO> securityContext;

    private String accessNotSelfExtractionMinRole;

    public ExtractionSecurityServiceImpl(SumarisConfiguration configuration,
                                         ExtractionProductService productService,
                                         ExtractionTypeService extractionTypeService,
                                         Optional<ISecurityContext<PersonVO>> securityContext) {
        this.configuration = configuration;
        this.productService = productService;
        this.extractionTypeService = extractionTypeService;
        this.securityContext = securityContext.orElse(null);
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        this.accessNotSelfExtractionMinRole = configuration.getApplicationConfig().getOption(ExtractionWebConfigurationOption.ACCESS_NOT_SELF_EXTRACTION_MIN_ROLE.getKey());
    }

    @Override
    public boolean canReadAll() {
        return (StringUtils.isNotBlank(accessNotSelfExtractionMinRole)
                && securityContext.hasAuthority(accessNotSelfExtractionMinRole))
                || securityContext.isAdmin();
    }

    @Override
    public boolean canRead(@NonNull IExtractionType type) {

        // User can read all extraction
        if (canReadAll()) return true; // OK if can read all

        type = extractionTypeService.getByExample(type);

        // KO: Live extraction not allowed, when cannot read all data
        if (ExtractionTypes.isLive(type)) return false;

        // OK if public
        if (ExtractionTypes.isPublic(type)) return true;

        // Get extraction type recorder
        PersonVO user = getAuthenticatedUser().orElse(null);
        if (user == null) throw new UnauthorizedException("User not login");

        if (!(type instanceof IWithRecorderPersonEntity)) {
            log.warn("Invalid ExtractionType class: {}. Should implement IWithRecorderPersonEntity.class", type.getClass().getSimpleName());
            return false; // No recorder fetched (should never occur)
        }

        // OK if same recorder
        IEntity recorderPerson = ((IWithRecorderPersonEntity)type).getRecorderPerson();
        Serializable recorderPersonId = recorderPerson != null ? recorderPerson.getId() : null;
        return Objects.equals(user.getId(), recorderPersonId);
    }

    @Override
    public boolean canWrite() {
        return securityContext.isSupervisor();
    }

    @Override
    public boolean canWriteAll() {
        return securityContext.isAdmin();
    }

    @Override
    public boolean canWrite(@NonNull ExtractionProductVO type) {

        // KO: only products are writable
        if (!ExtractionTypes.isProduct(type)) return false;

        // User can read all extraction
        if (canWriteAll()) return true; // OK if can read all

        // Get extraction type recorder
        PersonVO user = getAuthenticatedUser().orElse(null);
        if (user == null) throw new UnauthorizedException("User not login");

        // OK if same issuer
        Integer recorderPersonId = type.getRecorderPerson() != null ? type.getRecorderPerson().getId() : null;
        return Objects.equals(user.getId(), recorderPersonId);
    }

    @Override
    public boolean canRead(int productId) {
       IExtractionType checkedType = extractionTypeService.getByExample(ExtractionTypeVO.builder().id(productId).build());
       return canRead(checkedType);
    }

    @Override
    public boolean canWrite(int productId) throws UnauthorizedException {
        ExtractionProductVO product = productService.get(productId, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
        return canWrite(product);
    }

    @Override
    public Optional<PersonVO> getAuthenticatedUser() {
        return securityContext.getAuthenticatedUser();
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
