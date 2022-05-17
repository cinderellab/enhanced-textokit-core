
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

package com.textocat.textokit.morph.commons;

import com.textocat.textokit.morph.dictionary.MorphDictionaryAPIFactory;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import org.junit.Test;

import java.util.BitSet;

import static com.textocat.textokit.morph.commons.GramModelBasedTagMapper.parseTag;
import static com.textocat.textokit.morph.dictionary.resource.MorphDictionaryUtils.toGramBits;
import static org.junit.Assert.assertEquals;

/**
 * @author Rinat Gareev
 */
public class AgreementPredicateTest {

    private GramModel gm;

    public AgreementPredicateTest() throws Exception {
        gm = MorphDictionaryAPIFactory.getMorphDictionaryAPI().getGramModel();
    }

    @Test
    public void testCaseAgreement() {
        AgreementPredicate pred = AgreementPredicates.caseAgreement(gm);
        assertEquals(false, pred.apply(bits("NOUN"), bits("NOUN&gent")));
        assertEquals(true, pred.apply(bits("NOUN&gent"), bits("ADJF&gent")));
        assertEquals(true, pred.apply(bits("NOUN&gen1"), bits("ADJF&gent")));
        assertEquals(true, pred.apply(bits("NOUN&gen1"), bits("ADJF&gen1")));
        assertEquals(true, pred.apply(bits("NOUN&gent"), bits("NOUN&gen2")));
        assertEquals(true, pred.apply(bits("NOUN&gen2"), bits("NOUN&gen2")));
        assertEquals(false, pred.apply(bits("NOUN&gen1"), bits("NOUN&gen2")));
        assertEquals(true, pred.apply(bits("ADJF&nomn"), bits("ADJF&acc2")));
        assertEquals(false, pred.apply(bits("ADJF&loct"), bits("ADJF&ablt")));
        assertEquals(false, pred.apply(bits(""), bits("ADJF&ablt")));
        assertEquals(false, pred.apply(bits("CONJ"), bits("VERB")));
        assertEquals(true, pred.apply(bits("NOUN&accs&sing"), bits("ADJF&accs&plur")));
        assertEquals(true, pred.apply(bits("NOUN&nomn&sing"), bits("ADJF&nomn&plur")));
        assertEquals(false, pred.apply(bits("NOUN&nomn&sing"), bits("ADJF&datv&plur")));
        assertEquals(true, pred.apply(bits("NOUN&datv&sing"), bits("ADJF&datv&plur")));
        assertEquals(true, pred.apply(bits("NOUN&ablt&sing"), bits("ADJF&ablt&plur")));
        assertEquals(true, pred.apply(bits("NOUN&loct&sing"), bits("ADJF&loct&plur")));
        assertEquals(true, pred.apply(bits("NOUN&loct&sing"), bits("ADJF&loc1&plur")));
        assertEquals(true, pred.apply(bits("NOUN&loc2&sing"), bits("ADJF&loct&plur")));
        assertEquals(false, pred.apply(bits("NOUN&loc2&sing"), bits("ADJF&loc1&plur")));
    }

    @Test
    public void testNumberAggreement() {
        AgreementPredicate pred = AgreementPredicates.numberAgreement(gm);
        assertEquals(false, pred.apply(bits("CONJ"), bits("NOUN&sing")));
        assertEquals(false, pred.apply(bits("ADJF&plur"), bits("")));
        assertEquals(false, pred.apply(bits("ADJF&plur"), bits("NOUN&sing")));
        assertEquals(false, pred.apply(bits("ADJF&sing"), bits("NPRO&plur")));
        assertEquals(true, pred.apply(bits("ADJF&sing"), bits("NOUN&sing")));
        assertEquals(true, pred.apply(bits("ADJF&plur&Apro"), bits("NOUN&plur")));
    }

    @Test
    public void testGenderAgreement() {
        AgreementPredicate pred = AgreementPredicates.genderAgreement(gm);
        assertEquals(false, pred.apply(bits(""), bits("")));
        assertEquals(false, pred.apply(bits("VERB"), bits("")));
        assertEquals(false, pred.apply(bits("ADJF&GNdr&Apro"), bits("NUMR")));
        assertEquals(true, pred.apply(bits("ADJF&plur&masc&Apro"), bits("NOUN&sing&GNdr")));
        assertEquals(true, pred.apply(bits("NOUN&sing&GNdr"), bits("ADJF&plur&neut&Apro")));
        assertEquals(true, pred.apply(bits("ADJF&plur&femn&Apro"), bits("NOUN&sing&GNdr")));
        assertEquals(true, pred.apply(bits("NOUN&plur&GNdr"), bits("NOUN&sing&GNdr")));
        assertEquals(true, pred.apply(bits("NOUN&plur&masc"), bits("ADJF&sing&masc")));
        assertEquals(true, pred.apply(bits("NOUN&plur&neut"), bits("ADJF&sing&neut")));
        assertEquals(true, pred.apply(bits("NOUN&plur&femn"), bits("ADJF&sing&femn")));
        assertEquals(false, pred.apply(bits("NOUN&plur&femn"), bits("ADJF&sing&neut")));
        assertEquals(false, pred.apply(bits("NOUN&plur&femn"), bits("ADJF&sing&masc")));
        assertEquals(false, pred.apply(bits("NOUN&plur&masc"), bits("ADJF&sing&neut")));
    }

    private BitSet bits(String tag) {
        return toGramBits(gm, parseTag(tag));
    }
}