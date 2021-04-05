
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


package com.textocat.textokit.commons.io.axml;

import com.google.common.base.*;
import com.google.common.collect.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.TypeSystem;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Rinat Gareev
 */
class AXMLContentHandler extends DefaultHandler {
    private static final String E_DOC = "doc";
    private static final String E_META = "meta";
    private static final String E_ALIASES = "aliases";
    private static final String E_ALIAS = "alias";
    private static final String E_FEATURE_ALIAS = "featureAlias";
    private static final String E_BODY = "body";
    private static final String A_KEY = "key";
    private static final String A_TYPE = "type";
    private static final String A_NAME = "name";

    // config fields
    private TypeSystem typeSystem;
    private Locator locator;
    // state
    private Map<String, String> typeAliases;
    private Map<String, String> featNameAliases;
    private StringBuilder textBuilder;
    private boolean readCharacters = false;
    private boolean readingBody = false;
    private LinkedList<Annotation> openAnnotations;
    private Map<String, Annotation> annoIdIndex;
    // table of <anno, featName> => to single-string-id||list-of-ids
    private Table<Annotation, String, Object> fsFeatureTable;
    private Set<Annotation> annotations;
    private boolean finished = false;
    private String text;

    AXMLContentHandler(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    public String getText() {
        if (!finished) {
            throw new IllegalStateException();
        }
        return text;
    }

    public Set<Annotation> getAnnotations() {
        if (!finished) {
            throw new IllegalStateException();
        }
        return annotations;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
        if (text == null) {
            throw new IllegalStateException("Document does not have proper <body>");
        }
        finished = true;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        String elemName = getElementName(uri, localName, qName);
        if (readingBody) {
            onAnnotationStart(elemName, attributes);
        } else if (E_DOC.equals(elemName)) {
            onDocStart();
        } else if (E_META.equals(elemName)) {
            onMetaStart(attributes);
        } else if (E_BODY.equals(elemName)) {
            onBodyStart();
        } else if (E_ALIASES.equals(elemName)) {
            onAliasesStart();
        } else if (E_ALIAS.equals(elemName)) {
            onAliasStart(attributes);
        } else if (E_FEATURE_ALIAS.equals(elemName)) {
            onFeatureAliasStart(attributes);
        } else {
            throw new SAXParseException(String.format("Unknown element '%s'", elemName),
                    locator);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String elemName = getElementName(uri, localName, qName);
        if (readingBody) {
            if (E_BODY.equals(elemName)) {
                onBodyEnd();
            } else {
                onAnnotationEnd(elemName);
            }
        } else if (E_DOC.equals(elemName)) {
            onDocEnd();
        } else if (E_ALIASES.equals(elemName)) {
            onAliasesEnd();
        } else if (E_ALIAS.equals(elemName)) {
            onAliasEnd();
        } else if (E_FEATURE_ALIAS.equals(elemName)) {
            onFeatureAliasEnd();
        } else if (E_META.equals(elemName)) {
            onMetaEnd();
        } else {
            throw new SAXParseException(String.format("End of unknown element '%s'", elemName),
                    locator);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String newChars = String.valueOf(ch, start, length);
        if (!readCharacters) {
            if (!StringUtils.isBlank(newChars)) {
                throw new SAXParseException(String.format(
                        "Unexpected characters '%s'", String.valueOf(ch, start, length)),
                        locator);
            }
        } else {
            textBuilder.append(preprocessChars(newChars));
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (readingBody && readCharacters) {
            String newChars = String.valueOf(ch, start, length);
            textBuilder.append(preprocessChars(newChars));
        }
    }

    private String preprocessChars(String str) {
        // normalize line endings
        // return StringUtils.replaceChars(str, "\r\n", "");
        return str;
    }

    private void onDocStart() {
        // no op
    }

    private void onDocEnd() {
        // no op
    }

    private void onMetaStart(Attributes attrs) {
        // it is not used yet
    }

    private void onMetaEnd() {
        // it is not used yet
    }

    private void onAliasesStart() throws SAXParseException {
        if (typeAliases != null) {
            throw new SAXParseException("Duplicate aliases declaration", locator);
        }
        typeAliases = Maps.newHashMap();
        featNameAliases = Maps.newHashMap();
    }

    private void onAliasesEnd() {
        typeAliases = ImmutableMap.copyOf(typeAliases);
    }

    private void onAliasStart(Attributes attrs) throws SAXParseException {
        String key = requiredAttribute(attrs, A_KEY);
        String type = requiredAttribute(attrs, A_TYPE);
        if (typeSystem.getType(type) == null) {
            throw new SAXParseException(String.format("Unknown type: %s", type), locator);
        }
        if (typeAliases.put(key, type) != null) {
            throw new SAXParseException("Duplicate alias declaration", locator);
        }
    }

    private void onFeatureAliasStart(Attributes attrs) throws SAXParseException {
        String key = requiredAttribute(attrs, A_KEY);
        String featName = requiredAttribute(attrs, A_NAME);
        featNameAliases.put(key, featName);
    }

    private void onAliasEnd() {
        // no op
    }

    private void onFeatureAliasEnd() {
        // no op
    }

    private void onBodyStart() throws SAXParseException {
        if (!(typeAliases instanceof ImmutableMap)) {
            throw new SAXParseException("Type aliases must be defined before body", locator);
        }
        readingBody = true;
        readCharacters = true;
        textBuilder = new StringBuilder();
        openAnnotations = Lists.newLinkedList();
        annotations = Sets.newLinkedHashSet();
        fsFeatureTable = HashBasedTable.create();
        annoIdIndex = Maps.newHashMap();
    }

    private void onBodyEnd() throws SAXParseException {
        if (!openAnnotations.isEmpty()) {
            throw new SAXParseException(String.format("Open annotations: %s", openAnnotations),
                    locator);
        }
        openAnnotations = null;
        // set fs references
        for (Table.Cell<Annotation, String, Object> c : fsFeatureTable.cellSet()) {
            Annotation anno = c.getRowKey();
            String featName = c.getColumnKey();
            Object valObj = c.getValue();
            if (valObj instanceof String) {
                anno.setFeatureFSValue(featName, getAnnotationById((String) valObj));
            } else if (valObj instanceof List) {
                @SuppressWarnings("unchecked") List<String> valIds = (List<String>) valObj;
                anno.setFeatureFSArrayValue(featName, Lists.newArrayList(Lists.transform(valIds, ID2ANNO)));
            } else {
                // should never happen
                throw new IllegalStateException();
            }
        }
        //
        fsFeatureTable = null;
        annoIdIndex = null;
        text = textBuilder.toString();
        textBuilder = null;
        readingBody = false;
        readCharacters = false;
    }

    private Annotation getAnnotationById(String id) {
        Annotation valAnno = annoIdIndex.get(id);
        if (valAnno == null) {
            throw new IllegalStateException("Can't find FS with id=" + id);
        }
        return valAnno;
    }

    private final Function<String, Annotation> ID2ANNO = new Function<String, Annotation>() {
        @Override
        public Annotation apply(String id) {
            return getAnnotationById(id);
        }
    };

    private void onAnnotationStart(final String aType, Attributes attrs) throws SAXParseException {
        String type = toTypeName(aType);
        Annotation newAnno = new Annotation();
        newAnno.setType(type);
        newAnno.setBegin(textBuilder.length());
        // handle features
        for (int attrIndex = 0; attrIndex < attrs.getLength(); attrIndex++) {
            final String attrName = attrs.getLocalName(attrIndex);
            if ("id".equals(attrName)) {
                // handle as a special case
                String id = attrs.getValue(attrIndex);
                newAnno.setId(id);
                annoIdIndex.put(id, newAnno);
            } else {
                // handle the attr as feature name or alias
                String featName = attrName;
                if (featNameAliases.containsKey(attrName)) {
                    featName = featNameAliases.get(attrName);
                }
                String attrVal = attrs.getValue(attrIndex);
                if (attrVal.startsWith("id:")) {
                    String valId = attrVal.substring("id:".length());
                    fsFeatureTable.put(newAnno, featName, valId);
                } else if (attrVal.startsWith("ids:")) {
                    List<String> valIds = ID_SPLITTER.splitToList(attrVal.substring("ids:".length()));
                    fsFeatureTable.put(newAnno, featName, valIds);
                } else {
                    newAnno.setFeatureStringValue(featName, attrVal);
                }
            }
        }
        //
        openAnnotations.addFirst(newAnno);
    }

    private static final Splitter ID_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

    private String toTypeName(final String aType) throws SAXParseException {
        String type = typeAliases.get(aType);
        if (type == null) {
            type = aType;
        }
        if (typeSystem.getType(type) == null) {
            throw new SAXParseException(String.format("Unknown annotation type: %s", type),
                    locator);
        }
        return type;
    }

    private void onAnnotationEnd(final String aType) throws SAXParseException {
        final String type = toTypeName(aType);
        Optional<Annotation> annoOpt = Iterables.tryFind(openAnnotations,
                new Predicate<Annotation>() {
                    @Override
                    public boolean apply(Annotation input) {
                        return Objects.equal(type, input.getType());
                    }
                });
        if (annoOpt.isPresent()) {
            Annotation anno = annoOpt.get();
            openAnnotations.remove(anno);
            anno.setEnd(textBuilder.length());
            annotations.add(anno);
        } else {
            throw new SAXParseException("End of an element closes an unknown annotation", locator);
        }
    }

    private String requiredAttribute(Attributes attrs, String name) throws SAXParseException {
        String val = attrs.getValue(name);
        if (val == null) {
            throw new SAXParseException(String.format("Missing attribute '%s'", name),
                    locator);
        }
        return val;
    }

    private String getElementName(String uri, String localName, String qName)
            throws SAXParseException {
        String elemName = localName;
        if (elemName.isEmpty()) {
            elemName = qName;
        }
        if (elemName.isEmpty()) {
            throw new SAXParseException("Could not determine element name", locator);
        }
        return elemName;
    }
}