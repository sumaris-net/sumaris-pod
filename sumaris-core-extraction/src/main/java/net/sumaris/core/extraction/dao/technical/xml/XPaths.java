package net.sumaris.core.extraction.dao.technical.xml;

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
