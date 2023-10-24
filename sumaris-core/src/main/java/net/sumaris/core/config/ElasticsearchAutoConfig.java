package net.sumaris.core.config;


import lombok.NonNull;
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
        //.setApiCompatibilityMode(true)
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

      if (StringUtils.isNotBlank(properties.getUsername())) {
        clientConfiguration.withBasicAuth(properties.getUsername(), properties.getPassword());
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
}
