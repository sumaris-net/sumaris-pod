package net.sumaris.core.action;

/*-
 * #%L
 * Sumaris3 Core :: Sumaris3 Core Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.config.ApplicationConfigHelper;
import org.nuiton.config.ApplicationConfigProvider;
import org.nuiton.config.ConfigActionDef;
import org.nuiton.i18n.I18n;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * <p>ActionUtils class.</p>
 */
public class ActionUtils {
    /* Logger */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ActionUtils.class);

    /**
     * <p>Constructor for ActionUtils.</p>
     */
    protected ActionUtils() {
        // helper class
    }

    /**
     * <p>logConnectionProperties.</p>
     */
    public static void logConnectionProperties() {
        if (!log.isInfoEnabled()) {
            return;
        }
        SumarisConfiguration config = SumarisConfiguration.getInstance();
        boolean isFileDatabase = Daos.isFileDatabase(config.getJdbcURL());
        if (isFileDatabase) {
            log.info(String.format(" Database directory: %s", config.getDbDirectory()));
        }
        log.info(String.format(" JDBC Driver: %s", config.getJdbcDriver()));
        log.info(String.format(" JDBC URL: %s", config.getJdbcURL()));
        log.info(String.format(" JDBC Username: %s", config.getJdbcUsername()));
        String jdbcCatalog = config.getJdbcCatalog();
        if (StringUtils.isNotBlank(jdbcCatalog)) {
            log.info(String.format(" JDBC Catalog: %s", jdbcCatalog));
        }
        String jdbcSchema = config.getJdbcSchema();
        if (StringUtils.isNotBlank(jdbcSchema)) {
            log.info(String.format(" JDBC Schema: %s", jdbcSchema));
        }
    }

    /**
     * Check output file or directory validity
     *
     * @param isDirectory a boolean.
     * @param actionClass a {@link Class} object.
     * @return a {@link File} object.
     */
    public static File checkAndGetOutputFile(boolean isDirectory,
                                             Class<?> actionClass) {

        SumarisConfiguration config = SumarisConfiguration.getInstance();
        
        File output = config.getCliOutputFile();
        if (output == null) {
            log.error(I18n.t("sumaris.action.noOutput.error", "--output [...]", getActionAlias(actionClass)));
            System.exit(-1);
        }
        
        if (output.exists()) {
            if (isDirectory) {
                if (!output.isDirectory()) {
                    log.error(I18n.t("sumaris.action.outputNotADirectory.error", output.getPath()));
                    System.exit(-1);
                }
                else if (ArrayUtils.isNotEmpty(output.listFiles())) {
                    // Could be force, so delete the directory
                    if (config.isCliForceOutput() || !config.isProduction()) {
                        log.info(I18n.t("sumaris.action.deleteOutputDirectory", output.getPath()));
                        try {
                            FileUtils.deleteDirectory(output);
                        } catch (IOException e) {
                            log.error(e.getMessage());
                            System.exit(-1);
                        }
                    }
                    else {
                        log.error(I18n.t("sumaris.action.outputNotEmptyDirectory.error", output.getPath()));
                        System.exit(-1);
                    }
                }
            }
            else {
                // Could be force, so delete the file
                if (config.isCliForceOutput() || !config.isProduction()) {
                    log.info(I18n.t("sumaris.action.deleteOutputFile", output.getPath()));
                    try {
                        FileUtils.forceDelete(output);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        System.exit(-1);
                    }
                }
                else {
                    log.error(I18n.t("sumaris.action.outputNotAFile.error", output.getPath()));
                    System.exit(-1);
                }
            }
        }
        
        return output;
    }
    
    /**
     * <p>getActionAlias.</p>
     *
     * @param clazz a {@link Class} object.
     * @return a {@link String} object.
     */
    public static String getActionAlias(Class<?> clazz) {
        ConfigActionDef actionDefFound = null;
        
        // Retrieve the configActionDef for the given class
        Set<ApplicationConfigProvider> providers =
                ApplicationConfigHelper.getProviders(null,
                        null,
                        null,
                        true);
        String classname = clazz.getName();
        for(ApplicationConfigProvider provider: providers) {
            ConfigActionDef[] actionDefs = provider.getActions();
            if (ArrayUtils.isNotEmpty(actionDefs)) {
                for (ConfigActionDef actionDef:actionDefs) {
                    if (actionDef.getAction() != null
                            && actionDef.getAction().startsWith(classname)) {
                        actionDefFound = actionDef;
                        break;
                    }
                }
                if (actionDefFound != null) {
                    break;
                }
            }
        }
        
        // If a config action def exists, return the first alias
        if (actionDefFound != null) {
            String[] alias = actionDefFound.getAliases();
            if (ArrayUtils.isNotEmpty(alias)) {
                return alias[0];
            }
        }
        return I18n.t("sumaris.action.current");
    }

    public static boolean checkValidConnection() {
        SumarisConfiguration config = SumarisConfiguration.getInstance();

        ActionUtils.logConnectionProperties();

        boolean isValidConnection = Daos.isValidConnectionProperties(config.getJdbcDriver(),
                config.getJdbcURL(),
                config.getJdbcUsername(),
                config.getJdbcPassword());

        if (!isValidConnection) {
            log.warn("Connection error: invalid connection.");
            return false;
        }

        return true;
    }
}
