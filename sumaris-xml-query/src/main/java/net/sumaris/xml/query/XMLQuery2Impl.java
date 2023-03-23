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

package net.sumaris.xml.query;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sumaris.xml.query.utils.ClasspathEntityResolver;
import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.transform.JDOMSource;
import org.jdom2.util.IteratorIterable;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

public class XMLQuery2Impl {
    private Document document;
    private Map<String, String> bindings;
    private Map<String, Boolean> groupBindings;

    public XMLQuery2Impl() {
        bindings = new HashMap<>();
        groupBindings = new HashMap<>();
    }

    public void setQuery(String xmlResourcePath) throws IOException, JDOMException {
        InputStream xmlInputStream = getInputStream(xmlResourcePath);
        document = readXmlInputSTream(xmlInputStream, true);
    }

    public Document getDocument() {
        return this.document;
    }

    public void setGroup(String groupName, boolean enabled) {
        groupBindings.put(groupName, enabled);
    }

    public void bind(String variable, String value) {
        bindings.put(variable, value);
    }

    public Set<String> extractGroups() {
        Set<String> groups = new HashSet<>();

        IteratorIterable<Element> groupElements = document.getDescendants(Filters.element());
        for (Element groupElement : groupElements) {
            String groupName = groupElement.getAttributeValue("group");
            if (groupName != null && !groupName.isEmpty()) {
                groups.add(groupName);
            }
        }

        return groups;
    }

    /**
     * Injects an XML query from a file at a specified injection point.
     *
     * @param queryFilePath    The path to the file containing the XML query to inject.
     * @param injectionPointName   The name of the injection point where to inject the query (optional).
     *                             If not provided, the query will be appended to the end of the main query.
     */
    public void injectQuery(String queryFilePath, String injectionPointName) throws JDOMException, IOException {
        // Load the query file from the classpath
        InputStream queryInputStream = getInputStream(queryFilePath);

        // Parse the query file into a JDOM Document
        Document injectionDocument = readXmlInputSTream(queryInputStream, false);

        // Extract the root element of the query file
        Element injectionRoot = injectionDocument.getRootElement();

        // Find the injection point if specified
        if (injectionPointName != null) {
            XPathFactory xpathFactory = XPathFactory.instance();
            XPathExpression<Element> xpath = xpathFactory.compile("//injection[@name='" + injectionPointName + "']", Filters.element());
            Element injectionPoint = xpath.evaluateFirst(document);

            if (injectionPoint == null) {
                throw new IllegalArgumentException("Injection point not found: " + injectionPointName);
            }

            // Replace the injection point with the contents of the query file
            XPathExpression<Element> injectionXpath = xpathFactory.compile("//query", Filters.element());
            Element injectionQuery = injectionXpath.evaluateFirst(injectionDocument);

            List<Content> injectionContent = ImmutableList.copyOf(injectionQuery.getContent());
            injectionContent.forEach(Content::detach);

            injectionPoint.getParentElement().addContent(
                injectionPoint.getParentElement().indexOf(injectionPoint),
                injectionContent);
            injectionPoint.detach();
        } else {
            // Append the contents of the query file to the end of the main query
            injectionRoot.detach();
            injectionRoot.removeNamespaceDeclaration(Namespace.XML_NAMESPACE);
            document.getRootElement().addContent(injectionRoot.getContent());
        }
    }

    public String toSql() throws Exception {
        applyGroupBindings();
        removeDisabledGroups();
        String transformedXml = applyXslTransformation();

        return replaceBindings(transformedXml);
    }

    private void applyGroupBindings() {
        IteratorIterable<Element> groupElements = document.getDescendants(Filters.element());

        for (Element groupElement : groupElements) {
            String groupName = groupElement.getAttributeValue("group");
            if (groupName != null && !groupName.isEmpty() && groupBindings.containsKey(groupName)) {
                groupElement.setAttribute("enabled", String.valueOf(groupBindings.get(groupName)));
            }
        }
    }

    private void removeDisabledGroups() {
        IteratorIterable<Element> groupElements = document.getDescendants(Filters.element("where"));

        List<Element> disabledGroups = Lists.newArrayList();
        for (Element groupElement : groupElements) {
            String enabled = groupElement.getAttributeValue("enabled");
            if (enabled != null && !Boolean.parseBoolean(enabled)) {
                disabledGroups.add(groupElement);
            }
        }
        disabledGroups.forEach(Element::detach);
    }

    private String applyXslTransformation() throws Exception {
        InputStream xslInputStream = getInputStream("classpath:xsl/queryHsqldb.xsl");
        StreamSource xslStreamSource = new StreamSource(xslInputStream);

        // Remove the DTD
        DocType docType = document.getDocType();
        if (docType != null) {
            docType.detach();
        }

        JDOMSource xmlSource = new JDOMSource(document);
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer(xslStreamSource);
        transformer.transform(xmlSource, result);

        return stringWriter.toString();
    }

    private String replaceBindings(String input) {
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            input = input.replace("&" + entry.getKey(), entry.getValue());
        }
        return input;
    }

    private InputStream getInputStream(String resourcePath) throws IOException {
        InputStream result;
        if (resourcePath.startsWith("classpath:")) {
            resourcePath = resourcePath.substring("classpath:".length());
            result = getClass().getClassLoader().getResourceAsStream(resourcePath);
        }
        else {
            result = getClass().getResourceAsStream(resourcePath);
        }
        if (result == null) {
            throw new FileNotFoundException("Cannot find reource at: " + resourcePath);
        }
        return result;
    }

    /**
     * Parse the query file into a JDOM Document
     * @param xmlInputStream
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    private Document readXmlInputSTream(InputStream xmlInputStream, boolean validation) throws IOException, JDOMException {
        SAXBuilder saxBuilder = new SAXBuilder();
        if (validation) {
            saxBuilder.setValidation(true);
            saxBuilder.setEntityResolver(new ClasspathEntityResolver());
        }
        return saxBuilder.build(xmlInputStream);
    }
}
