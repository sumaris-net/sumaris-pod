package net.sumaris.core.service.technical;

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.dao.technical.SoftwareDao;
import net.sumaris.core.exception.VersionNotFoundException;
import net.sumaris.core.vo.technical.SoftwareVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.config.ApplicationConfigHelper;
import org.nuiton.config.ApplicationConfigProvider;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
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


@Component("softwareService")
public class SoftwareServiceImpl implements SoftwareService {

    private static final Log log = LogFactory.getLog(SoftwareServiceImpl.class);

    @Autowired
    private SoftwareDao dao;

    @Autowired
    private DatabaseSchemaDao databaseSchemaDao;

    private String defaultSoftwareLabel;


    public SoftwareServiceImpl(SumarisConfiguration configuration) {
        this.defaultSoftwareLabel = configuration.getAppName();
        Preconditions.checkNotNull(defaultSoftwareLabel);
    }

    @PostConstruct
    protected void afterPropertiesSet() {

        loadConfigurationFromDatabase();
    }

    @Override
    public SoftwareVO getDefault() {
        return dao.get(defaultSoftwareLabel);
    }

    @Override
    public SoftwareVO get(String label) {
        Preconditions.checkNotNull(label);

        return dao.get(label);
    }

    @Override
    public SoftwareVO save(SoftwareVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());

        return dao.save(source);
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

    protected void loadConfigurationFromDatabase() {

        try {
            Version dbVersion = databaseSchemaDao.getSchemaVersion();
            Version minVersion = VersionBuilder.create("0.9.5").build();

            // Test if software table exists, if not, skip
            if (dbVersion == null || minVersion.after(dbVersion)) {
                log.warn(String.format("Skipping configuration override from database (expected min schema version {%s})", minVersion.toString()));
                return; // skip
            }
        } catch(VersionNotFoundException e) {
            // ok, continue (schema should be a new one ?)
        }

        ApplicationConfig appConfig = SumarisConfiguration.getInstance().getApplicationConfig();
        // Override the configuration existing in the config file, using DB
        SoftwareVO software = getDefault();
        if (software == null) {
            log.info(String.format("No configuration for {%s} found in database. to enable configuration override from database, make sure to set the option '%s' to an existing row of the table SOFTWARE (column LABEL).", defaultSoftwareLabel, SumarisConfigurationOption.APP_NAME.getKey()));
        }
        else if (MapUtils.isNotEmpty(software.getProperties())) {
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

            software.getProperties().entrySet()
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

}
