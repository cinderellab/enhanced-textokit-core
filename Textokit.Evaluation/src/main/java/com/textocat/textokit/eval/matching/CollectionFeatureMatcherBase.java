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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * @author Rinat Gareev
 */
abstract class CollectionFeatureMatcherBase<FST extends FeatureStructure, E> extends
        MatcherBase<FST> {
    protected final Feature feature;
    protected final Matcher<E> elemMatcher;
    protected final boolean ignoreOrder;
    // delegate
    private final CollectionMatcher<E, Collection<E>> collectionMatcherDelegate;

    public CollectionFeatureMatcherBase(Feature feature, Matcher<E> elemMatcher, boolean ignoreOrder) {
        this.feature = feature;
        this.elemMatcher = elemMatcher;
        this.ignoreOrder = ignoreOrder;
        if (!MatchingUtils.isCollectionType(feature.getRange())) {
            throw new IllegalArgumentException(String.format(
                    "Feature '%s' (of type '%s') range is not array", feature, feature.getDomain()));
        }
        collectionMatcherDelegate = new CollectionMatcher<E, Collection<E>>(
                elemMatcher, ignoreOrder);
    }

    @Override
    public boolean match(FST ref, FST cand) {
        Collection<E> refCol = getCollection(ref);
        Collection<E> candCol = getCollection(cand);
        return collectionMatcherDelegate.match(refCol, candCol);
    }

    protected abstract Collection<E> getCollection(FST srcFS);

    @Override
    protected String toString(IdentityHashMap<Matcher<?>, Integer> idMap) {
        idMap.put(this, getNextId(idMap));
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("feature", feature)
                .append("elemMatcher", getToString(idMap, elemMatcher))
                .append("ignoreOrder", ignoreOrder).toString();
    }

    @Override
    protected Collection<Matcher<?>> getSubMatchers() {
        List<Matcher<?>> result = Lists.newLinkedList();
        result.add(elemMatcher);
        return result;
    }

    @Override
    public void print(StringBuilder out, FST value) {
        Collection<E> col = getCollection(value);
        out.append(feature.getShortName());
        out.append("=");
        collectionMatcherDelegate.print(out, col);
    }
}
