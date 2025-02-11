
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

package com.textocat.textokit.morph.commons;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

/**
 * @author Rinat Gareev
 */
public class PunctuationUtils {

    public static final Map<String, String> punctuationTagMap;
    // tag for unknown punctuation marks or special symbols
    public static final String OTHER_PUNCTUATION_TAG = "_P_";
    public static final Set<String> punctuationTags;

    static {
        ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
        // dashes
        b.put("\u2012", "--");
        b.put("\u2013", "--");
        b.put("\u2014", "--");
        b.put("\u2015", "--");
        // hyphens
        b.put("-", "-");
        b.put("\u2010", "-");
        b.put("\u00AD", "-");
        b.put("\u2011", "-");
        b.put("\u2043", "-");
        // apostrophe
        b.put("'", "'");
        b.put("\u2018", "'");
        b.put("\u2019", "'");
        // brackets
        b.put("(", "(");
        b.put(")", ")");
        b.put("[", "(");
        b.put("]", ")");
        b.put("{", "(");
        b.put("}", ")");
        // colon
        b.put(":", ":");
        // semicolon
        b.put(";", ";");
        // comma
        b.put(",", ",");
        // exclamation
        b.put("!", "!");
        b.put("\u203C", "!");
        // period
        b.put(".", ".");
        // question mark
        b.put("?", "?");
        // quotation marks
        b.put("\"", "\"");
        b.put("\u00AB", "\"");
        b.put("\u2039", "\"");
        b.put("\u00BB", "\"");
        b.put("\u203A", "\"");
        b.put("\u201A", "\"");
        b.put("\u201B", "\"");
        b.put("\u201C", "\"");
        b.put("\u201D", "\"");
        b.put("\u201E", "\"");
        b.put("\u201F", "\"");
        // slashes
        b.put("\\", "\\");
        b.put("/", "/");
        // well, these are not punctuation marks
        // but for simplicity we will put them in the same map
        b.put("$", "$");
        b.put("%", "%");
        punctuationTagMap = b.build();
        //
        punctuationTags = ImmutableSet.<String>builder()
                .addAll(punctuationTagMap.values())
                .add(OTHER_PUNCTUATION_TAG)
                .build();
    }

    public static String getPunctuationTag(String tokenStr) {
        String tag = punctuationTagMap.get(tokenStr);
        if (tag == null) {
            tag = OTHER_PUNCTUATION_TAG;
        }
        return tag;
    }

    public static Set<String> getAllPunctuationTags() {
        return punctuationTags;
    }

    public static boolean isPunctuationTag(String tag) {
        if (tag == null) {
            return false;
        }
        return punctuationTags.contains(tag);
    }

    private PunctuationUtils() {
    }
}