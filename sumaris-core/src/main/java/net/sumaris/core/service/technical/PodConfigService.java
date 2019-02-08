package net.sumaris.core.service.technical;

import net.sumaris.core.model.system.SystemVersion;
import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Optional;

@Component("podConfigService")
public class PodConfigService {

    @Autowired
    private DepartmentService departmentService;

    private UserSettingsVO user;

    private List<String> backGroundImages;

    private String cssTheme ;

    private String podURL;

    @Autowired
    private SystemVersion systemVersion;

   public PodConfigService(){
       podURL = whatsMyIp().orElse("couldn't find IP ");

    }



    /**
     * Auto detect IP
     *
     * @return the IP address or null
     */
    @Bean
    private Optional<String> whatsMyIp() {

        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            return Optional.of(in.readLine());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
