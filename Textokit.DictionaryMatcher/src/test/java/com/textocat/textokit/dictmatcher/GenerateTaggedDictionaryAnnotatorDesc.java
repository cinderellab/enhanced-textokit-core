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

package com.textocat.textokit.dictmatcher;

import com.textocat.textokit.dictmatcher.fs.DictionaryMatch;
import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.textocat.textokit.dictmatcher.DictionaryAnnotator.PARAM_CHUNK_ADAPTER_CLASS;
import static com.textocat.textokit.dictmatcher.TaggedChunkAnnotationAdapter.PARAM_RESULT_ANNOTATION_TYPE;
import static com.textocat.textokit.dictmatcher.TaggedChunkAnnotationAdapter.PARAM_TAG_FEATURE;

/**
 * @author Rinat Gareev
 */
public class GenerateTaggedDictionaryAnnotatorDesc {
    public static void main(String[] args) throws ResourceInitializationException, IOException, SAXException {
        String relOutPath = (DictionaryAnnotator.class.getName() + "-tagged").replace('.', '/') + ".xml";
        TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription(
                "com.textocat.textokit.dictmatcher.ts-dictmatcher");
        AnalysisEngineDescription desc = AnalysisEngineFactory.createEngineDescription(DictionaryAnnotator.class,
                tsd,
                PARAM_CHUNK_ADAPTER_CLASS, TaggedChunkAnnotationAdapter.class.getName(),
                PARAM_RESULT_ANNOTATION_TYPE, DictionaryMatch.class.getName(),
                PARAM_TAG_FEATURE, "tag");
        try (FileOutputStream os = FileUtils.