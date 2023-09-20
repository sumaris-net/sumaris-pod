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

package net.sumaris.xml.query.utils;

import org.apache.commons.collections4.Predicate;
import org.jdom2.Element;

public class ElementFilter extends org.jdom2.filter.ElementFilter {

    private Predicate<Element> predicate;

    public ElementFilter() {
        super();
    }

    public ElementFilter(Predicate<Element> predicate) {
        super();
        this.predicate = predicate;
    }

    @Override
    public Element filter(Object content) {
        Element el = super.filter(content);
        if (el != null && predicate != null) {
            return predicate.evaluate(el) ? el : null;
        }
        return el;
    }
}
