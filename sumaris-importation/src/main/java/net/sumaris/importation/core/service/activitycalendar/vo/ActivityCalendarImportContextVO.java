package net.sumaris.importation.core.service.activitycalendar.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.File;

@Data
@Builder
public class ActivityCalendarImportContextVO {
    @NonNull
    private Integer recorderPersonId;

    @NonNull
    private File processingFile;

    // result object containing messages and errors during process
    @NonNull
    @Builder.Default
    @JsonIgnore
    private ActivityCalendarImportResultVO result = new ActivityCalendarImportResultVO();
}
