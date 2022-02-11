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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import static com.textocat.textokit.morph.model.MorphConstants.*;

/**
 * @author Rinat Gareev
 */
public class SintagrusUtils {

    public static Set<String> mapToOpenCorpora(Set<String> srcSet) {
        srcSet = Sets.newHashSet(srcSet);
        Set<String> result = Sets.newHashSet();
        for (Submapper sm : submappers) {
            sm.apply(srcSet, result);
        }
        if (!srcSet.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Unknown grammemes: %s", srcSet));
        }
        return result;
    }

    private static final List<Submapper> submappers;

    static {
        Builder<Submapper> b = ImmutableList.<Submapper>builder();
        // noun
        b.add(replace("S", NOUN));
        // adjectives
        b.add(replace(new String[]{"A", "СРАВ"}, COMP));
        b.add(replace(new String[]{"A", "КР"}, ADJS));
        b.add(replace("A", ADJF));
        // verbs
        b.add(replace(new String[]{"V", "ПРИЧ", "КР"}, PRTS));
        b.add(replace(new String[]{"V", "ПРИЧ"}, PRTF));
        b.add(replace(new String[]{"V", "ИНФ"}, INFN));
        b.add(replace(new String[]{"V", "ДЕЕПР"}, GRND));
        b.add(replace("V", VERB));
        // adverb
        b.add(replace(new String[]{"ADV", "СРАВ"}, COMP));
        b.add(replace("ADV", ADVB));
        // numeral
        b.add(replace("NUM", NUMR));
        // preposition
        b.add(replace("PR", PREP));
        // TODO COM
        // conjunction
        b.add(replace("CONJ", CONJ));
        // particle
        b.add(replace("PART", PRCL));
        // P
        b.add(replace("P", PRCL));
        // interject