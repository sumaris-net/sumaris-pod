package net.sumaris.core.model.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum ActivityCalendarDirectSurveyInvestigationEnum {
    NO(0, "N"),
    YES(1, "Y"),
    OPPORTUNISTIC(2, "O");

    private final int id;
    private final String label;

    ActivityCalendarDirectSurveyInvestigationEnum(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public static Optional<ActivityCalendarDirectSurveyInvestigationEnum> findById(int id) {
        return Arrays.stream(values()).filter(ActivityCalendarDirectSurveyInvestigationEnum -> ActivityCalendarDirectSurveyInvestigationEnum.id == id).findFirst();
    }

    public static Optional<ActivityCalendarDirectSurveyInvestigationEnum> findByLabel(String label) {
        return Arrays.stream(values()).filter(ActivityCalendarDirectSurveyInvestigationEnum -> label.equals(ActivityCalendarDirectSurveyInvestigationEnum.label)).findFirst();
    }

}
