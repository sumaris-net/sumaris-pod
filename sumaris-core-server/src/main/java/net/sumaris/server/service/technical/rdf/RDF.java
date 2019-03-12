package net.sumaris.server.service.technical.rdf;


import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface RDF {


    String namespace() default "";

    String label() default "";

    String description() default "";

    String[] sameAs() default {} ;
}

