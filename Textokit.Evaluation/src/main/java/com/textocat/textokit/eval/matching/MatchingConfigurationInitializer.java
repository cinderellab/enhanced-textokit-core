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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.textocat.textokit.commons.cas.FSTypeUtils;
import com.textocat.textokit.eval.ConfigurationKeys;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.core.env.PropertyResolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static com.textocat.textokit.commons.cas.FSTypeUtils.getFeature;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * @author Rinat Gareev
 */
public class MatchingConfigurationInitializer {

    public static final String MATCHER_DELIMITER_CHARS = ",";
    public static final String SUBMATCHER_DELIMITER_CHARS = "&";
    public static final String BOUNDARY_MATCHER = "checkBoundaries";
    public static final String TYPE_MATCHER = "checkType";
    public static final String PREFIX_ATTRIBUTE_MATCHER = "feature.";

    private TypeSystem ts;
    private PropertyResolver propertyResolver;
    private Type uimaAnnotationType;
    // state fields
    private Map<String, CompositeMatcher.Builder<?>> id2Builder = Maps.newHashMap();

    public MatchingConfigurationInitializer(TypeSystem ts, PropertyResolver propertyResolver) {
        this.ts = ts;
        this.propertyResolver = propertyResolver;
        this.uimaAnnotationType = FSTypeUtils.getType(ts, "uima.tcas.Annotation", 