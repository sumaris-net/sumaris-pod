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

package net.sumaris.core.dao.technical.hibernate.types;

import java.io.Serializable;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class IntegerArrayUserType implements UserType {

    protected static final int SQL_TYPE = java.sql.Types.ARRAY;

    private Integer[] toIntegers(Object[] array){
        Integer[] a = new Integer[array.length];
        for(int i = 0 ; i < array.length ; i++)
            a[i] = (Integer)array[i];
        return a;
    }

    @Override
    public Object nullSafeGet(final ResultSet rs, final String[] names, final SharedSessionContractImplementor sharedSessionContractImplementor, final Object owner) throws HibernateException, SQLException {
        Array array = rs.getArray(names[0]);
        if (array == null) {
            return null;
        }
        return toIntegers((Object[])array.getArray());
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, final int index, SharedSessionContractImplementor sharedSessionContractImplementor) throws HibernateException, SQLException {
        Connection connection = st.getConnection();

        if (value == null) {
            st.setNull( index, sqlTypes()[0] );
        }
        else {
            Integer[] integers = toIntegers((Object[]) value);
            Array array = connection.createArrayOf("integer", integers);

            st.setArray(index, array);
        }
    }

    @Override
    public Object assemble(final Serializable cached, final Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object deepCopy(final Object o) throws HibernateException {
        return o == null ? null : ((Integer[]) o).clone();
    }

    @Override
    public Serializable disassemble(final Object o) throws HibernateException {
        return (Serializable) o;
    }

    @Override
    public boolean equals(final Object x, final Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(final Object o) throws HibernateException {
        return o == null ? 0 : o.hashCode();
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
        return original;
    }

    @Override
    public Class<Integer[]> returnedClass() {
        return Integer[].class;
    }

    @Override
    public int[] sqlTypes() {
        return new int[] {SQL_TYPE};
    }
}