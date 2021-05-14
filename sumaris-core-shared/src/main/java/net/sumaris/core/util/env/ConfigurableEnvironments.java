package net.sumaris.core.util.env;

import lombok.NonNull;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigurableEnvironments {

    protected ConfigurableEnvironments() {
        // Helper class
    }

    public static Properties readProperties(@NonNull ConfigurableEnvironment env) {
        List<MapPropertySource> sources = env.getPropertySources().stream()
                .filter(source -> source instanceof MapPropertySource)
                .map(source -> (MapPropertySource)source).collect(Collectors.toList());
        Properties target = null;
        for (MapPropertySource source: sources) {
            // Cascade properties (keep original order)
            target = new Properties(target);
            for (String key: source.getPropertyNames()) {
                Object value = source.getProperty(key);
                if (value != null) {
                    target.setProperty(key, value.toString());
                }
            }
        }

        return target;
    }
}
