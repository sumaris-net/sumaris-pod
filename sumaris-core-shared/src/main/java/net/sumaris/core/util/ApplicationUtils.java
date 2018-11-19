package net.sumaris.core.util;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.config.SumarisConfigurationOption;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ApplicationUtils {

    public static String[] adaptArgsForConfig(String... args) {

        final Pattern optionPattern = Pattern.compile("--([a-zA-Z0-9._]+)=([^ \t]+)");

        List<String> configArgs = ImmutableList.copyOf(args).stream()
                .flatMap(arg -> {
                    Matcher matcher = optionPattern.matcher(arg);
                    if (matcher.matches()) {
                        String name = matcher.group(1);
                        String value = matcher.group(2);
                        // If composite property name (e.g. 'xxx.yyy'): add '--option' before name and value
                        if (name.indexOf(".") != -1) {
                            return ImmutableList.of("--option", name, value).stream();
                        }
                        // If simple property (e.g. 'xxx'), separate name and value (do no add '--option')
                        // See alias defined in SumarisConfiguration
                        else {
                            return ImmutableList.of("--"+name, value).stream();
                        }
                    }
                    return ImmutableList.of(arg).stream();
                }).collect(Collectors.toList());

        return configArgs.toArray(new String[configArgs.size()]);
    }
}
