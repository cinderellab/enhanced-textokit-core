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

package com.textocat.textokit.postagger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.textocat.textokit.commons.cas.FSUtils;
import com.textocat.textokit.morph.dictionary.AnnotationAdapterBase;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.morph.model.Lemma;
import com.textocat.textokit.morph.model.Wordform;
import com.textocat.textokit.tokenizer.fstype.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;

/**
 * <p/>
 * Uses Wordform.pos to set general lexical category, e.g., NOUN,VERB, etc.
 * <p/>
 * Uses Wordform.grammems to set all grammatical categories, including general
 * one.
 *
 * @author Rinat Gareev
 */
public class DefaultAnnotationAdapter extends AnnotationAdapterBase {

    @Override
    public void apply(JCas jcas, Annotation token, Collection<Wordform> dictWfs) {
        Word w