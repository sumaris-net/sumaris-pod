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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.administration.user.UserTokenRepository;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.administration.user.UserToken;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.social.UserEventService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.I18nUtil;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.core.vo.administration.user.AccountVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.exception.InvalidEmailConfirmationException;
import net.sumaris.server.service.crypto.ServerCryptoService;
import net.sumaris.server.service.social.UserMessageService;
import org.apache.commons.collections.CollectionUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("accountService")
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final SumarisServerConfiguration configuration;
    private final PersonRepository personRepository;
    private final UserSettingsService userSettingsService;
    private final UserTokenRepository userTokenRepository;
    private final PersonService personService;
    private final UserMessageService userMessageService;
    private final ServerCryptoService serverCryptoService;
    private final UserEventService userEventService;

    private String serverUrl;
    private UserProfileEnum confirmedUserProfile;

    public AccountServiceImpl(SumarisServerConfiguration serverConfiguration,
                              PersonService personService,
                              PersonRepository personRepository,
                              UserSettingsService userSettingsService,
                              UserTokenRepository userTokenRepository,
                              ServerCryptoService serverCryptoService,
                              GenericConversionService conversionService,
                              UserMessageService userMessageService, UserEventService userEventService) {
        this.personService = personService;
        this.personRepository = personRepository;
        this.userSettingsService = userSettingsService;
        this.userTokenRepository = userTokenRepository;
        configuration = serverConfiguration;
        this.serverCryptoService = serverCryptoService;
        this.userMessageService = userMessageService;

        log.debug("Register {Account} converters");
        conversionService.addConverter(PersonVO.class, AccountVO.class, this::toAccountVO);
        conversionService.addConverter(Person.class, AccountVO.class, p -> getByPubkey(p.getPubkey()));
        this.userEventService = userEventService;
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        // Get server URL
        serverUrl = configuration.getServerUrl();

        // Get the default
        confirmedUserProfile = configuration.getConfirmedUserProfile();
    }

    @Override
    public AccountVO getById(int id) {

        PersonVO person = personService.getById(id);

        AccountVO account = new AccountVO();
        BeanUtils.copyProperties(person, account);

        UserSettingsVO settings = userSettingsService.findByIssuer(account.getPubkey()).orElse(null);
        account.setSettings(settings);

        return account;
    }

    @Override
    public AccountVO getByPubkey(String pubkey) {

        PersonVO person = personService.getByPubkey(pubkey);

        AccountVO account = new AccountVO();
        BeanUtils.copyProperties(person, account);

        UserSettingsVO settings = userSettingsService.findByIssuer(account.getPubkey()).orElse(null);
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
        // Mark account as temporary
        if (userMessageService.getEmailDefaultFrom() == null) {
            log.debug(I18n.t("sumaris.server.account.register.mail.skip"));
            account.setStatusId(StatusEnum.ENABLE.getId());
        } else {
            account.setStatusId(StatusEnum.TEMPORARY.getId());
        }

        // Set default profile as guest (will be changed later, when received the email confirmation)
        account.setProfiles(Lists.newArrayList(UserProfileEnum.GUEST.label));

        // Normalize email
        account.setEmail(StringUtils.trimToNull(account.getEmail()));

        // Save account
        AccountVO savedAccount = (AccountVO) personRepository.save(account);

        // Save settings
        UserSettingsVO settings = account.getSettings();
        if (settings != null) {
            settings.setIssuer(account.getPubkey());
            settings = userSettingsService.save(settings);
            account.setSettings(settings);
        }

        // Send confirmation Email
        sendConfirmationLinkByEmail(
                account.getEmail(),
                I18nUtil.toI18nLocale(account.getSettings().getLocale()));

        return savedAccount;
    }

    @Override
    public AccountVO updateAccount(AccountVO account) {

        // Check if valid
        checkValid(account);
        Preconditions.checkNotNull(account.getId());

        // Get existing account
        PersonVO existingPerson = personService.getById(account.getId());

        // Force keeping existing profiles (avoid any changes by user)
        // (This properties must be changed by the PersonService directly)
        account.setEmail(existingPerson.getEmail());
        account.setUsername(existingPerson.getUsername());
        account.setUsernameExtranet(existingPerson.getUsernameExtranet());
        account.setPubkey(existingPerson.getPubkey());
        account.setProfiles(existingPerson.getProfiles());

        // Do the save
        account = (AccountVO) personService.save(account);

        // Save settings
        UserSettingsVO settings = account.getSettings();
        if (settings != null) {
            settings.setIssuer(account.getPubkey());
            settings = userSettingsService.save(settings);
            account.setSettings(settings);
        }

        return account;
    }

    @Override
    public void updatePubkeyById(Integer id, String newPubkey) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(newPubkey);
        Preconditions.checkArgument(CryptoUtils.isValidPubkey(newPubkey), "Invalid new pubkey: " + newPubkey);

        PersonVO user = personService.getById(id);

        // Check user is enable or temporary
        Preconditions.checkArgument(
            Objects.equals(user.getStatusId(), StatusEnum.ENABLE.getId())
            || Objects.equals(user.getStatusId(), StatusEnum.TEMPORARY.getId())
        );

        // Check old pubkey is valid, and not same
        String oldPubkey = user.getPubkey();
        Preconditions.checkArgument(CryptoUtils.isValidPubkey(oldPubkey), "Invalid old pubkey: " + oldPubkey);
        Preconditions.checkArgument(!Objects.equals(oldPubkey, newPubkey), "Cannot replace with the same pubkey");

        // Check new pubkey is not already used
        boolean alreadyExists = personService.findByPubkey(newPubkey).isPresent();
        Preconditions.checkArgument(!alreadyExists, "New pubkey already used by another account.");

        // Update pubkey
        user.setPubkey(newPubkey);
        personService.save(user);

        // Replace pubkey in other tables (e.g. UserSettings, UserEvent)
        updatePubkey(oldPubkey, newPubkey);
    }

    @Override
    public void confirmEmail(String email, String signatureHash) throws InvalidEmailConfirmationException {
        Preconditions.checkNotNull(email);
        Preconditions.checkArgument(!email.trim().isEmpty());
        Preconditions.checkNotNull(signatureHash);
        Preconditions.checkArgument(!signatureHash.trim().isEmpty());

        String validSignatureHash = serverCryptoService.hash(serverCryptoService.sign(email.trim()));

        // Mark account as temporary
        PersonFilterVO filter = new PersonFilterVO();
        filter.setEmail(email.trim());
        List<PersonVO> matches = personService.findByFilter(filter, 0, 2, null, null);

        PersonVO account = null;
        boolean valid = CollectionUtils.size(matches) == 1 && validSignatureHash.equals(signatureHash);
        // Check the matched account status
        if (valid) {
            account = matches.get(0);
            valid = Objects.equals(account.getStatusId(), StatusEnum.TEMPORARY.getId());

            if (valid) {
                // Mark account status as valid
                account.setStatusId(StatusEnum.ENABLE.getId());

                // Set confirmed user profile (instead of GUEST)
                if (confirmedUserProfile != UserProfileEnum.GUEST) {
                    List<String> actualProfiles = Beans.getList(account.getProfiles());
                    // Process only if no profiles, or if profiles was still GUEST
                    // (e.g. keep any other profiles, given by the admin BEFORE received the email confirmation)
                    if (CollectionUtils.isEmpty(actualProfiles) || (
                        actualProfiles.size() == 1 && actualProfiles.contains(UserProfileEnum.GUEST.label))) {
                        account.setProfiles(Lists.newArrayList(confirmedUserProfile.label));
                    }
                }

                // Save account
                personService.save(account);
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
    public void sendResetPasswordEmail(@NonNull String username,
                                       String token,
                                       String locale) throws InvalidEmailConfirmationException {
        Preconditions.checkArgument(!username.trim().isEmpty());

        String email;
        // Test if username = email
        if (personService.existsByEmail(username)) {
            email = username;
        }
        else {
            email = personService.findEmailByUsername(username)
                .orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.error.person.notFound")));
        }

        sendResetPasswordEmail(email,
            token,
            I18nUtil.toI18nLocale(locale));

    }

    @Override
    public void sendConfirmationEmail(@NonNull String email, String locale) throws InvalidEmailConfirmationException {
        Preconditions.checkArgument(!email.trim().isEmpty());

        // Mark account as temporary
        PersonFilterVO filter = new PersonFilterVO();
        filter.setEmail(email.trim());
        List<PersonVO> matches = personService.findByFilter(filter, 0, 2, null, null);

        PersonVO account;
        boolean valid = CollectionUtils.size(matches) == 1;
        // Check the matched account status
        if (valid) {
            account = matches.get(0);
            valid = Objects.equals(account.getStatusId(), StatusEnum.TEMPORARY.getId());

            // Sent the confirmation email
            if (valid) {
                sendConfirmationLinkByEmail(email, I18nUtil.toI18nLocale(locale));
            }
        }

        if (!valid) {
            log.warn(I18n.t("sumaris.error.account.register.sentConfirmation.abort", email));
            throw new InvalidEmailConfirmationException("Could not sent confirmation email. Unknown email or already confirmed.");
        }

    }

    @Override
    public List<String> getAllTokensByPubkey(String pubkey) {
        return userTokenRepository.findTokenByPubkey(pubkey).stream()
                .map(UserTokenRepository.TokenOnly::getToken)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> deleteAllTokensByPubkey(String pubkey) {
        List<UserToken> tokens = userTokenRepository.findByPubkey(pubkey);
        userTokenRepository.deleteAll(tokens);
        return tokens.stream()
                .map(UserToken::getToken)
                .collect(Collectors.toList());
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
        if (person == null) {
            return null;
        }

        AccountVO account = new AccountVO();
        BeanUtils.copyProperties(person, account);

        UserSettingsVO settings = userSettingsService.findByIssuer(account.getPubkey()).orElse(null);
        account.setSettings(settings);

        return account;
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
        if (!userMessageService.isEmailEnabled()) {
            log.warn(I18n.t("sumaris.error.account.register.sentConfirmation.skipped"));
            return; // Skip if disable
        }

        try {

            String signatureHash = serverCryptoService.hash(serverCryptoService.sign(toAddress));
            String encodedSignatureHash = URLEncoder.encode(signatureHash, StandardCharsets.UTF_8);
            String confirmationLinkURL = configuration.getRegistrationConfirmUrlPattern()
                    .replace("{email}", toAddress)
                    .replace("{code}", encodedSignatureHash);

            Email email = DefaultEmail.builder()
                    .from(userMessageService.getEmailDefaultFrom())
                    .replyTo(userMessageService.getEmailDefaultFrom())
                    .to(Lists.newArrayList(new InternetAddress(toAddress)))
                    .subject(I18n.l(locale, "sumaris.server.mail.subject.prefix", configuration.getAppName())
                            + " " + I18n.l(locale, "sumaris.server.account.register.mail.subject"))
                    .body(I18n.l(locale, "sumaris.server.account.register.mail.body",
                            serverUrl,
                            confirmationLinkURL,
                            configuration.getAppName()))
                    .encoding(StandardCharsets.UTF_8.name())
                    .build();

            userMessageService.send(email);
        } catch (AddressException e) {
            throw new SumarisTechnicalException(ErrorCodes.INTERNAL_ERROR, I18n.t("sumaris.error.account.register.sendEmailFailed", e.getMessage()), e);
        }
    }

    private void sendResetPasswordEmail(@NonNull String toAddress,
                                        @NonNull String token, Locale locale) {
        if (!userMessageService.isEmailEnabled()) {
            log.warn(I18n.t("sumaris.error.account.register.sentConfirmation.skipped"));
            return; // Skip if disable
        }

        try {
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String resetPasswordLinkURL = configuration.getResetPasswordUrlPattern()
                .replace("{email}", toAddress)
                .replace("{code}", encodedToken);

            Integer tokenLifeTimeInMinutes = Math.round((float) configuration.getAuthResetTokenLifeTime() / 60);
            Email email = DefaultEmail.builder()
                    .from(userMessageService.getEmailDefaultFrom())
                    .replyTo(userMessageService.getEmailDefaultFrom())
                    .to(Lists.newArrayList(new InternetAddress(toAddress)))
                    .subject(I18n.l(locale, "sumaris.server.mail.subject.prefix", configuration.getAppName())
                            + " " + I18n.l(locale, "sumaris.server.account.reset.mail.subject"))
                    .body(I18n.l(locale, "sumaris.server.account.reset.mail.body",
                        serverUrl,
                        resetPasswordLinkURL,
                        tokenLifeTimeInMinutes,
                        configuration.getAppName()))
                    .encoding(StandardCharsets.UTF_8.name())
                    .build();

            userMessageService.send(email);
        } catch (AddressException e) {
            throw new SumarisTechnicalException(ErrorCodes.INTERNAL_ERROR, I18n.t("sumaris.error.account.register.sendEmailFailed", e.getMessage()), e);
        }

    }

    protected void sendRegistrationToAdmins(PersonVO confirmedAccount) {
        if (!userMessageService.isEmailEnabled()) {
            log.warn("New account registered, but no cannot send email to administrators!");
            // TODO: send as internal message ?
            return; // Skip if disable
        }

        try {

            List<String> adminEmails = personRepository.getEmailsByProfiles(
                    ImmutableList.of(UserProfileEnum.ADMIN.getId()),
                    ImmutableList.of(StatusEnum.ENABLE.getId())
            );

            // No admin: log then skip
            if (CollectionUtils.isEmpty(adminEmails)) {
                log.warn("New account registered, but no admin to validate it !");
                return;
            }

            // TODO: group email by locales (find it with the email, from personService)

            // Send the email
            Email email = DefaultEmail.builder()
                    .from(userMessageService.getEmailDefaultFrom())
                    .replyTo(userMessageService.getEmailDefaultFrom())
                    .to(toInternetAddress(adminEmails))
                    .subject(I18n.t("sumaris.server.mail.subject.prefix", configuration.getAppName())
                            + " " + I18n.t("sumaris.server.account.register.admin.mail.subject"))
                    .body(I18n.t("sumaris.server.account.register.admin.mail.body",
                            confirmedAccount.getFirstName(),
                            confirmedAccount.getLastName(),
                            confirmedAccount.getEmail(),
                            serverUrl,
                            serverUrl + "/admin/users",
                            configuration.getAppName()
                    ))
                    .encoding(Charsets.UTF_8.name())
                    .build();

            userMessageService.send(email);
        } catch (Throwable e) {
            // Just log, but continue
            log.error(I18n.t("sumaris.error.account.register.sendAdminEmailFailed", e.getMessage()), new SumarisTechnicalException(e));
        }
    }

    protected List<InternetAddress> toInternetAddress(List<String> emails) {
        return emails.stream()
                .map(email -> {
                    try {
                        return new InternetAddress(email);
                    } catch (AddressException e) {
                        log.debug("Invalid email address {" + email + "}: " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void updatePubkey(@NonNull String oldPubkey, @NonNull String newPubkey) {
        userSettingsService.updatePubkey(oldPubkey, newPubkey);
        userEventService.updatePubkey(oldPubkey, newPubkey);
    }
}
