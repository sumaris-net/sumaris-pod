package net.sumaris.core.test;

/*-
 * #%L
 * SUMARiS :: Test shared
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


import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.service.ServiceLocator;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.Ignore;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.File;

/**
 * Useful method around tests.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 3.3.1
 */
@Ignore
@Slf4j
public final class Tests {

	/**
	 * <p>Constructor for Tests.</p>
	 */
	protected Tests() {
	}

	/**
	 * <p>checkDbExists.</p>
	 *
	 * @param testClass a {@link Class} object.
	 * @param dbDirectory a {@link String} object.
	 */
	public static void checkDbExists(Class<?> testClass, String dbDirectory) {
		File db = new File(dbDirectory);
		if (!db.exists()) {

			if (log.isWarnEnabled()) {
				log.warn("Could not find db at " + db + ", test [" +
							testClass + "] is skipped.");
			}
			Assume.assumeTrue(false);
		}
	}

	/**
	 * <p>getBean.</p>
	 *
	 * @param name a {@link String} object.
	 * @param serviceType a {@link Class} object.
	 * @param <S> a S object.
	 * @return a S object.
	 */
	public static <S> S getBean(String name, Class<S> serviceType) {
        S result = ServiceLocator.instance().getService(name, serviceType);
        Assume.assumeNotNull(result);
        return result;
    }

	/**
	 * <p>createStatelessQuery.</p>
	 *
	 * @param hqlQuery a {@link String} object.
	 * @return a {@link Query} object.
	 */
	public static Query createStatelessQuery(String hqlQuery) {
		EntityManager entityManager = getBean("entityManager", EntityManager.class);
		return entityManager.createQuery(hqlQuery);
    }

	/**
	 * <p>countEntities.</p>
	 *
	 * @param entityClass a {@link Class} object.
	 * @return a long.
	 */
	public static long countEntities(Class<?> entityClass) {
        return countEntities(entityClass, null, "");
    }

	/**
	 * <p>countEntities.</p>
	 *
	 * @param entityClass a {@link Class} object.
	 * @param alias a {@link String} object.
	 * @param whereClause a {@link String} object.
	 * @return a long.
	 */
	public static long countEntities(Class<?> entityClass, String alias, String whereClause) {
        if (StringUtils.isNotBlank(whereClause)) {
            whereClause = " " + alias + " where " + whereClause;
        }
        Query query = createStatelessQuery("select count(*) from " + entityClass.getName() + whereClause);
        return (Long)query.getSingleResult();
    }
}
