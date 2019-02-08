package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.extraction.dao.cost.ExtractionCostDao;
import net.sumaris.core.extraction.dao.table.ExtractionTableDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.extraction.vo.cost.ExtractionCostContextVO;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Service("extractionService")
public class ExtractionServiceImpl implements ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionServiceImpl.class);

    @Autowired
    SumarisConfiguration configuration;

    @Autowired
    ExtractionCostDao extractionCostDao;

    @Autowired
    protected ExtractionTableDao extractionTableDao;

    @Override
    public void executeToFile(TripFilterVO filter, File outputFile) {
        ExtractionCostContextVO context;
        try {
            context = extractionCostDao.execute(filter);
        } catch (DataNotFoundException e) {
            // No data: skip
            return;
        }

        // dump to CSV file
//        try {
//        Map<String, String> fieldNamesByAlias = Maps.newHashMap();
//        Map<String, String> decimalFormats = Maps.newHashMap();
//        Map<String, String> dateFormats = Maps.newHashMap();
//            extractionDao.dumpQueryToCSV(new File("target", context.getId() + ".csv"),
//                    null, null, null, null, null
//            );
//        } catch (IOException e) {
//            LOG.error("Could not generate COST file", e);
//            throw new SumarisTechnicalException(e);
//        }

    }

    @Override
    public List<ExtractionTypeVO> getAllTypes() {
        return new ImmutableList.Builder()
                // Add tables
                .addAll(extractionTableDao.getAllTableNames()
                        .stream().map(tableName -> {
                            ExtractionTypeVO type = new ExtractionTypeVO();
                            type.setLabel(tableName);
                            type.setCategory(ExtractionTableDao.CATEGORY);
                            return type;
                        }).collect(Collectors.toList())
                )
                .build();
    }

    @Override
    public ExtractionResultVO getRows(ExtractionTypeVO type, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(type.getLabel());
        Preconditions.checkNotNull(type.getCategory());

        switch (type.getCategory()) {
            case "table":
                return getTableRows(type.getLabel(), filter, offset, size, sort, direction);
            default:
                throw new IllegalArgumentException("Unknown extraction category: " + type.getCategory());
        }
    }

    /* -- protected -- */

    protected ExtractionResultVO getTableRows(String tableName, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size <= 1000, "maximum value for 'size' is: 1000");
        Preconditions.checkArgument(size >= 0, "'size' must be greater or equals to 0");

        DatabaseTableEnum tableEnum = DatabaseTableEnum.valueOf(tableName.toUpperCase());

        return extractionTableDao.getTableRows(tableEnum, filter != null ? filter : new ExtractionFilterVO(), offset, size, sort, direction);
    }

}
