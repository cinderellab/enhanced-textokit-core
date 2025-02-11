
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


package com.textocat.textokit.commons.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.*;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import com.textocat.textokit.commons.DocumentMetadata;

import java.net.URI;
import java.net.URISyntaxException;

import static com.textocat.textokit.commons.util.AnnotatorUtils.featureExist;

/**
 * @author Rinat Gareev
 */
public class DocumentUtils {

    public static final String TYPESYSTEM_COMMONS = "com.textocat.textokit.commons.Commons-TypeSystem";

    private DocumentUtils() {
    }

    /**
     * @return a type name of feature structures that represent document
     * meta-data.
     */
    public static String getMetadataTypeName() {
        return DocumentMetadata.class.getName();
    }

    /**
     * @param tsd
     * @return a type description instance for document meta-data FS if any,
     * otherwise - null.
     */
    public static TypeDescription getMetadataTypeDescription(TypeSystemDescription tsd) {
        return tsd.getType(getMetadataTypeName());
    }

    /**
     * Search for a {@link DocumentMetadata} annotation in the given CAS and set
     * its 'uri' feature to the specified value.
     *
     * @param cas
     * @param uri
     * @param createDocMeta if true then this method will create a new
     *                      {@link DocumentMetadata} instance if there is none in the CAS.
     */
    public static void setDocumentUri(CAS cas, String uri, boolean createDocMeta) {
        TypeSystem ts = cas.getTypeSystem();
        Type docMetaType = ts.getType(getMetadataTypeName());
        if (docMetaType == null) {
            throw new IllegalStateException(
                    "The typesystem of the given CAS does not contain DocumentMetadata type");
        }
        Feature sourceUriFeat;
        try {
            sourceUriFeat = featureExist(docMetaType, "sourceUri");
        } catch (AnalysisEngineProcessException e) {
            throw new IllegalStateException(e);
        }
        AnnotationFS docMetaFS = null;
        FSIterator<AnnotationFS> dmIter = cas.getAnnotationIndex(docMetaType).iterator();
        if (dmIter.hasNext()) {
            docMetaFS = dmIter.next();
        }
        if (docMetaFS == null) {
            if (createDocMeta) {
                docMetaFS = cas.createAnnotation(docMetaType, 0, 0);
                cas.addFsToIndexes(docMetaFS);
            } else {
                throw new IllegalStateException("The given CAS does not contain a DocumentMetadata");
            }
        }
        docMetaFS.setStringValue(sourceUriFeat, uri);
    }

    /**
     * search for a {@link DocumentMetadata} annotation in given CAS and return
     * its 'sourceUri' feature value
     *
     * @param cas
     * @return sourceUri value or null if there is no a DocumentMetadata
     * annotation
     */
    public static String getDocumentUri(CAS cas) {
        TypeSystem ts = cas.getTypeSystem();
        Type docMetaType = ts.getType(getMetadataTypeName());
        if (docMetaType == null) {
            return null;
        }
        Feature sourceUriFeat;
        try {
            sourceUriFeat = featureExist(docMetaType, "sourceUri");
        } catch (AnalysisEngineProcessException e) {
            throw new IllegalStateException(e);
        }
        FSIterator<AnnotationFS> dmIter = cas.getAnnotationIndex(docMetaType).iterator();
        if (dmIter.hasNext()) {
            AnnotationFS docMeta = dmIter.next();
            return docMeta.getStringValue(sourceUriFeat);
        } else {
            return null;
        }
    }

    public static String getDocumentUri(JCas jcas) {
        // TODO is there more optimal solution?
        return getDocumentUri(jcas.getCas());
    }

    /**
     * @param cas a document CAS
     * @return filename (i.e., the last path segment) specified in the document URI. Returns null if:
     * <ul>
     * <li>the CAS does not contain a doc metadata</li>
     * <li>OR the source URI does not specify a filepath</li>
     * </ul>
     * @throws java.net.URISyntaxException
     */
    public static String getDocumentFilename(CAS cas) throws URISyntaxException {
        String uriStr = getDocumentUri(cas);
        if (uriStr == null) {
            return null;
        }
        return getFilename(uriStr);
    }

    public static String getFilename(String uriStr) throws URISyntaxException {
        URI uri = new URI(uriStr);
        String path;
        if (uri.isOpaque()) {
            // TODO this is a hotfix. In future we should avoid setting opaque source URIs in DocumentMetadata
            path = uri.getSchemeSpecificPart();
        } else {
            path = uri.getPath();
        }
        return FilenameUtils.getName(path);
    }

    public static String getFilenameSafe(String uriStr) {
        String name;
        try {
            name = getFilename(uriStr);
            if (StringUtils.isBlank(name)) {
                name = uriStr;
            }
        } catch (URISyntaxException e) {
            name = uriStr;
        }
        return name;
    }
}