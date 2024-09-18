package net.sumaris.extraction.core.vo;

/*-
 * #%L
 * SUMARiS:: Core Extraction
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

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.hsqldb.rights.User;

import java.time.Month;
import java.util.Map;

/**
 * @author Dorian MARCO <dorian.marco@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class MonitoringResultVO extends ExtractionResultVO {

    String ship;

    String name;

    String QIM;

    String length;

    String qualification;

    Map<Month, String> numberOfWorkPerMonth;

    String calendarActivity;

    User writer;

    public MonitoringResultVO() {
        super();
    }

    public <T extends ExtractionResultVO> MonitoringResultVO(T source) {
        super(source);
    }
}
