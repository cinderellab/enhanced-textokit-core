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
            result.set(i, gr);
            i++;
        }
        return result;
    }

    public static Set<String> toSet(StringArrayFS fsArr) {
        if (fsArr == null)
            return ImmutableSet.of();
        ImmutableSet.Builder<String> resultBuilder = ImmutableSet.builder();
        for (int i = 0; i < fsArr.size(); i++) {
            resultBuilder.add(fsArr.get(i));
        }
        return resultBuilder.build();
    }

    public static List<String> toList(StringArrayFS fsArr) {
        if (fsArr == null)
            return ImmutableList.of();
        ImmutableList.Builder<String> resultBuilder = ImmutableList.builder();
        for (int i = 0; i < fsArr.size(); i++) {
            resultBuilder.add(fsArr.get(i));
        }
        return resultBuilder.build();
    }

    public static FSTypeConstraint getTypeConstraint(Type firstType, Type... otherTypes) {
        FSTypeConstraint constr = ConstraintFactory.instance().createTypeConstraint();
        constr.add(firstType);
        for (Type t : otherTypes) {
            constr.add(t);
        }
        return constr;
    }

    public static FSTypeConstraint getTypeConstraint(String firstType, String... otherTypes) {
        FSTypeConstraint constr = ConstraintFactory.instance().createTypeConstraint();
        constr.add(firstType);
        for (String t : otherTypes) {
            constr.add(t);
        }
        return constr;
    }

    public static <FST extends FeatureStructure> List<FST> filterToList(CAS cas,
                                                                        FSIterator<FST> srcIter, FSMatchConstraint... constraints) {
        FSIterator<FST> resultIter = filter(cas, srcIter, constraints);
        return toList(resultIter);
    }

    public static <F extends FeatureStructure> FSIterator<F> filter(CAS cas,
                                                                    FSIterator<F> srcIter, FSMatchConstraint... constraints) {
        if (constraints.length == 0) {
            return srcIter;
        }
        FSMatchConstraint resultConstr = and(constraints);
        return cas.createFilteredIterator(srcIter, resultConstr);
    }

    public static <F extends FeatureStructure> List<F> filter(List<F> srcList,
                                                              FSMatchConstraint... constraints) {
        if (constraints.length == 0) {
            return ImmutableList.copyOf(srcList);
        }
        ArrayList<F> resultList = Lists.newArrayListWithCapacity(srcList.size());
        FSMatchConstraint conj = and(constraints);
        for (F fs : srcList) {
            if (conj.match(fs)) {
                resultList.add(fs);
            }
        }
        return Collections.unmodifiableList(resultList);
    }

    public static FSMatchConstraint and(FSMatchConstraint... constraints) {
        if (constraints.length == 0) {
            throw new IllegalArgumentException("Constraints array are empty");
        }
        ConstraintFactory cf = ConstraintFactory.instance();
        FSMatchConstraint resultConstr = constraints[0];
        for (int i = 1; i < constraints.length; i++) {
            resultConstr = cf.and(resultConstr, constraints[i]);
        }
        return resultConstr;
    }

    public static <FST extends FeatureStructure> List<FST> toList(FSIterator<FST> iter) {
        LinkedList<FST> result = newLinkedList();
        fill(iter, result);
        return result;
    }

    public static <FST extends FeatureStructure> Set<FST> toSet(FSIterator<FST> iter) {
        HashSet<FST> result = newHashSet();
        fill(iter, result);
        return result;
    }

    public static <FST extends FeatureStructure> void fill(FSIterator<FST> srcIter,
                                                           Collection<FST> destCol) {
        srcIter.moveToFirst();
        while (srcIter.isValid()) {
            destCol.add(srcIter.get());
            srcIter.moveToNext();
        }
    }

    /*
     * Note that getIntValue will return 0 if feature value is not set.
     */
    public static int intMinBy(Iterable<? extends FeatureStructure> fsCollection, Feature intFeat) {
        Integer min = Integer.MAX_VALUE;
        boolean hasResult = false;
        for (FeatureStructure fs : fsCollection) {
            int intValue = fs.getIntValue(intFeat);
            hasResult = true;
            if (intValue < min) {
                min = intValue;
            }
        }
        if (!hasResult) {
            throw new IllegalArgumentException("fsCollection is empty");
        }
        return min;
    }

    /*
     * Note that getIntValue will return 0 if feature value is not set.
     */
    public static int intMaxBy(Iterable<? extends FeatureStructure> fsCollection, Feature intFeat) {
        Integer max = Integer.MIN_VALUE;
        boolean hasResult = false;
        for (FeatureStructure fs : fsCollection) {
            int intValue = fs.getIntValue(intFeat);
            hasResult = true;
            if (intValue > max) {
                max = intValue;
            }
        }
        if (!hasResult) {
            throw new IllegalArgumentException("fsCollection is empty");
        }
        return max;
    }

    public static Function<FeatureStructure, String> stringFeatureFunction(Type fsType, String featName) {
        final Feature feat = fsType.getFeatureByBaseName(featName);
        if (feat == null) throw new IllegalStateException(
                format("%s does not have feature %s", fsType.getName(), featName));
        return new Function<FeatureStructure, String>() {
            @Override
            public String apply(FeatureStructure fs) {
                return fs.getStringValue(feat);
            }
        };
    }

    public static Function<FeatureStructure, String> stringFeaturePathFunc(
            CAS cas, Type inputFSType, String featurePath) throws CASException {
        final FeaturePath path = cas.createFeaturePath();
        path.initialize(featurePath);
        path.typeInit(inputFSType);
        return new Function<FeatureStructure, String>() {
            @Override
            public String apply(FeatureStructure fs) {
                return path.getStringValue(fs);
            }
        };
    }
}