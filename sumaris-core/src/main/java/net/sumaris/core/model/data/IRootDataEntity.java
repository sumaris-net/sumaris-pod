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

import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Person;

import java.io.Serializable;
import java.util.Date;

public interface IRootDataEntity<ID extends Serializable>
        extends IDataEntity<ID>,
        IWithRecorderPersonEntity<ID, Person> {

    interface Fields extends IDataEntity.Fields, IWithRecorderPersonEntity.Fields {
        String PROGRAM = "program";
        String CREATION_DATE = "creationDate";
        String COMMENTS = "comments";
        String VALIDATION_DATE = "validationDate";
    }

    Program getProgram();

    void setProgram(Program program);

    Date getCreationDate() ;

    void setCreationDate(Date creationDate);

    String getComments();

    void setComments(String comments);

    Date getValidationDate() ;

    void setValidationDate(Date validationDate);
}
