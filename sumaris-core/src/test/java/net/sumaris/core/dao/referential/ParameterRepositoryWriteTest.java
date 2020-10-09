package net.sumaris.core.dao.referential;

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.pmfm.ParameterRepository;
import net.sumaris.core.vo.referential.ParameterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author peck7 on 19/08/2020.
 */
public class ParameterRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ParameterRepository parameterRepository;

    @Test
    public void saveParameter() {

        ParameterVO vo = new ParameterVO();
        vo.setLabel("PARAM_TEST_1");
        vo.setName("parameter for test");
        vo.setType("string");
        vo.setStatusId(1);
        parameterRepository.save(vo);

    }

    @Test
    public void saveParameterWithQualitativeValues() {

        ParameterVO vo = new ParameterVO();
        vo.setLabel("PARAM_TEST_QV");
        vo.setName("parameter for test");
        vo.setType("QUALITATIVE_VALUE");
        vo.setStatusId(1);

        List<ReferentialVO> qvs = new ArrayList<>();
        ReferentialVO newQv1 = new ReferentialVO();
        newQv1.setLabel("QV_TEST_1");
        newQv1.setName("qualitative value test 1");
        newQv1.setStatusId(1);
        qvs.add(newQv1);
        ReferentialVO newQv2 = new ReferentialVO();
        newQv2.setLabel("QV_TEST_2");
        newQv2.setName("qualitative value test 2");
        newQv2.setStatusId(1);
        qvs.add(newQv2);
        vo.setQualitativeValues(qvs);

        parameterRepository.save(vo);

    }
}
