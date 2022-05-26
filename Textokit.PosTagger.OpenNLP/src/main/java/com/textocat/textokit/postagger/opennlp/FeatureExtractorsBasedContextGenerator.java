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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.textocat.textokit.cleartk.DefaultFeatureToStringEncoderChain;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionary;
import com.textocat.textokit.tokenizer.fstype.Token;
import opennlp.tools.util.BeamSearchContextGenerator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.encoder.CleartkEncoderException;
import org.cleartk.ml.encoder.features.FeatureEncoderChain;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import java.util.List;
import java.util.Set;

/**
 * @author Rinat Gareev
 */
public class FeatureExtractorsBasedContextGenerator implements BeamSearchContextGenerator<Token> {

    private final int prevTagsInHistory;
    private List<FeatureExtractor1> featureExtractors;
    private FeatureEncoderChain<String> featureEncoders = new DefaultFeatureToStringEncoderChain();
    private Set<String> targetGramCategories;
    private MorphDictionary morphDict;
    //
    private DictionaryBasedContextGenerator dictContextGen;

    public FeatureExtractorsBasedContextGenerator(int prevTagsInHistory,
                                                  List<FeatureExtractor1> featureExtractors,
                                                  Iterable<String> targetGramCategories,
                                                  MorphDictionary morphDict) {
        this.prevTagsInHistory = prevTagsInHistory;
        this.featureExtractors = ImmutableList.copyOf(featureExtractors);
        this.targetGramCategories = Sets.newLinkedHashSet(targetGramCategories);
        this.morphDict = morphDict;
        if (this.morphDict != null) {
            dictContextGen = new DictionaryBasedContextGenerator(targetGramCategories, morphDict);
        }
    }

    public int getPrevTagsInHistory() {
        return prevTagsInHistory;
    }

    public Iterable<String> getTargetGramCategories() {
        return targetGramCategories;
    }

    public MorphDictionary getMorphDict() {
        return morphDict;
    }

    @Override
    public String[] getContext(int index, Token[] sequence, String[] priorDecisions,
                               Object[] additionalContext) {
        if (additionalContext == null || additionalContext.length < 1) {
            throw sentenceExpected();
        }
        if (!(additionalContext[0] instanceof Annotation)) {
            throw sentenceExpected();
        }
        Annotation sent = (Annotation) additionalContext[0];
        // TODO cache features that does not dependent on prev tags
        Token curToken = sequence[index];
        List<Feature> features = Lists.newLinkedList();
     