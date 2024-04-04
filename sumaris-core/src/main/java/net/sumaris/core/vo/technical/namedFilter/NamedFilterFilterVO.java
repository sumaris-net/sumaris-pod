package net.sumaris.core.vo.technical.namedFilter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.vo.filter.IDataFilter;

import javax.annotation.Nullable;

@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedFilterFilterVO implements IDataFilter {

	public static NamedFilterFilterVO nullToEmpty(@Nullable NamedFilterFilterVO filter) {
		return filter == null ? new NamedFilterFilterVO() : filter;
	}

    private String searchText;
    private String entityName;

    private Integer recorderPersonId;
    private Integer recorderDepartmentId;

    private Integer[] qualityFlagIds;
    private DataQualityStatusEnum[] dataQualityStatus;
}
