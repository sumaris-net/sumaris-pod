package net.sumaris.server.service.technical;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Helper functions to convert Java Bean / Spring Jpa objects  to Jena  model
 * <p>
 * the methods described here are mostly generic, business code and objects are being passed as parameters
 */
public class RDFHelpers {

    private static final Logger LOG = LogManager.getLogger();

    // XML is default
    public static final Map<String, String> FORMATS = new HashMap<>();

    static {
        FORMATS.put("json", "JSON-LD");
        FORMATS.put("ttl", "TURTLE");
        FORMATS.put("ntriple", "N-TRIPLE");
        FORMATS.put("n3", "N3");
    }


    static Stream<Annotation> annotsOfField(Optional<Field> field) {
        return field.map(field1 -> Stream.of(field1.getAnnotations())).orElseGet(Stream::empty);
    }

    /**
     * check the getter and its corresponding field's annotations
     *
     * @param met the getter method to test
     * @return true if it is a technical id to exclude from the model
     */
    static boolean isId(Method met) {
        return "getId".equals(met.getName())
                && Stream.concat(annotsOfField(getFieldOfGetter(met)), Stream.of(met.getAnnotations()))
                .anyMatch(annot -> annot instanceof Id || annot instanceof org.springframework.data.annotation.Id);
    }

    static boolean isManyToOne(Method met) {
        return annotsOfField(getFieldOfGetter(met)).anyMatch(annot -> annot instanceof ManyToOne) // check the corresponding field's annotations
                ||
                Stream.of(met.getAnnotations()).anyMatch(annot -> annot instanceof ManyToOne)  // check the method's annotations
                ;
    }


    static boolean isGetter(Method met) {
        return met.getName().startsWith("get") // only getters
                && !"getBytes".equals(met.getName()) // ignore ugly
                && met.getParameterCount() == 0 // ignore getters that are not getters
                && getFieldOfGetter(met).isPresent()
                ;
    }


    static Method getterOfField(Class t, String field) {
        try {
            Method res = t.getMethod("get" + field.substring(0, 1).toUpperCase() + field.substring(1));
            LOG.info("Declared ALLOWED_MANY_TO_ONE :\n " + res);
            return res;
        } catch (NoSuchMethodException e) {
            LOG.error("error in the declaration of allowed ManyToOne " + e.getMessage());
        }
        return null;
    }

    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    public static String doWrite(Model model, String format) {

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            if (format == null) {
                model.write(os);
            } else {
                model.write(os, format);
            }
            return os.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    static Optional<Field> getFieldOfGetter(Method getter) {

        String fieldName = getter.getName().substring(3, 4).toLowerCase() + getter.getName().substring(4);
        //LOG.info("searching field : " + fieldName);
        try {
            return Optional.of(getter.getDeclaringClass().getDeclaredField(fieldName));
        } catch (Exception e) {
            //LOG.error("field not found : " + fieldName + " for class " + getter.getDeclaringClass() + "  " + e.getMessage());
            return Optional.empty();
        }
    }


    static boolean isGenericJava(Method getter) {
        return Stream.of(java.util.Date.class.getPackage(), java.lang.Integer.class.getPackage())
                .anyMatch(x -> getter.getReturnType().getPackage().equals(x));

    }

    /**
     * Performs a forced cast.
     * Returns null if the collection type does not match the items in the list.
     *
     * @param data     The list to cast.
     * @param listType The type of list to cast to.
     */
    static <T> List<? super T> castListSafe(List<?> data, Class<T> listType) {
        List<T> retval = null;
        //This test could be skipped if you trust the callers, but it wouldn't be safe then.
        if (data != null && !data.isEmpty() && listType.isInstance(data.iterator().next().getClass())) {
            LOG.info("  - castListSafe passed check ");

            @SuppressWarnings("unchecked")//It's OK, we know List<T> contains the expected type.
                    List<T> foo = (List<T>) data;
            return foo;
        }

        LOG.info("  - castListSafe failed check  forcing it though");
        return (List<T>) data;
    }


}
