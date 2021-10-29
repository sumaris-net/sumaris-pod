package net.sumaris.core.extraction.service;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.extraction.ExtractionProductRepository;
import net.sumaris.core.extraction.config.ExtractionConfiguration;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author benoit.lavenier@e-is.pro
 */
@Slf4j
@Service("extractionProductService")
@ConditionalOnBean({ExtractionConfiguration.class})
public class ExtractionProductServiceImpl implements ExtractionProductService {

    private ExtractionProductRepository productRepository;
    private ExtractionTableDao tableDao;

    public ExtractionProductServiceImpl(ExtractionProductRepository productRepository,
                                        ExtractionTableDao tableDao) {
        this.productRepository = productRepository;
        this.tableDao = tableDao;
    }

    @Override
    public List<ExtractionProductVO> findByFilter(ExtractionProductFilterVO filter, ExtractionProductFetchOptions fetchOptions) {
        return productRepository.findAll(filter, fetchOptions);
    }

    @Override
    public ExtractionProductVO get(int id, ExtractionProductFetchOptions fetchOptions) {
        return productRepository.get(id, fetchOptions);
    }

    @Override
    public ExtractionProductVO getByLabel(String label, ExtractionProductFetchOptions fetchOptions) {
        return productRepository.getByLabel(label, fetchOptions);
    }

    @Override
    public Optional<ExtractionProductVO> findById(int id, ExtractionProductFetchOptions fetchOptions) {
        return productRepository.findById(id, fetchOptions);
    }

    @Override
    public Optional<ExtractionProductVO> findByLabel(String label, ExtractionProductFetchOptions fetchOptions) {
        return productRepository.findByLabel(label, fetchOptions);
    }

    @Override
    public ExtractionProductVO save(ExtractionProductVO source) {
        // Save the product
        return productRepository.save(source);
    }

    @Override
    public void delete(int id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<ExtractionTableColumnVO> getColumnsBySheetName(int id, String sheetName, ExtractionTableColumnFetchOptions fetchOptions) {


        // Try to find columns from the DB
        List<ExtractionTableColumnVO> columns = null;
        if (StringUtils.isNotBlank(sheetName)) {
            columns = productRepository.getColumnsByIdAndTableLabel(id, sheetName);
        }

        // If nothing in the DB, create list from DB metadata
        if (CollectionUtils.isEmpty(columns)) {
            ExtractionProductVO source = get(id, ExtractionProductFetchOptions.TABLES_AND_RECORDER);

            String tableName = source.findTableNameBySheetName(sheetName)
                    .orElseThrow(() -> new DataRetrievalFailureException(String.format("Product id=%s has no sheetName '%s'", id, sheetName)));

            // Get columns
            columns = tableDao.getColumns(tableName,
                    ExtractionTableColumnFetchOptions.builder()
                        .withRankOrder(false) // skip rankOrder, because fill later, by format and sheetName (more accuracy)
                        .build());

            // Fill rank order, if need
            if (fetchOptions.isWithRankOrder()) {
                ExtractionTableColumnOrder.fillRankOrderByFormatAndSheet(source.getRawFormatLabel(), sheetName, columns);
            }
        }

        return columns;
    }

}
