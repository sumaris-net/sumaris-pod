package net.sumaris.server.http.security;

import org.springframework.security.access.annotation.Secured;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author peck7 on 04/12/2018.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Secured({ "ROLE_USER", "ROLE_SUPERVISOR", "ROLE_ADMIN" })
public @interface IsEditor {
}
