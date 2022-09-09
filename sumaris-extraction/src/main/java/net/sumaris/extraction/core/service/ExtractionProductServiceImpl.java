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
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.config.ExtractionCacheConfiguration;
import net.sumaris.core.dao.technical.extraction.ExtractionProductRepository;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.technical.extraction.*;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.util.ExtractionTypes;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author benoit.lavenier@e-is.pro
 */
@Slf4j
@Service("extractionProductService")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class ExtractionProductServiceImpl implements ExtractionProductService {

    private final ExtractionProductRepository extractionProductRepository;

    private final SumarisDatabaseMetadata databaseMetadata;


    public ExtractionProductServiceImpl(ExtractionProductRepository extractionProductRepository,
                                        SumarisDatabaseMetadata databaseMetadata) {
        this.extractionProductRepository = extractionProductRepository;
        this.databaseMetadata = databaseMetadata;
    }

    @Override
    public List<ExtractionProductVO> findByFilter(ExtractionTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions) {
        return extractionProductRepository.findAll(filter, fetchOptions);
    }

    @Override
    @Cacheable(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_ID)
    public ExtractionProductVO get(int id, ExtractionProductFetchOptions fetchOptions) {
        return extractionProductRepository.get(id, fetchOptions);
    }

    @Override
    public ExtractionProductVO getByLabel(String label, ExtractionProductFetchOptions fetchOptions) {
        return extractionProductRepository.getByLabel(label, fetchOptions);
    }

    @Override
    public Optional<ExtractionProductVO> findById(int id, ExtractionProductFetchOptions fetchOptions) {
        return extractionProductRepository.findVOById(id, fetchOptions);
    }

    @Override
    public Optional<ExtractionProductVO> findByLabel(String label, ExtractionProductFetchOptions fetchOptions) {
        return extractionProductRepository.findByLabel(label, fetchOptions);
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_ID, allEntries = true),
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPE_BY_EXAMPLE, allEntries = true),
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES, allEntries = true)
        }
    )
    public ExtractionProductVO save(ExtractionProductVO source, ExtractionProductSaveOptions saveOptions) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());
        Preconditions.checkNotNull(source.getName());
        Preconditions.checkNotNull(source.getFormat());
        Preconditions.checkNotNull(source.getVersion());
        Preconditions.checkNotNull(source.getRecorderDepartment());
        Preconditions.checkNotNull(source.getRecorderDepartment().getId());

        saveOptions = ExtractionProductSaveOptions.nullToEmpty(saveOptions);

        // Load the product
        ExtractionProductVO target = null;
        if (source.getId() != null) {
            target = findById(source.getId(), ExtractionProductFetchOptions.FOR_UPDATE).orElse(null);

            // Not found : warn
            if (target == null) {
                log.warn("Extraction #{} not exists. Saving as a new extraction...", source.getId());
                source.setId(null);
            }
        }
        boolean isNew = target == null;
        // Check label != format
        if (isNew) {
            Preconditions.checkArgument(!Objects.equals(source.getLabel().toUpperCase(), source.getFormat().toUpperCase()), "Invalid label. Expected pattern: <format>-NNN");
        }
        // Check label was not changed
        else {
            Preconditions.checkArgument(Objects.equals(target.getLabel(), source.getLabel()), "Cannot change a product label");
        }

        // Remember the table to remove
        Collection<String> tablesToRemove = Sets.newHashSet();
        if (!isNew && saveOptions.isWithTables()) {
            Collection<String> existingTables = Beans.getList(target.getTableNames());
            if (CollectionUtils.isNotEmpty(existingTables)) {
                Collection<String> sourceTables = Beans.getList(source.getTableNames());
                existingTables.stream()
                    .filter(t -> !sourceTables.contains(t))
                    .forEach(tablesToRemove::add);
            }
        }

        fillDefaultProperties(source);

        // Save it
        target = extractionProductRepository.save(source, saveOptions);

        // Drop old tables
        dropTables(tablesToRemove);

        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_ID, allEntries = true),
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPE_BY_EXAMPLE, allEntries = true),
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES, allEntries = true)
    })
    public void delete(int id) {
        extractionProductRepository.deleteById(id);
    }

    @Override
    public List<ExtractionTableColumnVO> getColumnsBySheetName(int id, String sheetName, ExtractionTableColumnFetchOptions fetchOptions) {

        // Try to find columns from the DB
        List<ExtractionTableColumnVO> columns = null;
        if (StringUtils.isNotBlank(sheetName)) {
            columns = extractionProductRepository.getColumnsByIdAndTableLabel(id, sheetName);
        }

        // If nothing in the DB, create list from DB metadata
        if (CollectionUtils.isEmpty(columns)) {
            ExtractionProductVO source = get(id, ExtractionProductFetchOptions.TABLES_AND_RECORDER);

            String tableName = source.findTableNameBySheetName(sheetName)
                    .orElseThrow(() -> new DataRetrievalFailureException(String.format("Product id=%s has no sheetName '%s'", id, sheetName)));

            // Get columns
            columns = getColumns(tableName,
                    ExtractionTableColumnFetchOptions.builder()
                        .withRankOrder(false) // skip rankOrder, because fill later, by format and sheetName (more accuracy)
                        .build());

            // Fill rank order, if need
            if (fetchOptions.isWithRankOrder()) {
                ExtractionTableColumnOrder.fillRankOrderByFormatAndSheet(source.getFormat(), sheetName, columns);
            }
        }

        return columns;
    }

    @Override
    public List<ExtractionTableColumnVO> getColumns(String tableName, ExtractionTableColumnFetchOptions fetchOptions) {
        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Preconditions.checkNotNull(table, "Unknown table: " + tableName);
        return toProductColumnVOs(table, table.getColumnNames(), fetchOptions);
    }

    protected void dropTables(Collection<String> tableNames) {
        Beans.getStream(tableNames).forEach(extractionProductRepository::dropTable);
    }

    /**
     * Read table metadata, to get column.
     *
     * /!\ Important: column order must be the unchanged !! Otherwise getTableGroupByRows() will not work well
     *
     * @param table
     * @param columnNames
     * @param fetchOptions
     * @return
     */
    protected List<ExtractionTableColumnVO> toProductColumnVOs(SumarisTableMetadata table,
                                                               Collection<String> columnNames,
                                                               ExtractionTableColumnFetchOptions fetchOptions) {

        List<ExtractionTableColumnVO> columns = columnNames.stream()
            // Get column metadata
            .map(table::getColumnMetadata)
            .filter(Objects::nonNull)
            // Transform in VO
            .map(ExtractionProducts::toProductColumnVO)
            .collect(Collectors.toList());

        if (fetchOptions.isWithRankOrder()) {
            ExtractionTableColumnOrder.fillRankOrderByTableName(table.getName(), columns);
        }

        return columns;
    }

    protected void fillDefaultProperties(ExtractionProductVO source) {
        if (source == null) return;

        // Set default frequency to manually
        ProcessingFrequencyEnum frequency = source.getProcessingFrequencyId() != null
            ? ProcessingFrequencyEnum.valueOf(source.getProcessingFrequencyId())
            : ProcessingFrequencyEnum.MANUALLY;
        source.setProcessingFrequencyId(frequency.getId());

        // Set default isSpatial
        source.setIsSpatial(ExtractionTypes.isAggregation(source));
    }
}
