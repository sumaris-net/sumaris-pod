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

package net.sumaris.core.vo.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sumaris.core.util.Beans;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperationFetchOptions implements IDataFetchOptions {

    public static final OperationFetchOptions DEFAULT = OperationFetchOptions.builder().build();

    public static OperationFetchOptions nullToEmpty(OperationFetchOptions options) {
        return options != null ? options : OperationFetchOptions.builder().build();
    }
    public static OperationFetchOptions clone(OperationFetchOptions options) {
        return options != null ? options.clone() : OperationFetchOptions.builder().build();
    }

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

    @Builder.Default
    private boolean withParentOperation = false;

    @Builder.Default
    private boolean withChildOperation = false;

    @Builder.Default
    private boolean withTrip = false;

    public OperationFetchOptions clone() {
        OperationFetchOptions target = new OperationFetchOptions();
        Beans.copyProperties(this, target);
        return target;
    }
}