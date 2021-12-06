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

package net.sumaris.core.dao.technical.hibernate;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import java.sql.Date;

public enum AdditionalSQLFunctions {

    // left pad (need by StrategyRepository)
    lpad("lpad", StandardBasicTypes.STRING),

    // nvl (need by VesselRepository and Vessel entity)
    nvl_end_date("nvl", new SQLFunctionTemplate(StandardBasicTypes.DATE, "nvl(?1, date'2100-01-01')"));

    private SQLFunction function;

    AdditionalSQLFunctions(String sqlName, Type type) {
        this.function = new StandardSQLFunction(sqlName, type);
    }

    AdditionalSQLFunctions(String sqlName) {
        this.function = new StandardSQLFunction(sqlName);
    }

    AdditionalSQLFunctions(String sqlName, SQLFunction function) {
        this.function = function;
    }

    public SQLFunction asRegisterFunction() {
        return function;
    }

}
