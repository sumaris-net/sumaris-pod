package net.sumaris.core.vo.technical;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PropertyVO {
   private  String label;
   private String value;
}
