package net.sumaris.rdf.cli.action;

/*-
 * #%L
 * Quadrige3 Core :: Quadrige3 Server Core
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 Ifremer
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

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.cli.action.ActionUtils;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.service.store.RdfDatasetService;
import net.sumaris.rdf.core.util.RdfFormat;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.system.Txn;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * <p>
 * RdfDatasetAction class.
 * </p>
 * 
 */
@Slf4j
public class RdfDatasetAction {

	public static final String LOAD_ALIAS = "--load";
	public static final String DUMP_ALIAS = "--dump";

	private RdfDatasetService service;
	private RdfConfiguration config;

	/**
	 * <p>
	 * Loading RDF dataset
	 * </p>
	 */
	public void load() {
		init();

		service.loadDataset();
	}

	public void dump() throws Throwable {
		init();

		// Get output format
		RdfFormat format = config.getRdfOutputFormat()
				// Dataset dump can be done only on TRIG or NQUADS
				.filter(f -> f == RdfFormat.TRIG || f == RdfFormat.NQUADS)
				.orElse(RdfFormat.TRIG);

		File outputFile = config.getCliOutputFile();
		Dataset ds = service.getDataset();

		// Dump to file
		if (outputFile != null) {
			outputFile = ActionUtils.checkAndGetOutputFile(false,
					this.getClass());

			try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
				Txn.executeRead(ds, () -> RDFDataMgr.write(fos, ds, format.toJenaLang()));
			}
		}

		// Dump to system out
		else {
			Txn.executeRead(ds, () -> RDFDataMgr.write(System.out, ds, format.toJenaLang()));
		}


	}

	/* -- protected functions -- */

	protected void init() {
		if (this.service == null) this.service = getDatasetService();
		if (this.config == null) this.config = getConfig();

		Preconditions.checkNotNull(service);
		Preconditions.checkNotNull(config);
	}

	protected RdfDatasetService getDatasetService() {
		return ServiceLocator.instance().getService("rdfDatasetService", RdfDatasetService.class);
	}

	protected RdfConfiguration getConfig() {
		return ServiceLocator.instance().getService("rdfConfiguration", RdfConfiguration.class);
	}
}
