package net.sumaris.core.service.technical;

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SoftwareDao;
import net.sumaris.core.vo.technical.PodConfigurationVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.config.ApplicationConfigHelper;
import org.nuiton.config.ApplicationConfigProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component("podConfigService")
public class SoftwareServiceImpl implements SoftwareService {

    private static final Log log = LogFactory.getLog(SoftwareServiceImpl.class);

    @Autowired
    private SoftwareDao dao;

    private String defaultSoftwareLabel;


    public SoftwareServiceImpl(SumarisConfiguration configuration) {
        this.defaultSoftwareLabel = configuration.getAppName();
        Preconditions.checkNotNull(defaultSoftwareLabel);
    }

    @PostConstruct
    protected void afterPropertiesSet() {
        ApplicationConfig appConfig = SumarisConfiguration.getInstance().getApplicationConfig();

        // Override the configuration existing in the config file, using DB
        PodConfigurationVO dbConfig = getDefault();
        if (dbConfig == null) {
            log.warn("No software found in DB, with label=" + defaultSoftwareLabel);
        }
        else if (MapUtils.isNotEmpty(dbConfig.getProperties())) {
            log.info(String.format("Overriding configuration options, using those found in database for {%s}", defaultSoftwareLabel));

            // Load options from configuration providers
            Set<ApplicationConfigProvider> providers =
                    ApplicationConfigHelper.getProviders(null,
                            null,
                            null,
                            true);
            Set<String> optionKeys = providers.stream().flatMap(p -> Stream.of(p.getOptions()))
                    .map(o -> o.getKey()).collect(Collectors.toSet());
            Set<String> transientOptionKeys = providers.stream().flatMap(p -> Stream.of(p.getOptions()))
                    .filter(o -> o.isTransient())
                    .map(o -> o.getKey()).collect(Collectors.toSet());

            dbConfig.getProperties().entrySet()
                    .forEach(entry -> {
                        if (!optionKeys.contains(entry.getKey())) {
                            if (log.isDebugEnabled()) log.debug(String.format(" - Skipping unknown configuration option {%s=%s} found in database for {%s}.", entry.getKey(), entry.getValue(), defaultSoftwareLabel));
                        }
                        else if (transientOptionKeys.contains(entry.getKey())) {
                            if (log.isDebugEnabled()) log.debug(String.format(" - Skipping transient configuration option {%s=%s} found in database for {%s}.", entry.getKey(), entry.getValue(), defaultSoftwareLabel));
                        }
                        else {
                            if (log.isDebugEnabled()) log.debug(String.format(" - Applying option {%s=%s}", entry.getKey(), entry.getValue()));

                            appConfig.setOption(entry.getKey(), entry.getValue());
                        }
                    });
        }

    }

    @Override
    public PodConfigurationVO getDefault() {
        return dao.get(defaultSoftwareLabel);
    }

    @Override
    public PodConfigurationVO get(String label) {
        Preconditions.checkNotNull(label);

        return dao.get(label);
    }

    @Override
    public PodConfigurationVO save(PodConfigurationVO configuration) {
        Preconditions.checkNotNull(configuration);
        Preconditions.checkNotNull(configuration.getLabel());

        // TODO Save properties

        return dao.save(configuration);
    }

    /**
     * Auto detect IP
     *
     * @return the IP address or null
     */
    @Bean
    private Optional<String> whatsMyIp() {

        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            return Optional.of(in.readLine());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
