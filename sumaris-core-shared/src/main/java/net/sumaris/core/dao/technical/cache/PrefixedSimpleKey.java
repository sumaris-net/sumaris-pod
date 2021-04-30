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

package net.sumaris.core.dao.technical.cache;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * <p>PrefixedSimpleKey class.</p>
 */
public class PrefixedSimpleKey implements Serializable {

    private final String prefix;
    private final Object[] params;
    private final String methodName;
    private int hashCode;

    /**
     * <p>Constructor for PrefixedSimpleKey.</p>
     *
     * @param prefix a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @param elements a {@link java.lang.Object} object.
     */
    public PrefixedSimpleKey(String prefix, String methodName, Object... elements) {
        Assert.notNull(prefix, "Prefix must not be null");
        Assert.notNull(elements, "Elements must not be null");
        this.prefix = prefix;
        this.methodName = methodName;
        this.params = new Object[elements.length];
        System.arraycopy(elements, 0, this.params, 0, elements.length);
        this.hashCode = prefix.hashCode();
        this.hashCode = 31 * this.hashCode + methodName.hashCode();
        this.hashCode = 31 * this.hashCode + Arrays.deepHashCode(this.params);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        return (this == other ||
            (other instanceof PrefixedSimpleKey && this.prefix.equals(((PrefixedSimpleKey) other).prefix) &&
                this.methodName.equals(((PrefixedSimpleKey) other).methodName) &&
                Arrays.deepEquals(this.params, ((PrefixedSimpleKey) other).params)));
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        return this.hashCode;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.prefix + " " + getClass().getSimpleName() + this.methodName + " [" + StringUtils.arrayToCommaDelimitedString(this.params) + "]";
    }
}