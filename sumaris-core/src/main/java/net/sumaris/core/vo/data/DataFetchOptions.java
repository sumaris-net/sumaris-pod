package net.sumaris.core.vo.data;

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
public class DataFetchOptions implements IDataFetchOptions {

    public static final DataFetchOptions DEFAULT = DataFetchOptions.builder().build();

    public static final DataFetchOptions MINIMAL = DataFetchOptions.builder()
        .withRecorderDepartment(false)
        .withRecorderPerson(false)
        .withObservers(false)
        .build();

    public static final DataFetchOptions FULL_GRAPH = DataFetchOptions.builder()
            .withChildrenEntities(true)
            .withMeasurementValues(true)
            .build();

    @Builder.Default
    private boolean withRecorderDepartment = true;

    @Builder.Default
    private boolean withRecorderPerson = true;

    @Builder.Default
    private boolean withObservers = true;

    @Builder.Default
    private boolean withChildrenEntities = false;

    @Builder.Default
    private boolean withMeasurementValues = false;


}
