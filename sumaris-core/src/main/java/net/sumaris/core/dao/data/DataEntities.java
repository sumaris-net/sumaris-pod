package net.sumaris.core.dao.data;

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

import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.referential.ObjectTypeEnum;

public class DataEntities {

    // TODO Check this
//    public static final List<Class<? extends Serializable>> CLASSES = ImmutableList.of(
//            Trip.class,
//            ObservedLocation.class,
//            Landing.class
//    );
//
//    public static final Map<String, Class<? extends Serializable>> CLASSES_BY_NAME = Maps.uniqueIndex(
//            CLASSES,
//            Class::getSimpleName);
//
//    public static <T extends IDataEntity> Class<T> getEntityClass(String entityName) {
//        Preconditions.checkNotNull(entityName, "Missing 'entityName' argument");
//        Class<? extends Serializable> entityClass = CLASSES_BY_NAME.get(entityName);
//        Preconditions.checkNotNull(entityClass, "DataEntity [%s] not exists".formatted(entityName));
//        return  (Class<T>) entityClass;
//    }

    public static <T extends IDataEntity> ObjectTypeEnum getObjectType(String entityName) {
//        Class<T> entityClass = getEntityClass(entityName);
//        switch (entityClass.getName()) {
        // TODO : find a way to do not use String for class name
        switch (entityName) {
            case  "Trip" -> {
                return ObjectTypeEnum.FISHING_TRIP;
            }
            case "ObservedLocation" -> {
                return ObjectTypeEnum.OBSERVED_LOCATION;
            }
            case "Landing" -> {
                return ObjectTypeEnum.LANDING;
            }
            default -> {
                throw new IllegalArgumentException("DataEntity [%s] not exists".formatted(entityName));
            }
        }
    }

}
