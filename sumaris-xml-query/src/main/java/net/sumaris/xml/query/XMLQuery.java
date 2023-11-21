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

/*
 * Created on 29 juil. 2004
 */
package net.sumaris.xml.query;

import fr.ifremer.common.xmlquery.XMLQueryException;
import org.apache.commons.collections4.Predicate;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author ludovic.pecquot@e-is.pro
 */
public interface XMLQuery {

    void setQuery(String resourcePath);

    void setQuery(InputStream is) throws IOException;

    String getSQLQueryAsString();
    Set<String> getHiddenColumnNames();

    Set<String> getVisibleColumnNames();

    Set<String> getNotNumericColumnNames();

    Set<String> getAllColumnNames();

    Set<String> getColumnNames(Predicate<Element> filter);

    boolean hasColumnName(String columnName);

    Stream<Element> streamSelectElements(Predicate<Element> filter);

    String getAlias(Element element);

    String getAlias(Element element, boolean forceLowerCase);

    String getTextContent(Element element, String separator);

    String getAttributeValue(Element element, String attrName, boolean forceLowerCase);

    boolean hasGroup(Element element, String groupName);

    void removeGroup(Element element, String groupName);

    Set<String> extractGroups();

    boolean hasDistinctOption();

    Document getDocument();

    Element getFirstQueryTag();

    List<Element> getGroupByTags(Predicate<Element> filter);

    void injectQuery(File pXmlFile) throws XMLQueryException;

    void injectQuery(URL pXmlFileURL) throws XMLQueryException;

    void injectQuery(String pXmlFilter) throws XMLQueryException;

    void injectQuery(File pXmlFile, String injectionPointName) throws XMLQueryException;

    void injectQuery(URL pXmlFileURL, String injectionPointName) throws XMLQueryException;

    void injectQuery(String pXmlFilter, String injectionPointName) throws XMLQueryException;

    void injectQuery(File pXmlFile, String pattern, String replacement) throws XMLQueryException;

    void injectQuery(URL pXmlFileURL, String pattern, String replacement) throws XMLQueryException;

    void injectQuery(String pXmlFilter, String pattern, String replacement) throws XMLQueryException;

    void injectQuery(File pXmlFile, String pattern, String replacement, String injectionPointName) throws XMLQueryException;

    void injectQuery(URL pXmlFileURL, String pattern, String replacement, String injectionPointName) throws XMLQueryException;

    void injectQuery(String pXmlFilter, String pattern, String replacement, String injectionPointName) throws XMLQueryException;

    void bind(String pName, String pValue) throws XMLQueryException;

    void bind(String pName, Integer pValue) throws XMLQueryException;

    void setGroup(String groupName, boolean active);

    void setDbms(String dbms);

    String getQueryName() throws XMLQueryException;

    int getSelectCount() throws XMLQueryException;

    int getSelectCountFirstLevel() throws XMLQueryException;

    String getGroupByParamName();

    boolean isDisabled(Element e);

    Map<String, String> getSqlParameters();

    void bindGroupBy(String groupByParamName);

}
