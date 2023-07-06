package net.sumaris.core.vo.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataValidateOptions implements IValidateOptions {

    public static DataValidateOptions DEFAULT = DataValidateOptions.builder().build();

    public static DataValidateOptions defaultIfEmpty(DataValidateOptions options) {
        return options != null ? options : DEFAULT;
    }

    @Builder.Default
    private Boolean withChildren = false;
}
