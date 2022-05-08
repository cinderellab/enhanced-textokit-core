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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.morph.fs.Wordform;
import com.textocat.textokit.postagger.MorphCasUtils;

import java.util.BitSet;
import java.util.Set;

import static com.textocat.textokit.morph.commons.PunctuationUtils.punctuationTagMap;
import static com.textocat.textokit.morph.model.MorphConstants.*;

/**
 * EXPERIMENTAL <br>
 * EXPERIMENTAL <br>
 * EXPERIMENTAL
 *
 * @author Rinat Gareev
 */
public class TagUtils {

    private static final Set<String> closedPosSet = ImmutableSet.of(NPRO, Apro, PREP, CONJ, PRCL);

    /**
     * @param dict
     * @return function that returns true if the given gram bits represents a
     * closed class tag
     */
    public static Function<BitSet, Boolean> getClosedClassIndicator(GramModel gm) {
        // initialize mask
        final BitSet closedClassTagsMask = new BitSet();
