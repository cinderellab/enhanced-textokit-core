
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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import com.textocat.textokit.commons.DocumentMetadata;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

/**
 * @author Rinat Gareev
 */
public class FileDirectoryCollectionReader extends CasCollectionReader_ImplBase {

    public static final String PARAM_DIRECTORY_PATH = "directoryPath";
    public static final String PARAM_FILE_EXTENSION = "fileExtension";
    public static final String PARAM_ENCODING = "encoding";
    public static final String PARAM_SET_RELATIVE_URI = "setRelativeURI";
    // config
    @ConfigurationParameter(name = PARAM_DIRECTORY_PATH, mandatory = true)
    private File directory;
    @ConfigurationParameter(name = PARAM_FILE_EXTENSION, defaultValue = "txt", mandatory = false)
    private String fileExtension;
    @ConfigurationParameter(name = PARAM_ENCODING, defaultValue = "utf-8", mandatory = false)
    private String encoding;
    @ConfigurationParameter(name = PARAM_SET_RELATIVE_URI, defaultValue = "true", mandatory = false)
    private boolean setRelativeURI;
    // derived
    private ArrayList<File> files;
    // state
    private int lastReadFileIdx;

    public static CollectionReaderDescription createDescription(File inputDir)
            throws ResourceInitializationException {
        TypeSystemDescription inputTSD = createTypeSystemDescription(
                "com.textocat.textokit.commons.Commons-TypeSystem");
        return CollectionReaderFactory.createReaderDescription(
                FileDirectoryCollectionReader.class,
                inputTSD,
                PARAM_DIRECTORY_PATH, inputDir);
    }

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        super.initialize(ctx);
        if (!directory.isDirectory()) {
            throw new IllegalStateException(String.format(
                    "%s is not existing file directory", directory));
        }
        IOFileFilter fileFilter = FileFilterUtils.suffixFileFilter(fileExtension);
        IOFileFilter subdirFilter = FileFilterUtils.trueFileFilter();
        files = Lists.newArrayList(FileUtils.listFiles(directory, fileFilter, subdirFilter));
        //
        lastReadFileIdx = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNext(CAS aCAS) throws IOException, CollectionException {
        if (!hasNext()) {
            throw new CollectionException(new NoSuchElementException());
        }
        final int curFileIdx = lastReadFileIdx + 1;
        File file = files.get(curFileIdx);
        lastReadFileIdx = curFileIdx;
        //
        String fileContent = FileUtils.readFileToString(file, encoding);
        aCAS.setDocumentText(fileContent);
        try {
            DocumentMetadata docMeta = new DocumentMetadata(aCAS.getJCas());
            docMeta.setSourceUri(getURIForMetadata(file).toString());
            docMeta.addToIndexes();
        } catch (CASException e) {
            throw new IllegalStateException(e);
        }
    }

    private URI getURIForMetadata(File f) {
        URI fURI = f.toURI();
        if (setRelativeURI) {
            URI dirURI = directory.toURI();
            return dirURI.relativize(fURI);
        } else {
            return fURI;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return (lastReadFileIdx + 1) < files.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Progress[] getProgress() {
        return new Progress[]{
                new ProgressImpl(lastReadFileIdx + 1, files.size(), Progress.ENTITIES)
        };
    }

}