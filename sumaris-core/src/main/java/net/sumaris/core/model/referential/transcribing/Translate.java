package net.sumaris.core.model.referential.transcribing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cache.annotation.Cacheable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Cacheable
public class Translate {
    private Integer objectId;
    private String externalCode;
    private String typeName;
    private Integer typeId;
}
