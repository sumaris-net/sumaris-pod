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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

/**
 * <p>Abstract AbstractDaoTest class.</p>
 */

public abstract class AbstractDaoTest {

	/** Logger. */
	private static final Log log = LogFactory.getLog(AbstractDaoTest.class);

	private TransactionStatus status;
	
	private boolean commitOnTearDown = true;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private EntityManager entityManager;

	/**
	 * <p>setUp.</p>
	 *
	 * @throws Exception if any.
	 */
	@Before
	public void setUp() throws Exception {
		status = transactionManager.getTransaction(null);
		if (log.isDebugEnabled()) {
			log.debug("Transaction initialized");
		}
	}

	/**
	 * <p>tearDown.</p>
	 *
	 * @throws Exception if any.
	 */
	@After
	public void tearDown() throws Exception {
		if (commitOnTearDown) {
			transactionManager.commit(status);
		}
		else {
			transactionManager.rollback(status);
		}
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

	/**
	 * <p>getEntityManager.</p>
	 *
	 * @return a {@link Session} object.
	 */
	protected EntityManager getEntityManager() {
		return entityManager;
	}
}
