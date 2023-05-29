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
     