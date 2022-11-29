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

package net.sumaris.extraction.core.dao;

import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.IExtractionTypeWithTablesVO;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionProductContextVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;

/**
 * @author blavenie
 */
@Slf4j
@Component("extractionDaoDispatcher")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class ExtractionDaoDispatcherImpl implements ExtractionDaoDispatcher {

    private final ApplicationContext applicationContext;

    private Map<IExtractionType, ExtractionDao<? extends ExtractionContextVO, ? extends ExtractionFilterVO>>
        daoByType = Maps.newHashMap();

    public ExtractionDaoDispatcherImpl(ApplicationContext applicationContext
    ) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    protected void init() {

        // Register daos
        applicationContext.getBeansOfType(ExtractionDao.class).values()
            .forEach(dao -> {
                    Set<IExtractionType> types = dao.getManagedTypes();
                    types.forEach(type -> {
                        // Check if unique, by format
                        if (daoByType.containsKey(type)) {
                            throw new BeanInitializationException(
                                String.format("More than one ExtractionDao class found for the format %s v%s: [%s, %s]",
                                    type.getLabel(),
                                    type.getVersion(),
                                    daoByType.get(type).getClass().getSimpleName(),
                                    dao.getClass().getSimpleName()));
                        }
                        // Register the dao
                        daoByType.put(type, dao);
                    });
                }
            );
    }

    public Set<IExtractionType> getManagedTypes() {
        return daoByType.keySet();
    }

    @Override
    public ExtractionContextVO execute(@NonNull IExtractionType type, ExtractionFilterVO filter) {

        filter = ExtractionFilterVO.nullToEmpty(filter);

        return getDao(type).execute(filter);
    }

    @Override
    public ExtractionResultVO read(@NonNull IExtractionType source,
                                   ExtractionFilterVO filter,
                                   Page page) {
        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Get a context
        ExtractionContextVO context;
        if (source instanceof ExtractionContextVO){
            context = (ExtractionContextVO)source;
        }
        else if (source instanceof ExtractionProductVO) {
            context = new ExtractionProductContextVO((ExtractionProductVO) source);
        }
        else {
            throw new SumarisTechnicalException("Unknown extraction type class: " + source.getClass().getSimpleName());
        }

        // Read the context
        return getDao(source).read(context, filter, page);
    }

    @Override
    public void clean(ExtractionContextVO context) {
        log.info("Cleaning extraction #{}", context.getId());
        getDao(context).clean(context);
    }


    /* -- protected -- */


    protected <C extends ExtractionContextVO, F extends ExtractionFilterVO> ExtractionDao<C, F> getDao(IExtractionType type) {

        try {
            IExtractionType daoType = ExtractionTypes.findOneMatch(daoByType.keySet(), type);
            return (ExtractionDao<C, F>) daoByType.get(daoType);
        }
        catch (IllegalArgumentException e) {
            throw new SumarisTechnicalException(String.format("Unknown aggregation format (no dao): %s", type));
        }
    }
}
