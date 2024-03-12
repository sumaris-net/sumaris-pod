package net.sumaris.core.vo.technical.namedFilter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NamedFilterFetchOptions implements IFetchOptions {

    public static final NamedFilterFetchOptions DEFAULT = NamedFilterFetchOptions.builder().build();

    public static NamedFilterFetchOptions defaultIfEmpty(NamedFilterFetchOptions options) {
        return options != null ? options : DEFAULT;
    }

    @Builder.Default
    private boolean withContent = false;

}
