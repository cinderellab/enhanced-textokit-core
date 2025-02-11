
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

import com.textocat.textokit.corpus.statistics.dao.corpus.CorpusDAO;
import com.textocat.textokit.corpus.statistics.dao.corpus.XmiFileTreeCorpusDAO;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class XmiFileTreeCorpusDAOResource implements CorpusDAO,
        SharedResourceObject {

    private CorpusDAO corpusDAO;

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
        try {
            corpusDAO = new XmiFileTreeCorpusDAO(aData.getUri().toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new ResourceInitializationException();
        }
    }

    @Override
    public Set<URI> getDocuments() throws URISyntaxException {
        return corpusDAO.getDocuments();
    }

    @Override
    public Set<String> getAnnotatorIds(URI docURI) throws IOException {
        return corpusDAO.getAnnotatorIds(docURI);
    }

    @Override
    public void getDocumentCas(URI docURI, String annotatorId, CAS aCAS)
            throws IOException, SAXException {
        corpusDAO.getDocumentCas(docURI, annotatorId, aCAS);
    }

    @Override
    public boolean hasDocument(URI docURI, String annotatorId) {
        return corpusDAO.hasDocument(docURI, annotatorId);
    }

    @Override
    public void persist(URI docUri, String annotatorId, CAS cas) throws IOException, SAXException {
        corpusDAO.persist(docUri, annotatorId, cas);
    }

}