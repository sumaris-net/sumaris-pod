package net.sumaris.core.util.env;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class ConfigurableEnvironments {


    protected ConfigurableEnvironments() {
        // Helper class
    }

    public static Properties readProperties(@NonNull ConfigurableEnvironment env, Properties defaultOptions) {
        List<MapPropertySource> sources = env.getPropertySources().stream()
                .filter(source -> source instanceof MapPropertySource)
                .map(source -> (MapPropertySource)source).collect(Collectors.toList());
        Properties target = defaultOptions;
        target = new Properties(target);
        for (MapPropertySource source: sources) {
            // Cascade properties (keep original order)
            for (String key: source.getPropertyNames()) {
                Object value = source.getProperty(key);
                if (value != null) {
                    //log.info(" {}={}", key, value.toString());
                    target.setProperty(key, value.toString());
                }
            }
        }

        return target;
    }
}
