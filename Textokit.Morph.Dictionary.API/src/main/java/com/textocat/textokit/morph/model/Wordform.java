
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

package com.textocat.textokit.morph.model;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import com.textocat.textokit.morph.dictionary.resource.MorphDictionary;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.BitSet;

/**
 * @author Rinat Gareev
 */
public class Wordform implements Serializable {

    private static final long serialVersionUID = -8435061415886880938L;
    private static final BitSet EMPTY_BITSET = new BitSet();

    public static Builder builder(GramModel gm, int lemmaId) {
        return new Builder(gm, lemmaId);
    }

    public static class Builder {
        private Wordform instance = new Wordform();
        private GramModel gm;

        public Builder(GramModel gm, int lemmaId) {
            instance.lemmaId = lemmaId;
            this.gm = gm;
        }

        public Builder addGrammeme(String gramId) {
            if (instance.grammems == null) {
                instance.grammems = new BitSet(gm.getGrammemMaxNumId() + 1);
            }
            int gramNumId = gm.getGrammemNumId(gramId);
            instance.grammems.set(gramNumId);
            return this;
        }

        public Wordform build() {
            if (instance.grammems == null) {
                instance.grammems = EMPTY_BITSET;
            }
            return instance;
        }
    }

    public static BitSet getAllGramBits(Wordform wf, MorphDictionary dict) {
        BitSet bs = wf.getGrammems();
        // TODO how to detect null lemma ids?
        bs.or(dict.getLemma(wf.getLemmaId()).getGrammems());
        return bs;
    }

    public static Function<Wordform, BitSet> allGramBitsFunction(final MorphDictionary dict) {
        return new Function<Wordform, BitSet>() {
            @Override
            public BitSet apply(Wordform arg) {
                return getAllGramBits(arg, dict);
            }
        };
    }

    private int lemmaId;
    private BitSet grammems;

    private Wordform() {
    }

    public Wordform(int lemmaId, BitSet grammems) {
        this.lemmaId = lemmaId;
        this.grammems = grammems;
    }

    public int getLemmaId() {
        return lemmaId;
    }

    public Wordform cloneWithLemmaId(int lemmaId) {
        return new Wordform(lemmaId, grammems);
    }

    public Wordform cloneWithGrammems(BitSet grammems) {
        Wordform result = new Wordform();
        result.lemmaId = this.lemmaId;
        result.grammems = grammems;
        return result;
    }

    public BitSet getGrammems() {
        // !!!
        return (BitSet) grammems.clone();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(lemmaId).append(grammems)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Wordform)) {
            return false;
        }
        Wordform that = (Wordform) obj;
        return new EqualsBuilder().append(this.lemmaId, that.lemmaId)
                .append(this.grammems, that.grammems).isEquals();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("lemmaId", lemmaId)
                .add("grammems", grammems)
                .toString();
    }

}