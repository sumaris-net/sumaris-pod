package net.sumaris.core.model.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;

public interface IMeasurementEntity extends IDataEntity<Integer> {

    int SEQUENCE_ALLOCATION_SIZE = 50;

    interface Fields extends IEntity.Fields {
        String PMFM = "pmfm";
        String NUMERICAL_VALUE = "numericalValue";
        String ALPHANUMERICAL_VALUE = "alphanumericalValue";
        String DIGIT_COUNT = "digitCount";
        String PRECISION_VALUE = "precisionValue";
        String QUALITATIVE_VALUE = "qualitativeValue";
    }

    Double getNumericalValue();

    void setNumericalValue(Double numericalValue);

    String getAlphanumericalValue();

    void setAlphanumericalValue(String alphanumericalValue);

    Integer getDigitCount();

    void setDigitCount(Integer digitCount);

    Double getPrecisionValue();

    void setPrecisionValue(Double precisionValue);

    QualitativeValue getQualitativeValue();

    void setQualitativeValue(QualitativeValue qualitativeValue);

    Pmfm getPmfm();

    void setPmfm(Pmfm pmfm);

}
