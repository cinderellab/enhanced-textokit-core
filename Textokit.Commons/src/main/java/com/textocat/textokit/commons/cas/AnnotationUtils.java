
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


package com.textocat.textokit.commons.cas;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.uima.cas.*;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Sets.newLinkedHashSet;

/**
 * @author Rinat Gareev
 */
public class AnnotationUtils {

    private static final Function<AnnotationFS, String> coveredTextFunction = new Function<AnnotationFS, String>() {
        @Override
        public String apply(AnnotationFS anno) {
            if (anno == null) {
                return null;
            }
            return anno.getCoveredText();
        }
    };

    private AnnotationUtils() {
    }

    /**
     * @param anno an annotation instance
     * @return string containing given annotation offset and covered text
     */
    public static String toPrettyString(AnnotationFS anno) {
        return new StringBuilder("(").append(anno.getBegin()).append(",'")
                .append(anno.getCoveredText()).append("')")
                .toString();
    }

    /**
     * @param anno           an annotation
     * @param contextCharNum number of characters
     * @return contextCharNum characters before the given annotation
     */
    public static String textBefore(AnnotationFS anno, int contextCharNum) {
        Preconditions.checkArgument(contextCharNum >= 0);
        int begin = Math.max(0, anno.getBegin() - contextCharNum);
        int end = anno.getBegin();
        return anno.getCAS().getDocumentText().substring(begin, end);
    }

    /**
     * @param anno           an annotation
     * @param contextCharNum number of characters
     * @return contextCharNum characters after the given annotation
     */
    public static String textAfter(AnnotationFS anno, int contextCharNum) {
        Preconditions.checkArgument(contextCharNum >= 0);
        String txt = anno.getCAS().getDocumentText();
        int begin = anno.getEnd();
        int end = Math.min(txt.length(), begin + contextCharNum);
        return txt.substring(begin, end);
    }

    public static int length(Annotation anno) {
        return anno.getEnd() - anno.getBegin();
    }

    public static boolean isEmpty(AnnotationFS anno) {
        return anno.getBegin() == anno.getEnd();
    }

    /**
     * Test two given annotations for overlapping.
     * Annotations overlap if they share at least one common character in a text.
     * Note that no annotations can overlap with an empty annotation that has equal begin and end.
     *
     * @param first  an annotation instance
     * @param second an annotation instance
     * @return do given annotations overlap or not.
     */
    public static boolean overlap(AnnotationFS first, AnnotationFS second) {
        if (isEmpty(first) || isEmpty(second)) {
            return false;
        }
        // see http://stackoverflow.com/questions/3269434
        return first.getBegin() < second.getEnd() && second.getBegin() < first.getEnd();
    }

    /**
     * @param first  an annotation instance
     * @param second an annotation instance
     * @return a number of shared characters between the two given annotations.
     */
    public static int overlapLength(Annotation first, Annotation second) {
        if (!overlap(first, second)) {
            return 0;
        }
        return Math.min(first.getEnd(), second.getEnd()) - Math.max(first.getBegin(), second.getBegin());
    }

    /**
     * @param cas        a CAS instance
     * @param iter       a source iterator
     * @param targetAnno an annotation which result annotations must overlap with
     * @return iterator over annotations that overlaps with targetAnno from the source iterator
     * @see #overlap(AnnotationFS, AnnotationFS)
     */
    public static <T extends FeatureStructure> FSIterator<T> getOverlapping(CAS cas,
                                                                            FSIterator<T> iter, AnnotationFS targetAnno) {
        ConstraintFactory cf = ConstraintFactory.instance();
        FSMatchConstraint beginConjunct;
        {
            FSIntConstraint beginConstraint = cf.createIntConstraint();
            beginConstraint.lt(targetAnno.getEnd());
            beginConjunct = cf.embedConstraint(newArrayList("begin"), beginConstraint);
        }
        FSMatchConstraint endConjunct;
        {
            FSIntConstraint endConstraint = cf.createIntConstraint();
            endConstraint.gt(targetAnno.getBegin());
            endConjunct = cf.embedConstraint(newArrayList("end"), endConstraint);
        }
        FSMatchConstraint overlapConstraint = cf.and(beginConjunct, endConjunct);
        return cas.createFilteredIterator(iter, overlapConstraint);
    }

    /**
     * @deprecated use {@link FSUtils#fill(FSIterator, Collection)} instead
     */
    @Deprecated
    public static <FST extends FeatureStructure> void fill(FSIterator<FST> srcIter,
                                                           Collection<FST> destCol) {
        FSUtils.fill(srcIter, destCol);
    }

    /**
     * @deprecated use {@link FSUtils#toList(FSIterator)} instead
     */
    @Deprecated
    public static <FST extends FeatureStructure> List<FST> toList(FSIterator<FST> iter) {
        return FSUtils.toList(iter);
    }

    /**
     * @deprecated use {@link FSUtils#toSet(FSIterator)} instead
     */
    @Deprecated
    public static <FST extends FeatureStructure> Set<FST> toSet(FSIterator<FST> iter) {
        return FSUtils.toSet(iter);
    }

    /**
     * @param iter
     * @return linked hashset to preserve iteration order
     */
    public static <FST extends FeatureStructure> Set<FST> toLinkedHashSet(FSIterator<FST> iter) {
        HashSet<FST> result = newLinkedHashSet();
        fill(iter, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <FST extends FeatureStructure> List<FST> toList(FSArray casArray,
                                                                  Class<FST> fstClass) {
        List<FST> result = newArrayListWithCapacity(casArray.size());
        for (int i = 0; i < casArray.size(); i++) {
            result.add((FST) casArray.get(i));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T extends FeatureStructure> T getFeatureValueSafe(
            FeatureStructure srcFS, String featureName) {
        Feature feat = srcFS.getType().getFeatureByBaseName(featureName);
        if (feat != null) {
            return (T) srcFS.getFeatureValue(feat);
        } else {
            return null;
        }
    }

    /**
     * @param cas
     * @param type
     * @param feature
     * @return feature value of the first annotation of given type from given
     * CAS. E.g., it is useful to get document metadata values. If there
     * is no such annotation method will return null.
     */
    public static String getStringValue(JCas cas, Type type, Feature feature) {
        return getStringValue(cas.getCas(), type, feature);
    }

    /**
     * @param cas
     * @param type
     * @param feature
     * @return feature value of the first annotation of given type from given
     * CAS. E.g., it is useful to get document metadata values. If there
     * is no such annotation method will return null.
     */
    public static String getStringValue(CAS cas, Type type, Feature feature) {
        AnnotationIndex<AnnotationFS> metaIdx = cas.getAnnotationIndex(type);
        if (metaIdx.size() > 0) {
            AnnotationFS meta = metaIdx.iterator().next();
            return meta.getFeatureValueAsString(feature);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <A extends AnnotationFS> A getSingleAnnotation(JCas cas, Class<A> typeJCasClass) {
        Type annoType = CasUtil.getAnnotationType(cas.getCas(), typeJCasClass);
        AnnotationIndex<Annotation> annoIdx = cas.getAnnotationIndex(annoType);
        if (annoIdx.size() == 0) {
            return null;
        } else if (annoIdx.size() == 1) {
            return (A) annoIdx.iterator().next();
        } else {
            throw new IllegalStateException(String.format(
                    "Too much (>1) annotations of type %s", annoType));
        }
    }

    // TODO test
    public static FSMatchConstraint getCoveredConstraint(AnnotationFS coveringAnnotation) {
        ConstraintFactory cf = ConstraintFactory.instance();

        FSIntConstraint beginConstraint = cf.createIntConstraint();
        beginConstraint.geq(coveringAnnotation.getBegin());

        FSIntConstraint endConstraint = cf.createIntConstraint();
        endConstraint.leq(coveringAnnotation.getEnd());

        return cf.and(cf.embedConstraint(newArrayList("begin"), beginConstraint),
                cf.embedConstraint(newArrayList("end"), endConstraint));
    }

    public static <A extends AnnotationFS> OverlapIndex<A> createOverlapIndex(Iterator<A> srcIter) {
        return new AITOverlapIndex<>(srcIter);
    }

    /**
     * Provides a convenient way to create an annotation a single line. Note that this method does not
     * add a new annotation into the index, as it is done in
     * {@link org.apache.uima.fit.factory.AnnotationFactory#createAnnotation(org.apache.uima.jcas.JCas, int, int, Class)}.
     *
     * @param <T>   the annotation type
     * @param jCas  the JCas to create the annotation in
     * @param begin the begin offset
     * @param end   the end offset
     * @param cls   the annotation class as generated by JCasGen
     * @return the new annotation
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T create(JCas jCas, int begin, int end, Class<T> cls) {
        return (T) jCas.getCas().createAnnotation(JCasUtil.getAnnotationType(jCas, cls), begin, end);
    }

    public static Function<AnnotationFS, String> coveredTextFunction() {
        return coveredTextFunction;
    }

    public static <A extends Annotation> A findByCoveredText(JCas jCas, Class<A> annoClass, final String coveredText) {
        Optional<A> findRes = Iterables.tryFind(jCas.getAnnotationIndex(annoClass), new Predicate<A>() {
            @Override
            public boolean apply(A input) {
                return Objects.equals(input.getCoveredText(), coveredText);
            }
        });
        return findRes.orNull();
    }

    public static boolean isBefore(AnnotationFS first, AnnotationFS second) {
        return first.getBegin() < second.getBegin();
    }
}