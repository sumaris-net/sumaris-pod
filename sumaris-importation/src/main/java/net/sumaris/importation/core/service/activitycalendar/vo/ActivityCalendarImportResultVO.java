package net.sumaris.importation.core.service.activitycalendar.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.vo.technical.job.IJobResultVO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityCalendarImportResultVO implements IJobResultVO {
    private Integer inserts;
    private Integer updates;
    private Integer disables;
    private Integer warnings;
    private Integer total;

    private Integer errors;

    private String message;

    private JobStatusEnum status;

    public boolean hasError() {
        return this.errors != null && this.errors > 0;
    }
}
