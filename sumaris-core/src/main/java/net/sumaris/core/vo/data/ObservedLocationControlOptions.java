package net.sumaris.core.vo.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservedLocationControlOptions {

    public static ObservedLocationControlOptions DEFAULT = ObservedLocationControlOptions.builder().build();

    public static ObservedLocationControlOptions defaultIfEmpty(ObservedLocationControlOptions options) {
        return options != null ? options : DEFAULT;
    }

    @Builder.Default
    private Boolean withChildren = false;
}
