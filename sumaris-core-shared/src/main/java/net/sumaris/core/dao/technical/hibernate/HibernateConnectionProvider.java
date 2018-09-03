package net.sumaris.core.dao.technical.hibernate;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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


import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.spi.Configurable;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Stoppable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * <p>HibernateConnectionProvider class.</p>
 */
public class HibernateConnectionProvider implements ConnectionProvider,Configurable,Stoppable {

    private static final long serialVersionUID = 6463355546534159477L;
    
	private static DataSource dataSource = null;

    private static Connection connection = null;

    /** {@inheritDoc} */
    @Override
    public void configure(Properties props) throws HibernateException {
        if (dataSource == null && connection == null) {
            throw new HibernateException("DataSource must be set before using ConnectionProvider.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource != null ? dataSource.getConnection() : connection;
    }

    /** {@inheritDoc} */
    @Override
    public void closeConnection(Connection conn) throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop(){
        // Release datasource
        dataSource = null;
        connection = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsAggressiveRelease() {
        // TODO A quoi cela sert il ?
        return false;
    }

    /**
     * <p>Getter for the field <code>dataSource</code>.</p>
     *
     * @return a {@link DataSource} object.
     */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * <p>Setter for the field <code>dataSource</code>.</p>
     *
     * @param dataSource a {@link DataSource} object.
     */
    public static void setDataSource(DataSource dataSource) {
        HibernateConnectionProvider.dataSource = dataSource;
    }

    /**
     * <p>Setter for the field <code>dataSource</code>.</p>
     *
     * @param dataSource a {@link DataSource} object.
     */
    public static void setConnection(Connection connection) {
        HibernateConnectionProvider.connection = connection;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
	@Override
    public boolean isUnwrappableAs(Class unwrapType) {
		return ConnectionProvider.class.equals(unwrapType) ||
		HibernateConnectionProvider.class.isAssignableFrom(unwrapType);
    }

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> unwrapType) {
		if (ConnectionProvider.class.equals(unwrapType) ||
				HibernateConnectionProvider.class.isAssignableFrom(unwrapType)) {
			return (T) this;
		} else {
			throw new UnknownUnwrapTypeException(unwrapType);
		}
    }

}
