package net.sumaris.core.vo.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;

import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 09/04/2020.
 */
@Data
@FieldNameConstants
@EqualsAndHashCode
public class PacketVO implements IDataVO<Integer> {

    @EqualsAndHashCode.Exclude
    private Integer id;
    private String comments;
    @EqualsAndHashCode.Exclude
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private Integer rankOrder;
    private Integer number;
    private Double weight;
    private List<Double> sampledWeights;
    private List<PacketCompositionVO> composition;
    private List<ProductVO> saleProducts;

    @EqualsAndHashCode.Exclude
    private OperationVO operation;
    private Integer operationId;

    public String toString() {
        return "PacketVO(id=" + id + ", rankOrder=" + rankOrder +
            ", number=" + number + ", weight=" + weight +
            ", composition=" + composition.toString() + ")";
    }
}
