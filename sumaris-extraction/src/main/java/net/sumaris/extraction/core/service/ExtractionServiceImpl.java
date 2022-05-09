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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableVO;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.dao.AggregationDao;
import net.sumaris.extraction.core.dao.ExtractionDao;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableDao;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionProductContextVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

/**
 * @author blavenie
 */
@Slf4j
@Service("extractionService")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class ExtractionServiceImpl implements ExtractionService {

    private final ExtractionConfiguration configuration;
    private final ApplicationContext applicationContext;

    private final LocationService locationService;
    private final ReferentialService referentialService;

    private final ExtractionTableDao extractionTableDao;

    private boolean enableTechnicalTablesUpdate = false;

    private Map<IExtractionType, ExtractionDao<? extends ExtractionContextVO, ? extends ExtractionFilterVO>>
        daosByType = Maps.newHashMap();

    public ExtractionServiceImpl(ExtractionConfiguration configuration,
                                 ApplicationContext applicationContext,
                                 ExtractionTableDao extractionTableDao,
                                 LocationService locationService,
                                 ReferentialService referentialService
    ) {
        this.configuration = configuration;
        this.applicationContext = applicationContext;
        this.extractionTableDao = extractionTableDao;
        this.locationService = locationService;
        this.referentialService = referentialService;
    }

    @PostConstruct
    protected void init() {

        // Register all extraction daos
        applicationContext.getBeansOfType(ExtractionDao.class).values()
                .forEach(dao -> {
                    IExtractionType type = dao.getFormat();
                    // Check if unique, by format
                    if (daosByType.containsKey(type)) {
                        throw new BeanInitializationException(
                            String.format("More than one ExtractionDao class found for the format %s v%s: [%s, %s]",
                                type.getLabel(),
                                type.getVersion(),
                                daosByType.get(dao.getFormat()).getClass().getSimpleName(),
                                dao.getClass().getSimpleName()));
                    }
                    // Register the dao
                    daosByType.put(type, dao);
                });

    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {

        // Update technical tables (if option changed)
        if (this.enableTechnicalTablesUpdate != configuration.enableTechnicalTablesUpdate()) {
            this.enableTechnicalTablesUpdate = configuration.enableTechnicalTablesUpdate();

            // Init rectangles
            if (this.enableTechnicalTablesUpdate) initRectangleLocations();
        }
    }

    public Set<IExtractionType> getTypes() {
        return daosByType.keySet();
    }

    @Override
    public ExtractionContextVO execute(@NonNull IExtractionType type, ExtractionFilterVO filter) {
        Preconditions.checkNotNull(type.getLabel());
        Preconditions.checkArgument(type.getCategory() == ExtractionCategoryEnum.LIVE);

        filter = filter != null ? filter : new ExtractionFilterVO();

        // Force full extraction (not a preview)
        filter.setPreview(false);

        return getDao(type).execute(filter);
    }

    @Override
    public ExtractionResultVO read(@NonNull IExtractionType source,
                                   ExtractionFilterVO filter,
                                   Page page) {

        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Get context
        ExtractionContextVO context;
        if (source instanceof ExtractionProductVO) {
            context = new ExtractionProductContextVO((ExtractionProductVO) source);
        }
        else if (source instanceof ExtractionContextVO){
            context = (ExtractionContextVO)source;
        }
        else {
            throw new SumarisTechnicalException("Unknown extraction type class: " + source.getClass().getSimpleName());
        }

        // Get table name
        String tableName;
        if (StringUtils.isNotBlank(filter.getSheetName())) {
            tableName = context.findTableNameBySheetName(filter.getSheetName())
                .orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.extraction.noData")));
        } else {
            tableName = Beans.getStream(context.getTableNames()).findFirst()
                .orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.extraction.noData")));
        }

        // Create a filter for rows previous, with only includes/exclude columns,
        // because criterion are not need (already applied when writing temp tables)
        ExtractionFilterVO readFilter = new ExtractionFilterVO();
        readFilter.setIncludeColumnNames(filter.getIncludeColumnNames()); // Copy given include columns
        readFilter.setExcludeColumnNames(SetUtils.union(
            SetUtils.emptyIfNull(filter.getIncludeColumnNames()),
            SetUtils.emptyIfNull(context.getHiddenColumns(tableName))
        ));

        // Force distinct if there is excluded columns AND distinct is enable on the XML query
        boolean enableDistinct = filter.isDistinct() || CollectionUtils.isNotEmpty(readFilter.getExcludeColumnNames())
            && context.isDistinctEnable(tableName);
        readFilter.setDistinct(enableDistinct);

        // Replace default sort attribute
        if (page != null && IEntity.Fields.ID.equalsIgnoreCase(page.getSortBy())) {
            page.setSortBy(null);
        }

        // Get rows from exported tables
        return extractionTableDao.read(tableName, readFilter, page);
    }


    @Override
    public void clean(ExtractionContextVO context) {
        log.info("Cleaning extraction #{}", context.getId());
        getDao(context).clean(context);
    }


    @Override
    public List<ExtractionTableVO> toProductTableVO(ExtractionContextVO source) {

        final List<String> tableNames = ImmutableList.copyOf(source.getTableNames());
        return tableNames.stream()
            .map(tableName -> {
                ExtractionTableVO table = new ExtractionTableVO();
                table.setTableName(tableName);

                // Keep rankOrder from original linked has map
                table.setRankOrder(tableNames.indexOf(tableName) + 1);

                // Label (=the sheet name)
                String sheetName = source.getSheetName(tableName);
                table.setLabel(sheetName);

                table.setName(ExtractionProducts.getSheetDisplayName(source.getFormat(), sheetName));
                table.setIsSpatial(false);

                return table;
            })
            .collect(Collectors.toList());
    }
    /* -- protected -- */


    /*protected File extractLiveAndDump(LiveExtractionTypeEnum format,
                                      ExtractionFilterVO filter) {
        Preconditions.checkNotNull(format);

        // Execute live extraction to temp tables
        ExtractionContextVO context = executeLiveDao(format, filter);
        Daos.commitIfHsqldbOrPgsql(dataSource);

        try {
            log.info(String.format("Dumping tables of extraction #%s to files...", context.getId()));

            // Dump tables
            return dumpTablesToFile(context, null *//*no filter, because already applied*//*);
        }
        finally {
            clean(context);
        }
    }*/



    protected boolean initRectangleLocations() {
        boolean updateAreas = false;
        try {
            // Insert missing rectangles
            long statisticalRectanglesCount = referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.RECTANGLE_ICES.getId())
                + referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.RECTANGLE_GFCM.getId());
            if (statisticalRectanglesCount == 0) {
                locationService.insertOrUpdateRectangleLocations();
                updateAreas = true;
            }

            // FIXME - We don't really need to store square 10x10, because extractions (and map) can compute it dynamically, using lat/long
            // Insert missing squares
            /*long square10minCount = referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.SQUARE_10.getId());
            if (square10minCount == 0) {
                locationService.insertOrUpdateSquares10();
                updateAreas = true;
            }*/

            if (updateAreas) {
                // Update area
                locationService.insertOrUpdateRectangleAndSquareAreas();

                // Update location hierarchy
                locationService.updateLocationHierarchy();
            }
            return true;

        } catch (Throwable t) {
            log.error("Error while initializing rectangle locations: " + t.getMessage(), t);
            return false;
        }

    }

    /**
     * Get self bean, to be able to use new transaction
     * @return
     */
    protected ExtractionService self() {
        return applicationContext.getBean("extractionService", ExtractionService.class);
    }

    protected <C extends ExtractionContextVO, F extends ExtractionFilterVO> ExtractionDao<C, F> getDao(IExtractionType type) {

        try {
            IExtractionType daoType = ExtractionTypes.findOneMatch(daosByType.keySet(), type);
            return (ExtractionDao<C, F>)daosByType.get(daoType);
        }
        catch (IllegalArgumentException e) {
            throw new SumarisTechnicalException(String.format("Unknown aggregation format (no dao): %s", type));
        }
    }
}
