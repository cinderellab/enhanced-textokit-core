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


package com.textocat.textokit.commons.cas;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.uima.cas.*;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;

import java.util.*;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;

/**
 * @author Rinat Gareev
 */
public class FSUtils {

    private FSUtils() {
    }

    public static JCas getJCas(FeatureStructure fs) {
        try {
            return fs.getCAS().getJCas();
        } catch (CASException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean contain(ArrayFS arr, FeatureStructure targetFS) {
        if (arr == null) {
            return false;
        }
        for (int i = 0; i < arr.size(); i++) {
            if (Objects.equal(arr.get(i), targetFS)) {
                return true;
            }
        }
        return false;
    }

    public static FSArray toFSArray(JCas cas, Collection<? extends FeatureStructure> srcCol) {
        return toFSArray(cas, srcCol, srcCol.size());
    }

    public static FSArray toFSArray(JCas cas, FeatureStructure... srcArr) {
        return toFSArray(cas, Arrays.asList(srcArr), srcArr.length);
    }

    public static FSArray toFSArray(JCas cas, Iterable<? extends FeatureStructure> srcCol,
                                    int srcSize) {
        FSArray result = new FSArray(cas, srcSize);
        int i = 0;
        for (FeatureStructure fs : srcCol) {
            result.set(i, fs);
            i++;
        }
        return result;
    }

    public static StringArray toStringArray(JCas cas, String... srcArr) {
        return toStringArray(cas, Arrays.asList(srcArr));
    }

    public static StringArray toStringArray(JCas cas, Collection<String> srcCol) {
        StringArray result = new StringArray(cas, srcCol.size());
        int i = 0;
        for (String gr : srcCol) {
            result.set(i, gr