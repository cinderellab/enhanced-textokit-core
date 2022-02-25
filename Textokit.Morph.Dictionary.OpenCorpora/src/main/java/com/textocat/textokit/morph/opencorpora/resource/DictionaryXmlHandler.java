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
    private static final String ELEM_LEMMA_GRAMMEM = ELEM_WF_GRAMMEM;
    private static final String ATTR_WF_GRAMMEME_ID = "v";
    private static final String ELEM_WORDFORM = "f";
    private static final String ELEM_LINK_TYPES = "link_types";
    private static final String ELEM_LINK_TYPE = "type";
    private static final String ATTR_LINK_TYPE_ID = "id";
    private static final String ELEM_LINKS = "links";
    private static final String ELEM_LINK = "link";
    @SuppressWarnings("unused")
    private static final String ATTR_LINK_ID = "id";
    private static final String ATTR_LINK_FROM = "from";
    private static final String ATTR_LINK_TO = "to";
    private static final String ATTR_LINK_TYPE = "type";

    private abstract class ElementHandler {
        protected final String qName;
        private ElementHandler parentHandler;

        protected ElementHandler(String qName) {
            if (qName == null)
                throw new NullPointerException(qName);
            this.qName = qName;
        }

        protected final <EH> EH getParent(Class<EH> parentClass) {
            return parentClass.cast(parentHandler);
        }

        protected final void setParent(ElementHandler parent) {
            this.parentHandler = parent;
        }

        protected abstract void startElement(Attributes attrs);

        protected abstract void endElement();

        protected abstract void characters(String str);

        /**
         * @param elem
         * @return return handler for child element elem
         */
        protected abstract ElementHandler getHandler(String elem);
    }

    private abstract class ElementHandlerBase extends ElementHandler {

        private Map<String, ElementHandler> children = ImmutableMap.of();

        protected ElementHandlerBase(String qName) {
            super(qName);
        }

        @Override
        protected final void startElement(Attributes attrs) {
            children = declareChildren();
            if (children != null) {
                for (ElementHandler child : children.values()) {
                    child.setParent(this);
                }
            }
            startSelf(attrs);
        }

        @Override
        protected final void endElement() {
            endSelf();
            // clear children
            this.children = null;
        }

        @Override
        protected void characters(String str) {
            if (!str.trim().isEmpty()) {
                throw new UnsupportedOperationException(String.format(
                        "Unexpected characters within %s:\n%s",
                        this.qName, str));
            }
        }

        @Override
        protected final ElementHandler getHandler(String elem) {
            return children == null ? null : children.get(elem);
        }

        protected abstract void startSelf(Attributes attrs);

        protected abstract void endSelf();

        protected abstract Map<String, ElementHandler> declareChildren();
    }

    private class RootHandler extends ElementHandler {
        private ElementHandlerBase topHandler;

        RootHandler(ElementHandlerBase topHandler) {
            super("%ROOT%");
            this.topHandler = topHandler;
        }

        @Override
        protected void startElement(Attributes attrs) {
            throw new IllegalStateException();
        }

        @Override
        protected void endElement() {
            throw new IllegalStateException();
        }

        @Override
        protected void characters(String str) {
            if (!str.trim().isEmpty()) {
                throw new IllegalStateException();
            }
        }

        @Override
        protected ElementHandler getHandler(String elem) {
            if (Objects.equal(elem, topHandler.qName)) {
                return topHandler;
            }
            return null;
        }
    }

    private abstract class NoOpHandler extends ElementHandlerBase {
        NoOpHandler(String qName) {
            super(qName);
        }

        @Override
        protected void startSelf(Attributes attrs) {
        }

        @Override
        protected void endSelf() {
        }
    }

    private class IgnoreHandler extends ElementHandler {
        protected IgnoreHandler(String qName) {
            super(qName);
        }

        @Override
        protected void startElement(Attributes attrs) {
            // ignore
        }

        @Override
        protected void endElement() {
            // ignore
        }

        @Override
        protected void characters(String str) {
            // ignore
        }

        @Override
        protected ElementHandler getHandler(String elem) {
            // ignore all children
            IgnoreHandler result = new IgnoreHandler(elem);
            result.setParent(this);
   