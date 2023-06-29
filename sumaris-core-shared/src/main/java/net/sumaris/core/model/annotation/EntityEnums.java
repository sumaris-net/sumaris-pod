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

package net.sumaris.core.model.annotation;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.nuiton.config.ConfigOptionDef;
import org.nuiton.i18n.I18n;
import org.reflections.Reflections;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class for enumerations
 */
@Slf4j
public final class EntityEnums {

    public static final int UNRESOLVED_ENUMERATION_ID = -1;

    public static final String MODEL_PACKAGE_NAME = "net.sumaris.core.model";
    public static final String DESCRIPTION_PROPERTY_PREFIX = "sumaris.config.option.enumeration.";

    private EntityEnums(){
        // Helper class
    }

    public static Set<Class<?>> getEntityEnumClasses(SumarisConfiguration config) {

        // Add annotations entities
        Reflections reflections = null;
        // Try to use saved reflexions file from classpath
        if (config != null && config.isProduction()) {
            reflections = Reflections.collect();
            if (reflections == null) {
                log.warn("Reflections.collect() in production mode returned null. Fallback to default scanner");
            }
        }
        // Or use reflexions scanner
        reflections = Optional.ofNullable(reflections).orElse(new Reflections(MODEL_PACKAGE_NAME));
        return reflections.getTypesAnnotatedWith(EntityEnum.class);
    }

    public static ConfigOptionDef[] getEntityEnumAsOptions(SumarisConfiguration config) {
        List<ConfigOptionDef> options = Lists.newArrayList();
        // Add options from model enumerations
        EntityEnums.getEntityEnumClasses(config).forEach(enumClass -> {
            // Get annotation detail
            final EntityEnum annotation = enumClass.getAnnotation(EntityEnum.class);
            final String entityClassName = annotation.entity().getSimpleName();
            final String[] configAttributes = annotation.configAttributes();
            final String[] resolveAttributes = annotation.resolveAttributes();

            // Compute a option key (e.g. 'sumaris.enumeration.MyEntity.MY_ENUM_VALUE.id')
            String configPrefixTemp = StringUtils.defaultIfBlank(annotation.configPrefix(), "");
            // Add trailing point
            if (configPrefixTemp.length() > 0  && configPrefixTemp.lastIndexOf(".") != configPrefixTemp.length() - 1) {
                configPrefixTemp += ".";
            }
            final String configPrefix = configPrefixTemp;

            final String[] attributes = ArrayUtils.isNotEmpty(configAttributes) ? configAttributes : resolveAttributes;

            for (Object enumValue : enumClass.getEnumConstants()) {
                for (String attribute : attributes) {
                    Object defaultJoinValue = Beans.getProperty(enumValue, attribute);
                    String key = configPrefix + StringUtils.doting(entityClassName, enumValue.toString(), attribute);
                    Class type = defaultJoinValue != null ? defaultJoinValue.getClass() : String.class;
                    String description = DESCRIPTION_PROPERTY_PREFIX + StringUtils.doting(entityClassName, enumValue.toString(), attribute, "description");
                    options.add(new ConfigOption(key, type, description, String.valueOf(defaultJoinValue), false, false));
                }
            }
        });

        return options.toArray(new ConfigOptionDef[options.size()]);
    }

    /**
     * Check if an entity enumeration (@EntityEnum) has been resolved. Typically, if id!=-1
     * @param enumerations
     */
    public static void checkResolved(String i18nMessageKey, @NonNull IEntityEnum... enumerations) {
        List<String> invalidEnumerationNames = Beans.getStream(enumerations)
            .filter(EntityEnums::isUnresolved)
            .map(EntityEnums::name)
            .toList();

        if (CollectionUtils.isNotEmpty(invalidEnumerationNames)) {
            throw new IllegalArgumentException(I18n.t(i18nMessageKey, Joiner.on(",").join(invalidEnumerationNames)));
        }
    }

    public static void checkResolved(@NonNull IEntityEnum... enumerations) {
        checkResolved("sumaris.error.enumeration.unresolved", enumerations);
    }

    public static String name(IEntityEnum enumeration) {
        return Beans.getProperty((Object)enumeration, "name").toString();
    }

    public static boolean isUnresolved(IEntityEnum enumeration) {
        try {
            Object id = Beans.getProperty((Object)enumeration, "id");
            return id == null || ((id instanceof Integer) && ((Integer)id) == UNRESOLVED_ENUMERATION_ID);
        } catch (Exception e) {
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    public static class ConfigOption implements ConfigOptionDef {
        private String key;
        private Class<?> type;
        private String description;
        private String defaultValue;
        private boolean isTransient = false;
        private boolean isFinal = false;
    }
}
