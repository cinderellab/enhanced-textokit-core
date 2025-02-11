
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.segmentation.fstype.Sentence;
import com.textocat.textokit.tokenizer.fstype.PM;
import com.textocat.textokit.tokenizer.fstype.Token;
import com.textocat.textokit.tokenizer.fstype.TokenBase;
import com.textocat.textokit.tokenizer.fstype.WhiteSpace;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.ContainmentIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.textocat.textokit.commons.util.DocumentUtils.getDocumentUri;

/**
 * @author Rinat Gareev
 */
public class AdjacentPunctuationFeatureExtractor implements FeatureExtractor1 {

    public static final String FEATURE_NAME_BEFORE = "PMBefore";
    public static final String FEATURE_NAME_AFTER = "PMAfter";
    // config fields
    private final Map<String, String> markToFeatureValue;

    {
        Builder<String, String> mb = ImmutableMap.builder();
        mb.put(",", ",");
        mb.put(":", ":");
        mb.put(";", ";");
        mb.put("-", "-");
        // em dash
        mb.put("\u2014", "-");
        // en dash
        mb.put("\u2013", "-");
        mb.put(".", ".");
        mb.put("?", "?");
        mb.put("!", "!");
        mb.put("(", "(");
        mb.put(")", ")");
        markToFeatureValue = mb.build();
    }

    // state fields
    private ContainmentIndex<Sentence, Token> sentenceContIndex;
    private JCas view;

    public AdjacentPunctuationFeatureExtractor(JCas view) {
        this.view = view;
        sentenceContIndex = ContainmentIndex.create(view, Sentence.class, Token.class,
                ContainmentIndex.Type.REVERSE);
    }

    @Override
    public List<Feature> extract(JCas view, final Annotation focusAnnotation)
            throws CleartkExtractorException {
        if (this.view != view) {
            throw new IllegalStateException();
        }
        final Token focusToken;
        if (focusAnnotation instanceof Token) {
            focusToken = (Token) focusAnnotation;
        } else if (focusAnnotation instanceof Word) {
            focusToken = (Token) ((Word) focusAnnotation).getToken();
        } else {
            throw CleartkExtractorException.wrongAnnotationType(Word.class, focusAnnotation);
        }
        Sentence sent;
        {
            Collection<Sentence> sents = sentenceContIndex.containing(focusToken);
            if (sents.isEmpty()) {
                throw new IllegalStateException(String.format(
                        "No sentence covers %s in %s", focusToken, getDocumentUri(view)));
            }
            if (sents.size() > 1) {
                throw new IllegalStateException(String.format(
                        "Too much sentences cover %s in %s", focusToken, getDocumentUri(view)));
            }
            sent = sents.iterator().next();
        }
        // tb ~ TokenBase
        AnnotationIndex<Annotation> tbIndex = view.getAnnotationIndex(TokenBase.type);
        FSIterator<Annotation> tbIter = tbIndex.iterator(focusToken);
        if (!tbIter.isValid() && !tbIter.get().equals(focusToken)) {
            throw new IllegalStateException();
        }
        PM pmBefore;
        // skip whitespace before
        tbIter.moveToPrevious();
        while (tbIter.isValid() && (tbIter.get() instanceof WhiteSpace)) {
            tbIter.moveToPrevious();
        }
        if (!tbIter.isValid()) {
            // sentence begin
            pmBefore = null;
        } else if (tbIter.get() instanceof PM) {
            pmBefore = (PM) tbIter.get();
            if (pmBefore.getBegin() < sent.getBegin()) {
                // sentence begin
                pmBefore = null;
            }
        } else {
            pmBefore = null;
        }
        // return to focus
        tbIter.moveTo(focusToken);
        PM pmAfter;
        // skip whitespace after
        tbIter.moveToNext();
        while (tbIter.isValid() && (tbIter.get() instanceof WhiteSpace)) {
            tbIter.moveToNext();
        }
        if (!tbIter.isValid()) {
            // sentence end
            pmAfter = null;
        } else if (tbIter.get() instanceof PM) {
            pmAfter = (PM) tbIter.get();
            if (pmAfter.getEnd() > sent.getEnd()) {
                // sentence end
                pmAfter = null;
            }
        } else {
            pmAfter = null;
        }
        List<Feature> result = Lists.newLinkedList();
        Feature pmBeforeFeat = toFeature(pmBefore, FEATURE_NAME_BEFORE);
        if (pmBeforeFeat != null) {
            result.add(pmBeforeFeat);
        }
        Feature pmAfterFeat = toFeature(pmAfter, FEATURE_NAME_AFTER);
        if (pmAfterFeat != null) {
            result.add(pmAfterFeat);
        }
        return result;
    }

    private Feature toFeature(PM pmAnno, String featureName) {
        if (pmAnno == null) {
            return null;
        }
        String featValue = markToFeatureValue.get(pmAnno.getCoveredText());
        if (featValue == null) {
            return null;
        }
        return new Feature(featureName, featValue);
    }
}