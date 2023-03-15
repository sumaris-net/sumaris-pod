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
package net.sumaris.extraction.core.dao.technical.xml;

import com.google.common.base.Preconditions;
import fr.ifremer.common.xmlquery.*;
import lombok.NonNull;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ludovic.pecquot@e-is.pro
 */
public class XMLQuery {

    private DatabaseType dbms;
    private AbstractSingleXMLQuery delegate;

    public XMLQuery(@NonNull DatabaseType dbms) {
        super();
        this.dbms = dbms;
        switch (dbms) {
            case hsqldb:
                delegate = new HSQLDBSingleXMLQuery();
                delegate.bind("true", "1");
                delegate.bind("false", "0");
                break;
            case postgresql:
                delegate = new PgsqlSingleXMLQuery();
                delegate.bind("true", "True");
                delegate.bind("false", "False");
                break;
            case oracle:
                delegate =  new OracleSingleXMLQuery();
                delegate.bind("true", "1");
                delegate.bind("false", "0");
                break;
            default:
                throw new IllegalArgumentException("Not XMLQuery instance found for database type: " + dbms.name());
        }
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
                .map(Attribute::getValue)
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

    public Set<String> getAllColumnNames() {
        return getColumnNames(element -> true);
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
                .map(Attribute::getValue)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception e) {
            throw new SumarisTechnicalException(e);
        }
    }

    public String getAttributeValue(final Element element, String attrName, boolean forceLowerCase) {
        Attribute attr = element.getAttribute(attrName);
        if (attr == null) return null;
        String value = attr.getValue();
        if (forceLowerCase) return value.toLowerCase();
        return value;
    }

    public boolean hasGroup(final Element element, String groupName) {
        String attrValue = getAttributeValue(element, "group", false);
        if (StringUtils.isBlank(attrValue)) return false;
        return Arrays.asList(attrValue.split(",")).contains(groupName);
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

    public Document getDocument() {
        return delegate.getDocument();
    }


    public Element getFirstQueryTag() {
        return delegate.getFirstQueryTag();
    }

    /* -- delegated functions -- */

    public void manageRootElement() throws XMLQueryException {
        delegate.manageRootElement();
    }

    public String getSQLQueryAsString() {
        String query = delegate.getSQLQueryAsString();

        // Oracle replacements
        if (dbms == DatabaseType.oracle) {
            return query
                // Replace true/false => 1/0
                .replaceAll("(?i)=[/s]*true", "=1")
                .replaceAll("(?i)=[/s]*false", "=0")
                // null columns must have a type (avoid error ORA-01723)
                .replaceAll("(?i)null (\"[^\"]+\"[,\n\r]+)", "CAST('' AS VARCHAR2(1)) $1")
                ;
        }
        return query;
    }

    public Map<String, String> getSqlParameters() {
        return delegate.getSqlParameters();
    }

    public void setSqlParameters(Map<String, String> sqlParameters) {
        delegate.setSqlParameters(sqlParameters);
    }

    public String getSort() {
        return delegate.getSort();
    }

    public void setSort(String sort) {
        delegate.setSort(sort);
    }

    public String getSortDirection() {
        return delegate.getSortDirection();
    }

    public void setSortDirection(String sortDirection) {
        delegate.setSortDirection(sortDirection);
    }

    public Document getDocumentQuery() {
        return delegate.getDocumentQuery();
    }

    public void setQuery(String pXml) throws XMLQueryException {
        delegate.setQuery(pXml);
    }

    public void setQuery(URL pFileURL) throws XMLQueryException {
        delegate.setQuery(pFileURL);
    }

    public void setQuery(String pXml, boolean pManageRootElement) throws XMLQueryException {
        delegate.setQuery(pXml, pManageRootElement);
    }

    public void setQuery(File pQueryFile) throws XMLQueryException {
        delegate.setQuery(pQueryFile);
    }

    public void setQuery(File pQueryFile, boolean pManageRootElement) throws XMLQueryException {
        delegate.setQuery(pQueryFile, pManageRootElement);
    }

    public void setQuery(URL fileURL, boolean pManageRootElement) throws XMLQueryException {
        delegate.setQuery(fileURL, pManageRootElement);
    }

    public void addSelect(File pXmlFile) throws XMLQueryException {
        delegate.addSelect(pXmlFile);
    }

    public void addSelect(URL pXmlFileURL) throws XMLQueryException {
        delegate.addSelect(pXmlFileURL);
    }

    public void addSelect(String pXmlFilter) throws XMLQueryException {
        delegate.addSelect(pXmlFilter);
    }

    public void addSelect(String pQueryName, File pXmlFile) throws XMLQueryException {
        delegate.addSelect(pQueryName, pXmlFile);
    }

    public void addSelect(String pQueryName, URL pXmlFileURL) throws XMLQueryException {
        delegate.addSelect(pQueryName, pXmlFileURL);
    }

    public void addSelect(String pQueryName, String pXmlFilter) throws XMLQueryException {
        delegate.addSelect(pQueryName, pXmlFilter);
    }

    public void addFrom(File pXmlFile) throws XMLQueryException {
        delegate.addFrom(pXmlFile);
    }

    public void addFrom(URL pXmlFileURL) throws XMLQueryException {
        delegate.addFrom(pXmlFileURL);
    }

    public void addFrom(File pXmlFile, String pTag, String pAttributeName, String pAttributeValue) throws XMLQueryException {
        delegate.addFrom(pXmlFile, pTag, pAttributeName, pAttributeValue);
    }

    public void addFrom(URL pXmlFileURL, String pTag, String pAttributeName, String pAttributeValue) throws XMLQueryException {
        delegate.addFrom(pXmlFileURL, pTag, pAttributeName, pAttributeValue);
    }

    public void addFrom(String pXmlFilter) throws XMLQueryException {
        delegate.addFrom(pXmlFilter);
    }

    public void addWhere(File pFilterFile) throws XMLQueryException {
        delegate.addWhere(pFilterFile);
    }

    public void addWhere(URL pFilterFileURL) throws XMLQueryException {
        delegate.addWhere(pFilterFileURL);
    }

    public void addWhere(String pXmlFilter) throws XMLQueryException {
        delegate.addWhere(pXmlFilter);
    }

    public void addWhere(String pQueryName, File pFilterFile) throws XMLQueryException {
        delegate.addWhere(pQueryName, pFilterFile);
    }

    public void addWhere(String pQueryName, URL pFilterFileURL) throws XMLQueryException {
        delegate.addWhere(pQueryName, pFilterFileURL);
    }

    public void addWhere(String pQueryName, String pXmlFilter) throws XMLQueryException {
        delegate.addWhere(pQueryName, pXmlFilter);
    }

    public void addGroupBy(File pXmlFile) throws XMLQueryException {
        delegate.addGroupBy(pXmlFile);
    }

    public void addGroupBy(URL pXmlFileURL) throws XMLQueryException {
        delegate.addGroupBy(pXmlFileURL);
    }

    public void addGroupBy(String pXmlFilter) throws XMLQueryException {
        delegate.addGroupBy(pXmlFilter);
    }

    public void injectQuery(File pXmlFile) throws XMLQueryException {
        delegate.injectQuery(pXmlFile);
    }

    public void injectQuery(URL pXmlFileURL) throws XMLQueryException {
        delegate.injectQuery(pXmlFileURL);
    }

    public void injectQuery(String pXmlFilter) throws XMLQueryException {
        delegate.injectQuery(pXmlFilter);
    }

    public void injectQuery(File pXmlFile, String injectionPointName) throws XMLQueryException {
        delegate.injectQuery(pXmlFile, injectionPointName);
    }

    public void injectQuery(URL pXmlFileURL, String injectionPointName) throws XMLQueryException {
        delegate.injectQuery(pXmlFileURL, injectionPointName);
    }

    public void injectQuery(String pXmlFilter, String injectionPointName) throws XMLQueryException {
        delegate.injectQuery(pXmlFilter, injectionPointName);
    }

    public void injectQuery(File pXmlFile, String pattern, String replacement) throws XMLQueryException {
        delegate.injectQuery(pXmlFile, pattern, replacement);
    }

    public void injectQuery(URL pXmlFileURL, String pattern, String replacement) throws XMLQueryException {
        delegate.injectQuery(pXmlFileURL, pattern, replacement);
    }

    public void injectQuery(String pXmlFilter, String pattern, String replacement) throws XMLQueryException {
        delegate.injectQuery(pXmlFilter, pattern, replacement);
    }

    public void injectQuery(File pXmlFile, String pattern, String replacement, String injectionPointName) throws XMLQueryException {
        delegate.injectQuery(pXmlFile, pattern, replacement, injectionPointName);
    }

    public void injectQuery(URL pXmlFileURL, String pattern, String replacement, String injectionPointName) throws XMLQueryException {
        delegate.injectQuery(pXmlFileURL, pattern, replacement, injectionPointName);
    }

    public void injectQuery(String pXmlFilter, String pattern, String replacement, String injectionPointName) throws XMLQueryException {
        delegate.injectQuery(pXmlFilter, pattern, replacement, injectionPointName);
    }

    public void replaceAllBindings(String pattern, String replacement) {
        delegate.replaceAllBindings(pattern, replacement);
    }

    public void replaceAllBindings(Element element, String pattern, String replacement) {
        delegate.replaceAllBindings(element, pattern, replacement);
    }

    public int injectQuery(int pPosition, File pXmlFile) throws XMLQueryException {
        return delegate.injectQuery(pPosition, pXmlFile);
    }

    public int injectQuery(int pPosition, String pXmlFilter) throws XMLQueryException {
        return delegate.injectQuery(pPosition, pXmlFilter);
    }

    public String getXmlQueryAsString() throws XMLQueryException {
        return delegate.getXmlQueryAsString();
    }

    public List<Document> getXmlQueryAsDocuments() throws XMLQueryException {
        return delegate.getXmlQueryAsDocuments();
    }

    public void bind(String pName, String pValue) throws XMLQueryException {
        delegate.bind(pName, pValue);
    }

    public void validate() throws XMLQueryException {
        delegate.validate();
    }

    public void setGroup(String groupName, boolean active) {
        delegate.setGroup(groupName, active);
    }

    public void setDbms(String dbms) {
        delegate.setDbms(dbms);
    }

    public String getQueryName() throws XMLQueryException {
        return delegate.getQueryName();
    }

    public int getSelectCount() throws XMLQueryException {
        return delegate.getSelectCount();
    }

    public int getSelectCountFirstLevel() throws XMLQueryException {
        return delegate.getSelectCountFirstLevel();
    }

    public String getQName() {
        return delegate.getQName();
    }

    public void setQName(String name) {
        delegate.setQName(name);
    }

    public Element getFirstTag(String pTag, String pAttributeName, String pAttributeValue) {
        return delegate.getFirstTag(pTag, pAttributeName, pAttributeValue);
    }

    public Element getFirstTag(Element pElement, String pTag, String pAttributeName, String pAttributeValue) {
        return delegate.getFirstTag(pElement, pTag, pAttributeName, pAttributeValue);
    }

    public void setDocument(Document pDocumentQuery) throws XMLQueryException {
        delegate.setDocument(pDocumentQuery);
    }

    public void setXml(String pXml) throws XMLQueryException {
        delegate.setXml(pXml);
    }

    public void setXml(File pFile) throws XMLQueryException {
        delegate.setXml(pFile);
    }

    public void setXml(InputStream pInputStream) throws XMLQueryException {
        delegate.setXml(pInputStream);
    }

    public void setXml(URL pFileURL) throws XMLQueryException {
        delegate.setXml(pFileURL);
    }

    public String getXmlAsString(Format pFormat) throws XMLQueryException {
        return delegate.getXmlAsString(pFormat);
    }

    public String getXmlAsString() throws XMLQueryException {
        return delegate.getXmlAsString();
    }

    public String getXmlAsString(Element pElement, Format pFormat) throws XMLQueryException {
        return delegate.getXmlAsString(pElement, pFormat);
    }

    public String getXmlAsString(Element pElement) throws XMLQueryException {
        return delegate.getXmlAsString(pElement);
    }


}
