
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


package com.textocat.textokit.commons.annotator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.*;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.CasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.textocat.textokit.commons.util.AnnotatorUtils.annotationTypeExist;

/**
 * @author Rinat Gareev
 */
public class FeatureValueReplacer extends CasAnnotator_ImplBase {

    public static final String PARAM_REPLACE_BY = "replaceBy";
    public static final String PARAM_PATTERN = "pattern";
    public static final String PARAM_FEATURE_PATH = "featurePath";
    public static final String PARAM_ANNO_TYPE = "annotationType";

    @ConfigurationParameter(name = PARAM_ANNO_TYPE, mandatory = true)
    private String annoTypeName;

    @ConfigurationParameter(name = PARAM_FEATURE_PATH, mandatory = true)
    private String featurePathString;

    @ConfigurationParameter(name = PARAM_PATTERN, mandatory = true)
    private String patternString;

    @ConfigurationParameter(name = PARAM_REPLACE_BY, mandatory = true)
    private String replaceBy;

    // derivde
    private Pattern pattern;
    private Type annoType;
    private List<Feature> featurePath;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        pattern = Pattern.compile(patternString);
    }

    @Override
    public void typeSystemInit(TypeSystem ts) throws AnalysisEngineProcessException {
        super.typeSystemInit(ts);

        annoType = ts.getType(annoTypeName);
        annotationTypeExist(annoTypeName, annoType);

        String[] pathElems = featurePathString.split("\\.");
        for (int i = 0; i < pathElems.length; i++) {
            pathElems[i] = pathElems[i].trim();
        }
        featurePath = newArrayListWithExpectedSize(pathElems.length);

        Type curElemType = annoType;
        for (String curElem : pathElems) {
            Feature curElemFeature = curElemType.getFeatureByBaseName(curElem);
            if (curElemFeature == null) {
                throw new AnalysisEngineProcessException(
                        new IllegalStateException(String.format(
                                "Feature '%s' does not exist in the path '%s'",
                                curElem, featurePathString)));
            }
            featurePath.add(curElemFeature);
            curElemType = curElemFeature.getRange();
        }

        if (!ts.getType("uima.cas.String").equals(curElemType)) {
            throw new AnalysisEngineProcessException(
                    new IllegalStateException("Final type of feature path is not uima.cas.String"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(CAS cas) throws AnalysisEngineProcessException {
        for (AnnotationFS anno : cas.getAnnotationIndex(annoType)) {
            String featValue = extractValue(anno);
            if (featValue == null) {
                return;
            }
            Matcher matcher = pattern.matcher(featValue);
            if (matcher.matches()) {
                String newFeatValue = matcher.replaceAll(replaceBy);
                if (!newFeatValue.equals(featValue)) {
                    cas.removeFsFromIndexes(anno);
                    setValue(anno, newFeatValue);
                    cas.addFsToIndexes(anno);
                    getLogger().info(String.format(
                            "Replacement done for feature %s.%s: '%s' => '%s'",
                            annoTypeName, featurePathString, featValue, newFeatValue));
                }
            }
        }

    }

    private void setValue(AnnotationFS target, String value) {
        FeatureStructure curElemAnno = target;
        for (int i = 0; i < featurePath.size() - 1; i++) {
            FeatureStructure featValue = curElemAnno.getFeatureValue(featurePath.get(i));
            curElemAnno = featValue;
        }
        Feature lastFeature = featurePath.get(featurePath.size() - 1);
        curElemAnno.setStringValue(lastFeature, value);
    }

    private String extractValue(AnnotationFS target) {
        FeatureStructure curElemAnno = target;
        for (int i = 0; i < featurePath.size() - 1; i++) {
            FeatureStructure featValue = curElemAnno.getFeatureValue(featurePath.get(i));
            curElemAnno = featValue;
        }
        Feature lastFeature = featurePath.get(featurePath.size() - 1);
        return curElemAnno.getStringValue(lastFeature);
    }
}