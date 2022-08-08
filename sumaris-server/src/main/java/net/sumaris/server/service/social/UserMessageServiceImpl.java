/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.server.service.social;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import it.ozimov.springboot.mail.service.EmailService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.social.EventLevelEnum;
import net.sumaris.core.model.social.EventTypeEnum;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.social.UserEventService;
import net.sumaris.core.util.StreamUtils;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.social.UserEventVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.exception.InvalidMessageException;
import net.sumaris.server.util.social.MessageTypeEnum;
import net.sumaris.server.util.social.MessageVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.nuiton.i18n.I18n;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service("userMessage")
public class UserMessageServiceImpl implements UserMessageService {


    private final SumarisServerConfiguration configuration;
    private final PersonService personService;
    private final UserEventService userEventService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    private InternetAddress emailDefaultFromAddress;
    private boolean emailEnabled = false; // Will be updated by onConfigurationReady()

    public UserMessageServiceImpl(SumarisServerConfiguration configuration,
                                  PersonService personService,
                                  UserEventService userEventService,
                                  ObjectMapper objectMapper,
                                  Optional<EmailService> emailService) {
        this.configuration = configuration;
        this.personService = personService;
        this.userEventService = userEventService;
        this.objectMapper = objectMapper;
        this.emailService = emailService.orElse(null);
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {

        boolean emailEnable = (emailService != null && configuration.enableMailService());

        // Get mail 'from'
        if (emailEnable) {
            String mailFrom = StringUtils.trimToNull(configuration.getMailFrom());
            if (StringUtils.isBlank(mailFrom)) {
                mailFrom = StringUtils.trimToNull(configuration.getAdminMail());
            }
            if (StringUtils.isBlank(mailFrom)) {
                log.warn(I18n.t("sumaris.error.account.register.mail.disable", SumarisServerConfigurationOption.MAIL_FROM.name()));
                this.emailDefaultFromAddress = null;
                emailEnable = false;
            } else {
                try {
                    this.emailDefaultFromAddress = new InternetAddress(mailFrom, configuration.getAppName());
                } catch (UnsupportedEncodingException e) {
                    log.error(I18n.t("sumaris.error.email.invalid", mailFrom, e.getMessage()));
                    emailEnable = false;
                }
            }
        }

        // Update enable state, if changed
        if (this.emailEnabled != emailEnable) {
            this.emailEnabled = emailEnable;
            if (emailEnable) {
                log.info(I18n.t("sumaris.server.email.started", configuration.getMailHost(), configuration.getMailPort()));
            }
            else {
                log.warn("/!\\ Email service disabled!");
            }
        }
    }

    @Override
    public InternetAddress getEmailDefaultFrom() {
        return this.emailDefaultFromAddress;
    }

    @Override
    public boolean isEmailEnabled() {
        return this.emailEnabled;
    }

    @Override
    public String getEmailSubjectPrefix() {
        return String.format("[%s] ", configuration.getAppName());
    }

    @Override
    public void send(@NonNull MessageVO message) {

        MessageTypeEnum type = MessageTypeEnum.nullToDefault(message.getType(), MessageTypeEnum.INBOX_MESSAGE);
        message.setType(type);

        // Send as email
        if (type == MessageTypeEnum.EMAIL) {
            send(toEmailBuilder(message), true /*allow fallback*/);
        }

        // Send as user event(s)
        else {
            send(toUserEvents(message));
        }
    }

    @Override
    public void send(@NonNull DefaultEmail.DefaultEmailBuilder emailBuilder) {
        send(emailBuilder, false);
    }

    @Override
    public void send(@NonNull Email email) {
        send(email, false);
    }

    public void send(UserEventVO... userEvents) {
        this.send(Arrays.asList(userEvents));
    }


    /* -- protected -- */

    protected void send(@NonNull DefaultEmail.DefaultEmailBuilder emailBuilder, boolean fallbackAsUserEvent) {

        try {
            // Build email
            Email email = emailBuilder.build();

            // Send the email
            send(email, fallbackAsUserEvent);
        } catch(NullPointerException npe) {
            // Missing from: add default, then loop
            if ("from".equals(npe.getMessage())) {
                emailBuilder.from(this.getEmailDefaultFrom());
                send(emailBuilder);
                return;
            }
            throw npe;
        }
    }


    protected void send(@NonNull Email email, boolean fallbackAsUserEvent) {
        if (!emailEnabled) {
            if (fallbackAsUserEvent) send(toUserEvents(email));
            else log.warn("Trying to send an email, but service has been disabled. Skip");
            return;
        }

        int recipientsCount = CollectionUtils.size(email.getTo())
            + CollectionUtils.size(email.getCc())
            + CollectionUtils.size(email.getBcc());
        log.info("Sending email to {} recipients, from {} ...", recipientsCount, email.getFrom());

        emailService.send(email);
    }

    protected void send(@NonNull List<UserEventVO> userEvents) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(userEvents));
        userEvents.forEach(userEventService::save);
    }

    protected List<UserEventVO> toUserEvents(@NonNull MessageVO message) {
        Preconditions.checkNotNull(message.getSubject(), "No subject in the message");
        Preconditions.checkNotNull(message.getType(), "No type in the message");

        // Create a builder
        UserEventVO.UserEventVOBuilder builder = UserEventVO.builder()
            .type(message.getType().toEventType().getLabel())
            .level(EventLevelEnum.INFO.getLabel())
            .creationDate(new Date());

        // Get issuer
        String issuer = null;
        Integer issuerId = message.getIssuerId() != null
            ? message.getIssuerId()
            : (message.getIssuer() != null ? message.getIssuer().getId() : null);
        if (issuerId != null) {
            issuer = personService.getPubkeyById(issuerId);
        }
        else if (message.getIssuer() != null && StringUtils.isNotBlank(message.getIssuer().getPubkey())) {
            // Make sure this pubkey exists, and user is valid
            PersonVO p = personService.getByPubkey(message.getIssuer().getPubkey());

            // Make sure account is enable
            if (!Objects.equals(StatusEnum.ENABLE.getId(), p.getStatusId())) {
                throw new UnauthorizedException();
            }

            issuer = p.getPubkey();
        }

        if (StringUtils.isBlank(issuer))
            throw new DataNotFoundException(I18n.t("sumaris.error.email.noIssuer"));
        builder.issuer(issuer);

        // Get recipients
        Stream<String> recipients;
        Integer recipientId = message.getRecipientId() != null
            ? message.getRecipientId()
            : (message.getRecipient() != null ? message.getRecipient().getId() : null);

        // Only one recipient
        if (recipientId != null) {
            String recipientPubkey = personService.getPubkeyById(recipientId);

            // No pubkey
            if (StringUtils.isBlank(recipientPubkey))
                throw new DataNotFoundException(I18n.t("sumaris.error.person.noPubkey"));

            recipients = Stream.of(recipientPubkey);
        }

        // Many recipients
        else if (ArrayUtils.isNotEmpty(message.getRecipients())) {
            recipients = Arrays.stream(message.getRecipients())
                .map(p -> (StringUtils.isNotBlank(p.getEmail())) ? p.getEmail()
                    : (p.getId() != null ? personService.getEmailById(p.getId()) : null)
                )
                .filter(StringUtils::isNotBlank);
        }

        // Recipients resolved by filter
        else if (message.getRecipientFilter() != null) {
            recipients = personService.findByFilter(message.getRecipientFilter(), null)
                .map(PersonVO::getPubkey)
                .filter(StringUtils::isNotBlank)
                .get();
        }
        else {
            throw new InvalidMessageException(I18n.t("sumaris.error.email.noRecipient"));
        }

        // Set content
        String content = toJsonString(message.getSubject(), message.getBody());
        builder.content(content);

        List<UserEventVO> events = recipients
            .map(recipient -> builder.recipient(recipient).build())
            .collect(Collectors.toList());

        // No recipient found
        if (CollectionUtils.isEmpty(events))
            throw new InvalidMessageException(I18n.t("sumaris.error.email.noRecipient"));


        return events;
    }

    protected DefaultEmail.DefaultEmailBuilder toEmailBuilder(@NonNull MessageVO message) {
        Preconditions.checkNotNull(message.getSubject(), "No subject in the message");

        DefaultEmail.DefaultEmailBuilder builder = DefaultEmail.builder()
            .subject(getEmailSubjectPrefix() + message.getSubject())
            .body(message.getBody())
            .encoding(StandardCharsets.UTF_8.name());

        // Set recipient
        Integer recipientId = message.getRecipientId() != null
            ? message.getRecipientId()
            : (message.getRecipient() != null ? message.getRecipient().getId() : null);
        if (recipientId != null) {

            String email = personService.getEmailById(recipientId);

            // No email address
            if (StringUtils.isBlank(email))
                throw new DataNotFoundException(I18n.t("sumaris.error.person.noEmail"));

            // Parse address
            try {
                builder.to(Lists.newArrayList(new InternetAddress(email)));
            }
            catch (AddressException e) {
                throw new SumarisTechnicalException(ErrorCodes.INTERNAL_ERROR, I18n.t("sumaris.error.email.sendMessageFailed",
                    I18n.t("sumaris.error.email.invalid", email)), e);
            }
        }

        // Many recipients
        else {
            Stream<String> emails;
            if (ArrayUtils.isNotEmpty(message.getRecipients())) {
                emails = Arrays.stream(message.getRecipients())
                    .map(p -> (StringUtils.isNotBlank(p.getEmail())) ? p.getEmail()
                        : (p.getId() != null ? personService.getEmailById(p.getId()) : null)
                    )
                    .filter(StringUtils::isNotBlank);
            }

            else if (message.getRecipientFilter() != null) {
                emails = personService.findEmailsByFilter(message.getRecipientFilter(),null).stream()
                    .filter(StringUtils::isNotBlank);
            }
            else {
                throw new InvalidMessageException(I18n.t("sumaris.error.email.noRecipient"));
            }

            // Parse emails
            List<InternetAddress> recipients = emails.map(recipientAddress -> {
                    try {
                        return new InternetAddress(recipientAddress);
                    } catch (AddressException e) {
                        log.warn(I18n.t("sumaris.error.email.invalid", recipientAddress));
                        return null; // Skip if invalid
                    }
                })
                // Filter invalid address
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // No recipients
            if (CollectionUtils.isEmpty(recipients))
                throw new InvalidMessageException(I18n.t("sumaris.error.email.noRecipient"));

            builder.bcc(recipients);
        }

        return builder;
    }


    protected List<UserEventVO> toUserEvents(@NonNull Email email) {
        UserEventVO.UserEventVOBuilder builder = UserEventVO.builder()
            .type(EventTypeEnum.INBOX_MESSAGE.getLabel())
            .creationDate(new Date());

        // Set content
        String content = toJsonString(email.getSubject(), email.getBody());
        builder.content(content);

        // Resolve recipients, by emails
        Stream<String> recipients;
        if (CollectionUtils.isNotEmpty(email.getTo()) || CollectionUtils.isNotEmpty(email.getCc())
            || CollectionUtils.isNotEmpty(email.getBcc())) {

            recipients = StreamUtils.concat(email.getTo(), email.getCc(),email.getBcc())
                .map(InternetAddress::getAddress)
                .distinct()
                .map(address -> {
                    try {
                        return personService.getByEmail(address);
                    } catch (DataNotFoundException e) {
                        log.warn(e.getMessage() + " - email: " + address);
                        return null; // Skip if invalid
                    }
                })
                .filter(Objects::nonNull)
                .map(PersonVO::getPubkey)
                .filter(StringUtils::isNotBlank);
        }
        else {
            throw new InvalidMessageException(I18n.t("sumaris.error.email.noRecipient"));
        }

        List<UserEventVO> events = recipients
            .map(recipient -> builder.recipient(recipient).build())
            .collect(Collectors.toList());

        // No recipient found
        if (CollectionUtils.isEmpty(events))
            throw new InvalidMessageException(I18n.t("sumaris.error.email.noRecipient"));

        return events;
    }

    protected String toJsonString(@NonNull String subject, String body) {
        try {
            return objectMapper.writeValueAsString(ImmutableMap.of(
                MessageVO.Fields.SUBJECT, subject,
                MessageVO.Fields.BODY, body
            ));
        } catch (JsonProcessingException e) {
            throw new SumarisTechnicalException(e);
        }
    }
}
