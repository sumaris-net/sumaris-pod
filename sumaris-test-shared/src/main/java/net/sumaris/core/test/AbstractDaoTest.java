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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.cache.Caches;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import javax.cache.CacheManager;
import javax.persistence.EntityManager;

/**
 * <p>Abstract AbstractDaoTest class.</p>
 */

@Slf4j
public abstract class AbstractDaoTest {

	private TransactionStatus status;
	
	private boolean commitOnTearDown = true;

	@Autowired
	protected SumarisConfiguration config;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Autowired
	protected EntityManager entityManager;

	@Autowired(required = false)
	protected CacheManager cacheManager;

	/**
	 * <p>setUp.</p>
	 *
	 * @throws Exception if any.
	 */
	@Before
	public void setUp() throws Exception {
		if (transactionManager != null) {
			status = transactionManager.getTransaction(null);
			if (log.isDebugEnabled()) {
				log.debug("Transaction initialized");
			}
		}
	}

	/**
	 * <p>tearDown.</p>
	 *
	 * @throws Exception if any.
	 */
	@After
	public void tearDown() throws Exception {
		if (transactionManager != null) {
			if (commitOnTearDown) {
				transactionManager.commit(status);
			} else {
				transactionManager.rollback(status);
			}
		}
		// Clear all cache, if any
		if (cacheManager != null) Caches.clearAll(cacheManager);
	}
	
	/**
	 * Allow to apply a commit on tear down (by default a rollback is applied)
	 *
	 * @param commitOnTearDown true to force to commit after unit test
	 */
	protected void setCommitOnTearDown(boolean commitOnTearDown) {
		this.commitOnTearDown = commitOnTearDown;
	}

	/**
	 * <p>commit.</p>
	 */
	protected void commit() {
		// Commit
		transactionManager.commit(status);
		// Then open a new transaction
		status = transactionManager.getTransaction(null);
	}

}
