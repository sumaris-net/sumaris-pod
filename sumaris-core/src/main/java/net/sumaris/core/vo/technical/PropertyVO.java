package net.sumaris.core.vo.technical;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PropertyVO {
   private String name;
   private  String label;

   public PropertyVO(String name, String label ){
      this.name=name;
      this.label=label;
   }
}
