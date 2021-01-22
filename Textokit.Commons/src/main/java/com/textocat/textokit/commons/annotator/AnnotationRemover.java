
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.textocat.textokit.commons.cas.FSTypeUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.CasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import static com.textocat.textokit.commons.util.AnnotatorUtils.annotationTypeExist;

/**
 * @author Rinat Gareev
 */
public class AnnotationRemover extends CasAnnotator_ImplBase {

    public static final String PARAM_NAMESPACES_TO_REMOVE = "NamespacesToRemove";
    public static final String PARAM_TYPES_TO_REMOVE = "TypesToRemove";

    @ConfigurationParameter(name = PARAM_NAMESPACES_TO_REMOVE, mandatory = false)
    private String[] namespacesToRemove;
    @ConfigurationParameter(name = PARAM_TYPES_TO_REMOVE, mandatory = false)
    private String[] typeNamesToRemove;
    // derived config
    private Set<Type> typesToRemove;
    private Type annotationType;

    @Override
    public void typeSystemInit(TypeSystem ts) throws AnalysisEngineProcessException {
        super.typeSystemInit(ts);
        annotationType = ts.getType("uima.tcas.Annotation");
        annotationTypeExist("uima.tcas.Annotation", annotationType);
        typesToRemove = Sets.newHashSet();
        // process namespaces
        if (namespacesToRemove != null) {
            Set<String> namespacesToRemove = Sets.newHashSet(this.namespacesToRemove);
            Iterator<Type> typeIter = ts.getTypeIterator();
            while (typeIter.hasNext()) {
                Type t = typeIter.next();
                if (isAnnotationType(t, ts)) {
                    Set<String> tNamespaces = FSTypeUtils.getNamespaces(t);
                    if (!Sets.intersection(tNamespaces, namespacesToRemove).isEmpty()) {
                        typesToRemove.add(t);
                    }
                }
            }
        }
        // process certain types
        if (typeNamesToRemove != null) {
            for (String tName : typeNamesToRemove) {
                Type t = ts.getType(tName);
                annotationTypeExist(tName, t);
                if (isAnnotationType(t, ts)) {
                    typesToRemove.add(t);
                } else {
                    getLogger().warn(String.format("%s is not annotation type", t));
                }
            }
        }
        if (typesToRemove.isEmpty()) {
            getLogger().warn(
                    "Configuration of AnnotationRemover yields empty set of types to remove.");
        } else if (getLogger().isInfoEnabled()) {
            StringBuilder msgBuilder = new StringBuilder(
                    "Annotations of the following types will be removed from CAS:\n");
            Joiner.on('\n').appendTo(msgBuilder, typesToRemove);
            getLogger().info(msgBuilder.toString());
        }
        typesToRemove = ImmutableSet.copyOf(typesToRemove);
    }

    private boolean isAnnotationType(Type t, TypeSystem ts) {
        return ts.subsumes(annotationType, t);
    }

    @Override
    public void process(CAS cas) throws AnalysisEngineProcessException {
        for (Type t : typesToRemove) {
            removeAnnotations(cas, t);
        }
    }

    private void removeAnnotations(CAS cas, Type t) {
        LinkedList<AnnotationFS> annoToRemove = Lists.newLinkedList(cas.getAnnotationIndex(t));
        for (AnnotationFS anno : annoToRemove) {
            cas.removeFsFromIndexes(anno);
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(String.format(
                    "%s annotations of type %s have been removed",
                    annoToRemove.size(), t));
        }
    }
}