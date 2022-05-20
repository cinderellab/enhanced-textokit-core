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

package com.textocat.textokit.postagger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.textocat.textokit.commons.cpe.XmiCollectionReader;
import com.textocat.textokit.commons.util.Slf4jLoggerImpl;
import com.textocat.textokit.segmentation.SentenceSplitterAPI;
import com.textocat.textokit.tokenizer.TokenizerAPI;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;
import java.util.List;

import static com.textocat.textokit.morph.dictionary.MorphDictionaryAPIFactory.getMorphDictionaryAPI;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

/**
 * @author Rinat Gareev
 */
@Parameters(separators = " =")
public class DictionaryComplianceCheckerBootstrap {

    public static void main(String[] args) throws Exception {
        DictionaryComplianceCheckerBootstrap instance = new DictionaryComplianceCheckerBootstrap();
        new JCommander(instance, args);
        // setup logging
        Slf4jLoggerImpl.forceUsingThisImplementation();
        instance.run();
    }

    @Parameter(names = {"-c", "--corpus-dir"}, required = true)
    private File corpusDir;
    @Parameter(names = {"-o", "--output-file"}, required = true)
    private File outFile;
    @Parameter(names = {"-p", "--pos-categories"}, required = true)
    private List<String> posCategories;

    private DictionaryComplianceCheckerBootstrap() {
    }

    private void run() throws Exception {
        //
        CollectionReaderDescription colReaderDesc;
        {
            TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
                    .createTypeSystemDescription(
                            "com.textocat.textokit.commons.Commons-TypeSystem",
                            TokenizerAPI.TYPESYSTEM_TOKENIZER,
                            SentenceSplitterAPI.TYPESYSTEM_SENTENCES,
                            PosTaggerAPI.TYPESYSTEM_POSTAGGER);
            //
            colReaderDesc = CollectionReaderFactory.createReaderDescription(
                    XmiCollectionReader.class,
                    tsDesc,
                    XmiCollectionReader.PARAM_INPUTDIR, corpusDir.getPath());
        }
        //
        AnalysisEngineDescription dcCheckerDesc = createEngineDescription(
                DictionaryComplianceChecker.class,
                DictionaryComplianceChecker.PARAM_OUT_FILE, outFile,
                DictionaryComplianceChecker.PARAM_TARGET_POS_CATEGORIES, posCategories);
        //
        ExternalResourceDescription morphDictDesc = getMorphDictionaryAPI()
                .getResourceDescriptionForCachedInstance();
        ExternalResourceFactory.bindResource(dcCheckerDesc,
                DictionaryComplianceChecker.RESOURCE_DICTIONARY, morphDictDesc);
        // make AGGREGATE
        AnalysisEngineDescription aggregateDesc = createEngineDescription(dcCheckerDesc);
        //
        SimplePipeline.runPipeline(colReaderDesc, aggregateDesc);
    }
}