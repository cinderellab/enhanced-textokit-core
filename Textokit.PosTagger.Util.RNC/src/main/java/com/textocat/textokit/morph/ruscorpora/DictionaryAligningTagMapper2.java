
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

package com.textocat.textokit.morph.ruscorpora;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.textocat.textokit.commons.cas.FSUtils;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionary;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionaryHolder;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.morph.model.Wordform;
import com.textocat.textokit.postagger.PosTrimmer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.component.initialize.ExternalResourceInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.initializable.Initializable;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import static com.textocat.textokit.commons.util.BitUtils.contains;
import static com.textocat.textokit.commons.util.DocumentUtils.getDocumentUri;
import static com.textocat.textokit.morph.dictionary.WordUtils.normalizeToDictionaryForm;
import static com.textocat.textokit.morph.dictionary.resource.MorphDictionaryUtils.toGramBits;
import static com.textocat.textokit.morph.model.MorphConstants.*;
import static com.textocat.textokit.morph.model.Wordform.allGramBitsFunction;

/**
 * @author Rinat Gareev
 */
public class DictionaryAligningTagMapper2 implements RusCorporaTagMapper, Initializable, Closeable {

    public static final String RESOURCE_KEY_MORPH_DICTIONARY = "MorphDictionary";
    public static final String PARAM_OUT_FILE = "outFile";
    // config fields
    private RusCorporaTagMapper delegate = new RusCorpora2OpenCorporaTagMapper();
    @ExternalResource(key = RESOURCE_KEY_MORPH_DICTIONARY, mandatory = true)
    private MorphDictionaryHolder dictHolder;
    @ConfigurationParameter(name = PARAM_OUT_FILE, mandatory = true)
    private File outFile;
    // config-derived
    private MorphDictionary dict;
    private GramModel gm;
    private PrintWriter out;
    //
    private BitSet rncDistortionsMask;
    private PosTrimmer rncTrimmer;

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        ExternalResourceInitializer.initialize(this, ctx);
        ConfigurationParameterInitializer.initialize(this, ctx);
        dict = dictHolder.getDictionary();
        gm = dict.getGramModel();
        try {
            FileOutputStream os = FileUtils.openOutputStream(outFile);
            Writer bw = new BufferedWriter(new OutputStreamWriter(os, "utf-8"));
            out = new PrintWriter(bw);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        //
        rncDistortionsMask = new BitSet();
        rncDistortionsMask.set(gm.getGrammemNumId(Dist));
        rncDistortionsMask.set(gm.getGrammemNumId(RNCMorphConstants.RNC_Abbr));
        //
        rncTrimmer = new PosTrimmer(gm, POST, Anum, Apro, Prnt,
                GNdr, ANim, NMbr, CAse, Supr,
                ASpc, TRns, VOic, MOod, TEns, PErs, INvl,
                Name, Patr, Surn, Fixd,
                Dist, RNCMorphConstants.RNC_Abbr, RNCMorphConstants.RNC_INIT);
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(out);
    }

    @Override
    public void mapFromRusCorpora(RusCorporaWordform srcWf, com.textocat.textokit.morph.fs.Wordform targetWf) {
        Word wordAnno = targetWf.getWord();
        delegate.mapFromRusCorpora(srcWf, targetWf);
        // first - check whether tag is in tagset
        final BitSet wfTag = toGramBits(gm, FSUtils.toList(targetWf.getGrammems()));
        // skip INIT as a whole new category
        if (wfTag.get(gm.getGrammemNumId(RNCMorphConstants.RNC_INIT))) {
            return;
        }
        if (wfTag.intersects(rncDistortionsMask)) {
            wfTag.andNot(rncDistortionsMask);
            if (!dict.containsGramSet(wfTag)) {
                onDistortedWordWithUnknownTag(wordAnno, wfTag);
            }
            return;
        }
        //
        // if there is no such tag in dictionary then look the word in it
        String wordStr = wordAnno.getCoveredText();
        List<Wordform> dictWfs = dict.getEntries(normalizeToDictionaryForm(wordStr));
        if (dictWfs == null || dictWfs.isEmpty()) {
            // wfTag.isEmpty => NON-LEX
            if (!wfTag.isEmpty() && !dict.containsGramSet(wfTag)) {
                onUnknownWordWithUnknownTag(wordAnno, wfTag);
            }
            return;
        }
        // retain only the gram categories that are represented in RNC tagset
        Set<BitSet> dictTags = rncTrimmer.trimAndMerge(Lists.transform(dictWfs,
                allGramBitsFunction(dict)));
        // search for a unique dictionary entry that has a tag extending the given one
        List<BitSet> wfExtensions = Lists.newLinkedList();
        for (BitSet dTag : dictTags) {
            if (contains(dTag, wfTag)) {
                wfExtensions.add(dTag);
            }
        }
        if (wfExtensions.isEmpty()) {
            onConflictingTag(wordAnno, wfTag);
        } else if (wfExtensions.size() > 1) {
            onAmbiguousWordform(wordAnno, wfTag);
        } else {
            BitSet newTag = wfExtensions.get(0);
            if (newTag.cardinality() > wfTag.cardinality()) {
                List<String> newTagStr = gm.toGramSet(newTag);
                targetWf.setGrammems(FSUtils.toStringArray(getCAS(targetWf), newTagStr));
                onTagExtended(wordAnno, wfTag, newTag);
            }
        }
    }

    private static JCas getCAS(FeatureStructure fs) {
        try {
            return fs.getCAS().getJCas();
        } catch (CASException e) {
            throw new IllegalStateException(e);
        }
    }

    private void onTagExtended(Word wordAnno, BitSet wfTag, BitSet newTag) {
        out.println(String.format("[+]\t%s\t%s\t%s\t%s",
                wordAnno.getCoveredText(),
                toGramString(wfTag), toGramString(newTag),
                getPrettyLocation(wordAnno)));
    }

    private void onAmbiguousWordform(Word wordAnno, BitSet wfTag) {
        out.println(String.format("[A]\t%s\t%s\t%s",
                wordAnno.getCoveredText(), toGramString(wfTag), getPrettyLocation(wordAnno)));
    }

    private void onConflictingTag(Word wordAnno, BitSet wfTag) {
        out.println(String.format("[C]\t%s\t%s\t%s",
                wordAnno.getCoveredText(), toGramString(wfTag), getPrettyLocation(wordAnno)));
    }

    private void onUnknownWordWithUnknownTag(Word wordAnno, BitSet wfTag) {
        out.println(String.format("[U]\t%s\t%s\t%s",
                wordAnno.getCoveredText(), toGramString(wfTag), getPrettyLocation(wordAnno)));
    }

    private void onDistortedWordWithUnknownTag(Word wordAnno, BitSet wfTag) {
        out.println(String.format("[D]\t%s\t%s\t%s",
                wordAnno.getCoveredText(), toGramString(wfTag), getPrettyLocation(wordAnno)));
    }

    private static String getPrettyLocation(Word anno) {
        String docUri = getDocumentUri(anno.getCAS());
        return String.format("%s:%s", docUri, anno.getBegin());
    }

    private String toGramString(BitSet gramBits) {
        return gramJoiner.join(dict.getGramModel().toGramSet(gramBits));
    }

    private static final Joiner gramJoiner = Joiner.on(',');
}