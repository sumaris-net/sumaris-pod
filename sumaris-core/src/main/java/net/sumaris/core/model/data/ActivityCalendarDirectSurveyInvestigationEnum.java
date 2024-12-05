package net.sumaris.core.model.data;

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
