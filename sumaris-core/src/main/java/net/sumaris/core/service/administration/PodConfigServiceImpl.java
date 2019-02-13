package net.sumaris.core.service.administration;

 import net.sumaris.core.dao.technical.SoftwarePropertyRepository;
 import net.sumaris.core.vo.technical.PropertyVO;
 import net.sumaris.core.vo.administration.user.DepartmentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
 import java.util.Optional;


@Component("podConfigService")
public class PodConfigServiceImpl implements PodConfigService {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SoftwarePropertyRepository softwareProperty;


    public PodConfigServiceImpl() { }


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


    @Override
    public List<DepartmentVO> listDepartments() {
        return departmentService.findByFilter(null, 0, 100, null, null);
    }

    @Override
    public List<String> listBackgrounds() {
        return new ArrayList<>();
    }

    @Override
    public List<PropertyVO> propertiesVO(String name) {
        return softwareProperty.propertiesVO(name);
    }

}
