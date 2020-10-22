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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import it.ozimov.springboot.mail.service.EmailService;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.administration.user.UserSettingsRepository;
import net.sumaris.core.dao.administration.user.UserTokenRepository;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.AccountVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.exception.InvalidEmailConfirmationException;
import net.sumaris.server.service.crypto.ServerCryptoService;
import org.apache.commons.collections.CollectionUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("accountService")
public class AccountServiceImpl implements AccountService {


    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserTokenRepository userTokenRepository;

    private EmailService emailService;

    @Autowired
    private ServerCryptoService serverCryptoService;

    @Autowired
    private GenericConversionService conversionService;

    @Autowired
    private AccountService self; // loop back to force transactional handling

    private SumarisServerConfiguration config;

    private InternetAddress mailFromAddress;

    private String serverUrl;

    private boolean emailEnable = false; // Will be update after config ready

    @Autowired
    public AccountServiceImpl(SumarisServerConfiguration config, EmailService emailService) {
        this.config = config;
        this.emailService = emailService;
        if (this.emailService != null) {
            log.warn(I18n.t("sumaris.server.email.started", config.getMailHost(), config.getMailPort()));
        }
        else {
            log.debug(I18n.t("sumaris.error.email.service",
                    SumarisServerConfigurationOption.MAIL_HOST.getKey(),
                    SumarisServerConfigurationOption.MAIL_PORT.getKey()));
        }
    }

    @PostConstruct
    public void init() {
        log.debug("Register {Account} converters");
        conversionService.addConverter(PersonVO.class, AccountVO.class, p -> self.toAccountVO(p));
        conversionService.addConverter(Person.class, AccountVO.class, p -> self.getByPubkey(p.getPubkey()));
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {

        boolean emailEnable = (emailService != null);

        // Get mail 'from'
        String mailFrom = config.getMailFrom();
        if (StringUtils.isEmpty(mailFrom)) {
            mailFrom = config.getAdminMail();
        }
        if (StringUtils.isEmpty(mailFrom)) {
            log.warn(I18n.t("sumaris.error.account.register.mail.disable", SumarisServerConfigurationOption.MAIL_FROM.name()));
            this.mailFromAddress = null;
            emailEnable = false;
        }
        else {
            try {
                this.mailFromAddress = new InternetAddress(mailFrom, config.getAppName());
            } catch (UnsupportedEncodingException e) {
                log.error(I18n.t("sumaris.error.email.invalid", mailFrom, e.getMessage()));
                emailEnable = false;
            }
        }

        // Get server URL
        this.serverUrl = config.getServerUrl();

        // Update enable state, if changed
        if (this.emailEnable != emailEnable) {
            this.emailEnable = emailEnable;
            if (!emailEnable) {
                log.warn("/!\\ Email service disabled! (see previous errors)");
            } else {
                log.info(I18n.t("sumaris.server.email.started", config.getMailHost(), config.getMailPort()));
            }
        }
    }

    @Override
    public AccountVO getByPubkey(String pubkey) {

        PersonVO person = personRepository.findByPubkey(pubkey);
        if (person == null) {
            throw new DataRetrievalFailureException(I18n.t("sumaris.error.account.notFound"));
        }

        AccountVO account = new AccountVO();
        BeanUtils.copyProperties(person, account);

        UserSettingsVO settings = userSettingsRepository.getByIssuer(account.getPubkey());
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
        List<PersonVO> duplicatedPersons = personRepository.findByFilter(filter, 0, 2, null, null);
        if (CollectionUtils.isNotEmpty(duplicatedPersons)) {
            throw new SumarisTechnicalException(ErrorCodes.ACCOUNT_ALREADY_EXISTS, I18n.t("sumaris.error.account.register.duplicatedPerson"));
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

        // Set default profile
        account.setProfiles(Lists.newArrayList(UserProfileEnum.GUEST.label));

        // Normalize email
        account.setEmail(org.apache.commons.lang3.StringUtils.trimToNull(account.getEmail()));

        // Save account
        AccountVO savedAccount = (AccountVO) personRepository.save(account);

        // Save settings
        if (account.getSettings() != null) {
            account.getSettings().setIssuer(account.getPubkey());
            UserSettingsVO savedSettings = userSettingsRepository.save(account.getSettings());
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

        // Get existing account
        PersonVO existingPerson = personRepository.findById(account.getId().intValue());
        if (existingPerson == null) {
            throw new DataNotFoundException(I18n.t("sumaris.error.account.notFound"));
        }

        // Check same email
        Preconditions.checkArgument(Objects.equals(existingPerson.getEmail(), account.getEmail()), "Email could not be changed by the user, but only by an administrator.");

        // Make sure to restore existing profiles, to avoid any changes by the user himself
        account.setProfiles(existingPerson.getProfiles());

        // Do the save
        account = (AccountVO) personRepository.save(account);

        // Save settings
        UserSettingsVO settings = account.getSettings();
        if (settings != null) {
            settings.setIssuer(account.getPubkey());
            settings = userSettingsRepository.save(settings);
            account.setSettings(settings);
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
        List<PersonVO> matches = personRepository.findByFilter(filter, 0, 2, null, null);

        PersonVO account = null;
        boolean valid = CollectionUtils.size(matches) == 1 && validSignatureHash.equals(signatureHash);
        // Check the matched account status
        if (valid) {
            account = matches.get(0);
            valid = account.getStatusId() == config.getStatusIdTemporary();

            if (valid) {
                // Mark account status as valid
                account.setStatusId(config.getStatusIdValid());

                // Save account
                personRepository.save(account);
            }
        }

        if (!valid) {
            // Log success
            log.warn(I18n.t("sumaris.error.account.register.badEmailOrCode", email));
            throw new InvalidEmailConfirmationException("Invalid confirmation: bad email or code.");
        }

        // Log success
        log.info(I18n.t("sumaris.server.account.register.confirmed", email));

        // Send email to admins
        sendRegistrationToAdmins(account);
    }

    @Override
    public void sendConfirmationEmail(String email, String locale) throws InvalidEmailConfirmationException {
        Preconditions.checkNotNull(email);
        Preconditions.checkArgument(email.trim().length() > 0);

        // Mark account as temporary
        PersonFilterVO filter = new PersonFilterVO();
        filter.setEmail(email.trim());
        List<PersonVO> matches = personRepository.findByFilter(filter, 0, 2, null, null);

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

    @Override
    public List<Integer> getProfileIdsByPubkey(String pubkey) {
        PersonVO person = personRepository.findByPubkey(pubkey);
        if (person == null) {
            throw new DataNotFoundException(I18n.t("sumaris.error.person.notFound"));
        }
        return Beans.getStream(person.getProfiles())
                .map(UserProfileEnum::valueOf)
                .map(up -> up.id)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllTokensByPubkey(String pubkey) {
        return userTokenRepository.findTokenByPubkey(pubkey).stream().map(UserTokenRepository.TokenOnly::getToken).collect(Collectors.toList());
    }

    @Override
    public boolean isStoredToken(String token, String pubkey) {
        return userTokenRepository.existsByTokenAndPubkey(token, pubkey);
    }

    @Override
    public void addToken(String token, String pubkey) {
        userTokenRepository.add(token, pubkey);
    }

    @Override
    public AccountVO toAccountVO(PersonVO person) {
        if (person == null) return null;

        AccountVO account = new AccountVO();
        BeanUtils.copyProperties(person, account);

        UserSettingsVO settings = userSettingsRepository.getByIssuer(account.getPubkey());
        account.setSettings(settings);

        return account;
    }

    @JmsListener(destination = "createTrip", containerFactory = "jmsListenerContainerFactory")
    public void onTripCreated(TripVO entity) {
        log.info(String.format("New trip {%s}",  entity.getId()));
        // TODO send event for supervisor
    }

    @JmsListener(destination = "updateTrip", containerFactory = "jmsListenerContainerFactory")
    public void onTripUpdated(TripVO entity) {
        log.info(String.format("Updated trip {%s}",  entity.getId()));
        // TODO send event for supervisor
    }

    /* -- protected methods -- */



    protected void checkValid(AccountVO account) {
        Preconditions.checkNotNull(account);
        Preconditions.checkNotNull(account.getPubkey(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.pubkey")));
        Preconditions.checkNotNull(account.getEmail(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.email")));
        Preconditions.checkNotNull(account.getFirstName(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.firstName")));
        Preconditions.checkNotNull(account.getLastName(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.lastName")));
        Preconditions.checkNotNull(account.getDepartment(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.department")));
        Preconditions.checkNotNull(account.getDepartment().getId(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.department")));

        // Check email
        try {
            new InternetAddress(account.getEmail());
        } catch (AddressException e) {
            throw new SumarisTechnicalException(ErrorCodes.BAD_REQUEST, I18n.t("sumaris.error.email.invalid", account.getEmail(), e.getMessage()), e);
        }

        // Check settings and settings.locale
        if (account.getSettings() != null) {
            checkValid(account.getSettings());

            // Check settings issuer
            if (account.getSettings().getIssuer() != null) {
                Preconditions.checkArgument(Objects.equals(account.getPubkey(), account.getSettings().getIssuer()), "Bad value for 'settings.issuer' (Should be equals to 'pubkey')");
            }
        }
    }

    protected void checkValid(UserSettingsVO settings) {
        Preconditions.checkNotNull(settings);
        // Check settings and settings.locale
        Preconditions.checkNotNull(settings, I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.account.settings")));
        Preconditions.checkNotNull(settings.getLocale(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.settings.locale")));
        Preconditions.checkNotNull(settings.getLatLongFormat(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.settings.latLongFormat")));
    }

    /**
     * Send confirmation Email
     *
     * @param toAddress
     * @param locale
     */
    private void sendConfirmationLinkByEmail(String toAddress, Locale locale) {
        if (!this.emailEnable) return; // Skip if disable

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
            throw new SumarisTechnicalException(ErrorCodes.INTERNAL_ERROR, I18n.t("sumaris.error.account.register.sendEmailFailed", e.getMessage()), e);
        }
    }

    private Locale getLocale(String localeStr) {
        if (localeStr.toLowerCase().startsWith("fr")) {
            return Locale.FRANCE;
        }
        return Locale.UK;
    }

    protected void sendRegistrationToAdmins(PersonVO confirmedAccount) {
        if (!this.emailEnable) return; // Skip if disable

        try {

            List<String> adminEmails = personRepository.getEmailsByProfiles(
                    ImmutableList.of(UserProfileEnum.ADMIN.getId()),
                    ImmutableList.of(StatusEnum.ENABLE.getId())
            );

            // No admin: log on server
            if (CollectionUtils.isEmpty(adminEmails) || this.mailFromAddress == null) {
                log.warn("New account registered, but no admin to validate it !");
                return;
            }

            // No from address: could not send email
            if (this.mailFromAddress == null) {
                log.warn("New account registered, but no from address configured to send to administrators!!");
                return;
            }

            // TODO: group email by locales (find it with the email, from personService)

            // Send the email
            final Email email = DefaultEmail.builder()
                    .from(this.mailFromAddress)
                    .replyTo(this.mailFromAddress)
                    .to(toInternetAddress(adminEmails))
                    .subject(I18n.t("sumaris.server.mail.subject.prefix", config.getAppName())
                            + " " + I18n.t("sumaris.server.account.register.admin.mail.subject"))
                    .body(I18n.t("sumaris.server.account.register.admin.mail.body",
                            confirmedAccount.getFirstName(),
                            confirmedAccount.getLastName(),
                            confirmedAccount.getEmail(),
                            this.serverUrl,
                            this.serverUrl + "/admin/users",
                            config.getAppName()
                            ))
                    .encoding(CHARSET_UTF8.name())
                    .build();

            emailService.send(email);
        }
        catch(Throwable e) {
            // Just log, but continue
            log.error(I18n.t("sumaris.error.account.register.sendAdminEmailFailed", e.getMessage()), new SumarisTechnicalException(e));
        }
    }

    protected List<InternetAddress> toInternetAddress(List<String> emails) {
        return emails.stream()
                .map(email -> {
                        try {
                            return new InternetAddress(email);
                        } catch(AddressException e) {
                            log.debug("Invalid email address {" + email + "}: " + e.getMessage());
                            return null;
                        }
                    })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
