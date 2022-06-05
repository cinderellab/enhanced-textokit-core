
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

package com.textocat.textokit.postagger.opennlp;

import com.textocat.textokit.commons.util.CachedResourceTuple;
import com.textocat.textokit.commons.util.ConfigPropertiesUtils;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionary;
import com.textocat.textokit.tokenizer.fstype.Token;
import opennlp.tools.util.BaseToolFactory;
import opennlp.tools.util.BeamSearchContextGenerator;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactProvider;
import opennlp.tools.util.model.ArtifactSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import static com.textocat.textokit.morph.dictionary.MorphDictionaryAPIFactory.getMorphDictionaryAPI;

/**
 * Custom implementation of {@link BaseToolFactory} for PoS-tagger.
 * <p/>
 * Description of the {@link MorphDictionary} injection:
 * <ul>
 * <li>training: passed as a part of contextGenerator, during the serialization
 * of contextGenerator store only a version of the dictionary;
 * <li>tagging: passed to a model (with InputStream of serialized artifacts),
 * that is {@link ArtifactProvider}, it will be injected back to a
 * contextGenerator.
 * </ul>
 *
 * @author Rinat Gareev
 */
public class POSTaggerFactory extends BaseToolFactory {

    private static final String FEATURE_EXTRACTORS_ENTRY_NAME = "feature.extractors";

    private FeatureExtractorsBasedContextGenerator contextGenerator;

    /**
     * This constructor is required for OpenNLP model deserialization
     */
    public POSTaggerFactory() {
    }

    public POSTaggerFactory(FeatureExtractorsBasedContextGenerator contextGenerator) {
        this.contextGenerator = contextGenerator;
    }

    @Override
    protected void init(ArtifactProvider artifactProvider) {
        super.init(artifactProvider);
    }

    public BeamSearchContextGenerator<Token> getContextGenerator() {
        if (contextGenerator == null && artifactProvider != null) {
            contextGenerator = artifactProvider.getArtifact(FEATURE_EXTRACTORS_ENTRY_NAME);
        }
        return contextGenerator;
    }

    @Override
    public Map<String, Object> createArtifactMap() {
        Map<String, Object> artMap = super.createArtifactMap();
        artMap.put(FEATURE_EXTRACTORS_ENTRY_NAME, contextGenerator);
        return artMap;
    }

    @Override
    public Map<String, String> createManifestEntries() {
        return super.createManifestEntries();
    }

    @Override
    public void validateArtifactMap() throws InvalidFormatException {
        Object featExtractorsEntry = artifactProvider.getArtifact(FEATURE_EXTRACTORS_ENTRY_NAME);
        if (featExtractorsEntry == null) {
            throw new InvalidFormatException("No featureExtractors in artifacts map");
        }
        if (!(featExtractorsEntry instanceof FeatureExtractorsBasedContextGenerator)) {
            throw new InvalidFormatException(String.format(
                    "Unknown type of feature extractors aggregate: %s",
                    featExtractorsEntry.getClass()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, ArtifactSerializer> createArtifactSerializersMap() {
        Map<String, ArtifactSerializer> artSerMap = super.createArtifactSerializersMap();
        artSerMap.put("extractors", new FeatureExtractorsSerializer());
        // artSerMap.put("dict", new MorphologyDictionarySerializer());
        return artSerMap;
    }

	/*
    static class MorphologyDictionarySerializer implements ArtifactSerializer<MorphDictionary> {
		@Override
		public MorphDictionary create(InputStream in) throws IOException, InvalidFormatException {
			// MUST never be called
			// because a dictionary instance is supposed to be managed by UIMA
			throw new UnsupportedOperationException("MorphDictionary should be injected by UIMA!");
			// TOD insert here the code that will check compatibility of the injected dictionary with one that defined in the model
		}

		@Override
		public void serialize(MorphDictionary artifact, OutputStream out) throws IOException {
			// do nothing
		}
	}
	*/

    class FeatureExtractorsSerializer implements
            ArtifactSerializer<FeatureExtractorsBasedContextGenerator> {

        // A serializer instance is hold in a BaseModel instance,
        // so this key will have the same life-time as this BaseModel
        private Object dictCacheKey;

        @Override
        public FeatureExtractorsBasedContextGenerator create(InputStream in) throws IOException {
            if (dictCacheKey != null) {
                throw new UnsupportedOperationException();
            }
            Properties props = new Properties();
            props.load(in);
            MorphDictionary dict = null;
            if (ConfigPropertiesUtils.getStringProperty(props,
                    DefaultFeatureExtractors.PROP_DICTIONARY_VERSION, false) != null) {
                // load dictionary
                CachedResourceTuple<MorphDictionary> dictTuple;
                try {
                    dictTuple = getMorphDictionaryAPI()
                            .getCachedInstance();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                dict = dictTuple.getResource();
                dictCacheKey = dictTuple.getCacheKey();
            }

            return DefaultFeatureExtractors.from(props, dict);
        }

        @Override
        public void serialize(FeatureExtractorsBasedContextGenerator artifact, OutputStream out)
                throws IOException {
            if (!(artifact instanceof DefaultFeatureExtractors)) {
                throw new UnsupportedOperationException();
            }
            Properties props = new Properties();
            DefaultFeatureExtractors.to((DefaultFeatureExtractors) artifact, props);
            props.store(out, "");
        }

    }
}