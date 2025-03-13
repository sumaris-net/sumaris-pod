package net.sumaris.core.util;

/*-
 * #%L
 * SUMARiS:: Core shared
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

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ApplicationUtils {

    /**
     * Transform args as expected by nuiton-config.
     * <p>
     * E.g.
     * <ul>
     * <li>the argument '--key=value' will become '--key value'</li>
     * <li>the argument '--key.path=value' will become '--option key.path
     * value'</li>
     * </ul>
     * </p>
     * 
     * @param args
     * @return
     */
    public static String[] toApplicationConfigArgs(String... args) {

        final Pattern optionPattern = Pattern.compile("(--?)([a-zA-Z0-9._]+)=([^ \t]+)");

        List<String> configArgs = ImmutableList.copyOf(args).stream()
                .flatMap(arg -> {
                    Matcher matcher = optionPattern.matcher(arg);
                    if (matcher.matches()) {
                        String prefix = matcher.group(1);
                        String name = matcher.group(2);
                        String value = matcher.group(3);
                        // If composite property name (e.g. 'xxx.yyy'): add '--option' before name and
                        // value
                        if (name.contains(".")) {
                            return ImmutableList.of("--option", name, value).stream();
                        }
                        // If simple property (e.g. 'xxx'), separate name and value (do no add
                        // '--option')
                        // See alias defined in SumarisConfiguration
                        else {
                            return ImmutableList.of(prefix + name, value).stream();
                        }
                    }
                    return ImmutableList.of(arg).stream();
                }).collect(Collectors.toList());

        return configArgs.toArray(new String[configArgs.size()]);
    }
}
