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
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import fr.ifremer.common.xmlquery.HSQLDBSingleXMLQuery;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.xml.XPaths;
import org.apache.commons.collections4.CollectionUtils;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ludovic.pecquot@e-is.pro
 */
@Component("xmlQuery")
@Scope("prototype")
public class XMLQuery extends HSQLDBSingleXMLQuery {

    // let default values here for HSQLDB

    /**
     * Get column names, with type="hidden"
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

        try {
            List<Element> selectElements = XPaths.compile("//query/select", Filters.element())
                    .evaluate(getDocument());
            if (CollectionUtils.isEmpty(selectElements)) return null;

            return selectElements.stream()
                    // Exclude hidden columns
                    .filter(element -> {
                        Attribute typeAttr = element.getAttribute("type");
                        return typeAttr == null || !"hidden".equalsIgnoreCase(typeAttr.getValue());
                    })
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
     * @return
     */
    public boolean hasDistinctOption() {
        Attribute optionAtr = getFirstQueryTag().getAttribute("option");
        return optionAtr != null && "distinct".equalsIgnoreCase(optionAtr.getValue());
    }
}
