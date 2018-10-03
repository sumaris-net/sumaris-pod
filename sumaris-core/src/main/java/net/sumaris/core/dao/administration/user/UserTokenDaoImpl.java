package net.sumaris.core.dao.administration.user;

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

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.Dates;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.exception.BadUpdateDateException;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.administration.user.UserSettings;
import net.sumaris.core.model.administration.user.UserToken;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("userTokenDao")
public class UserTokenDaoImpl extends HibernateDaoSupport implements UserTokenDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(UserTokenDaoImpl.class);

    @Override
    public boolean existsByPubkey(String token, String pubkey) {

        EntityManager session = getEntityManager();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<UserToken> root = query.from(UserToken.class);

        ParameterExpression<String> pubkeyParam = builder.parameter(String.class);
        ParameterExpression<String> tokenParam = builder.parameter(String.class);

        query.select(builder.count(root.get(IReferentialEntity.PROPERTY_ID)))
                .where(builder.and(
                        builder.equal(root.get(UserToken.PROPERTY_PUBKEY), pubkeyParam),
                        builder.equal(root.get(UserToken.PROPERTY_TOKEN), tokenParam)
                    )
                );

        try {
            return session.createQuery(query)
                    .setParameter(pubkeyParam, pubkey)
                    .setParameter(tokenParam, token)
                    .getSingleResult() > 0;
        } catch (EmptyResultDataAccessException | NoResultException e) {
            return false;
        }
    }

    @Override
    public List<String> getAllByPubkey(String pubkey) {

        EntityManager session = getEntityManager();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<UserToken> query = builder.createQuery(UserToken.class);
        Root<UserToken> root = query.from(UserToken.class);

        ParameterExpression<String> pubkeyParam = builder.parameter(String.class);

        query.select(root)
             .where(builder.equal(root.get(UserToken.PROPERTY_PUBKEY), pubkeyParam));

        try {
            return session.createQuery(query)
                    .setParameter(pubkeyParam, pubkey)
                    .getResultList().stream().map(ut -> ut.getToken())
                    .collect(Collectors.toList());
        } catch (EmptyResultDataAccessException | NoResultException e) {
            return null;
        }
    }

    @Override
    public void add(String token, String pubkey) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(pubkey);

        EntityManager entityManager = getEntityManager();
        UserToken entity = new UserToken();

        entity.setCreationDate(new Date());
        entity.setToken(token);
        entity.setPubkey(pubkey);

        // Save entityName
        entityManager.persist(entity);

        getEntityManager().flush();
        getEntityManager().clear();
    }

    @Override
    public void delete(String token) {

        log.debug(String.format("Deleting user token {%s}...", token));

        EntityManager session = getEntityManager();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<UserToken> query = builder.createQuery(UserToken.class);
        Root<UserToken> root = query.from(UserToken.class);

        ParameterExpression<String> tokenParam = builder.parameter(String.class);

        query.select(root)
                .where(builder.equal(root.get(UserToken.PROPERTY_TOKEN), tokenParam));

        try {
            UserToken existingToken = session.createQuery(query)
                    .setParameter(tokenParam, token)
                    .getSingleResult();
            getEntityManager().remove(existingToken);
        } catch (EmptyResultDataAccessException | NoResultException e) {
            return; // not exists: continue
        }
    }

    /* -- protected methods -- */

}
