
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.textocat.textokit.morph.dictionary.resource.MorphDictionaryUtils.toGramBits;

/**
 * @author Rinat Gareev
 */
public class AgreementPredicate implements TwoTagPredicate {

    public static class Builder {
        private Multimap<Set<String>, Set<String>> agreementMap = HashMultimap.create();

        public Builder agree(String gram1) {
            return agree(gram1, gram1);
        }

        public Builder agree(String gram1, String gram2) {
            return agree(ImmutableSet.of(gram1), ImmutableSet.of(gram2));
        }

        public Builder agree(String gram1, Set<String> gram2) {
            return agree(ImmutableSet.of(gram1), gram2);
        }

        public Builder agree(Set<String> gram1, Set<String> gram2) {
            gram1 = ImmutableSet.copyOf(gram1);
            gram2 = ImmutableSet.copyOf(gram2);
            agreementMap.put(gram1, gram2);
            if (!gram1.equals(gram2)) {
                // add symmetrical
                agreementMap.put(gram2, gram1);
            }
            return this;
        }

        // MUST NOT change the state of this builder
        public AgreementPredicate build(GramModel gm) {
            // convert Strings to BitSet
            Multimap<BitSet, BitSet> bitAgrMap = HashMultimap.create();
            for (Set<String> key : agreementMap.keySet()) {
                BitSet keyBits = toGramBits(gm, key);
                for (Set<String> value : agreementMap.get(key)) {
                    BitSet valueBits = toGramBits(gm, value);
                    bitAgrMap.put(keyBits, valueBits);
                }
            }
            return new AgreementPredicate(gm, bitAgrMap);
        }
    }

    private final GramModel gramModel;
    private final BitSet categoryMask;
    private final Multimap<BitSet, BitSet> agreementMap;

    private static final Logger log = LoggerFactory.getLogger(AgreementPredicate.class);

    private AgreementPredicate(GramModel gm, Multimap<BitSet, BitSet> agreementMap) {
        this.gramModel = gm;
        this.agreementMap = ImmutableMultimap.copyOf(agreementMap);
        categoryMask = new BitSet();
        for (Map.Entry<BitSet, BitSet> e : agreementMap.entries()) {
            categoryMask.or(e.getKey());
            categoryMask.or(e.getValue());
        }
        // done
    }

    @Override
    public boolean apply(BitSet _first, BitSet _second) {
        BitSet first = (BitSet) _first.clone();
        BitSet second = (BitSet) _second.clone();
        //
        first.and(categoryMask);
        if (first.isEmpty()) {
            return false;
        }
        second.and(categoryMask);
        if (second.isEmpty()) {
            return false;
        }
        //
        Collection<BitSet> equivs = agreementMap.get(first);
        if (equivs.isEmpty()) {
            log.warn("Unknown combination of gramemmes: {}", gramModel.toGramSet(first));
        }
        for (BitSet aggr : equivs) {
            if (aggr.equals(second)) {
                return true;
            }
        }
        return false;
    }
}