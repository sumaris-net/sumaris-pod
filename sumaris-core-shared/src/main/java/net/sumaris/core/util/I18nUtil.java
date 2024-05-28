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

package net.sumaris.core.util;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.io.FileUtils;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.I18nLanguage;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class I18nUtil extends org.nuiton.i18n.I18nUtil {
    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(I18nUtil.class);

    protected I18nUtil() {
        // Helper class
    }

    public static Optional<Locale> findI18nLocale(@NonNull String localeStr) {
        // try full match (with country)
        Locale result = ArrayUtils.stream(I18n.getStore().getLanguages())
                .map(I18nLanguage::getLocale)
                .filter(l -> localeStr.equalsIgnoreCase(l.toString()))
                .findFirst().orElse(null);
        if (result != null) return Optional.of(result);

        // retry without country
        if (localeStr.indexOf('_') == 2) {
            final String language = localeStr.substring(0 ,2);
            result = ArrayUtils.stream(I18n.getStore().getLanguages())
                    .map(I18nLanguage::getLocale)
                    .filter(l -> language.equalsIgnoreCase(l.getLanguage()))
                    .findFirst().orElse(null);
            if (result != null) return Optional.of(result);
        }

        return Optional.empty();
    }

    public static Optional<Locale> findFirstI18nLocale(@NonNull String str) {

        // Compute lower case list
        List<Locale> locales = ImmutableList.copyOf(I18nUtil.parseLocales(str));

        // Find on locales (with country)
        Locale result = ArrayUtils.stream(I18n.getStore().getLocales())
                .filter(locales::contains)
                .findFirst().orElse(null);
        if (result != null) return Optional.of(result);

        // Find on language (without country)
        List<String> languages = locales.stream()
                .map(Locale::getLanguage) // Extract 2 first characters
                .collect(Collectors.toList());
        result = java.util.Arrays.stream(I18n.getStore().getLocales())
                .filter(l -> languages.contains(l.getLanguage()))
                .findFirst().orElse(null);
        if (result != null) return Optional.of(result);

        return Optional.empty();
    }


    public static Locale toI18nLocale(@NonNull String localeStr) {
        return findI18nLocale(localeStr).orElse(Locale.UK);
    }

    /**
     * <p>
     * init I18n.
     * </p>
     *
     * @throws IOException
     *             if any.
     */
    public static void init(String bundleName) {
        init(SumarisConfiguration.getInstance(), bundleName);
    }

    /**
     * <p>
     * init I18n.
     * </p>
     *
     * @throws IOException
     *             if any.
     */
    public static void init(SumarisConfiguration config, String bundleName) {
        try {

            File i18nDirectory = new File(config.getDataDirectory(), "i18n");
            if (i18nDirectory.exists()) {
                // clean i18n cache
                FileUtils.cleanDirectory(i18nDirectory);
            }

            FileUtils.forceMkdir(i18nDirectory);

            Locale i18nLocale = config.getI18nLocale();

            if (log.isInfoEnabled()) {
                log.info(String.format("Starts i18n with locale {%s} at {%s}",
                        i18nLocale, i18nDirectory));
            }
            I18n.init(new UserI18nInitializer(
                            i18nDirectory, new DefaultI18nInitializer(bundleName)),
                    i18nLocale);
        } catch (IOException e) {
            throw new SumarisTechnicalException("i18n initialization failed", e);
        }
    }
}
