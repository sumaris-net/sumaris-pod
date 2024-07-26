package net.sumaris.core.dao.technical.hibernate;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.model.naming.Identifier;

public class IdentifierHelper {

    protected IdentifierHelper() {
        // helper class
    }

    /**
     * Convert change case into underscore (snakecase). E.g. "myColumnName" becomes "my_column_name"
     * @param identifier
     * @return
     */
    public static Identifier normalize(Identifier identifier) {
        if (identifier == null || StringUtils.isBlank(identifier.getText())) {
            return identifier;
        }

        // Replace case change by an underscore
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        String newName = identifier.getText().replaceAll(regex, replacement);

        // change to lower case
        newName = newName.toLowerCase();

        return new Identifier(newName, identifier.isQuoted());
    }
}
