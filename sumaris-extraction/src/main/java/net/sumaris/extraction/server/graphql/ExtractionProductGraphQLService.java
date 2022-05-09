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

package net.sumaris.extraction.server.graphql;

import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.extraction.*;
import net.sumaris.extraction.core.service.ExtractionManager;
import net.sumaris.extraction.core.service.ExtractionProductService;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.extraction.server.security.ExtractionSecurityService;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@GraphQLApi
@Service
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
public class ExtractionProductGraphQLService {

    private final ExtractionProductService productService;

    private final ExtractionManager extractionManager;

    private final ExtractionSecurityService securityService;

    public ExtractionProductGraphQLService(ExtractionProductService productService,
                                           ExtractionManager extractionManager, ExtractionSecurityService securityService) {
        this.productService = productService;
        this.extractionManager = extractionManager;
        this.securityService = securityService;
    }

    /* -- aggregation service -- */

    @GraphQLQuery(name = "extractionProduct", description = "Get one extraction product")
    @Transactional(readOnly = true)
    public ExtractionProductVO getProduct(@GraphQLArgument(name = "id") int id,
                                          @GraphQLEnvironment ResolutionEnvironment env) {
        securityService.checkReadAccess(id);
        return productService.get(id, getFetchOptions(GraphQLUtils.fields(env)));
    }

    @GraphQLQuery(name = "extractionProducts", description = "Get all available extraction products")
    @Transactional(readOnly = true)
    public List<ExtractionProductVO> findProductsByFilter(@GraphQLArgument(name = "filter") ExtractionTypeFilterVO filter,
                                                          @GraphQLEnvironment ResolutionEnvironment env) {
        filter = fillFilterDefaults(filter);
        return productService.findByFilter(filter, getFetchOptions(GraphQLUtils.fields(env)));
    }

    @GraphQLMutation(name = "saveProduct", description = "Create or update a extraction product")
    public ExtractionProductVO saveProduct(@GraphQLNonNull @GraphQLArgument(name = "product") ExtractionProductVO source,
                                           @GraphQLArgument(name = "filter") ExtractionFilterVO filter
    ) throws ExecutionException, InterruptedException {

        boolean isNew = source.getId() == null;
        if (isNew) {
            securityService.checkWriteAccess();
        }
        else {
            securityService.checkWriteAccess(source.getId());
        }

        // Execute, then save
        if (isNew) {
            return extractionManager.executeAndSave(source, filter, null);
        }

        // Save only
        return productService.save(source);
    }

    @GraphQLMutation(name = "deleteProducts", description = "Delete many products")
    @Transactional
    public void deleteProducts(@GraphQLArgument(name = "ids") int[] ids) {

        // Make sure can be deleted
        Arrays.stream(ids).forEach(securityService::checkWriteAccess);

        // Do deletion
        Arrays.stream(ids).forEach(productService::delete);
    }


    @GraphQLQuery(name = "extractionColumns", description = "Read columns from an extraction")
    @Transactional(readOnly = true)
    public List<ExtractionTableColumnVO> getProductColumns(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                                           @GraphQLArgument(name = "sheet") String sheetName,
                                                           @GraphQLEnvironment ResolutionEnvironment env) {

        // Check type
        ExtractionProductVO checkedType = getByExample(type);


        // Check access right
        securityService.checkReadAccess(checkedType);

        Set<String> fields = GraphQLUtils.fields(env);

        ExtractionTableColumnFetchOptions fetchOptions = ExtractionTableColumnFetchOptions.builder()
            .withRankOrder(fields.contains(ExtractionTableColumnVO.Fields.RANK_ORDER))
            .build();

        return productService.getColumnsBySheetName(checkedType.getId(), sheetName, fetchOptions);
    }

    @GraphQLMutation(name = "updateProduct", description = "Update an extraction product")
    public ExtractionProductVO updateProduct(@GraphQLArgument(name = "id") int id) throws ExecutionException, InterruptedException {

        // Make sure can update
        securityService.checkWriteAccess(id);

        // Do update
        return extractionManager.executeAndSave(id);
    }

    /* -- protected methods --*/

    protected ExtractionProductFetchOptions getFetchOptions(Set<String> fields) {
        return ExtractionProductFetchOptions.builder()
                .withDocumentation(fields.contains(ExtractionProductVO.Fields.DOCUMENTATION))
                .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
                // Tables (=sheets)
                .withTables(fields.contains(ExtractionTypeVO.Fields.SHEET_NAMES))
                // Columns not need
                .withColumns(false)
                // Stratum
                .withStratum(
                        fields.contains(StringUtils.slashing(ExtractionProductVO.Fields.STRATUM, IEntity.Fields.ID))
                        || fields.contains(StringUtils.slashing(ExtractionProductVO.Fields.STRATUM, AggregationStrataVO.Fields.SPATIAL_COLUMN_NAME))
                )

                .build();
    }


    protected ExtractionTypeFilterVO fillFilterDefaults(ExtractionTypeFilterVO filter) {
        filter = ExtractionTypeFilterVO.nullToEmpty(filter);

        // Restrict to self data - issue #199
        if (!securityService.canReadAll()) {
            PersonVO user = securityService.getAuthenticatedUser().orElse(null);
            if (user != null) {
                filter.setRecorderPersonId(user.getId());
                filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
            }
            else {
                filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId()});
            }
        }

        return filter;
    }

    protected ExtractionProductVO getByExample(IExtractionType source) {
        IExtractionType checkedType = extractionManager.getByExample(source);

        if (!(checkedType instanceof ExtractionProductVO)) throw new SumarisTechnicalException("Not a product extraction");

        return (ExtractionProductVO)checkedType;
    }
}
