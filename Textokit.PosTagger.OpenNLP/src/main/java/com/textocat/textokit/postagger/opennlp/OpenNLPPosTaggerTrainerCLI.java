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

package com.textocat.textokit.postagger.opennlp;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.textocat.textokit.commons.cpe.AnnotationIteratorOverCollection;
import com.textocat.textokit.commons.cpe.XmiCollectionReader;
import com.textocat.textokit.commons.util.Slf4jLoggerImpl;
import com.textocat.textokit.morph.commons.GramModelBasedTagMapper;
import com.textocat.textokit.morph.commons.TagAssembler;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionary;
import com.textocat.textokit.postagger.PosTaggerAPI;
import com.textocat.textokit.postagger.PosTrimmingAnnotator;
import com.textocat.textokit.segmentation.SentenceSplitterAPI;
import com.textocat.textokit.segmentation.fstype.Sentence;
import com.textocat.textokit.tokenizer.TokenizerAPI;
import opennlp.tools.util.TrainingParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static com.textocat.textokit.morph.dictionary.MorphDictionaryAPIFactory.getMorphDictionaryAPI;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindExternalResource;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

/**
 * @author Rinat Gareev
 */
public class OpenNLPPosTaggerTrainerCLI {

    static {
        Slf4jLoggerImpl.forceUsingThisImplementation();
    }

    public static void main(String[] args) throws Exception {
        OpenNLPPosTaggerTrainerCLI cli = new OpenNLPPosTaggerTrainerCLI();
        new JCommander(cli, args);
        //
        OpenNLPPosTaggerTrainer trainer = new OpenNLPPosTaggerTrainer();
        trainer.setLanguageCode(cli.languageCode);
        trainer.setModelOutFile(cli.modelOutFile);
        // train params
        {
            FileInputStream fis = FileUtils.openInputStream(cli.trainParamsFile);
            TrainingParameters trainParams;
            try {
                trainParams = new TrainingParameters(fis);
            } finally {
                IOUtils.closeQuietly(fis);
            }
            trainer.setTrainingParameters(trainParams);
        }
        // feature extractors
        {
            FileInputStream fis = FileUtils