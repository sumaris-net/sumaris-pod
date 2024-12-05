package net.sumaris.core.model.data;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum ActivityCalendarDirectSurveyInvestigationEnum {
    NO(0, "NO", "N"),
    YES(1, "YES", "Y"),
    OPPORTUNISTIC(2, "OPPORTUNISTIC", "O");

    private final int id;
    private final String label;
    private final String code;

    ActivityCalendarDirectSurveyInvestigationEnum(int id, String label, String code) {
        this.id = id;
        this.label = label;
        this.code = code;
    }

    public static Optional<ActivityCalendarDirectSurveyInvestigationEnum> findById(int id) {
        return Arrays.stream(values()).filter(ActivityCalendarDirectSurveyInvestigationEnum -> ActivityCalendarDirectSurveyInvestigationEnum.id == id).findFirst();
    }

    public static Optional<ActivityCalendarDirectSurveyInvestigationEnum> findByLabel(String label) {
        return Arrays.stream(values()).filter(ActivityCalendarDirectSurveyInvestigationEnum -> label.equals(ActivityCalendarDirectSurveyInvestigationEnum.label)).findFirst();
    }

    public static Optional<ActivityCalendarDirectSurveyInvestigationEnum> findByCode(String code) {
        return Arrays.stream(values()).filter(ActivityCalendarDirectSurveyInvestigationEnum -> code.equals(ActivityCalendarDirectSurveyInvestigationEnum.code)).findFirst();
    }
}
