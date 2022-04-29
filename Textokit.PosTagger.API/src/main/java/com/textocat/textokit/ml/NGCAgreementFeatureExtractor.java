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

package com.textocat.textokit.ml;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.textocat.textokit.morph.commons.AgreementPredicates;
import com.textocat.textokit.morph.commons.GramModelBasedTagMapper;
import com.textocat.textokit.morph.commons.TagMapper;
import com.textocat.textokit.morph.commons.TwoTagPredicate;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.morph.fs.Wordform;
import com.textocat.textokit.postagger.MorphCasUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * A feature generator for the Number-Gender-Case agreement .
 *
 * @author Rinat Gareev
 */
public class NGCAgreementFeatureExtractor implements FeatureExtractor1 {

    private GramModel gramModel;
    private TagMapper tagMapper;
    //
    private Map<String, TwoTagPredicate> namedPredicates;

    public NGCAgreementFeatureExtractor(GramModel gramModel) {
        this.gramModel = gramModel;
        this.namedPredicates = AgreementPredicates.numberGenderCaseCombinations(gramModel);
        // TODO:LOW
        tagMapper = new GramModelBasedTagMapper(gramModel);
    }

    @Override
    public List<Feature> extract(JCas view, Annotation focusAnnotation)
            throws CleartkExtractorException {
        Word focusWord = PUtils.getWordAnno(view, focusAnnotation);
        if (focusWord == null || focusWord.getWordforms() == null
                || focusWord.getWordforms().size() == 0) {
            return ImmutableList.of();
        }
        Word precWord = getPrecedingWord(view, focusWord);
        if (precWord == null || precWord.getWordforms() == null
                || precWord.getWordforms().size() == 0) {
            return ImmutableList.of();
        }
        Wordform curWf = (Wordform) focusWord.getWordforms().get(0);
        Wordform precWf = (Wordform) precWord.getWordforms().get(0);
        if (precWf == null) {
            return ImmutableList.of();
        }
        // to Bitset
        BitSet curTag = MorphCasUtils.toGramBitSet(gramModel, curWf);
        BitSet precTag = MorphCasUtils.toGramBitSet(gramModel, precWf);
        // generate features
        List<Feature> result = Lists.newLinkedList();
        for (Map.Entry<String, TwoTagPredicate> npEntry : namedPredicates.entrySet()) {
            TwoTagPredicate predicate = npEntry.getValue();
            String predicateName = npEntry.getKey();
            if (predicate.apply(precTag, curTag)) {
                result.add(new Feature(predicateName, true));
            }
        }
        return result;
    }

    private Word getPrecedingWord(JCas jCas, Word curWord) {
        List<Word> precedingWords = JCasUtil.selectPreceding(jCas, Word.class, curWord, 1);
        if (!precedingWords.isEmpty()) {
            return precedingWords.get(0);
        } else {
            return null;
        }
    }
}
