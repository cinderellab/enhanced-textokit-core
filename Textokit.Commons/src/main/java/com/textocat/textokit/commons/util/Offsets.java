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


package com.textocat.textokit.commons.util;

import org.apache.uima.cas.text.AnnotationFS;

import java.util.Objects;

/**
 * Represents annotation offsets, i.e., a left-closed right-closed integer interval.
 *
 * @author Rinat Gareev
 */
public class Offsets {
    private int begin;
    private int end;

    public Offsets(AnnotationFS anno) {
        this(anno.getBegin(), anno.getEnd());
    }

    public Offsets(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public boolean isIdenticalWith(AnnotationFS anno) {
        return anno.getBegin() == begin && anno.getEnd() == end;
    }

    public boolean overlaps(Offsets that) {
        return this.begin < that.end && that.begin < this.end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != 