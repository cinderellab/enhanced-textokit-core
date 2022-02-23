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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.textocat.textokit.morph.dictionary.resource.GramModel;
import com.textocat.textokit.morph.model.Grammeme;
import com.textocat.textokit.morph.model.Lemma;
import com.textocat.textokit.morph.model.LemmaLinkType;
import com.textocat.textokit.morph.model.Wordform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.collect.Sets.newHashSet;

class DictionaryXmlHandler extends DefaultHandler {

    private static final Logger log = LoggerFactory
            .getLogger(DictionaryXmlHandler.class);

    private static final String ELEM_DICTIONARY = "dictionary";
    private static final String ATTR_DICTIONARY_VERSION = "version";
    private static final String ATTR_DICTIONARY_REVISION = "revision";
    private static final String ELEM_GRAMMEMS = "grammemes";
    private static final String ELEM_GRAMMEM = "grammeme";
    private static final String ATTR_GRAMMEM_PARENT = "parent";
    private static final String ELEM_GRAMMEM_NAME = "name";
    private static final String ELEM_GRAMMEM_ALIAS = "alias";
    private static final String ELEM_GRAMMEM_DESCRIPTION = "description";

    private static final String ELEM_RESTRICTIONS = "restrictions";

    private static final String ELEM_LEMMATA = "lemmata";
    private static final String ELEM_LEMMA = "lemma";
    private static final String ATTR_LEMMA_ID = "id";
    @SuppressWarnings("unused")
    private static final String ATTR_LEMMA_REV = "rev";
    private static final String ELEM_LEMMA_NORM = "l";
    private static final String ATTR_TEXT = "t";
    private static final String ELEM_WF_GRAMMEM = "g";
    