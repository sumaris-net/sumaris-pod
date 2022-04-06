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

import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import it.ozimov.springboot.mail.service.EmailService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.util.social.MessageVO;
import org.nuiton.i18n.I18n;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service("userMessage")
public class UserMessageServiceImpl implements UserMessageService {


    private final SumarisServerConfiguration configuration;
    private final PersonRepository personRepository;
    private final EmailService emailService;

    private InternetAddress emailDefaultFromAddress;
    private boolean emailEnabled = false; // Will be updated by onConfigurationReady()

    public UserMessageServiceImpl(SumarisServerConfiguration configuration,
                                  PersonRepository personRepository,
                                  Optional<EmailService> emailService) {
        this.configuration = configuration;
        this.personRepository = personRepository;
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
        return String.format("[%s]", configuration.getAppName());
    }

    @Override
    public void send(MessageVO message) {


        DefaultEmail.DefaultEmailBuilder emailBuilder = DefaultEmail.builder()
            .subject(getEmailSubjectPrefix() + message.getSubject())
            .body(message.getBody())
            .encoding(StandardCharsets.UTF_8.name());

        send(emailBuilder);
    }

    @Override
    public void send(@NonNull DefaultEmail.DefaultEmailBuilder emailBuilder) {

        try {
            // Build email
            Email email = emailBuilder.build();

            // Send the email
            send(email);
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


    @Override
    public void send(@NonNull Email email) {

        if (emailEnabled) {
            emailService.send(email);
        }
        else {
            // TODO: send as user event
            //
        }
    }

}
