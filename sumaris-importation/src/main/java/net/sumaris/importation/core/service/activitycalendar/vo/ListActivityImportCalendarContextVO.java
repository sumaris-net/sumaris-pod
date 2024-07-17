package net.sumaris.importation.core.service.activitycalendar.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.File;

@Data
@Builder
public class ListActivityImportCalendarContextVO {
    @NonNull
    private Integer recorderPersonId;

    @NonNull
    private File processingFile;

    // result object containing messages and errors during process
    @NonNull
    @Builder.Default
    @JsonIgnore
    private ListActivityCalendarImportResultVO result = new ListActivityCalendarImportResultVO();
}
