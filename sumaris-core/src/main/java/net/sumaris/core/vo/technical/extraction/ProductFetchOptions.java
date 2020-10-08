package net.sumaris.core.vo.technical.extraction;

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

import lombok.Builder;
import lombok.Data;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;

@Data
@Builder
public class ProductFetchOptions implements IFetchOptions {

    public static ProductFetchOptions MINIMAL = builder()
            .withRecorderDepartment(false)
            .withRecorderPerson(false)
            .withTables(true)
            .withStratum(true)
            .withColumns(false)
            .withColumnValues(false)
            .build();
    public static ProductFetchOptions FOR_UPDATE = builder()
            .withRecorderDepartment(true)
            .withRecorderPerson(true)
            .withTables(true)
            .withStratum(true)
            .withColumns(false)
            .withColumnValues(false)
            .build();
    public static ProductFetchOptions MINIMAL_WITH_TABLES = builder()
            .withRecorderDepartment(false)
            .withRecorderPerson(false)
            .withTables(true)
            .withStratum(true)
            .withColumns(false)
            .withColumnValues(false)
            .build();
    public static ProductFetchOptions NO_COLUMNS = builder()
            .withColumns(false)
            .withColumnValues(false)
            .build();

    @Builder.Default()
    private boolean withRecorderDepartment = true;
    @Builder.Default()
    private boolean withRecorderPerson = true;
    @Builder.Default()
    private boolean withTables = true;
    @Builder.Default()
    private boolean withColumns = false;
    @Builder.Default()
    private boolean withColumnValues = false;
    @Builder.Default()
    private boolean withStratum = false;

}
