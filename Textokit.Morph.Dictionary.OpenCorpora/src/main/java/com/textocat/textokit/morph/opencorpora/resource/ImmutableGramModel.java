
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

package com.textocat.textokit.morph.opencorpora.resource;

import com.google.common.collect.*;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import com.textocat.textokit.morph.model.Grammeme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

import static com.google.common.collect.ImmutableMap.copyOf;

/**
 * @author Rinat Gareev
 */
public class ImmutableGramModel implements GramModel, Serializable {

    private static final long serialVersionUID = -9043821142848807256L;
    // gram set fields
    private Map<String, Grammeme> gramMap;
    private SortedMap<Integer, Grammeme> numToGram;
    // grammem indexes
    private Multimap<String, Grammeme> gramByParent;
    private BitSet posBits;

    private ImmutableGramModel() {
    }

    @Override
    public int getGrammemMaxNumId() {
        /*if (!gramSetLocked) {
			throw new IllegalStateException("gramSet is not locked");
		}*/
        return numToGram.lastKey();
    }

    @Override
    public int getGrammemNumId(String gramId) {
        Grammeme gr = gramMap.get(gramId);
        if (gr == null) {
            noGrammem(gramId);
        }
        return gr.getNumId();
    }

    @Override
    public Grammeme getGrammem(int numId) {
        return numToGram.get(numId);
    }

    @Override
    public BitSet getGrammemWithChildrenBits(String gramId, boolean includeTarget) {
        Grammeme targetGram = getGrammem(gramId);
        if (targetGram == null) {
            return null;
        }
        return getGrammemWithChildrenBits(targetGram, includeTarget);
    }

    private BitSet getGrammemWithChildrenBits(Grammeme gram, boolean includeTarget) {
        BitSet result = new BitSet(getGrammemMaxNumId());
        if (includeTarget) {
            result.set(gram.getNumId());
        }
        for (Grammeme childGram : gramByParent.get(gram.getId())) {
            result.or(getGrammemWithChildrenBits(childGram, true));
        }
        return result;
    }

    @Override
    public Set<String> getTopGrammems() {
        ImmutableSet.Builder<String> resultBuilder = ImmutableSet.builder();
        for (Grammeme gr : gramByParent.get(null)) {
            resultBuilder.add(gr.getId());
        }
        return resultBuilder.build();
    }

    @Override
    public Grammeme getGrammem(String id) {
        return gramMap.get(id);
    }

    @Override
    public List<String> toGramSet(BitSet gramBits) {
        ImmutableList.Builder<String> rb = ImmutableList.builder();
        for (int i = gramBits.nextSetBit(0); i >= 0; i = gramBits.nextSetBit(i + 1)) {
            rb.add(getGrammem(i).getId());
        }
        return rb.build();
    }

    @Override
    public BitSet getPosBits() {
        return (BitSet) posBits.clone();
    }

    @Override
    public String getPos(BitSet lGrams) {
        lGrams.and(getPosBits());
        if (lGrams.isEmpty()) {
            return null;
        }
        if (lGrams.cardinality() > 1) {
            List<String> posList = Lists.newLinkedList();
            for (int i = lGrams.nextSetBit(0); i >= 0; i = lGrams.nextSetBit(i + 1)) {
                posList.add(getGrammem(i).getId());
            }
            throw new IllegalArgumentException(String.format(
                    "More than 1 POS grammeme:\n%s", posList));
        }
        int gramNumId = lGrams.nextSetBit(0);
        Grammeme result = getGrammem(gramNumId);
        notNull(result);
        return result.getId();
    }

    private void noGrammem(String id) {
        throw new IllegalStateException(String.format(
                "Grammem with id = %s is not registered", id));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ImmutableGramModel instance = new ImmutableGramModel();
        private final Logger log = LoggerFactory.getLogger(getClass());

        private Builder() {
            instance.gramMap = Maps.newHashMap();
            instance.numToGram = Maps.newTreeMap();
        }

        public Builder addGrammeme(Grammeme gram) {
            /*if (gramSetLocked) {
				throw new IllegalStateException("Gram set was locked");
			}*/
            if (instance.gramMap.put(gram.getId(), gram) != null) {
                throw new IllegalStateException(String.format(
                        "Duplicate grammem id - %s", gram.getId()));
            }
            if (instance.numToGram.put(gram.getNumId(), gram) != null) {
                throw new IllegalStateException(String.format(
                        "Duplicate grammem num id - %s", gram.getNumId()));
            }
            return this;
        }

        public Builder addGrammeme(String id) {
            return addGrammeme(id, null);
        }

        public Builder addGrammeme(String id, String parentId) {
            return addGrammeme(new Grammeme(id, parentId, null, null));
        }

        public ImmutableGramModel build() {
            instance.gramMap = copyOf(instance.gramMap);
            instance.numToGram = ImmutableSortedMap.copyOf(instance.numToGram);
            log.info("Grammeme set has been locked");
            // build indices
            instance.gramByParent = HashMultimap.create();
            for (Grammeme gr : instance.gramMap.values()) {
                instance.gramByParent.put(gr.getParentId(), gr);
            }
            //
            instance.posBits = instance.getGrammemWithChildrenBits("POST", true);
            isTrue(!instance.posBits.isEmpty());
            log.info("Grammeme indices have been built");
            return instance;
        }
    }

    private static void isTrue(boolean condition) {
        if (!condition) {
            throw new IllegalStateException("Assertion failed");
        }
    }

    private static void notNull(Object obj) {
        if (obj == null) {
            throw new IllegalStateException("Unexpected null value");
        }
    }
}