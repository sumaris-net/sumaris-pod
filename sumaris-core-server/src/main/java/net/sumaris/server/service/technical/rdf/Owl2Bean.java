package net.sumaris.server.service.technical.rdf;

import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public interface Owl2Bean extends Helpers {
    /**
     * Logger.
     */
    Logger LOG = LoggerFactory.getLogger(Owl2Bean.class);


    default boolean classEquals(Class c, Class<?> d) {
        return Objects.equals(d.getTypeName(), c.getTypeName());
    }


    /**
     * try to convert remote entity to a local one automatically
     *
     * @param val
     * @param setterParam
     * @param obj
     * @return
     */
    default Object getTranslatedReference(RDFNode val, Class<?> setterParam, Object obj) {


        String identifier = val.toString();
        String ontClass = null;
        if (identifier.contains("#")) {
            ontClass = identifier.substring(0, identifier.indexOf("#"));
            identifier = identifier.substring(identifier.indexOf("#") + 1);
        }
        if (setterParam == TaxonomicLevel.class) {

            LOG.warn("TaxonomicLevel " + getCacheTL().size());
            for (Object ctl : getCacheTL()) {
                String lab = ((TaxonomicLevel) ctl).getLabel();
                if (identifier.endsWith(lab)) {
                    return ctl;
                }
            }


            // if none were cached, create a new TaxonomicLevel
            TaxonomicLevel tl = (TaxonomicLevel) URI_2_OBJ_REF.getOrDefault(val.toString(), new TaxonomicLevel());
            tl.setLabel(identifier);
            tl.setCreationDate(new Date());
            tl.setName("");
            tl.setRankOrder(1);
            tl.setStatus((Status) getCacheStatus().get(0));

            getEntityManager().persist(tl);
            //B2O_ARBITRARY_MAPPER.get(ontClass).apply( val.as(OntResource.class));

            return URI_2_OBJ_REF.putIfAbsent(val.toString(), tl);
        }


        // Default case, try to fetch reference (@Id) as Integer or String
        LOG.warn("mapping getEntityManager().getReference " + identifier + " - " + val + " - " + obj);
        Object ref;
        try {
            Integer asInt = Integer.parseInt(identifier);
            ref = getEntityManager().getReference(setterParam, asInt);
        } catch (NumberFormatException e) {
            ref = getEntityManager().getReference(setterParam, identifier);
        }
        return ref;
    }

    default String attributeOf(String pred) {

        String fName = pred.substring(pred.indexOf("#") + 1);
        fName = fName.substring(0, 1).toLowerCase() + fName.substring(1);
        return fName;
    }


    default void fillObjectWithStdAttribute(Method setter, Object obj, RDFNode val) {
        Class<?> setterParam = setter.getParameterTypes()[0];
        try {
            if (classEquals(setterParam, String.class)) {
                setter.invoke(obj, val.asLiteral().getString());
            }
            if (classEquals(setterParam, Long.class) || classEquals(setterParam, long.class)) {
                setter.invoke(obj, val.asLiteral().getLong());
            } else if (classEquals(setterParam, Integer.class) || classEquals(setterParam, int.class)) {
                setter.invoke(obj, Integer.parseInt(val.toString()));
            } else if (classEquals(setterParam, Date.class)) {
                setter.invoke(obj, sdf.parse(val.asLiteral().getString()));
            } else if (classEquals(setterParam, Boolean.class) || classEquals(setterParam, boolean.class)) {
                setter.invoke(obj, val.asLiteral().getBoolean());
            }
        } catch (Exception e) {
            LOG.warn("fillObjectWithStdAttribute couldnt reconstruct attribute " + setterParam + " for val " + val, e);
        }
    }

    default Optional<Object> owl2Bean(Resource ont, OntResource ontResource, Class clazz) {
        LOG.info("processing ont Instance " + ontResource + " - " +
                ontResource
                        .asIndividual()
                        .listProperties().toList().size());

        try {
            Object obj = clazz.newInstance();

            ontResource
                    .asIndividual()
                    .listProperties()
                    .toList()
                    .forEach(stmt -> {
                        String pred = stmt.getPredicate().getURI();
                        RDFNode val = stmt.getObject();
                        if ((pred.startsWith(MY_PREFIX) || pred.startsWith(ADAGIO_PREFIX)) && pred.contains("#")) {
                            String fName = attributeOf(pred);
                            try {
                                Optional<Method> setter = findSetterAnnotatedID(ont, clazz);

                                if (!setter.isPresent()) {
                                    setter = setterOfField(ont, clazz, fName);
                                }

                                if (!setter.isPresent()) {
                                    return;
                                }

                                Class<?> setterParam = setter.get().getParameterTypes()[0];
                                // LOG.info("trying to insert  " + fName + " => " + val + " using method " + me + " !!  " + huhu);

                                if (isJavaType(setterParam)) {
                                    fillObjectWithStdAttribute(setter.get(), obj, val);
                                } else {
                                    //FIXME if entity  is different we shouldn't use the invoked method
                                    setter.get().invoke(obj, getTranslatedReference(val, setterParam, obj));
                                }
                                //myAccessor.setPropertyValue(fName,safeCastRDFNode(val, fName,clazz));
                            } catch (Exception e) {
                                LOG.error(e.getClass().getSimpleName() + " on field " + fName + " => " + val + " using class " + clazz + " using method " + setterOfField(ont, clazz, fName) + " " + e.getMessage(), e);
                            }

                            //values.put(fName, safeCastRDFNode(val, fName, clazz));
                        }

                    });
            //getEntityManager().merge(obj);
            LOG.info("  - created object " + ontResource + " - " + " of class " + ontResource.getClass() + "  - ");
            return Optional.of(obj);
        } catch (Exception e) {
            LOG.error(" processing individual " + ontResource + " - " + clazz, e);
        }
        return Optional.empty();
    }


    default Optional<Method> findSetterAnnotatedID(Resource ont, Class clazz) {
        for (Field f : clazz.getDeclaredFields())
            for (Annotation an : f.getDeclaredAnnotations())
                if (an instanceof Id) {
                    return setterOfField(ont, clazz, f.getName());

                }
        return Optional.empty();
    }

}
