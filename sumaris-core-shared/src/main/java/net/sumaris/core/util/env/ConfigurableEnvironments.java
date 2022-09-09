package net.sumaris.core.util.env;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2021 SUMARiS Consortium
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

import com.google.common.collect.Lists;
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
        boolean debug = log.isDebugEnabled();
        List<MapPropertySource> sources = env.getPropertySources().stream()
            .filter(source -> source instanceof MapPropertySource)
            .map(source -> (MapPropertySource)source).collect(Collectors.toList());
        final Properties target = new Properties(defaultOptions);

        if (debug) log.debug("-- Reading environment properties... ---\n");
        for (MapPropertySource source: Lists.reverse(sources)) {

            if (debug) log.debug("Processing source {} ...", source.getName());

            // Cascade properties (keep original order)
            for (String key: source.getPropertyNames()) {
                Object value = source.getProperty(key);
                if (value != null) {
                    if (debug) {
                        if (target.containsKey(key)) log.debug(" {}={} /!\\ Overriding previous value", key, value);
                        else log.debug(" {}={}", key, value);
                    }
                    target.setProperty(key, value.toString());
                }
            }
        }

        // DEBUG
        if (debug) {
            log.debug("-- Environment properties - final summary ---\n");
            target.keySet()
                .stream()
                .map(Object::toString)
                .sorted()
                .forEach(key -> {
                    Object value = target.getProperty(key);
                    log.debug(" {}={}", key, value);
                });
        }

        return target;
    }
}
