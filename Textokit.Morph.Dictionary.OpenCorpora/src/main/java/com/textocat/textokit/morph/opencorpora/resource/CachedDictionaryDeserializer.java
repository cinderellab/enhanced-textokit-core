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

package com.textocat.textokit.morph.opencorpora.resource;

import com.google.common.base.Objects;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionary;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Rinat Gareev
 */
public class CachedDictionaryDeserializer {

    private static final CachedDictionaryDeserializer instance = new CachedDictionaryDeserializer();

    public static CachedDictionaryDeserializer getInstance() {
        return instance;
    }

    public static class GetDictionaryResult {
        public final CacheResourceKey cacheKey;
        public final MorphDictionary dictionary;

        public GetDictionaryResult(CacheResourceKey cacheKey, MorphDictionary dictionary) {
            this.cacheKey = cacheKey;
            this.dictionary = dictionary;
        }
    }

    // config fields
    private final Logger log = LoggerFactory.getLogger(getClass());
    // state fields
    private final WeakHashMap<CacheResourceKey, MorphDictionary> instanceCache = new WeakHashMap<CacheResourceKey, MorphDictionary>();

    private CachedDictionaryDeserializer() {
    }

    public GetDictionaryResult getDictionary(URL url, InputStream in) throws Exception {
        CacheResourceKey cacheKey = null;
        MorphDictionary dictionary = null;
        synchronized (instanceCache) {
            