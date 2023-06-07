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
package com.textocat.textokit.tokenizer.simple;

import com.google.common.collect.*;
import com.textocat.textokit.tokenizer.fstype.*;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;

/**
 * @author Rinat Gareev
 */
public class PostTokenizer extends JCasAnnotator_ImplBase {

    public static AnalysisEngineDescription createDescription()
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(PostTokenizer.class);
    }

    // per-CAS state
    private Map<AnnotationFS, Collection<? extends AnnotationFS>> mergedMap;
    private Type wordType;
    private Type numType;

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        CAS cas = jCas.getCas();
        mergedMap = Maps.newHashMap();
        wordType = jCas.getCasType(W.type);
        numType = jCas.getCasType(NUM.type);
        try {
            AnnotationIndex<Annotation> tokenBases = jCas.getAnnotationIndex(TokenBase.typeIndexID);
            // sequence of tokens that does not contain whitespace
            List<Token> curTokenSeq = Lists.newLinkedList();
            for (Annotation tokenBase : tokenBases) {
                if (tokenBase instanceof WhiteSpace) {
                    handle(cas, ImmutableList.copyOf(curTokenSeq));
                    curTokenSeq.clear();
                } else {
                    // it's Token
                    curTokenSeq.add((Token) tokenBase);
                }
            }
            // handle last seq
            handle(cas, ImmutableList.copyOf(curTokenSeq));
            curTokenSeq.clear();
            // index/unindex
            Set<String> mergedTokenStrings = Sets.newHashSet();
            for (Map.Entry<AnnotationFS, Collection<? extends AnnotationFS>> entry : mergedMap
                    .entrySet()) {
                jCas.addFsToIndexes(entry.getKey());
                mergedTokenStrings.add(entry.getKey().getCoveredText());
                for (AnnotationFS anno : entry.getValue()) {
                    jCas.removeFsFromIndexes(anno);
                }
            }
            getLogger().debug("Merged tokens: " + mergedTokenStrings);
        } finally {
            mergedMap.clear();
        }
    }

    private boolean handle(CAS cas, List<Token> tokens) {
        if (tokens.size() <= 1) {
            return false;
        } else if (tokens.size() == 2) {
            // check abbreviation dictionary
            if (isWord(tokens.get(0)) && isDot(tokens.get(1))
                    && isAbbreviation(getCoveredText(tokens))) {
                makeAnnotation(cas, tokens.get(0).getType(), tokens);
                return true;
            }
            if (!hasPMOrSpecial(tokens)) {
                makeAnnotation(cas,
                        isWord(tokens.get(0)) ? tokens.get(0).getType() : wordType,
                        tokens);
            }
        } else if (tokens.size() == 3) {
            Token t0 = tokens.get(0);
            Token t1 = tokens.get(1);
            Token t2 = tokens.get(2);
            if (isPossibleInnerPM(t1) && hasWord(t0, t2)) {
                makeAnnotation(cas, isWord(t0) ? t0.getType() : wordType, tokens);
 