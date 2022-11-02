package net.sumaris.core.util;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.*;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * <p>Springs resources helper class.</p>
 */
@Slf4j
public class ResourceUtils {


	protected ResourceUtils() {
		// Helper class : do not instantiate
	}
	
	/**
	 * <p>getResourcesFromPaths.</p>
	 *
	 * @param paths an array of {@link String} objects.
	 * @param appContext a {@link ApplicationContext} object.
	 * @param checkIfResourceExists a boolean.
	 * @return a {@link List} object.
	 * @throws IOException if any.
	 */
	public static List<Resource> getResourcesFromPaths(String[] paths, ApplicationContext appContext, boolean checkIfResourceExists) throws IOException {
		Preconditions.checkArgument(ArrayUtils.isNotEmpty(paths));
		Preconditions.checkNotNull(appContext);

		// For each path, retrieve corresponding resources
		List<Resource> resources = Lists.newArrayList();
		for (String path : paths) {
	        try {
	        	Resource[] pathResources = appContext.getResources(path);
		        resources.addAll(Arrays.asList(pathResources));
	        } catch (IOException e) {
	        	throw new IOException(String.format("Error while getting files from path: %s", path), e);
	        }
		}

        // Check if all resources exists
        if (checkIfResourceExists) {
	    	for(Resource resource : resources) {
	    		if (!resource.exists()) {
	    			throw new FileNotFoundException(String.format("File not found: %s", resource.getFilename()));
	    		}
	    	}
        }

        return resources;
	}


    /**
     * <p>getResource.</p>
     *
     * @param location a {@link String} object.
     * @return a {@link Resource} object.
     */
    public static Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");
        if (location.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length()), getClassLoader());
        }
        else {
            try {
                // Try to parse the location as a URL...
                URL url = new URL(location);
                return new UrlResource(url);
            }
            catch (MalformedURLException ex) {
                // No URL -> resolve as resource path.
                return getResourceByPath(location);
            }
        }
    }

    public static byte[] readAllBytes(Resource resource) throws IOException {
        try(InputStream is = new BufferedInputStream(resource.getInputStream())) {
            return Files.readAllBytes(is);
        }
    }

    public static String readContent(Resource resource, Charset charset) throws IOException {
        try(InputStream is = new BufferedInputStream(resource.getInputStream())) {
            return Files.readContent(is, charset);
        }
    }

    public static Optional<Resource> findResource(String location) {
        Resource resource = getResource(location);
        if (resource.exists()) return Optional.of(resource);
        return Optional.empty();
    }

    /* -- protected functions -- */

    /**
     * <p>getResourceByPath.</p>
     *
     * @param path a {@link String} object.
     * @return a {@link Resource} object.
     */
    protected static Resource getResourceByPath(String path) {
        return new ClassPathContextResource(path, getClassLoader());
    }

    /**
     * <p>getClassLoader.</p>
     *
     * @return a {@link ClassLoader} object.
     */
    protected static ClassLoader getClassLoader() {
        return ClassUtils.getDefaultClassLoader();
    }
    
    protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

        public ClassPathContextResource(String path, ClassLoader classLoader) {
            super(path, classLoader);
        }

        @Override
        public String getPathWithinContext() {
            return getPath();
        }

        @Override
        public Resource createRelative(String relativePath) {
            String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
            return new ClassPathContextResource(pathToUse, getClassLoader());
        }
    }

}
