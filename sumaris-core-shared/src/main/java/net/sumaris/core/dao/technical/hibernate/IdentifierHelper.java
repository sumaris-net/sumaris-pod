package net.sumaris.core.dao.technical.hibernate;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.model.naming.Identifier;

public class IdentifierHelper {

    protected IdentifierHelper() {
        // helper class
    }

    public static Identifier normalize(Identifier identifier) {
        if (identifier == null || StringUtils.isBlank(identifier.getText())) {
            return identifier;
        }

        // Replace case change by an underscore
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        String newName = identifier.getText().replaceAll(regex, replacement).toLowerCase();

        // change to lower case
        newName = newName.toLowerCase();

        return new Identifier(newName, identifier.isQuoted());
    }
}
