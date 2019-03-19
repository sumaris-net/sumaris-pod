package net.sumaris.server.service.technical.rdf;


import net.sumaris.server.config.O2BConfig;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfxml.xmlinput.JenaReader;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static net.sumaris.server.service.technical.rdf.Helpers.delta;

public class Synchro implements OwlMappers {
    Logger LOG =  LoggerFactory.getLogger(Synchro.class);



    public OntModel overwriteFromRemote(String url, String ontIRI) {

        long start = System.nanoTime();

        OntModel m = ModelFactory.createOntologyModel();
        new JenaReader().read(m, url);
        LOG.info("Found " + m.size() + " triples remotely, reconstructing model now " + delta(start));

        List<? extends Object> recomposed = objectsFromOnt(m);
        LOG.info("Mapped ont to list of " + recomposed.size() + " objects, Making it OntClass again " + delta(start));



        Stream<Class> classes = Stream.of();

        OntModel m2 =  ontOfClasses(ontIRI, classes, new HashMap<>());

        recomposed.forEach(r -> bean2Owl(m2, r, 2));

        LOG.info("Recomposed list of Object is " + m2.size() + " triple  " + delta(start) + " - " + (100.0 * m2.size() / m.size()) + "%");

        if (m.size() == m2.size()) {
            LOG.info(" QUANTITATIVE SUCCESSS   ");
            if (m.isIsomorphicWith(m2)) {
                LOG.info(" ISOMORPHIC SUCCESS " + delta(start));

            }
        }

        // recomposed.forEach(obj->getEntityManager().persist(obj));

        return m2;
    }

    EntityManager em;

    @Override
    public EntityManager getEntityManager() {
        return em;
    }
}
