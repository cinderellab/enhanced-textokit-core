
/*
 *    Copyright 2015 Textocat
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.textocat.textokit.morph.dictionary;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * @author Rinat Gareev
 */
public class MorphDictionaryAPIFactory {

    private static final Logger log = LoggerFactory.getLogger(MorphDictionaryAPIFactory.class);
    private static MorphDictionaryAPI defaultApi;

    /**
     * Return the only implementation of {@link MorphDictionaryAPI} that can be
     * found in the classpath using a default (thread-context) class-loader.
     *
     * @return API instance, never null
     * @throws IllegalStateException <ul>
     *                               <li>If there are several implementations (more than one)
     *                               available in the classpath.
     *                               <li>If any implementation can not be found.
     *                               <li>If errors occur during the scan for implementations.
     *                               </ul>
     */
    public static MorphDictionaryAPI getMorphDictionaryAPI() {
        if (defaultApi == null) {
            initialize();
        }
        return defaultApi;
    }

    private static synchronized void initialize() {
        if (defaultApi != null) {
            return;
        }
        log.info("Searching for MorphDictionaryAPI implementations...");
        Set<String> implClassNames = Sets.newHashSet();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            for (Resource implDeclRes : resolver
                    .getResources("classpath*:META-INF/uima-ext/morph-dictionary-impl.txt")) {
                InputStream is = implDeclRes.getInputStream();
                try {
                    String implClassName = IOUtils.toString(is, "UTF-8").trim();
                    if (!implClassNames.add(implClassName)) {
                        throw new IllegalStateException(
                                String.format(
                                        "The classpath contains duplicate declaration of implementation '%s'. "
                                                + "Last one has been read from %s.",
                                        implClassName, implDeclRes.getURL()));
                    }
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (implClassNames.isEmpty()) {
            throw new IllegalStateException(
                    String.format("Can't find an implementation of MorphDictionaryAPI"));
        }
        if (implClassNames.size() > 1) {
            throw new IllegalStateException(
                    String.format("More than one implementations have been found:\n%s\n"
                                    + "Adjust the app classpath or get an implementation by ID.",
                            implClassNames));
        }
        String implClassName = implClassNames.iterator().next();
        log.info("Found MorphDictionaryAPI implementation: {}", implClassName);
        try {
            @SuppressWarnings("unchecked")
            Class<? extends MorphDictionaryAPI> implClass = (Class<? extends MorphDictionaryAPI>)
                    Class.forName(implClassName);
            defaultApi = implClass.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Can't instantiate the MorphDictionaryAPI implementation", e);
        }
    }

    private MorphDictionaryAPIFactory() {
    }
}