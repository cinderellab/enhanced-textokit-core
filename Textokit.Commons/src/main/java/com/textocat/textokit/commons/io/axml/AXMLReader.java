
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


package com.textocat.textokit.commons.io.axml;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.textocat.textokit.commons.io.IoUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * An experimental tool to convert document text with annotations formatted in
 * simple XML schema:
 * <dl>
 * <dt> {@code <doc>}
 * <dd>A root element
 * <dt> {@code <meta>}
 * <dd>Attributes of this element is currently ignored; useful to give credits
 * to authors of a source text.
 * <dt> {@code <aliases>}
 * <dd>A block to define aliases (i.e., short labels) for annotation type names
 * that are used in {@code body}.
 * <dt> {@code <alias key="X_alias" type="FullyQualifiedNameOfX" />}
 * <dd>Defines a single alias.
 * <dt> {@code <body>}
 * <dd>Encloses a document text. Inside this element annotations are defined as:
 * <p>
 * {@code text before <X_alias>covered text</X_alias> text after}.
 * </p>
 * </dl>
 * <p/>
 * It's primary purpose it to facilitate generation of test XMI files.
 * <p/>
 * See example data in {@code test-data/} sub-folder of this module.
 *
 * @author Rinat Gareev
 */
public class AXMLReader {

    private AXMLReader() {
    }

    /**
     * Populate the specified CAS by a text and annotations from the specified
     * input assuming that it is formatted as described above.
     *
     * @param in  input Reader. It is a caller's responsibility to close this
     *            reader instance.
     * @param cas CAS
     * @throws IOException
     * @throws SAXException
     */
    public static void read(Reader in, final CAS cas) throws IOException, SAXException {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        AXMLContentHandler contentHandler = new AXMLContentHandler(cas.getTypeSystem());
        xmlReader.setContentHandler(contentHandler);
        InputSource inputSource = new InputSource(in);
        xmlReader.parse(inputSource);
        cas.setDocumentText(contentHandler.getText());
        // from axml ID to CAS FS
        final Map<Annotation, FeatureStructure> mapped = Maps.newHashMap();
        List<Runnable> delayedFeatureAssignments = Lists.newLinkedList();
        //
        for (Annotation _anno : contentHandler.getAnnotations()) {
            String typeName = _anno.getType();
            Type type = cas.getTypeSystem().getType(typeName);
            if (type == null) {
                throw new IllegalStateException(String.format("Unknown type: %s", typeName));
            }
            final AnnotationFS anno = cas.createAnnotation(type, _anno.getBegin(), _anno.getEnd());
            // set primitive features
            for (String featName : _anno.getFeatureNames()) {
                final Feature feat = type.getFeatureByBaseName(featName);
                if (feat == null) throw new IllegalStateException(String.format(
                        "%s does not have feature %s", type.getName(), featName));
                if (feat.getRange().isPrimitive()) {
                    String featValStr = _anno.getFeatureStringValue(featName);
                    if (featValStr != null) {
                        anno.setFeatureValueFromString(feat, featValStr);
                    }
                } else {
                    if (feat.getRange().isArray()) {
                        final List<Annotation> srcFSes = _anno.getFeatureFSArrayValue(featName);
                        delayedFeatureAssignments.add(new Runnable() {
                            @Override
                            public void run() {
                                List<FeatureStructure> mappedFSes = Lists.transform(srcFSes, new Function<Annotation, FeatureStructure>() {
                                    @Override
                                    public FeatureStructure apply(Annotation srcFS) {
                                        FeatureStructure mappedFS = mapped.get(srcFS);
                                        if (mappedFS == null) throw new IllegalStateException();
                                        return mappedFS;
                                    }
                                });
                                anno.setFeatureValue(feat, FSCollectionFactory.createArrayFS(cas, mappedFSes));
                            }
                        });
                    } else {
                        final Annotation srcFS = _anno.getFeatureFSValue(featName);
                        delayedFeatureAssignments.add(new Runnable() {
                            @Override
                            public void run() {
                                FeatureStructure mappedFS = mapped.get(srcFS);
                                if (mappedFS == null) throw new IllegalStateException();
                                anno.setFeatureValue(feat, mappedFS);
                            }
                        });
                    }
                }
            }
            cas.addFsToIndexes(anno);
            mapped.put(_anno, anno);
        }
        // PHASE II -- set FS and FSArray features
        for (Runnable r : delayedFeatureAssignments) {
            r.run();
        }
    }

    public static void read(File file, CAS cas) throws IOException, SAXException {
        BufferedReader in = IoUtils.openReader(file);
        try {
            read(in, cas);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}