package net.sumaris.server.http.graphql.data;

/*-
 * #%L
 * SUMARiS:: Server
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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.leangen.graphql.annotations.*;
import net.sumaris.core.dao.data.RootDataRepository;
import net.sumaris.core.dao.data.trip.TripRepository;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.*;
import net.sumaris.server.http.security.IsSupervisor;
import net.sumaris.server.http.security.IsUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Tentative de créer un service générique de controle/validation
 * => FIXME: Problème: le cache coté client doit etre mis à jour (updateDate) mais l'entité retournées n'est pas du type d'origine (TripVO, etc.)
 *
 */
@Service
@Transactional
public class DataQualityGraphQLService {
    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(DataQualityGraphQLService.class);

    @Autowired
    private TripRepository tripRepository;

    private Map<String, RootDataRepository<?, ? extends IRootDataVO<Integer>, ?, ?>> serviceByEntityNameMap;
    private Map<String, Class<? extends IRootDataVO<Integer>>> voTypeByEntityNameMap;
    private Map<String, Function<DataReferenceVO, ? extends IRootDataVO<Integer>>> conversionMap;

    @PostConstruct
    protected void init() {
        serviceByEntityNameMap = ImmutableMap.of(
                TripVO.TYPENAME, tripRepository
        );
        voTypeByEntityNameMap = ImmutableMap.of(
                TripVO.TYPENAME, TripVO.class
        );
    }

    @GraphQLMutation(name = "control", description = "Control a data, by reference")
    @IsUser
    public DataReferenceVO control(@GraphQLArgument(name = "reference") DataReferenceVO reference) {
        checkReference(reference);

        Date updateDate = getDataService(reference.getEntityName())
                .control(reference.getId(), reference.getUpdateDate());

        reference.setUpdateDate(updateDate);

        return reference;
    }

    /*@GraphQLMutation(name = "validate", description = "Validate a data, by reference")
    @IsSupervisor
    public DataReferenceVO validate(@GraphQLArgument(name = "reference") DataReferenceVO ref) {
        checkReference(ref);

        Date updateDate = getDataService(ref.getEntityName())
                .validate(ref.getId(), ref.getUpdateDate());

        ref.setUpdateDate(updateDate);

        return ref;
    }

    @GraphQLMutation(name = "unvalidate", description = "Unvalidate a data, by reference")
    @IsSupervisor
    public DataReferenceVO unvalidate(@GraphQLArgument(name = "reference") DataReferenceVO ref) {
        checkReference(ref);

        Date updateDate = getDataService(ref.getEntityName())
                .unValidate(ref.getId(), ref.getUpdateDate());

        ref.setUpdateDate(updateDate);

        return ref;
    }

    @GraphQLMutation(name = "qualify", description = "Qualify a data, by reference")
    @IsSupervisor
    public DataReferenceVO qualify(@GraphQLArgument(name = "reference") DataReferenceVO ref) {
        checkReference(ref);

        Date updateDate = getDataService(ref.getEntityName())
                .qualify(ref.getId(), ref.getUpdateDate());

        ref.setUpdateDate(updateDate);

        return ref;
    }*/

    /* -- protected -- */

    protected RootDataRepository<?, ? extends IRootDataVO<Integer>, ?, ?> getDataService(String entityName) {
        RootDataRepository service = serviceByEntityNameMap.get(entityName);
        Preconditions.checkNotNull(service, String.format("EntityName '%s' is not a root entity", entityName));
        return service;
    }

    protected IRootDataVO<Integer> toVO(DataReferenceVO source) {
        checkReference(source);

        String entityName = source.getEntityName();
        Class<? extends IRootDataVO<Integer>> clazz = voTypeByEntityNameMap.get(entityName);
        Preconditions.checkNotNull(clazz, String.format("EntityName '%s' is not a root entity", entityName));
        try {
            IRootDataVO<Integer> target = clazz.newInstance();
            Beans.copyProperties(source, target);
            return target;
        }
        catch (Exception e) {
            throw new SumarisTechnicalException("Unable to create VO of type: " + clazz.getCanonicalName(), e);
        }
    }

    protected void checkReference(DataReferenceVO data) {
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(data.getId());
        Preconditions.checkNotNull(data.getUpdateDate());
        Preconditions.checkNotNull(data.getEntityName());
    }
}
