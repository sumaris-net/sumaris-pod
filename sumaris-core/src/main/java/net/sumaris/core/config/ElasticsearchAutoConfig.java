package net.sumaris.core.config;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2023 SUMARiS Consortium
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


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.converter.BigDecimalConverter;
import net.sumaris.core.util.converter.DateToLongConverter;
import net.sumaris.core.util.converter.IntegerToDateConverter;
import net.sumaris.core.util.converter.LongToDateConverter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestHighLevelClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
@Order(1)
@RequiredArgsConstructor
@EnableConfigurationProperties({ElasticsearchProperties.class})
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "net.sumaris.core.dao.technical.elasticsearch")
@Slf4j
public class ElasticsearchAutoConfig extends ElasticsearchConfiguration {

  private final ElasticsearchProperties properties;

  @Bean(name = {"elasticsearchOperations", "elasticsearchTemplate"})
  public ElasticsearchRestTemplate elasticsearchRestTemplate(RestHighLevelClient restHighLevelClient, ElasticsearchConverter converter) {
    return new ElasticsearchRestTemplate(restHighLevelClient, converter);
  }

  @Bean
  public RestHighLevelClient restHighLevelClient(RestClient restClient) {
    log.info("Starting Elastisearch client on {}", restClient.getNodes().stream().map(Node::getHost).toList());
    RestHighLevelClient client = new RestHighLevelClientBuilder(restClient)
        //.setApiCompatibilityMode(true) // Not need on standard ES cluster (e.g. Ifremer)
        .build();
    return client;
  }

  @Bean
  @Override
  public ClientConfiguration clientConfiguration() {
    ClientConfiguration.MaybeSecureClientConfigurationBuilder clientConfiguration;

    // Connect to configured endpoints
    String[] nodes = getNodes(properties);
    if (ArrayUtils.isNotEmpty(nodes)) {
      clientConfiguration =
          ClientConfiguration
              .builder()
              .connectedTo(nodes);

      // Enable basic auth
      if (StringUtils.isNotBlank(properties.getUsername())) {
        clientConfiguration.withBasicAuth(properties.getUsername(), properties.getPassword());
      }

      // Enable SSL
      boolean useSsl = useSsl(properties);
      if (useSsl) {
        log.info("Elastisearch client use SSL");
        clientConfiguration.usingSsl();
      }
    }

    // Default configuration (use localhost)
    else {
      clientConfiguration = ClientConfiguration
          .builder()
          .connectedToLocalhost();
    }

    // Timeout
    clientConfiguration
        .withConnectTimeout(properties.getConnectionTimeout())
        .withSocketTimeout(properties.getSocketTimeout());

    return clientConfiguration.build();
  }

  @Bean
  @Override
  public ElasticsearchCustomConversions elasticsearchCustomConversions() {
    return new ElasticsearchCustomConversions(
        Arrays.asList(
            new BigDecimalConverter(),
            new LongToDateConverter(),
            new DateToLongConverter(),
            new IntegerToDateConverter()
        ));
  }

  private String[] getNodes(ElasticsearchProperties properties) {
    return Beans.getStream(properties.getUris())
        .map(uriStr -> {
          try {
            URI uri = new URI(uriStr);
            String host = uri.getHost();
            int port = uri.getPort();
            return host + (port != -1 ? ":" + port : "");
          } catch (URISyntaxException e) {
            log.warn("Invalid URI in option {}", "spring.elasticsearch.uris");
            return null;
          }
        })
        .filter(StringUtils::isNotBlank)
        .toArray(String[]::new);
  }

  private boolean useSsl(ElasticsearchProperties properties) {
    return Beans.getStream(properties.getUris())
            .anyMatch(uriStr -> {
              try {
                URI uri = new URI(uriStr);
                int port = uri.getPort();
                return port == 443 || "https".equals(uri.getScheme());
              } catch (URISyntaxException e) {
                return false;
              }
            });
  }
}
