package net.sumaris.xml.query.utils;

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

import com.google.common.collect.Maps;
import org.jdom2.filter.Filter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class XPaths {

    private static final ThreadLocal<XPathFactory> factoryMapper = ThreadLocal.withInitial(XPathFactory::instance);

    private static final ThreadLocal<Map<String, XPathExpression<?>>> expressionMapper = ThreadLocal.withInitial(Maps::newHashMap);

    public static XPathFactory getThreadXPathFactory() {
        return factoryMapper.get();
    }

    public static <T> XPathExpression<T> compile(String expression, Filter<T> filter) {
        Map<String, XPathExpression<?>> expressionMap = expressionMapper.get();
        XPathExpression<?> xpathExpression = expressionMap.get(expressionMap);
        if (xpathExpression == null) {
            xpathExpression = getThreadXPathFactory().compile(expression, filter);
            expressionMap.put(expression, xpathExpression);
        }
        return (XPathExpression<T>)xpathExpression;
    }

    public static <T> List<T> evaluate(String expression, Filter<T> filter, Object object) {
        return compile(expression, filter).evaluate(object);
    }

    public static <T> T evaluateFirst(String expression, Filter<T> filter, Object object) {
        return compile(expression, filter).evaluateFirst(object);
    }
}
