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
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.config.SumarisConfiguration;
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
    protected void onConfigurationReady(ConfigurationEvent event) {

        boolean enableAnalyticReferences = event.getConfiguration().enableAnalyticReferencesService();

        if (this.enableAnalyticReferences != enableAnalyticReferences) {
            this.enableAnalyticReferences = enableAnalyticReferences;

            // Load references
            if (this.enableAnalyticReferences) {
                loadAnalyticReferences();
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

        if (!enableAnalyticReferences) throw new UnsupportedOperationException("Analytic references not supported");

        loadAnalyticReferences();

        return analyticReferences.stream()
                .filter(getAnalyticReferencesPredicate(filter))
                .sorted(Beans.naturalComparator(sortAttribute, sortDirection))
                .skip(offset)
                .limit((size < 0) ? analyticReferences.size() : size)
                .collect(Collectors.toList()
                );
    }

    // TODO NRannou: add cache (with a time to live) - or transcribing
    public void loadAnalyticReferences() {

        Date updateDate = new Date();
        int delta = DateUtil.getDifferenceInDays(analyticReferencesUpdateDate, updateDate);
        int delay = config.getAnalyticReferencesServiceDelay();
        String urlStr = config.getAnalyticReferencesServiceUrl();
        String authStr = config.getAnalyticReferencesServiceAuth();

        // load analyticReferences if not loaded or too old
        if (StringUtils.isNotBlank(urlStr) && (delta > delay || analyticReferences == null)) {
            log.info(String.format("Loading analytic references from {%s}", urlStr));
            BufferedReader content = requestAnalyticReferenceService(urlStr, authStr);
            analyticReferences = parseAnalyticReferencesToVO(content);
            analyticReferencesUpdateDate = updateDate;
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

    private List<ReferentialVO> parseAnalyticReferencesToVO(BufferedReader content) {
        List<ReferentialVO> results = new ArrayList<>();

        try {
            JsonNode node = objectMapper.readTree(content);
            node.get("d").get("results").forEach(source -> {
                ReferentialVO target = new ReferentialVO();
                target.setId(source.get("Code").asText().hashCode());
                target.setLabel(source.get("Code").asText());
                target.setName(source.get("Description").asText());
                target.setLevelId(source.get("Niveau").asInt());
                int statusId = "O".equals(source.get("Imputable").asText())
                        ? StatusEnum.ENABLE.getId() : StatusEnum.DISABLE.getId();
                target.setStatusId(statusId);
                target.setEntityName("AnalyticReference");
                results.add(target);
            });
        } catch (Exception e) {
            throw new SumarisTechnicalException("Unable to parse analytic references: " + e.getMessage(), e);
        }

        return results;
    }

    private Predicate<ReferentialVO> getAnalyticReferencesPredicate(ReferentialFilterVO filter) {
        Preconditions.checkNotNull(filter, "Missing 'filter' argument");
        filter.setSearchText(StringUtils.trimToNull(filter.getSearchText()));

        return s -> (filter.getId() == null || filter.getId().equals(s.getId()))
                && (filter.getLabel() == null || filter.getLabel().equalsIgnoreCase(s.getLabel()))
                && (filter.getName() == null || filter.getName().equalsIgnoreCase(s.getName()))
                && (filter.getLevelId() == null || filter.getLevelId().equals(s.getLevelId()))
                && (filter.getLevelIds() == null || Arrays.asList(filter.getLevelIds()).contains(s.getLevelId()))
                && (filter.getStatusIds() == null || Arrays.asList(filter.getStatusIds()).contains(s.getStatusId()))
                && (filter.getSearchText() == null || like(s.getLabel(), filter.getSearchText(),false) || like(s.getName(), filter.getSearchText(),true));
    }

    private static boolean like(String text, String searchText, boolean searchAny) {
        if (StringUtils.isEmpty(text) || StringUtils.isEmpty(searchText)) return false;

        // add leading wildcard (if searchAny specified) and trailing wildcard
        searchText = ((searchAny ? "*" : "") + searchText + "*");

        // translate searchText in regexp
        StringBuilder sb = new StringBuilder();
        String[] searchArray = searchText.split("\\*", -1);
        for (int i = 0; i < searchArray.length; i++) {
            if (!StringUtils.isEmpty(searchArray[i])) {
                sb.append(Pattern.quote(searchArray[i]));
            }
            if (i < searchArray.length - 1) {
                sb.append(".*");
            }
        }
        Pattern p = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        return p.matcher(text).matches();
    }

}
