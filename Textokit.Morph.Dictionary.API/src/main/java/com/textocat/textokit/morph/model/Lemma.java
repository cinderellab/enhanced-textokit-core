
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

import com.textocat.textokit.morph.dictionary.resource.GramModel;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.BitSet;

/**
 * TODO rename to Lexeme
 *
 * @author Rinat Gareev
 */
public class Lemma implements Serializable {

    private static final long serialVersionUID = 7426278009038784123L;
    private static final BitSet EMPTY_BITSET = new BitSet();

    public static Builder builder(GramModel gm, int id) {
        return new Builder(gm, id);
    }

    public static class Builder {
        private Lemma instance = new Lemma();
        private GramModel gm;

        public Builder(GramModel gm, int id) {
            instance.id = id;
            instance.grammems = new BitSet(gm.getGrammemMaxNumId() + 1);
            this.gm = gm;
        }

        public Builder copy(int newId) {
            Builder result = new Builder(gm, newId);
            result.instance.grammems = (BitSet) instance.grammems.clone();
            result.instance.string = instance.string;
            return result;
        }

        public int getLemmaId() {
            return instance.id;
        }

        public Builder setLemmaId(int id) {
            instance.id = id;
            return this;
        }

        public String getString() {
            return instance.string;
        }

        public Builder setString(String string) {
            instance.string = string;
            return this;
        }

        public BitSet getGrammems() {
            return instance.grammems;
        }

        public Builder addGrammeme(String gramId) {
            int gramNumId = gm.getGrammemNumId(gramId);
            instance.grammems.set(gramNumId);
            return this;
        }

        public Lemma build() {
            if (instance.string == null) {
                throw new IllegalStateException("'string' is null");
            }
            if (instance.grammems == null) {
                instance.grammems = EMPTY_BITSET;
            }
            return instance;
        }
    }

    private int id;
    private String string;
    private BitSet grammems;

    public Lemma() {
    }

    public Lemma(String string, BitSet grammems) {
        this.string = string;
        this.grammems = grammems;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getString() {
        return string;
    }

    public BitSet getGrammems() {
        return (BitSet) grammems.clone();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append(string).append("id", id).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(string).append(grammems).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Lemma)) {
            return false;
        }
        Lemma that = (Lemma) obj;
        return new EqualsBuilder().append(this.string, that.string)
                .append(this.grammems, that.grammems).isEquals();
    }

    public Lemma cloneWithoutIdAndString() {
        return new Lemma("", grammems);
    }

    public Lemma cloneWithGrammems(BitSet grammems) {
        Lemma result = new Lemma();
        result.id = this.id;
        result.string = this.string;
        result.grammems = grammems;
        return result;
    }
}