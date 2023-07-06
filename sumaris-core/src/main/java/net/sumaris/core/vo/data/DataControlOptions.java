package net.sumaris.core.vo.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataControlOptions implements IControlOptions {

    public static DataControlOptions DEFAULT = DataControlOptions.builder().build();

    public static DataControlOptions defaultIfEmpty(DataControlOptions options) {
        return options != null ? options : DEFAULT;
    }

    @Builder.Default
    private Boolean withChildren = false;
}
