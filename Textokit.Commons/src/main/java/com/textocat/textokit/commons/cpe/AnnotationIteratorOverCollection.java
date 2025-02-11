
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

import com.google.common.collect.AbstractIterator;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

/**
 * @author Rinat Gareev
 */
public class AnnotationIteratorOverCollection<AT extends Annotation> extends AbstractIterator<AT>
        implements Closeable {

    private final Class<AT> annotationClass;
    //
    private Iterator<JCas> jCasIter;
    private Iterator<AT> curJCasAnnotationIter;
    public AnnotationIteratorOverCollection(Iterable<JCas> jCasIterable, Class<AT> annotationClass) {
        this.annotationClass = annotationClass;
        jCasIter = jCasIterable.iterator();
        // init
        if (jCasIter.hasNext()) {
            getNextCas();
        }
    }

    public static <A extends Annotation> Iterator<A> createIterator(
            Class<A> annotationClass,
            CollectionReaderDescription colReaderDesc,
            AnalysisEngineDescription aeDesc) throws UIMAException, IOException {
        if (aeDesc == null) {
            aeDesc = createEngineDescription(NoOpAnnotator.class);
        }
        return new AnnotationIteratorOverCollection<A>(new JCasIterable(colReaderDesc, aeDesc),
                annotationClass);
    }

    private void getNextCas() {
        JCas curJCas = jCasIter.next();
        onCasChange(curJCas);
        curJCasAnnotationIter = getIterator(curJCas);
    }

    @Override
    protected AT computeNext() {
        if (curJCasAnnotationIter == null) {
            return endOfData();
        }
        while (curJCasAnnotationIter != null && !curJCasAnnotationIter.hasNext()) {
            if (jCasIter.hasNext()) {
                getNextCas();
            } else {
                curJCasAnnotationIter = null;
            }
        }
        if (curJCasAnnotationIter == null) {
            return endOfData();
        }
        return curJCasAnnotationIter.next();
    }

    private Iterator<AT> getIterator(JCas jCas) {
        return JCasUtil.select(jCas, annotationClass).iterator();
    }

    protected void onCasChange(JCas jCas) {
        // override in subclasses
    }

    @Override
    public void close() throws IOException {
        if (jCasIter instanceof Closeable) {
            ((Closeable) jCasIter).close();
        }
    }
}