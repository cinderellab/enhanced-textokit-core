
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


package com.textocat.textokit.commons.cpe;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;

import java.io.*;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * @author Rinat Gareev
 */
public class LinesCollectionReader extends CasCollectionReader_ImplBase {

    private static final String DEFAULT_ENCODING = "utf-8";

    @ConfigurationParameter(name = "inputFile", mandatory = true)
    private File inputFile;
    @ConfigurationParameter(name = "inputFileEncoding", defaultValue = DEFAULT_ENCODING, mandatory = false)
    private String inputFileEncoding = DEFAULT_ENCODING;
    // state fields
    private BufferedReader reader;
    private String currentLine;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        if (!inputFile.isFile()) {
            throw new ResourceInitializationException(
                    new IllegalStateException(String.format(
                            "File %s does not exist", inputFile)));
        }

        InputStream is = null;
        try {
            is = new FileInputStream(inputFile);
            InputStreamReader isr = new InputStreamReader(is, inputFileEncoding);
            reader = new BufferedReader(isr);
        } catch (IOException e) {
            closeQuietly(is);
            throw new ResourceInitializationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNext(CAS cas) throws IOException, CollectionException {
        String text = consumeLine();
        cas.setDocumentText(text);
    }

    private String consumeLine() throws CollectionException, IOException {
        if (currentLine == null) {
            peekLine();
        }
        if (currentLine == null) {
            throw new CollectionException(new IllegalStateException("End of file detected"));
        }
        String result = currentLine;
        // consume!
        currentLine = null;
        return result;
    }

    private String peekLine() throws IOException {
        if (currentLine == null) {
            currentLine = reader.readLine();
        }
        return currentLine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return peekLine() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Progress[] getProgress() {
        return null;
    }

    @Override
    public void close() throws IOException {
        super.close();
        closeQuietly(reader);
    }
}