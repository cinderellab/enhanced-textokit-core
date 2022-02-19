
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

import com.textocat.textokit.morph.dictionary.resource.GramModel;
import com.textocat.textokit.morph.dictionary.resource.GramModelHolder;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionary;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionaryHolder;
import com.textocat.textokit.morph.opencorpora.OpencorporaMorphDictionaryAPI;
import com.textocat.textokit.resource.ClasspathResourceBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

import java.io.InputStream;
import java.util.Map;

/**
 * @author Rinat Gareev
 */
public class ClasspathMorphDictionaryResource extends ClasspathResourceBase
        implements MorphDictionaryHolder, GramModelHolder {

    // state fields
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private CacheResourceKey cacheKey;
    private MorphDictionary dict;

    @Override
    protected String locateDefaultResourceClassPath() {
        return OpencorporaMorphDictionaryAPI.locateDictionaryClassPath();
    }

    @Override
    public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
            throws ResourceInitializationException {
        if (!super.initialize(aSpecifier, aAdditionalParams))
            return false;
        try {
            try (InputStream resourceIS = resource.getInputStream()) {
                CachedDictionaryDeserializer.GetDictionaryResult getDictResult =
                        CachedDictionaryDeserializer.getInstance().getDictionary(resource.getURL(), resourceIS);
                this.cacheKey = getDictResult.cacheKey;
                this.dict = getDictResult.dictionary;
            }
        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
        return true;
    }

    @Override
    public MorphDictionary getDictionary() {
        return dict;
    }

    @Override
    public GramModel getGramModel() {
        return dict.getGramModel();
    }
}