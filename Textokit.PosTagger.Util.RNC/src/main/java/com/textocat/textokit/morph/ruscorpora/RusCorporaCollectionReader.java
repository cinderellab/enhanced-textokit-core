
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

package com.textocat.textokit.morph.ruscorpora;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.textocat.textokit.commons.DocumentMetadata;
import com.textocat.textokit.commons.cas.FSUtils;
import com.textocat.textokit.commons.util.CorpusUtils;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.morph.fs.Wordform;
import com.textocat.textokit.segmentation.fstype.Paragraph;
import com.textocat.textokit.segmentation.fstype.Sentence;
import com.textocat.textokit.tokenizer.TokenUtils;
import com.textocat.textokit.tokenizer.fstype.SPECIAL;
import com.textocat.textokit.tokenizer.fstype.Token;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.initializable.InitializableFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URI;
import java.util.List;

import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

/**
 * @author Rinat Gareev
 */
public class RusCorporaCollectionReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_INPUT_DIR = "InputDirectory";
    public static final String PARAM_TAG_MAPPER_CLASS = "TagMapperClass";
    private static final String DEFAULT_INPUT_FILE_EXT = ".xhtml";

    @ConfigurationParameter(name = PARAM_INPUT_DIR, mandatory = true)
    private File inputDir;
    @ConfigurationParameter(name = PARAM_TAG_MAPPER_CLASS, mandatory = true,
            defaultValue = "RusCorpora2OpenCorporaTagMapper")
    private String tagMapperClassName;
    // derived
    private Function<File, URI> relativeURIFunc;
    private Function<File, String> relativePathFunc;
    private List<File> inputFiles;
    private RusCorporaTagMapper tagMapper;
    // state fields
    private XMLReader xmlReader;
    private int lastReadFileIndex = -1;
    private String curFileName;
    private int wordCounter;

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        super.initialize(ctx);
        tagMapper = InitializableFactory.create(ctx, tagMapperClassName, RusCorporaTagMapper.class);
        if (!inputDir.isDirectory()) {
            throw new IllegalArgumentException(String.format(
                    "%s is not existing directory", inputDir));
        }
        relativeURIFunc = CorpusUtils.relativeURIFunction(inputDir);
        relativePathFunc = CorpusUtils.relativePathFunction(inputDir);
        String inputFileExt = DEFAULT_INPUT_FILE_EXT;
        inputFiles = ImmutableList.copyOf(
                FileUtils.listFiles(inputDir,
                        suffixFileFilter(inputFileExt),
                        TrueFileFilter.INSTANCE));
        getLogger().info(String.format("Detected *%s files in %s: %s",
                inputFileExt, inputDir, inputFiles.size()));
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            xmlReader = saxParser.getXMLReader();
        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return lastReadFileIndex < inputFiles.size() - 1;
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        File inputFile = inputFiles.get(++lastReadFileIndex);
        curFileName = relativePathFunc.apply(inputFile);
        InputStream is = new BufferedInputStream(new FileInputStream(inputFile));
        RusCorporaXmlContentHandler xmlHandler = new RusCorporaXmlContentHandler();
        try {
            InputSource xmlSource = new InputSource(is);
            xmlReader.setContentHandler(xmlHandler);
            xmlReader.parse(xmlSource);
        } catch (Exception e) {
            throw new CollectionException(new IllegalStateException(
                    "Parsing " + curFileName + "...", e));
        } finally {
            IOUtils.closeQuietly(is);
        }
        String docText = xmlHandler.getDocumentText();
        jCas.setDocumentText(docText);
        // set document meta
        DocumentMetadata docMeta = new DocumentMetadata(jCas, 0, 0);
        docMeta.setDocumentSize(docText.length());
        docMeta.setSourceUri(relativeURIFunc.apply(inputFile).toString());
        docMeta.addToIndexes();
        // add paragraphs
        for (RusCorporaAnnotation para : xmlHandler.getParagraphs()) {
            new Paragraph(jCas, para.getBegin(), para.getEnd()).addToIndexes();
        }
        // add sentences
        for (RusCorporaAnnotation sent : xmlHandler.getSentences()) {
            new Sentence(jCas, sent.getBegin(), sent.getEnd()).addToIndexes();
        }
        // add wordforms
        for (RusCorporaWordform srcWf : xmlHandler.getWordforms()) {
            int wBegin = srcWf.getBegin();
            int wEnd = srcWf.getEnd();
            Word w = new Word(jCas, wBegin, wEnd);
            // make token anno
            Token tok;
            try {
                tok = TokenUtils.makeToken(jCas,
                        docText.substring(wBegin, wEnd),
                        wBegin, wEnd);
            } catch (Exception e) {
                throw new IllegalStateException(String.format("In %s", curFileName));
            }
            if (tok instanceof SPECIAL) {
                getLogger().info(String.format("SPECIAL detected in %s: %s",
                        curFileName, tok.getCoveredText()));
            }
            tok.addToIndexes();
            w.setToken(tok);
            // make wordform anno
            Wordform wf = new Wordform(jCas);
            wf.setWord(w);
            tagMapper.mapFromRusCorpora(srcWf, wf);
            w.setWordforms(FSUtils.toFSArray(jCas, wf));
            w.addToIndexes();
            wordCounter++;
        }
        // clear per-CAS state
        curFileName = null;
    }

    @Override
    public Progress[] getProgress() {
        int filesRead = lastReadFileIndex + 1;
        return new Progress[]{
                new ProgressImpl(filesRead, inputFiles.size(), Progress.ENTITIES)
        };
    }

    @Override
    public void close() throws IOException {
        getLogger().info(String.format(
                "Words parsed: %s", wordCounter));
        if (tagMapper instanceof Closeable) {
            ((Closeable) tagMapper).close();
        }
        super.close();
    }
}