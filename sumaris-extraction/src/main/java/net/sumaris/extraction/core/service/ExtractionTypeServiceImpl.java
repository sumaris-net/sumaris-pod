package net.sumaris.extraction.core.service;

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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.config.ExtractionCacheConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTypeFilterVO;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manage extraction types
 *
 * @author benoit.lavenier@e-is.pro
 * @since 1.25.0
 */
@Service("extractionTypeService")
@Slf4j
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class ExtractionTypeServiceImpl implements ExtractionTypeService {

    @Autowired
    private ExtractionConfiguration configuration;

    @Autowired
    private ExtractionProductService extractionProductService;

    private Set<IExtractionType> availableLiveTypes = Sets.newHashSet();

    private boolean enableProduct = false;

    @PostConstruct
    protected void init() {
        enableProduct = configuration.enableExtractionProduct();
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        boolean enableProduct = configuration.enableExtractionProduct();
        if (enableProduct != this.enableProduct) {
            this.enableProduct = enableProduct;
            // TODO: clear cache ?
        }
    }

    public void registerLiveTypes(Set<IExtractionType> types) {
        synchronized (this.availableLiveTypes) {
            types.stream()
                .filter(t -> !this.availableLiveTypes.contains(t))
                .forEach(this.availableLiveTypes::add);
        }
    }

    @Override
    public List<ExtractionTypeVO> getLiveTypes() {

        return availableLiveTypes.stream()
            // Sort by format
            .sorted(Comparator.comparing(IExtractionType::getFormat))
            .map(ExtractionTypeVO::new)
            .collect(Collectors.toList());
    }

    public List<ExtractionTypeVO> findAll() {
        return findAllByFilter(null, null);
    }

    @Override
    @Cacheable(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES)
    public List<ExtractionTypeVO> findAllByFilter(@Nullable ExtractionTypeFilterVO filter,
                                                  @Nullable Page page) {
        if (page == null) {
            return this.findAllByFilter(filter, 0, 1000, null, null);
        }
        return this.findAllByFilter(filter, (int)page.getOffset(), page.getSize(), page.getSortBy(), page.getSortDirection());
    }

    @Override
    public IExtractionType getByExample(@NonNull IExtractionType source) {
        return getByExample(source, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
    }

    @Override
    @Cacheable(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPE_BY_EXAMPLE,
        key = "#source.id + #source.label + #source.format + #source.version + #fetchOptions.hashCode()",
        condition = " #source != null", unless = "#result == null")
    public IExtractionType getByExample(@NonNull IExtractionType source, @NonNull ExtractionProductFetchOptions fetchOptions) {

        // Product
        if (enableProduct && ExtractionTypes.isProduct(source)) {
            // Product by id
            if (source.getId() != null) return extractionProductService.get(source.getId(), fetchOptions);
            //  Product by label
            if (source.getLabel() != null) return extractionProductService.getByLabel(source.getLabel(), fetchOptions);
        }

        // Live format
        return ExtractionTypes.findOneMatch(availableLiveTypes, source);
    }

    /* -- protected functions -- */

    private List<ExtractionTypeVO> findAllByFilter(@Nullable ExtractionTypeFilterVO filter,
                                                   int offset,
                                                   int size,
                                                   String sortAttribute,
                                                   SortDirection sortDirection) {
        List<ExtractionTypeVO> types = Lists.newArrayList();
        filter = ExtractionTypeFilterVO.nullToEmpty(filter);

        // Exclude types with a DISABLE status, by default
        if (ArrayUtils.isEmpty(filter.getStatusIds())) {
            filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
        }

        boolean includeLiveTypes = ArrayUtils.contains(filter.getStatusIds(), StatusEnum.TEMPORARY.getId()) &&
            filter.getRecorderPersonId() == null;
        ExtractionCategoryEnum filterCategory = ExtractionCategoryEnum.fromString(filter.getCategory()).orElse(null);

        // Add live extraction types (= private by default)
        if (includeLiveTypes && (filterCategory == null || filterCategory == ExtractionCategoryEnum.LIVE)) {
            types.addAll(getLiveTypes());
        }

        // Add product types
        if (enableProduct && (filterCategory == null || filterCategory == ExtractionCategoryEnum.PRODUCT)) {
            types.addAll(findProductsByFilter(filter));
        }

        return types.stream()
            .filter(ExtractionTypeFilterVO.toPredicate(filter))
            .sorted(Beans.naturalComparator(sortAttribute, sortDirection))
            .skip(offset)
            .limit((size < 0) ? types.size() : size)
            .collect(Collectors.toList()
            );
    }

    protected List<ExtractionTypeVO> findProductsByFilter(ExtractionTypeFilterVO filter) {
        Preconditions.checkNotNull(filter);

        return ListUtils.emptyIfNull(
                extractionProductService.findByFilter(filter, ExtractionProductFetchOptions.TABLES_AND_RECORDER))
            .stream()
            .map(this::toExtractionTypeVO)
            .collect(Collectors.toList());
    }

    protected ExtractionTypeVO toExtractionTypeVO(ExtractionProductVO product) {
        ExtractionTypeVO type = new ExtractionTypeVO();
        toExtractionTypeVO(product, type);
        return type;
    }

    protected void toExtractionTypeVO(ExtractionProductVO source, ExtractionTypeVO target) {

        Beans.copyProperties(source, target);

        // Recorder department
        target.setRecorderDepartment(source.getRecorderDepartment());
        target.setRecorderPerson(source.getRecorderPerson());
    }

}
