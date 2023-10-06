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

package net.sumaris.core.dao.technical.hibernate.spatial.dialect;

import net.sumaris.core.dao.technical.hibernate.AdditionalSQLFunctions;
import net.sumaris.core.dao.technical.hibernate.types.IntegerArrayUserType;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

import java.sql.Types;

public class PostgisPG10Dialect extends org.hibernate.spatial.dialect.postgis.PostgisPG10Dialect {

    public PostgisPG10Dialect() {
        super();

        // Register new array type
        registerHibernateType(Types.ARRAY, IntegerArrayUserType.class.getName());

        // Register additional functions
        for (AdditionalSQLFunctions function: AdditionalSQLFunctions.values()) {
            if (function != AdditionalSQLFunctions.nvl_end_date) {
                // Register 'nvl' to use 'coalesce' function
                registerFunction(function.name(), new SQLFunctionTemplate(StandardBasicTypes.DATE, "coalesce(?1, date'2100-01-01')"));
            }
            else {
                registerFunction(function.name(), function.asRegisterFunction());
            }
        }
    }
}
