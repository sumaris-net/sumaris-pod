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

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ImageAttachmentFetchOptions implements IDataFetchOptions {

    public static final ImageAttachmentFetchOptions DEFAULT = ImageAttachmentFetchOptions.builder().build();

    public static final ImageAttachmentFetchOptions MINIMAL = ImageAttachmentFetchOptions.builder()
        .withRecorderDepartment(false)
        .withRecorderPerson(false)
        .build();

    public static final ImageAttachmentFetchOptions WITH_CONTENT = ImageAttachmentFetchOptions.builder()
        .withRecorderDepartment(false)
        .withRecorderPerson(false)
        .withContent(true)
        .build();

    @Builder.Default
    private boolean withRecorderDepartment = true;

    @Builder.Default
    private boolean withRecorderPerson = true;

    @Builder.Default
    private boolean withContent = false; // Important: should be disabled by default (for performance reason)


    @Builder.Default
    private boolean withObservers = false; // Make no sens here

    @Builder.Default
    private boolean withChildrenEntities = false; // Make no sens here

    @Builder.Default
    private boolean withMeasurementValues = false; // Make no sens here
}
