package net.sumaris.server.service.technical.rdf;


import net.sumaris.core.model.referential.IItemReferentialEntity;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfxml.xmlinput.JenaReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static net.sumaris.server.service.technical.rdf.Helpers.delta;

public interface Synchro extends OwlMappers {
    Logger LOG = LoggerFactory.getLogger(Synchro.class);

    String BASE_SYNCHRONIZE_MODEL_PACKAGE = "net.sumaris.core.model.referential";


    default OntModel overwriteFromRemote(String url, String ontIRI) {

        long start = System.nanoTime();

        OntModel model = ModelFactory.createOntologyModel();
        new JenaReader().read(model, url);
        LOG.info("Found " + model.size() + " triples remotely, reconstructing model now " + delta(start));

        List<? extends Object> recomposed = objectsFromOnt(model);
        LOG.info("Mapped ont to list of " + recomposed.size() + " objects, Making it OntClass again " + delta(start));


        Stream<Class> classes = Stream.of();

        OntModel m2 = ontOfClasses(ontIRI, classes, new HashMap<>());

        recomposed.forEach(r -> bean2Owl(m2, r, 2));

        LOG.info("Recomposed list of " + recomposed.size() + " objects is " + m2.size() + " triples.  " + delta(start) + " - " + (100.0 * m2.size() / model.size()) + "%");
        LOG.info("Recomposed list of " + recomposed.size() + " objects is " + m2.size() + " triples.  " + delta(start) + " - " + (100.0 * m2.size() / model.size()) + "%");

        if (model.size() == m2.size()) {
            LOG.info(" QUANTITATIVE SUCCESSS   ");
            if (model.isIsomorphicWith(m2)) {
                LOG.info(" ISOMORPHIC SUCCESS " + delta(start));

            }
        }
//        recomposed.forEach(obj -> {
//            try {
//                if(obj instanceof IItemReferentialEntity)
//                    ((IItemReferentialEntity)obj).setId(null);
//                getEntityManager().persist(obj);
//            } catch (Exception e) {
//                LOG.warn("didnt save "+obj+"  "+ e.getMessage());
//            }
//        });

        getEntityManager().flush();

        return m2;
    }


}
