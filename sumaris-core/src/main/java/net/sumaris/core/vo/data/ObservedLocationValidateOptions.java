package net.sumaris.core.vo.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ObservedLocationValidateOptions {

    public static ObservedLocationValidateOptions DEFAULT = ObservedLocationValidateOptions.builder().build();

    public static ObservedLocationValidateOptions defaultIfEmpty(ObservedLocationValidateOptions options) {
        return options != null ? options : DEFAULT;
    }

    @Builder.Default
    private Boolean withChildren = false;
}
