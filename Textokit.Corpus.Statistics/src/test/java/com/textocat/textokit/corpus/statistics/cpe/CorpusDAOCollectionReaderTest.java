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

package com.textocat.textokit.corpus.statistics.cpe;

import com.google.common.collect.Sets;
import com.textocat.textokit.commons.util.DocumentUtils;
import com.textocat.textokit.corpus.statistics.dao.corpus.XmiFileTreeCorpusDAO;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class CorpusDAOCollectionReaderTest {

    String corpusPathString = Thread.currentThread().getContextClassLoader()
            .getResource("corpus_example").getPath();
    ExternalResourceDescription daoDesc;
    TypeSystemDescription tsd;
    CollectionReader reader;

    @Before
    public void setUp() throws Exception {
        daoDesc = ExternalResourceFactory.createExternalResourceDescription(
                XmiFileTreeCorpusDAOResource.class, corpusPathString);
        reader = CollectionReaderFactory.createReader(
                CorpusDAOCollectionReader.class,
                XmiFileTreeCorpusDAO.getTypeSystem(corpusPathString),
                CorpusDAOCollectionReader.CORPUS_DAO_KEY, daoDesc);
        tsd = CasCreationUtils.mergeTypeSystems(Sets.newHashSet(
                XmiFileTreeCorpusDAO.getTypeSystem(corpusPathString),
                TypeSystemDescriptionFactory.createTypeSystemDescription()));
        CAS aCAS = CasCreationUtils.createCas(tsd, null, null, null);
        reader.typeSystemInit(aCAS.getTypeSystem());
    }

    @Test
    public void testGetNext() throws CollectionException, IOException,
            ResourceInitializationException, URISyntaxException, SAXException,
            ParserConfigurationException {
        Set<String> sourceUris = new HashSet<String>();
        while (reader.hasNext()) {
            CAS aCAS = CasCreationUtils.createCas(tsd, null, null, null);
            reader.getNext(aCAS);
            assertThat(aCAS.getDocumentText(), containsString("д"));
            String sourceUri = DocumentUtils.getDocumentUri(aCAS);
            sourceUris.add(sourceUri.substring(sourceUri.length() - 11));
        }
        assertEquals(Sets.newHashSet("1/62007.txt", "1/65801.txt",
                "5/62007.txt", "5/75788.txt"), sourceUris);
    }

}
