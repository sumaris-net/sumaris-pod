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

import com.google.common.base.Joiner;
import net.sumaris.core.dao.technical.extraction.ExtractionProductRepository;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import org.apache.commons.collections4.CollectionUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author benoit.lavenier@e-is.pro
 */
@Service("extractionProductService")
public class ExtractionProductServiceImpl implements ExtractionProductService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionProductServiceImpl.class);


    @Autowired
    private ExtractionProductRepository repository;

    @Autowired
    private ExtractionTableDao tableDao;

    @Autowired
    private ExtractionProductService self;

    @Override
    public List<ExtractionProductVO> findByFilter(ExtractionProductFilterVO filter, ExtractionProductFetchOptions fetchOptions) {
        return repository.findAll(filter, fetchOptions);
    }

    @Override
    public ExtractionProductVO get(int id, ExtractionProductFetchOptions fetchOptions) {
        return repository.get(id, fetchOptions);
    }

    @Override
    public ExtractionProductVO getByLabel(String label, ExtractionProductFetchOptions fetchOptions) {
        return repository.getByLabel(label, fetchOptions);
    }

    @Override
    public Optional<ExtractionProductVO> findById(int id, ExtractionProductFetchOptions fetchOptions) {
        return repository.findById(id, fetchOptions);
    }

    @Override
    public Optional<ExtractionProductVO> findByLabel(String label, ExtractionProductFetchOptions fetchOptions) {
        return repository.findByLabel(label, fetchOptions);
    }

    @Override
    public ExtractionProductVO save(ExtractionProductVO source) {
        // Save the product
        return repository.save(source);
    }

    @Override
    public void delete(int id) {
        repository.deleteById(id);
    }

    @Override
    public List<ExtractionTableColumnVO> getColumnsBySheetName(int id, String sheetName) {

        ExtractionProductVO source = self.get(id, ExtractionProductFetchOptions.MINIMAL);

        // Try to find columns from the DB
        List<ExtractionTableColumnVO> dataColumns = null;
        if (StringUtils.isNotBlank(sheetName)) {
            dataColumns = repository.getColumnsByIdAndTableLabel(id, sheetName);
        }

        // If nothing in the DB, find from table metadata
        if (CollectionUtils.isEmpty(dataColumns)) {
            String tableName = source.findTableNameBySheetName(sheetName)
                    .orElseThrow(() -> new DataRetrievalFailureException(String.format("Product id=%s has no sheetName '%s'", id, sheetName)));
            dataColumns = tableDao.getColumns(tableName);

            ExtractionTableColumnOrder.fillRankOrderByFormatAndSheet(source.getFormat(), sheetName, dataColumns);
        }

        return dataColumns;
    }

}
