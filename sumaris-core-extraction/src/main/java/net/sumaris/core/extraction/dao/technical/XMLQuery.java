/*
 * Created on 29 juil. 2004
 */
package net.sumaris.core.extraction.dao.technical;

/*-
 * #%L
 * Quadrige3 Core :: Shared
 * %%
 * Copyright (C) 2017 - 2018 Ifremer
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
import fr.ifremer.common.xmlquery.HSQLDBSingleXMLQuery;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.xml.XPaths;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ludovic.pecquot@e-is.pro
 */
@Component("xmlQuery")
@Scope("prototype")
public class XMLQuery extends HSQLDBSingleXMLQuery {

    private String xslFileName;

    private boolean lowercase;

    public XMLQuery() {
        super();
        xslFileName = super.getXSLFileName();
        this.lowercase = false;
    }

    public XMLQuery(boolean lowercase) {
        super();
        xslFileName = super.getXSLFileName();
        this.lowercase = lowercase;
    }

    @Override
    protected String getXSLFileName() {
        return xslFileName;
    }

    protected void setXSLFileName(String xslFileName) {
        this.xslFileName = xslFileName;
    }

    // let default values here for HSQLDB

    public boolean isLowercase() {
        return lowercase;
    }

    public void setLowercase(boolean lowercase) {
        this.lowercase = lowercase;
    }

    /**
     * Get column names, with type="hidden"
     *
     * @return
     */
    public Set<String> getHiddenColumnNames() {

        try {
            List<Element> selectElements = XPaths.compile("//query/select[contains(@type, 'hidden')]", Filters.element())
                    .evaluate(getDocument());
            if (CollectionUtils.isEmpty(selectElements)) return null;

            return selectElements.stream()
                    .map(element -> element.getAttribute("alias"))
                    .filter(Objects::nonNull)
                    .map(attribute -> attribute.getValue())
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception e) {
            throw new SumarisTechnicalException(e);
        }
    }

    public Set<String> getVisibleColumnNames() {
        return getColumnNames(element -> {
            Attribute typeAttr = element.getAttribute("type");
            return typeAttr == null || !"hidden".equalsIgnoreCase(typeAttr.getValue());
        });
    }

    public Set<String> getNotNumericColumnNames() {
        return getColumnNames(element -> {
            Attribute typeAttr = element.getAttribute("type");
            return typeAttr == null || !"number".equalsIgnoreCase(typeAttr.getValue());
        });
    }

    public Set<String> getColumnNames(final Predicate<Element> filter) {
        Preconditions.checkNotNull(filter);

        try {
            List<Element> selectElements = XPaths.compile("//query/select", Filters.element())
                    .evaluate(getDocument());
            if (CollectionUtils.isEmpty(selectElements)) return null;

            return selectElements.stream()
                    // Apply filter
                    .filter(filter::evaluate)
                    // Get alias
                    .map(element -> element.getAttribute("alias"))
                    .filter(Objects::nonNull)
                    .map(attribute -> attribute.getValue())
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception e) {
            throw new SumarisTechnicalException(e);
        }
    }

    /**
     * Return if option="DISTINCT" has been set on the query
     *
     * @return
     */
    public boolean hasDistinctOption() {
        Attribute optionAtr = getFirstQueryTag().getAttribute("option");
        return optionAtr != null && "distinct".equalsIgnoreCase(optionAtr.getValue());
    }

    public String getSQLQueryAsString(){
        String query = super.getSQLQueryAsString();
        if (this.lowercase){
            query = query.toLowerCase();
        }
        return query;
    }

}
