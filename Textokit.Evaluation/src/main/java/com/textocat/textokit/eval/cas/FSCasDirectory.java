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

package com.textocat.textokit.eval.cas;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.textocat.textokit.eval.anno.DocumentMetaExtractor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

/**
 * @author Rinat Gareev
 */
public class FSCasDirectory implements CasDirectory, BeanNameAware {

    protected String beanName;
    protected File dir;
    @Autowired
    protected TypeSystem ts;
    @Autowired
    protected Environment env;
    @Autowired
    protected DocumentMetaExtractor docMetaExtractor;
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void setTypeSystem(TypeSystem ts) {
        this.ts = ts;
    }

    public void setDir(File dir) {
        this.dir = dir;
        if (!dir.isDirectory()) {
            throw new IllegalStateException(dir + " is not file directory");
        }
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @PostConstruct
    @Override
    public void init() {
        this.dir = env.getProperty(beanName + ".dir", File.class);
        if (dir == null) {
            throw new IllegalStateException(String.format(
                    "'dir' value is not specified for %s", beanName));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CAS getCas(String docUriStr) throws Exception {
        File xmiFile = getUriToXmiFileMap().get(docUriStr);
        if (xmiFile == null) {
            throw new IllegalArgumentException(String.format(
                    "There is no XMI file with doc URI '%s'",
                    docUriStr));
        }
        if (!xmiFile.isFile()) {
            throw new IllegalStateException("Not a file: " + xmiFile);
        }
        return deserialize(xmiFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<CAS> iterator() {
        Iterator<File> xmiFileIter = getXmiFiles().iterator();
        return Iterators.transform(xmiFileIter, deserializeFunc());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return getXmiFiles().size();
    }

    /**
     * Subclasses may override this to perform alignment, filtering,
     * transformation and etc. operations on deserialized CAS.
     *
     * @param cas
     */
    protected void postProcessCAS(CAS cas) {
        // default impl do nothing
    }

    /**
     * Subclasses may override this to provide additional filtering logic.
     *
     * @return file filter for source files in the base dir
     */
    protected IOFileFilter getSourceFileFilter() {
        return suffixFileFilter(".xmi");
    }

    private Collection<File> xmiFiles;

    private Collection<File> getXmiFiles() {
        if (xmiFiles == null) {
            IOFileFilter sourceFileFilter = getSourceFileFilter();
            xmiFiles = FileUtils.listFiles(dir, sourceFileFilter, trueFileFilter());
        }
        return xmiFiles;
    }

    private Map<String, File> uriToXmiFileMap;

    private Map<String, File> getUriToXmiFileMap()
            throws ResourceInitializationException, IOException, SAXException {
        if (uriToXmiFileMap == null) {
            log.info("Scanning {} XMIs for document URIs...", dir);
            uriToXmiFileMap = Maps.newHashMap();
            CAS wrkCas = createCas();
            for (final File xmiFile : getXmiFiles()) {
                deserialize(xmiFile, wrkCas);
                String docURI = docMetaExtractor.getDocumentUri(wrkCas);
                final File prevFile;
                if ((prevFile = uriToXmiFileMap.put(docURI, xmiFile)) != null) {
                    throw new IllegalStateException(
                            String.format(
                                    "There are at least 2 files which metadata has the same URI '%s':\n%s\n%s",
                                    docURI, prevFile, xmiFile));
                }
                wrkCas.reset();
            }
            log.info("Scanning {} XMIs for document URIs is finished", dir);
        }
        return uriToXmiFileMap;
    }

    private Function<File, CAS> deserializeFunc() {
        return new Function<File, CAS>() {
            @Override
            public CAS apply(File input) {
                try {
                    return deserialize(input);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private CAS deserialize(File xmiFile) throws UIMAException, SAXException, IOException {
        CAS cas = createCas();
        deserialize(xmiFile, cas);
        postProcessCAS(cas);
        return cas;
    }

    private void deserialize(File xmiFile, CAS cas) throws IOException, SAXException {
        InputStream is = openStream(xmiFile);
        try {
            XmiCasDeserializer.deserialize(is, cas);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private CAS createCas() throws ResourceInitializationException {
        return CasCreationUtils.createCas(ts, null, null, null);
    }

    private InputStream openStream(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        return new BufferedInputStream(fis);
    }
}
