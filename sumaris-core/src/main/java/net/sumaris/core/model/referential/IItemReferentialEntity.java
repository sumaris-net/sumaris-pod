package net.sumaris.core.model.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

public interface IItemReferentialEntity extends IReferentialWithStatusEntity {

    interface Fields extends IReferentialWithStatusEntity.Fields {
        String LABEL = "label";
        String NAME = "name";
    }

    int LENGTH_LABEL = 50;
    int LENGTH_NAME = 100;
    int LENGTH_COMMENTS = 2000;

    String getLabel();

    void setLabel(String label);

    String getName();

    void setName(String name);

}