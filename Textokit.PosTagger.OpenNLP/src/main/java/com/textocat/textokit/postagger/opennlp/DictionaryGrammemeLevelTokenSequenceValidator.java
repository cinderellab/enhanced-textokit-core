
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.textocat.textokit.commons.util.BitUtils;
import com.textocat.textokit.morph.commons.*;
import com.textocat.textokit.morph.dictionary.WordUtils;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionary;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionaryHolder;
import com.textocat.textokit.morph.model.MorphConstants;
import com.textocat.textokit.morph.model.Wordform;
import com.textocat.textokit.tokenizer.fstype.Token;
import opennlp.tools.util.SequenceValidator;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.initialize.ExternalResourceInitializer;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.initializable.Initializable;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.BitSet;
import java.util.List;

import static com.textocat.textokit.morph.dictionary.resource.MorphDictionaryUtils.toGramBits;
import static com.textocat.textokit.morph.model.Wordform.allGramBitsFunction;

/**
 * @author Rinat Gareev
 */
public class DictionaryGrammemeLevelTokenSequenceValidator
        implements SequenceValidator<Token>, Initializable {

    public static final String RESOURCE_MORPH_DICT = "morphDict";

    @ExternalResource(key = RESOURCE_MORPH_DICT, mandatory = true)
    private MorphDictionaryHolder morphDictionaryHolder;
    //
    private MorphDictionary morphDictionary;
    private GramModel gramModel;
    //
    private List<BitSet> skipMasks;
    private TwoTagPredicate agreementPredicate;
    private int adjfId;
    private int nounId;

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        ExternalResourceInitializer.initialize(this, ctx);
        morphDictionary = morphDictionaryHolder.getDictionary();
        gramModel = morphDictionary.getGramModel();
        // TODO this is application-specific tunings. Refactor them out
        skipMasks = Lists.newLinkedList();
        {
            BitSet mask = new BitSet();
            mask.set(gramModel.getGrammemNumId(MorphConstants.Abbr));
            skipMasks.add(mask);
        }
        {
            BitSet mask = new BitSet();
            // mask.set(gramModel.getGrammemNumId(RNCMorphConstants.RNC_INIT));
            // FIXME eliminate this hard-coding, derive skipMask, etc. from the configuration
            mask.set(gramModel.getGrammemNumId("RNC_INIT"));
            skipMasks.add(mask);
        }
        {
            BitSet mask = new BitSet();
            mask.set(gramModel.getGrammemNumId(MorphConstants.Prnt));
            skipMasks.add(mask);
        }
        skipMasks = ImmutableList.copyOf(skipMasks);
        //
        agreementPredicate = TwoTagPredicateConjunction.and(
                AgreementPredicates.numberAgreement(gramModel),
                AgreementPredicates.genderAgreement(gramModel),
                AgreementPredicates.caseAgreement(gramModel));
        //
        adjfId = gramModel.getGrammemNumId(MorphConstants.ADJF);
        nounId = gramModel.getGrammemNumId(MorphConstants.NOUN);
    }

    @Override
    public boolean validSequence(int i, Token[] inputSequence, String[] outcomesSequence,
                                 String outcome) {
        Token curToken = inputSequence[i];
        // do basic check
        if (!PunctuationTokenSequenceValidator.checkForPunctuationTag(curToken, outcome)) {
            return false;
        }
        // do not validate punctuation tags as it is done before
        if (PunctuationUtils.isPunctuationTag(outcome)) {
            return true;
        }
        outcome = TagUtils.postProcessExternalTag(outcome);
        // dictionary look-up
        String tokenStr = curToken.getCoveredText();
        tokenStr = WordUtils.normalizeToDictionaryForm(tokenStr);
        List<Wordform> dictEntries = morphDictionary.getEntries(tokenStr);
        if (dictEntries == null || dictEntries.isEmpty()) {
            return !TagUtils.isClosedClassTag(outcome);
        }
        // dictEntries is not empty so null-tag is not valid in most cases
        if (outcome == null) {
            return false;
        }
        // parse tag
        // TODO do not rely on the specific implementation of TagMapper
        Iterable<String> candidateGrams = GramModelBasedTagMapper.parseTag(outcome);
        BitSet candidateBS = toGramBits(gramModel, candidateGrams);
        for (BitSet sm : skipMasks) {
            if (BitUtils.contains(candidateBS, sm)) {
                return true;
            }
        }
        // check containment
        List<BitSet> dictBSes = Lists.transform(dictEntries, allGramBitsFunction(morphDictionary));
        for (BitSet de : dictBSes) {
            if (BitUtils.contains(de, candidateBS)) {
                return true;
            }
            // check nominalization
            if (candidateBS.get(nounId) && de.get(adjfId)
                    && agreementPredicate.apply(candidateBS, de)) {
                return true;
            }
        }
        return false;
    }
}