package net.sumaris.core.dao.data;

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

import net.sumaris.core.dao.administration.programStrategy.ProgramDao;
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.core.vo.filter.IRootDataFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.Date;

public class RootDataRepositoryImpl<
        E extends IRootDataEntity<ID>,
        ID extends Integer,
        V extends IRootDataVO<ID>,
        F extends IRootDataFilter
        >
        extends DataRepositoryImpl<E, ID, V, F>
        implements RootDataRepository<E, ID, V, F> {

    /**
     * Logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(RootDataRepositoryImpl.class);

    @Autowired
    private PersonDao personDao;

    @Autowired
    private ProgramDao programDao;

    public RootDataRepositoryImpl(Class<E> domainClass,
                                  EntityManager entityManager) {
        super(domainClass, entityManager);
    }

    @Override
    public V save(V vo) {
        E entity = toEntity(vo);

        // TODO: let this following code in this overidden method, or set creationDate in createEntity ?
        if (entity.getId() == null) {
            entity.setCreationDate(new Date());
        }

        // Check update date
        Daos.checkUpdateDateForUpdate(vo, entity);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        E savedEntity = save(entity);

        vo.setId(savedEntity.getId());

        return vo;
    }


    //@Override
    public void toEntity(V source, E target, boolean copyIfNull) {

        DataDaos.copyRootDataProperties(getEntityManager(), source, target, copyIfNull);

        super.toEntity(source, target, copyIfNull);
    }


    //@Override
    public void toVO(E source, V target, DataFetchOptions fetchOptions, boolean copyIfNull) {

        super.toVO(source, target, fetchOptions, copyIfNull);

        // Program
        if (source.getProgram() != null) {
            target.setProgram(programDao.toProgramVO(source.getProgram(),
                    ProgramFetchOptions.builder().withProperties(false)
                            .build()));
        }

        // Recorder person
        if ((fetchOptions == null || fetchOptions.isWithRecorderPerson()) && source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
        }


    }

}
