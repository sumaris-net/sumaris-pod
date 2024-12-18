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

package net.sumaris.core.util.elasticsearch;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.util.StringUtils;
import org.junit.rules.ExternalResource;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.shaded.com.google.common.base.Preconditions;

/**
 * JUnit4 Rule that sets up and tears down a local Elasticsearch node.
 *
 * @author Benoit LAVENIER
 */
@Slf4j
public class ElasticsearchResource extends ExternalResource {

	public static String ELASTICSEARCH_VERSION = "7.17.7";

	private ElasticsearchContainer container;

	private final String version;

	private String uris;

	public ElasticsearchResource() {
		this(ELASTICSEARCH_VERSION);
	}

	public ElasticsearchResource(@NonNull String version) {
		this.version = version;
	}


	@Override
	protected void before() throws Throwable {

		log.debug("Starting Elasticsearch container {{}}...", this.version);
		container = ElasticsearchTestUtils.createContainer(this.version);
		container.start();
		String host = container.getHost();
		Integer port = container.getMappedPort(9200);
		this.uris = String.format("http://%s:%s", host, port);
		log.info("Elasticsearch container URI {{}}", this.uris);

		this.initConfiguration();

		Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
		container.followOutput(logConsumer, OutputFrame.OutputType.STDERR);

		Wait.forHttp("/")
			.forStatusCode(200);
	}

	@Override
	protected void after() {
		if (container != null) {
			log.debug("Stopping Elasticsearch container...");
			try {
				container.close();
			} catch (Throwable t) {
				log.error("Failed to stop Elasticsearch container {{}}", this.uris, t);
			}
		}
	}

	private void initConfiguration() {
		Preconditions.checkArgument(StringUtils.isNotBlank(uris), "Container not created");

		// Set elasticsearch node URI
		System.setProperty(SumarisConfigurationOption.ELASTICSEARCH_ENABLED.getKey(), Boolean.TRUE.toString());

		// Set elasticsearch node URI
		System.setProperty(SumarisConfigurationOption.ELASTICSEARCH_URIS.getKey(), uris);

		// Set elasticsearch password (v8+)
		//System.setProperty(SumarisConfigurationOption.ELASTICSEARCH_PASSWORD.getKey(), ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);

		// Enable elasticsearch features
		System.setProperty(SumarisConfigurationOption.ELASTICSEARCH_ENABLED.getKey(), Boolean.TRUE.toString());
	}
}