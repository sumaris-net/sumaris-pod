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

package net.sumaris.core.dao.technical.model.annotation;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import org.nuiton.config.ConfigOptionDef;
import org.reflections.Reflections;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for enumerations
 */
public final class EntityEnums {

    private static final String MODEL_PACKAGE_NAME = "net.sumaris.core.model";

    private EntityEnums(){
        // Helper class
    }

    public static Set<Class<?>> getEntityEnumClasses(SumarisConfiguration config) {

        // Add annotations entities
        Reflections reflections = (config != null && config.isProduction() ? Reflections.collect() : new Reflections(MODEL_PACKAGE_NAME));
        return reflections.getTypesAnnotatedWith(EntityEnum.class);
    }

    public static ConfigOptionDef[] getEntityEnumAsOptions(SumarisConfiguration config) {
        List<ConfigOptionDef> options = Lists.newArrayList();
        // Add options from model enumerations
        EntityEnums.getEntityEnumClasses(config).forEach(enumClass -> {
            // Get annotation detail
            final EntityEnum annotation = enumClass.getAnnotation(EntityEnum.class);
            final String entityClassName = annotation.entity().getSimpleName();
            final String[] joinAttributes = annotation.joinAttributes();

            // Compute a option key (e.g. 'sumaris.enumeration.MyEntity.MY_ENUM_VALUE.id')
            String configPrefixTemp = StringUtils.defaultIfBlank(annotation.configPrefix(), "");
            // Add trailing point
            if (configPrefixTemp.length() > 0  && configPrefixTemp.lastIndexOf(".") != configPrefixTemp.length() - 1) {
                configPrefixTemp += ".";
            }
            final String configPrefix = configPrefixTemp;

            String descriptionPrefix = "sumaris.config.option.enumeration.";

            Stream.of(enumClass.getEnumConstants()).forEach(enumValue -> Stream.of(joinAttributes)
                .forEach(joinAttribute -> {
                    Object defaultJoinValue = Beans.getProperty(enumValue, joinAttribute);
                    String key = configPrefix + StringUtils.doting(entityClassName, enumValue.toString(), joinAttribute);
                    Class type = defaultJoinValue != null ? defaultJoinValue.getClass() : String.class;
                    String description = descriptionPrefix + StringUtils.doting(entityClassName, enumValue.toString(), joinAttribute, "description");
                    options.add(new ConfigOption(key, type, description, String.valueOf(defaultJoinValue), false, false));
                }));
        });

        return options.toArray(new ConfigOptionDef[options.size()]);
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