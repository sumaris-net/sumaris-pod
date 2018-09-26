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
import net.sumaris.core.model.administration.user.UserSettings;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("userSettingsDao")
public class UserSettingsDaoImpl extends HibernateDaoSupport implements UserSettingsDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(UserSettingsDaoImpl.class);

    @Override
    public UserSettingsVO getByIssuer(String issuer) {

        EntityManager session = getEntityManager();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<UserSettings> query = builder.createQuery(UserSettings.class);
        Root<UserSettings> root = query.from(UserSettings.class);

        ParameterExpression<String> issuerParam = builder.parameter(String.class);

        query.select(root)
             .where(builder.equal(root.get(UserSettingsVO.PROPERTY_ISSUER), issuerParam));

        try {
            return toUserSettingsVO(session.createQuery(query)
                    .setParameter(issuerParam, issuer)
                    .getSingleResult());
        } catch (EmptyResultDataAccessException | NoResultException e) {
            return null;
        }
    }

    @Override
    public UserSettingsVO get(int id) {
        return toUserSettingsVO(get(UserSettings.class, id));
    }

    @Override
    public UserSettingsVO save(UserSettingsVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLocale(), "Missing 'settings.locale'");
        Preconditions.checkNotNull(source.getLatLongFormat(), "Missing 'settings.latLongformat'");

        EntityManager entityManager = getEntityManager();
        UserSettings entity = null;
        if (source.getId() != null) {
            entity = get(UserSettings.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new UserSettings();
        }

        if (!isNew) {
            // Check update date
            if (entity.getUpdateDate() != null) {
                Timestamp serverUpdateDtNoMillisecond = Dates.resetMillisecond(entity.getUpdateDate());
                Timestamp sourceUpdateDtNoMillisecond = Dates.resetMillisecond(source.getUpdateDate());
                if (!Objects.equals(sourceUpdateDtNoMillisecond, serverUpdateDtNoMillisecond)) {
                    throw new BadUpdateDateException(I18n.t("sumaris.persistence.error.badUpdateDate",
                            I18n.t("sumaris.persistence.table.userSettings"), source.getId(), serverUpdateDtNoMillisecond,
                            sourceUpdateDtNoMillisecond));
                }
            }

            // Lock entityName
            /*try {
                Session.LockRequest lockRequest = entityManager.buildLockRequest(LockOptions.UPGRADE);
                lockRequest.setLockMode(LockMode.UPGRADE_NOWAIT);
                lockRequest.setScope(true); // cascaded to owned collections and relationships.
                lockRequest.lock(entity);
            } catch (LockTimeoutException e) {
                throw new DataLockedException(I18n.t("sumaris.persistence.error.locked",
                        I18n.t("sumaris.persistence.table.userSettings"), source.getId()), e);
            }*/
        }

        userSettingsVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        getEntityManager().flush();
        getEntityManager().clear();

        return source;
    }

    @Override
    public void delete(int id) {

        log.debug(String.format("Deleting user settings {id=%s}...", id));
        delete(UserSettings.class, id);
    }

    @Override
    public UserSettingsVO toUserSettingsVO(UserSettings source) {
        if (source == null) return null;
        UserSettingsVO target = new UserSettingsVO();

        Beans.copyProperties(source, target);

        // Issuer
        //target.setIssuer(source.getIssuer());

        return target;
    }

    /* -- protected methods -- */

    protected List<UserSettingsVO> toUserSettingsVOs(List<UserSettings> source) {
        return source.stream()
                .map(this::toUserSettingsVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void userSettingsVOToEntity(UserSettingsVO source, UserSettings target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

       /* // Person
        if (copyIfNull || source.getPersonId() != null) {
            if (source.getPersonId() == null) {
                target.setPerson(null);
            }
            else {
                target.setPerson(load(Person.class, source.getPersonId()));
            }
        }*/

    }
}
