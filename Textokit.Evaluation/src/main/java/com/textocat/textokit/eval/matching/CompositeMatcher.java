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

package com.textocat.textokit.eval.matching;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

import static com.textocat.textokit.commons.cas.FSTypeUtils.getFeature;

/**
 * @author Rinat Gareev
 */
public class CompositeMatcher<FST extends FeatureStructure> extends MatcherBase<FST> {

    private List<Matcher<FST>> matchers;

    private CompositeMatcher() {
    }

    public List<Matcher<FST>> getMatchers() {
        return ImmutableList.copyOf(matchers);
    }

    @Override
    public boolean match(FST ref, FST cand) {
        for (Matcher<FST> curMatcher : matchers) {
            if (!curMatcher.match(ref, cand)) {
                return false;
            }
        }
        return true;
    }

	/* 'equals' implementation has to deal with cyclic graph of matchers.
     * It is not necessary. See also equality checking in tests
	 */
    // public boolean equals(Object obj) {

    @Override
    protected String toString(IdentityHashMap<Matcher<?>, Integer> idMap) {
        if (matchers == null)
            return "null-list";
        else {
            idMap.put(this, getNextId(idMap));
            StringBuilder sb = new StringBuilder("[");
            Iterator<Matcher<FST>> tmIter = matchers.iterator();
            while (tmIter.hasNext()) {
                Matcher<FST> m = tmIter.next();
                sb.append(getToString(idMap, m));
                if (tmIter.hasNext())
                    sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    @Override
    public void print(StringBuilder out, FST value) {
        boolean printExtBrackets = out.length() > 0;
        if (printExtBrackets)
            out.append("[");
        Iterator<Matcher<FST>> iter = matchers.iterator();
        if (iter.hasNext()) {
            iter.next().print(out, value);
        }
        while (iter.hasNext()) {
            out.append("|");
            iter.next().print(out, value);
        }
        if (printExtBrackets)
            out.append("]");
    }

    @Override
    protected Collection<Matcher<?>> getSubMatchers() {
        List<Matcher<?>> result = Lists.newLinkedList();
        result.addAll(matchers);
        return result;
    }

    public static <FST extends FeatureStructure> Builder<FST> builderForFS(Type targetType) {
        return new Builder<FST>(targetType);
    }

    public static AnnotationMatcherBuilder builderForAnnotation(Type targetType) {
        return new AnnotationMatcherBuilder(targetType);
    }

    public static class Builder<FST extends FeatureStructure> {
        protected Type targetType;
        protected CompositeMatcher<FST> instance = new CompositeMatcher<FST>();

        protected Builder(Type targetType) {
            this.targetType = targetType;
            instance.matchers = Lists.newLinkedList();
        }

        public Builder<FST> addTypeChecker() {
            instance.matchers.add(new FSTypeMatcher<FST>(true));
            return this;
        }

        public Builder<FST> addPrimitiveFeatureMatcher(String featName) {
            Feature feature = getFeature(targetType, featName, true);
            instance.matchers.add(new PrimitiveFeatureMatcher<FST>(feature));
            return this;
        }

        public <FVT extends FeatureStru