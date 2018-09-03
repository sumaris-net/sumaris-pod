package net.sumaris.pod.core;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Plugin extends org.elasticsearch.plugins.Plugin {

    private static final Logger log = LoggerFactory.getLogger(Plugin.class);

    @Inject
    public Plugin(Settings settings) {
        log.warn("STARTING plugin.....");
    }
}
