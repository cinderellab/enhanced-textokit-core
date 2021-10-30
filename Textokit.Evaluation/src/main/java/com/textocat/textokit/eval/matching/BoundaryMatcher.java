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

import org.apache.uima.cas.text.AnnotationFS;

/**
 * @author Rinat Gareev
 */
public class BoundaryMatcher implements Matcher<AnnotationFS> {

    public static final BoundaryMatcher INSTANCE = new BoundaryMatcher();

    private BoundaryMatcher() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(AnnotationFS ref, AnnotationFS cand) {
        return ref.getBegin() == cand.getBegin() && ref.getEnd() == cand.getEnd();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void print(StringBuilder out, AnnotationFS value) {
        out.append(value.getCoveredText());
    }
}