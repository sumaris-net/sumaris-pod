package net.sumaris.core.dao.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Repository("referentialExternalDao")
public class ReferentialExternalDaoImpl implements ReferentialExternalDao {

    private static final Logger log = LoggerFactory.getLogger(ReferentialExternalDaoImpl.class);

    @Autowired
    private SumarisConfiguration config;

    @Autowired
    private ObjectMapper objectMapper;

    private boolean enableAnalyticReferences = false;
    private List<ReferentialVO> analyticReferences;
    private Date analyticReferencesUpdateDate = new Date(0L);


    @Autowired
    public ReferentialExternalDaoImpl(SumarisConfiguration config) {
        this.config = config;
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {

        boolean enableAnalyticReferences = event.getConfiguration().enableAnalyticReferencesService();

        if (this.enableAnalyticReferences != enableAnalyticReferences) {
            this.enableAnalyticReferences = enableAnalyticReferences;

            // Load references
            if (this.enableAnalyticReferences) {
                loadOrUpdateAnalyticReferences();
            }
        }
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.ANALYTIC_REFERENCES_BY_FILTER)
    public List<ReferentialVO> findAnalyticReferencesByFilter(ReferentialFilterVO filter,
                                            int offset,
                                            int size,
                                            String sortAttribute,
                                            SortDirection sortDirection) {

        if (!enableAnalyticReferences) {
            if (config.isProduction()) throw new UnsupportedOperationException("Analytic references not supported");
            // In DEV mode: return a fake UNK value
            return ImmutableList.of(ReferentialVO.builder()
                .id(-1)
                .label("UNK")
                .name("Unknown")
                .build());
        }

        // Make sure references have been load, or refreshed
        loadOrUpdateAnalyticReferences();

        // Prepare search pattern
        filter.setSearchText(StringUtils.trimToNull(filter.getSearchText()));


        return analyticReferences.stream()
                .filter(getAnalyticReferencesPredicate(filter))
                .sorted(Beans.naturalComparator(sortAttribute, sortDirection))
                .skip(offset)
                .limit((size < 0) ? analyticReferences.size() : size)
                .collect(Collectors.toList()
                );
    }

    // TODO NRannou: add cache (with a time to live) - or transcribing
    public void loadOrUpdateAnalyticReferences() {

        Date updateDate = new Date();
        int delta = DateUtil.getDifferenceInDays(analyticReferencesUpdateDate, updateDate);
        int delay = config.getAnalyticReferencesServiceDelay();
        String urlStr = config.getAnalyticReferencesServiceUrl();
        String authStr = config.getAnalyticReferencesServiceAuth();
        String filter = config.getAnalyticReferencesServiceFilter();

        // load analyticReferences if not loaded or too old
        if (StringUtils.isNotBlank(urlStr) && (delta > delay || analyticReferences == null)) {
            log.info(String.format("Loading analytic references from {%s}", urlStr));
            BufferedReader content = requestAnalyticReferenceService(urlStr, authStr);
            analyticReferences = parseAnalyticReferencesToVO(content, filter);
            analyticReferencesUpdateDate = updateDate;
            log.info(String.format("Analytic references loaded {%s}", analyticReferences.size()));
        }
    }

    private BufferedReader requestAnalyticReferenceService(String urlStr, String authStr) {
        Preconditions.checkNotNull(urlStr, "Missing 'urlStr' argument");
        Preconditions.checkNotNull(authStr, "Missing 'authStr' argument");
        BufferedReader content;

        try {
            URL url = new URL(urlStr);
            String encoding = Base64.getEncoder().encodeToString(authStr.getBytes("utf-8"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", "Basic " + encoding);
            InputStream input = connection.getInputStream();
            content = new BufferedReader(new InputStreamReader(input));
        } catch (Exception e) {
            throw new SumarisTechnicalException(String.format("Unable to get analytic references"), e);
        }

        return content;
    }

    private List<ReferentialVO> parseAnalyticReferencesToVO(BufferedReader content, String filter) {
        List<ReferentialVO> results = new ArrayList<>();
        Pattern filterPattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);

        try {
            JsonNode node = objectMapper.readTree(content);
            node.get("d").get("results").forEach(source -> {
                String code = source.get("Code").asText();
                if (filterPattern.matcher(code).matches()) {
                    ReferentialVO target = new ReferentialVO();
                    target.setId(code.hashCode());
                    target.setLabel(code);
                    target.setName(source.get("Description").asText());
                    target.setLevelId(source.get("Niveau").asInt());
                    int statusId = "O".equals(source.get("Imputable").asText())
                            ? StatusEnum.ENABLE.getId() : StatusEnum.DISABLE.getId();
                    target.setStatusId(statusId);
                    target.setEntityName("AnalyticReference");
                    results.add(target);
                }
            });
        } catch (Exception e) {
            throw new SumarisTechnicalException("Unable to parse analytic references: " + e.getMessage(), e);
        }

        return results;
    }

    private Predicate<ReferentialVO> getAnalyticReferencesPredicate(ReferentialFilterVO filter) {
        Preconditions.checkNotNull(filter, "Missing 'filter' argument");

        Pattern searchPattern = Daos.searchTextIgnoreCasePattern(filter.getSearchText(), false);
        Pattern searchAnyPattern = Daos.searchTextIgnoreCasePattern(filter.getSearchText(), true);

        return s -> (filter.getId() == null || filter.getId().equals(s.getId()))
                && (filter.getLabel() == null || filter.getLabel().equalsIgnoreCase(s.getLabel()))
                && (filter.getName() == null || filter.getName().equalsIgnoreCase(s.getName()))
                && (filter.getLevelId() == null || filter.getLevelId().equals(s.getLevelId()))
                && (filter.getLevelIds() == null || Arrays.asList(filter.getLevelIds()).contains(s.getLevelId()))
                && (filter.getLevelLabels() == null || Arrays.asList(filter.getLevelLabels()).contains(s.getLabel()))
                && (filter.getStatusIds() == null || Arrays.asList(filter.getStatusIds()).contains(s.getStatusId()))
                && (searchPattern == null || searchPattern.matcher(s.getLabel()).matches() || searchAnyPattern.matcher(s.getName()).matches());
    }

}
