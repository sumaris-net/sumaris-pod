package net.sumaris.core.service.technical;

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.SoftwareDao;
import net.sumaris.core.dao.technical.SoftwareEntities;
import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.vo.technical.PodConfigurationVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component("podConfigService")
public class SoftwareServiceImpl implements SoftwareService {

    private static final Log log = LogFactory.getLog(SoftwareServiceImpl.class);


    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SoftwareDao dao;

    @Autowired
    private SoftwareEntities repository;

    private String defaultSoftwareLabel;

    private final List<String> bgADAP = Stream.of("boat-1.jpg", "boat-2.jpg", "boat-3.jpg", "boat-4.jpg", "ray-1.jpg").collect(Collectors.toList());
    private final List<String> bgSumaris = Stream.of("boat-1.jpg", "boat-2.jpg", "boat-3.jpg", "boat-4.jpg", "boat-5.jpg", "boat-6.jpg", "boat-7.jpg", "ray-1.jpg", "ray-2.jpg", "ray-3.jpg").collect(Collectors.toList());


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
        if (MapUtils.isNotEmpty(dbConfig.getProperties())) {
            dbConfig.getProperties()
                    .entrySet().forEach(entry -> {
                appConfig.setOption(entry.getKey(), entry.getValue());
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
