package net.sumaris.core.model.data;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum ActivityCalendarDirectSurveyInvestigationEnum {
    NO(0, "NO"),
    YES(1, "YES"),
    OPPORTUNISTIC(2, "OPPORTUNISTIC");

    private final int id;
    private final String label;

    ActivityCalendarDirectSurveyInvestigationEnum(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public static Optional<ActivityCalendarDirectSurveyInvestigationEnum> findById(int id) {
        return Arrays.stream(values()).filter(userProfileEnum -> userProfileEnum.id == id).findFirst();
    }

    public static Optional<ActivityCalendarDirectSurveyInvestigationEnum> findByLabel(String label) {
        return Arrays.stream(values()).filter(userProfileEnum -> label.equals(userProfileEnum.label)).findFirst();
    }

}
