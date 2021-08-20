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

package com.textocat.textokit.dictmatcher;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.textocat.textokit.chunk.Chunk;
import com.textocat.textokit.chunk.Chunker;
import com.textocat.textokit.commons.cas.FSTypeUtils;
import com.textocat.textokit.commons.cas.FSUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.*;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.CasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.initializable.InitializableFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.List;
import java.util.RandomAccess;
import java.util.Set;

/**
 * @param <V> a type of dictionary metadata ({@link com.textocat.textokit.chunk.Chunk}) metadata.
 * @author Rinat Gareev
 */
public class DictionaryAnnotator<V> extends CasAnnotator_ImplBase {

    /**
     * Creates a description with {@link DefaultChunkAnnotationAdapter} and other defaults.
     *
     * @param resultAnnoClass a type of annotation that will be created for each matched chunk
     * @return a description instance
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createDescription(Class<? extends AnnotationFS> resultAnnoClass)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(DictionaryAnnotator.class,
                DefaultChunkAnnotationAdapter.PARAM_RESULT_ANNOTATION_TYPE, resultAnnoClass.getName());
    }

    public static final String RESOURCE_CHUNKER = "chunker";
    public static final String PARAM_CHUNK_ADAPTER_CLASS = "chunkAdapterClass";
    public static final String PARAM_INPUT_TOKEN_TYPE = "inputTokenType";
    public static final String PARAM_NORM_FORM_FEATURE_PATH = "normFormFeaturePath";
    public static final String PARAM_BOUNDARY_ANNOTATION_TYPE = "boundaryAnnoType";
    public static final String PARAM_NORM_FALLBACK_TO_COVERED_TEXT = "normFallbackToCoveredText";

    @ExternalResource(key = RESOURCE_CHUNKER)
    private Chunker<V> dictMatcher;
    @ConfigurationParameter(name = PARAM_CHUNK_ADAPTER_CLASS, mandatory = false,
            defaultValue = "com.textocat.textokit.dictmatcher.DefaultChunkAnnotationAdapter")
    private Class<? extends ChunkAnnotationAdapter> chunkAdapterClass;
    @ConfigurationParameter(name = PARAM_INPUT_TOKEN_TYPE, mandatory = false,
            defaultValue = "com.textocat.textokit.morph.fs.Simp