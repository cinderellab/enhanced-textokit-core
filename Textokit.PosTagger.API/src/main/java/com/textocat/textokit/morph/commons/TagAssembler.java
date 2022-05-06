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

package com.textocat.textokit.morph.commons;

import com.textocat.textokit.commons.cas.FSUtils;
import com.textocat.textokit.morph.dictionary.resource.GramModelHolder;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.morph.fs.Wordform;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.initializable.InitializableFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

/**
 * @author Rinat Gareev
 */
public class TagAssembler extends JCasAnnotator_ImplBase {

    /**
     * Create description of this annotator with default parameter values, i.e.:
     * <ul>
     * <li> {@link GramModelBasedTagMapper} is used, that requires to bind
     * {@link GramModelHolder} to resource key
     * {@value GramModelBasedTagMapper#RESOURCE_GRAM_MODEL}
     * </ul>
     *
     * @return a description instance
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createDescription()
            throws ResourceInitializationException {
        AnalysisEngineDescription desc = createEngineDescription(
                TagAssembler.class, // it does not produce any additional annotations => no need in TS
                PARAM_TAG_MAPPER_CLASS, GramModelBasedTagMapper.class.getName());
        GramModelBasedTagMapper.declareResourceDependencies(desc);
        return desc;
    }

    public static final String PARAM_TAG_MAPPER_CLASS = "tagMapperClass";

    // config
    @ConfigurationParameter(name = PARAM_TAG_MAPPER_CLASS,
            defaultValue = "GramModelBasedTagMapper",
            mandatory = false)
    private String tagMapperClassName;
    private TagMapper tagMapper;

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        super.initialize(ctx);
        tagMapper = InitializableFactory.create(ctx, tagMapperClassName, TagMapper.class);
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        for (Word word : JCasUtil.select(jCas, Word.class)) {
            FSArray wfs = word.getWordforms();
            if (wfs == null) {
                continue;
            }
            for (Wordform wf : FSCollectionFactory.create(wfs, Wordform.class)) {
                String tag = tagMapper.toTag(FSUtils.toSet(wf.getGrammems()));
                wf.setPos(tag);
            }
        }
    }

}
