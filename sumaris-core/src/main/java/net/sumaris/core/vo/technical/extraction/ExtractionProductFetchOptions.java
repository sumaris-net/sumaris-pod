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
public class ExtractionProductFetchOptions implements IFetchOptions {

    public static ExtractionProductFetchOptions TABLES = builder()
            .withRecorderDepartment(false)
            .withRecorderPerson(false)
            .withTables(true)
            .withStratum(true)
            .withColumns(false)
            .withColumnValues(false)
            .withDocumentation(false)
            .build();
    public static ExtractionProductFetchOptions TABLES_AND_RECORDER = builder()
            .build();
    public static ExtractionProductFetchOptions TABLES_AND_STRATUM = builder()
            .withRecorderDepartment(false)
            .withRecorderPerson(false)
            .withTables(true)
            .withStratum(true)
            .withColumns(false)
            .withColumnValues(false)
            .withDocumentation(false)
            .build();
    public static ExtractionProductFetchOptions TABLES_AND_COLUMNS = builder()
            .withRecorderDepartment(false)
            .withRecorderPerson(false)
            .withTables(true)
            .withStratum(false)
            .withColumns(true)
            .withColumnValues(false)
            .withDocumentation(false)
            .build();
    public static ExtractionProductFetchOptions FOR_UPDATE = builder()
            .withRecorderDepartment(true)
            .withRecorderPerson(true)
            .withTables(true)
            .withStratum(true)
            .withColumns(false)
            .withColumnValues(false)
            .withDocumentation(false)
            .build();
    public static ExtractionProductFetchOptions DOCUMENTATION = builder()
            .withRecorderDepartment(false)
            .withRecorderPerson(false)
            .withTables(false)
            .withDocumentation(true)
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
    @Builder.Default()
    private boolean withDocumentation = false;
}
