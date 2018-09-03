package net.sumaris.server.service.administration;

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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import it.ozimov.springboot.mail.service.EmailService;
import net.sumaris.core.dao.administration.PersonDao;
import net.sumaris.core.dao.administration.UserSettingsDao;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.vo.administration.user.AccountVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.exception.InvalidEmailConfirmationException;
import net.sumaris.server.service.crypto.ServerCryptoService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.i18n.I18n;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service("accountService")
public class AccountServiceImpl implements AccountService {


    /* Logger */
    private static final Log log = LogFactory.getLog(AccountServiceImpl.class);

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    @Autowired
    private PersonDao personDao;

    @Autowired
    private UserSettingsDao userSettingsDao;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ServerCryptoService serverCryptoService;

    private SumarisServerConfiguration config;

    private InternetAddress mailFromAddress;

    private String serverUrl;

    @Autowired
    public AccountServiceImpl(SumarisServerConfiguration config) {
        this.config = config;

        // Get mail 'from'
        String mailFrom = config.getMailFrom();
        if (StringUtils.isEmpty(mailFrom)) {
            mailFrom = config.getAdminMail();
        }
        if (StringUtils.isEmpty(mailFrom)) {
            log.warn(I18n.t("sumaris.error.account.register.mail.disable", SumarisServerConfigurationOption.MAIL_FROM.name()));
            this.mailFromAddress = null;
        }
        else {
            try {
                this.mailFromAddress = new InternetAddress(mailFrom);
            } catch (AddressException e) {
                throw new SumarisTechnicalException(I18n.t("sumaris.error.email.invalid", mailFrom, e.getMessage()), e);
            }
        }

        // Get server URL
        this.serverUrl = config.getServerUrl();
    }

    @Override
    public AccountVO getByPubkey(String pubkey) {

        PersonVO person = personDao.getByPubkeyOrNull(pubkey);
        if (person == null) {
            throw new DataRetrievalFailureException(I18n.t("sumaris.error.account.notFound"));
        }

        AccountVO account = new AccountVO();
        BeanUtils.copyProperties(person, account);

        UserSettingsVO settings = userSettingsDao.getByIssuer(account.getPubkey());
        account.setSettings(settings);

        return account;
    }

    @Override
    public AccountVO saveAccount(AccountVO account) {
        if (account != null && account.getId() == null) {
            return createAccount(account);
        }

        return updateAccount(account);
    }

    @Override
    public AccountVO createAccount(AccountVO account) {

        // Check if valid
        checkValid(account);

        // Check if not already exists
        PersonFilterVO filter = new PersonFilterVO();
        BeanUtils.copyProperties(account, filter);
        List<PersonVO> duplicatedPersons = personDao.findByFilter(filter, 0, 2, null, null);
        if (CollectionUtils.isNotEmpty(duplicatedPersons)) {
            throw new SumarisTechnicalException(I18n.t("sumaris.error.account.register.duplicatedPerson"));
        }

        // Generate confirmation code


        // Skip mail confirmation
        if (this.mailFromAddress == null) {
            log.debug(I18n.t("sumaris.server.account.register.mail.skip"));
            account.setStatusId(config.getStatusIdValid());
        }
        else {
            // Mark account as temporary
            account.setStatusId(config.getStatusIdTemporary());
        }

        // Reset user profiles
        account.setUserProfiles(null);

        // Normalize email
        account.setEmail(org.apache.commons.lang3.StringUtils.trimToNull(account.getEmail()));

        // Save account
        AccountVO savedAccount = (AccountVO) personDao.save(account);

        // Save settings
        if (account.getSettings() != null) {
            account.getSettings().setIssuer(account.getPubkey());
            UserSettingsVO savedSettings = userSettingsDao.save(account.getSettings());
            savedAccount.setSettings(savedSettings);
        }

        // Send confirmation Email
        sendConfirmationLinkByEmail(
                account.getEmail(),
                getLocale(account.getSettings().getLocale()));

        return savedAccount;
    }

    @Override
    public AccountVO updateAccount(AccountVO account) {

        // Check if valid
        checkValid(account);
        Preconditions.checkNotNull(account.getId());

        // TODO: check
        // - same email
        // - same profiles, etc.
        account = (AccountVO) personDao.save(account);

        // Save settings
        UserSettingsVO settingsVO = account.getSettings();
        if (settingsVO != null) {
            settingsVO.setIssuer(account.getPubkey());
            settingsVO = userSettingsDao.save(settingsVO);
            account.setSettings(settingsVO);
        }

        return account;
    }

    @Override
    public void confirmEmail(String email, String signatureHash) throws InvalidEmailConfirmationException {
        Preconditions.checkNotNull(email);
        Preconditions.checkArgument(email.trim().length() > 0);
        Preconditions.checkNotNull(signatureHash);
        Preconditions.checkArgument(signatureHash.trim().length() > 0);

        String validSignatureHash = serverCryptoService.hash(serverCryptoService.sign(email.trim()));

        // Mark account as temporary
        PersonFilterVO filter = new PersonFilterVO();
        filter.setEmail(email.trim());
        List<PersonVO> matches = personDao.findByFilter(filter, 0, 2, null, null);

        PersonVO account;
        boolean valid = CollectionUtils.size(matches) == 1 && validSignatureHash.equals(signatureHash);
        // Check the matched account status
        if (valid) {
            account = matches.get(0);
            valid = account.getStatusId() == config.getStatusIdTemporary();

            if (valid) {
                // Mark account status as valid
                account.setStatusId(config.getStatusIdValid());

                // Save account
                personDao.save(account);
            }
        }

        if (!valid) {
            // Log success
            log.warn(I18n.t("sumaris.error.account.register.badEmailOrCode", email));
            throw new InvalidEmailConfirmationException("Invalid confirmation: bad email or code.");
        }

        // Log success
        log.info(I18n.t("sumaris.server.account.register.confirmed", email));
    }

    @Override
    public void sendConfirmationEmail(String email, String locale) throws InvalidEmailConfirmationException {
        Preconditions.checkNotNull(email);
        Preconditions.checkNotNull(email.trim().length() > 0);

        // Mark account as temporary
        PersonFilterVO filter = new PersonFilterVO();
        filter.setEmail(email.trim());
        List<PersonVO> matches = personDao.findByFilter(filter, 0, 2, null, null);

        PersonVO account;
        boolean valid = CollectionUtils.size(matches) == 1;
        // Check the matched account status
        if (valid) {
            account = matches.get(0);
            valid = account.getStatusId() == config.getStatusIdTemporary();

            if (valid) {
                // Sent the confirmation email
                sendConfirmationLinkByEmail(email, getLocale(locale));
            }
        }

        if (!valid) {
            log.warn(I18n.t("sumaris.error.account.register.sentConfirmation.abort", email));
            throw new InvalidEmailConfirmationException("Could not sent confirmation email. Unknown email or already confirmed.");
        }

    }

    /* -- protected methods -- */

    protected void checkValid(AccountVO account) {
        Preconditions.checkNotNull(account);
        Preconditions.checkNotNull(account.getPubkey(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.account.pubkey")));
        Preconditions.checkNotNull(account.getEmail(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.account.email")));
        Preconditions.checkNotNull(account.getFirstName(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.account.firstName")));
        Preconditions.checkNotNull(account.getLastName(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.account.lastName")));

        // Check email
        try {
            new InternetAddress(account.getEmail());
        } catch (AddressException e) {
            throw new SumarisTechnicalException(I18n.t("sumaris.error.email.invalid", account.getEmail(), e.getMessage()), e);
        }

        // Check settings and settings.locale
        Preconditions.checkNotNull(account.getSettings(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.account.settings")));
        Preconditions.checkNotNull(account.getSettings().getLocale(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.account.settings.locale")));

        // Check settings issuer
        if (account.getSettings().getIssuer() != null) {
            Preconditions.checkArgument(Objects.equals(account.getPubkey(), account.getSettings().getIssuer()), "Bad value for 'settings.issuer' (Should be equals to 'pubkey')");
        }
    }

    private void sendConfirmationLinkByEmail(String toAddress, Locale locale) {

        // Send confirmation Email
        if (this.mailFromAddress != null) {
            try {

                String signatureHash = serverCryptoService.hash(serverCryptoService.sign(toAddress));

                String confirmationLinkURL = config.getRegistrationConfirmUrlPattern()
                        .replace("{email}", toAddress)
                        .replace("{code}", signatureHash);

                final Email email = DefaultEmail.builder()
                        .from(this.mailFromAddress)
                        .replyTo(this.mailFromAddress)
                        .to(Lists.newArrayList(new InternetAddress(toAddress)))
                        .subject(I18n.l(locale,"sumaris.server.mail.subject.prefix", config.getAppName())
                                + " " + I18n.l(locale, "sumaris.server.account.register.mail.subject"))
                        .body(I18n.l(locale, "sumaris.server.account.register.mail.body",
                                this.serverUrl,
                                confirmationLinkURL,
                                config.getAppName()))
                        .encoding(CHARSET_UTF8.name())
                        .build();

                emailService.send(email);
            }
            catch(AddressException e) {
                throw new SumarisTechnicalException(I18n.t("sumaris.error.account.register.sendEmailFailed", e.getMessage()), e);
            }
        }
    }

    private Locale getLocale(String localeStr) {
        if (localeStr.toLowerCase().startsWith("fr")) {
            return Locale.FRANCE;
        }
        return Locale.UK;
    }
}
