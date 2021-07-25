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

package com.textocat.textokit.corpus.statistics.dao.corpus;

import com.google.common.collect.Sets;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class XmiFileTreeCorpusDAOTest {

    String corpusPathString = "src/test/resources/corpus_example";
    CorpusDAO corpusDAO;

    @Before
    public void setUp() throws URISyntaxException {
        corpusDAO = new XmiFileTreeCorpusDAO(corpusPathString);
    }

    @Test
    public void testGetDocuments() throws URISyntaxException {
        assertEquals(Sets.newHashSet(new URI("62007.txt"),
                        new URI("65801.txt"), new URI("75788.txt")),
                corpusDAO.getDocuments());
    }

    @Test
    public void testG