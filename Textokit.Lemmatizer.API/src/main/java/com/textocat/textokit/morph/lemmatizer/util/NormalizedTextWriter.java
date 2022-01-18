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

    public static final String PARAM_OUTPUT_DIR = "outputDir";
    public static final String OUTPUT_FILENAME_SUFFIX = "-normalized";
    public static final String OUTPUT_FILENAME_EXTENSION = ".txt";

    @ConfigurationParameter(name = PARAM_OUTPUT_DIR)
    private File outputDir;

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        super.initialize(ctx);
        try {
            FileUtils.forceMkdir(outputDir);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas cas) throws AnalysisEngineProcessException {
        // initialize
        String docFilename;
        try {
            docFilename = DocumentUtils.getDocumentFilename(cas.getCas());
        } catch (URISyntaxException e) {
            throw new AnalysisEngineProcessException(e);
        }
        if (docFilename == null) {
            throw new IllegalStateException("Can't extract a document filename from CAS");
        }
        String outFilename = FilenameUtils.getBaseName(docFilename)
                + OUTPUT_FILENAME_SUFFIX + OUTPUT_FILENAME_EXTENSION;
        File outFile = new File(outputDir, outFilename);
        Map<Token, Word> token2WordIndex = MorphCasUtils.getToken2WordIndex(cas);
        @SuppressWarnings("unchecked")
        FSIterator<TokenBase> tbIter = (FSIterator) cas.getAnnotationIndex(TokenBase.typeIndexID).iterator();
        try (PrintWriter out = IoUtils.openPrintWriter(outFile)) {
            Token lastProcessedTok = null;
            for (Token curTok : JCasUtil.select(cas, Token.class)) {
                // normalize space between
                out.print(normalizeSpaceBetween(tbIter, lastProcessedTok, curTok));
                // normalize current token
                String curTokNorm;
                Word w = token2WordIndex.get(curTok);
                if (w != null) {
                    curTokNorm = MorphCasUtils.getFirstLemma(w);
                } else {
                    curTokNorm = curTok.getCoveredText();
                }
                out.print(curTokNorm);
                //
                lastProcessedTok = curTok;
            }
            // handle a possible line ending after the last token
            out.print(normalizeSpaceBetween(tbIter, lastProcessedTok, null));
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private String normalizeSpaceBetween(FSIterator<TokenBase> tbIter, final Token x, final Token y) {
        // X must be before Y
        Preconditions.checkArgument(x == null || y == null || x.getCAS().getAnnotationIndex().compare(x, y) < 0);
        if (x == null) {
            tbIter.moveToFirst();
        } else {
            tbIter.moveTo(x);
        }
        while (// if Y is null then iterate till the end
                (y == null && tbIter.isValid())
                        // else - iterate till the Y
                        || (y != null && !tbIter.get().equals(y))) {
            if (tbIter.get() instanceof BREAK) {
                return "\n";
            }
            tbIter.moveToNext();
        }
        return " ";
    }
}
