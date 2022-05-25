/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package net.sumaris.core.vo.technical.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import lombok.*;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.ITreeNodeEntityBean;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
public class RuleVO implements IEntity<Integer>, IReferentialVO<Integer>, ITreeNodeEntityBean<Integer, RuleVO> {

    @ToString.Include
    @EqualsAndHashCode.Exclude
    private Integer id;

    private String label;

    @ToString.Include
    private String name; // the controlled attribut
    private String description;

    @EqualsAndHashCode.Exclude
    private Date updateDate;
    private Date creationDate;

    private Integer statusId;

    @ToString.Include
    private String operator; // See OperatorEnum

    @ToString.Include
    private String value;

    @ToString.Include
    private String[] values;

    private boolean bidirectional;
    private boolean precondition;
    private boolean blocking;

    private String message; // User message
    private List<RuleVO> children;

    @EqualsAndHashCode.Exclude
    private RuleVO parent;
    private Integer parentId;


    @JsonIgnore
    public boolean isEmpty() {
        return (StringUtils.isEmpty(operator) && StringUtils.isEmpty(name))
            && CollectionUtils.isEmpty(children);
    }

}
