package net.sumaris.server.http.rest;

import net.sumaris.server.service.technical.rdf.OpenDataController;
import org.apache.jena.ontology.OntModel;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping(value = "/jena")
@ConditionalOnProperty(name="sumaris.rdf.enable", havingValue = "true")
public class RDFRestController extends OpenDataController {

    private static final Logger LOG = LoggerFactory.getLogger(RDFRestController.class);

    @Autowired
    private EntityManager entityManager;

    private OntModel ontologyOfJPAEntities;

    private OntModel ontologyOfVOPackages;

    private OntModel ontologyOfModule;


    @PostConstruct
    public void buildOntologies() {
        initModule();

        Map<String, String> opts = new HashMap<>();
        opts.put("disjoints", "false");
        opts.put("methods", "false");




        ontologyOfJPAEntities = ontOfCapturedClasses(MY_PREFIX + "entities",
                Reflections.collect().getTypesAnnotatedWith(Entity.class).stream(),
                opts);


        opts.put("packages", "net.sumaris.core.vo");
        ontologyOfVOPackages = ontOfPackage(MY_PREFIX + "vo", opts);


        opts.put("methods", "true");
        opts.put("packages", "net.sumaris.server.service.technical.rdf");
        ontologyOfModule = ontOfPackage(MY_PREFIX + "module", opts);


        LOG.info("Loaded Ontoglogies : " +
                " JPA => " + ontologyOfJPAEntities.size() +
                " VO => " + ontologyOfVOPackages.size() +
                " O2B => " + ontologyOfModule.size());

    }


    public OntModel onto(String name, Map<String, String> opts) {

        switch (name) {
            case "vo":
                return ontologyOfVOPackages;
            case "entities":
                return ontologyOfJPAEntities;
            case "module":
                return ontologyOfModule;
        }


        return ontOfPackage(MY_PREFIX + name, opts);
    }


    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }
}