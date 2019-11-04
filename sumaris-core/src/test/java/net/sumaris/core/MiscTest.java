package net.sumaris.core;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author peck7 on 30/10/2019.
 */
public class MiscTest {

    @Test
    public void testMapByRegEx() {

        Map<String, String> source = ImmutableMap.<String, String>builder()
                .put("sumaris.userProfile.ADMIN.label", "ALLEGRO_ADMINISTRATEUR")
                .put("sumaris.userProfile.USER.label", "ALLEGRO_UTILISATEUR")
                .put("sumaris.userProfile.SUPERVISOR.label", "ALLEGRO_SUPER_UTILISATEUR")
                .put("sumaris.userProfile.GUEST.label", "SIH_AUTRE")
                .build();

        Map<String, String> target = new HashMap<>();

        Pattern pattern = Pattern.compile("sumaris.userProfile.(\\w+).label");
        source.forEach((key, value) -> {
            Matcher matcher = pattern.matcher(key);
            if (matcher.find())
                target.put(matcher.group(1), value);
        });

        System.out.println(target);

    }
}
