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

package com.textocat.textokit.morph.lemmatizer.util;

import com.google.common.base.Preconditions;
import com.textocat.textokit.commons.io.IoUtils;
import com.textocat.textokit.commons.util.DocumentUtils;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.postagger.MorphCasUtils;
import com.textocat.textokit.tokenizer.fstype.BREAK;
import com.textocat.textokit.tokenizer.fstype.Token;
import com.textocat.textokit.tokenizer.fstype.TokenBase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Write a transformed text where
 * <ul>
 * <li> each word is replaced by its lemma </li>
 * <li> each token (word, punctuation or special symbol) is separated by a single space from its adjacent tokens</li>
 * <li> sequence of whitespaces (including tabs, excluding line endings) is merged into single space</li>
 * <li> sequence of whitespaces with a line ending is merged into single line ending</li>
 * </ul>
 *
 * @author Rinat Gareev
 */
public class NormalizedTextWriter extends JCasAnnotator_ImplBase {

    public static AnalysisEngineDescription createDescription(File outputDir) throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(NormalizedTextWriter.class,
                PARAM_OUTPUT_DIR, outputDir);
    }

    public static final String PARA