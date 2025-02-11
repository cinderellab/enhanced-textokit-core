
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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.textocat.textokit.commons.cas.FSUtils;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionary;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionaryHolder;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.morph.model.Wordform;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import static com.textocat.textokit.morph.dictionary.WordUtils.normalizeToDictionaryForm;
import static com.textocat.textokit.morph.dictionary.resource.MorphDictionaryUtils.toGramBits;
import static com.textocat.textokit.morph.model.Wordform.allGramBitsFunction;

/**
 * @author Rinat Gareev
 */
@OperationalProperties(multipleDeploymentAllowed = false)
public class DictionaryComplianceChecker extends JCasAnnotator_ImplBase {

    public static final String PARAM_OUT_FILE = "outFile";
    public static final String PARAM_TARGET_POS_CATEGORIES = "targetPosCategories";
    public static final String RESOURCE_DICTIONARY = "MorphDictionary";

    // config fields
    @ExternalResource(key = RESOURCE_DICTIONARY, mandatory = true)
    private MorphDictionaryHolder dictHolder;
    private MorphDictionary dict;
    private GramModel gramModel;
    @ConfigurationParameter(name = PARAM_OUT_FILE, mandatory = true)
    private File outFile;
    @ConfigurationParameter(name = PARAM_TARGET_POS_CATEGORIES, mandatory = true)
    private Set<String> targetPosCategories;
    private PosTrimmer posTrimmer;

    // state fields
    private PrintWriter out;
    private int notDictNum;
    private int matchedNum;
    private int notMatchedNum;

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        super.initialize(ctx);
        dict = dictHolder.getDictionary();
        gramModel = dict.getGramModel();
        posTrimmer = new PosTrimmer(dict.getGramModel(), targetPosCategories);
        try {
            FileOutputStream os = FileUtils.openOutputStream(outFile);
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(os, "utf-8")),
                    true);
            // write header
            out.println("Word\tGrams_diff\tCorpus_grams\tDict_grams");
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        for (Word word : JCasUtil.select(jCas, Word.class)) {
            com.textocat.textokit.morph.fs.Wordform docWf = MorphCasUtils.getOnlyWordform(word);
            if (docWf == null) {
                continue;
            }
            String wordTxt = normalizeToDictionaryForm(word.getCoveredText());
            List<Wordform> dictWfs = dict.getEntries(wordTxt);
            if (dictWfs == null || dictWfs.isEmpty()) {
                notDictNum++;
                continue;
            }
            // convert to BitSet
            BitSet docBits = toGramBits(gramModel, FSUtils.toSet(docWf.getGrammems()));
            posTrimmer.trimInPlace(docBits);
            List<BitSet> _dictBitSets = Lists.transform(dictWfs, allGramBitsFunction(dict));
            Set<BitSet> dictBitSets = posTrimmer.trimAndMerge(_dictBitSets);
            dictBitSets = selectClosest(docBits, dictBitSets);
            if (calcDistance(docBits, dictBitSets.iterator().next()) == 0) {
                matchedNum++;
            } else {
                notMatchedNum++;
                StringBuilder record = new StringBuilder(wordTxt);
                record.append('\t');
                List<String> gramDiffs = Lists.newLinkedList();
                for (BitSet dictBits : dictBitSets) {
                    List<String> grams = Lists.newLinkedList();
                    {
                        BitSet positiveBits = (BitSet) docBits.clone();
                        positiveBits.andNot(dictBits);
                        grams.addAll(Lists.transform(gramModel.toGramSet(positiveBits),
                                positiveGramFunc));
                    }
                    {
                        BitSet negativeBits = (BitSet) dictBits.clone();
                        negativeBits.andNot(docBits);
                        grams.addAll(Lists.transform(gramModel.toGramSet(negativeBits),
                                negativeGramFunc));
                    }
                    gramDiffs.add(gramJoiner.join(grams));
                }
                gramSetJoiner.appendTo(record, gramDiffs);
                // write corpus grams
                record.append('\t');
                gramJoiner.appendTo(record, gramModel.toGramSet(docBits));
                // write dict grams
                record.append('\t');
                gramSetJoiner.appendTo(record,
                        Collections2.transform(dictBitSets, gramBitsToString));
                out.println(record);
            }
        }
    }

    private static final Joiner gramJoiner = Joiner.on(',');
    private static final Joiner gramSetJoiner = Joiner.on("||");

    private static Set<BitSet> selectClosest(final BitSet targetBits, Iterable<BitSet> srcBitSets) {
        Set<BitSet> result = Sets.newHashSet();
        int minDistance = Integer.MAX_VALUE;
        for (final BitSet srcBits : srcBitSets) {
            int curDistance = calcDistance(targetBits, srcBits);
            if (curDistance < minDistance) {
                result.clear();
                result.add(srcBits);
                minDistance = curDistance;
            } else if (curDistance == minDistance) {
                result.add(srcBits);
            }
            // else curDistance > minDistance => do nothing
        }
        return result;
    }

    private static int calcDistance(final BitSet xArg, final BitSet yArg) {
        BitSet x = (BitSet) xArg.clone();
        x.xor(yArg);
        return x.cardinality();
    }

    private static Function<String, String> prefixFunction(final String prefix) {
        return new Function<String, String>() {
            @Override
            public String apply(String arg) {
                return prefix + arg;
            }
        };
    }

    private static Function<String, String> positiveGramFunc = prefixFunction("+");
    private static Function<String, String> negativeGramFunc = prefixFunction("-");

    private Function<BitSet, String> gramBitsToString = new Function<BitSet, String>() {
        @Override
        public String apply(BitSet bits) {
            return gramJoiner.join(gramModel.toGramSet(bits));
        }
    };

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        IOUtils.closeQuietly(out);
        getLogger().info(String.format("%s report:\n" +
                        "not dictionary words: %s\n" +
                        "match dictionary tags: %s\n" +
                        "do not match dictionary tags:%s",
                getClass().getSimpleName(),
                notDictNum, matchedNum, notMatchedNum));
    }
}