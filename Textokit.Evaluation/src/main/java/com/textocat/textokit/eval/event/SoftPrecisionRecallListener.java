
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

package com.textocat.textokit.eval.event;

import com.textocat.textokit.eval.measure.RecognitionMeasures;
import org.apache.uima.cas.text.AnnotationFS;

import javax.annotation.PostConstruct;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * TODO rename to softPRListener based on overlap length
 *
 * @author Rinat Gareev
 */
public class SoftPrecisionRecallListener extends TypedPrintingEvaluationListener {

    // state fields
    private RecognitionMeasures measures = new RecognitionMeasures();
    //
    private int exactMatchingCounter;
    private int partialMatchingCounter;
    // per document state
    private Set<AnnotationFS> sysPartiallyMatched = newHashSet();
    private Set<AnnotationFS> goldPartiallyMatched = newHashSet();

    public SoftPrecisionRecallListener() {
        typeRequired = true;
    }

    @PostConstruct
    @Override
    protected void init() throws Exception {
        super.init();
    }

    @Override
    public void onDocumentChange(String docUri) {
        super.onDocumentChange(docUri);
        sysPartiallyMatched.clear();
        goldPartiallyMatched.clear();
    }

    @Override
    public void onMissing(AnnotationFS goldAnno) {
        if (!checkType(goldAnno)) {
            return;
        }
        // Here goldAnno will be considered missing only if it is not partially matched
        if (!goldPartiallyMatched.contains(goldAnno)) {
            measures.incrementMissing(1);
        }
    }

    @Override
    public void onExactMatch(AnnotationFS goldAnno, AnnotationFS sysAnno) {
        if (!checkType(goldAnno)) {
            return;
        }
        onMatch(goldAnno, sysAnno);
    }

    @Override
    public void onPartialMatch(AnnotationFS goldAnno, AnnotationFS sysAnno) {
        if (!checkType(goldAnno)) {
            return;
        }
        if (!goldPartiallyMatched.contains(goldAnno) && !sysPartiallyMatched.contains(sysAnno)) {
            onMatch(goldAnno, sysAnno);
            sysPartiallyMatched.add(sysAnno);
            goldPartiallyMatched.add(goldAnno);
        }
    }

    private void onMatch(AnnotationFS gold, AnnotationFS sys) {
        OverlapHelper.evaluateOverlap(gold, sys, measures);
        if (gold.getBegin() == sys.getBegin() && gold.getEnd() == sys.getEnd()) {
            exactMatchingCounter++;
        } else {
            partialMatchingCounter++;
        }
    }

    @Override
    public void onSpurious(AnnotationFS sysAnno) {
        if (!checkType(sysAnno)) {
            return;
        }
        if (!sysPartiallyMatched.contains(sysAnno)) {
            measures.incrementSpurious(1);
        }
    }

    @Override
    public void onEvaluationComplete() {
        StringBuilder sb = new StringBuilder();
        sb.append("Results for type '").append(targetType.getName()).append("':\n");
        if (measures.getMatchedScore() == 0 && measures.getSpuriousScore() == 0) {
            sb.append("System did not matched any annotation of this type");
        } else {
            sb.append("Matches score:   ").append(formatAsFloating(
                    measures.getMatchedScore())).append("\n");
            sb.append("where:\n\tMatched exactly: ").append(exactMatchingCounter);
            sb.append("\n\tPartially matched: ").append(partialMatchingCounter).append("\n");
            sb.append("Misses score:    ").append(formatAsFloating(
                    measures.getMissedScore())).append("\n");
            sb.append("Spurious score:  ").append(formatAsFloating(
                    measures.getSpuriousScore())).append("\n");
            sb.append("Precision: ").append(formatAsPercentage(
                    measures.getPrecision())).append("\n");
            sb.append("Recall:    ").append(formatAsPercentage(
                    measures.getRecall())).append("\n");
            sb.append("F1:        ").append(formatAsPercentage(
                    measures.getF1())).append("\n");
        }
        printer.println(sb.toString());
        clean();
    }

    public RecognitionMeasures getMeasures() {
        return measures;
    }

    private boolean checkType(AnnotationFS anno) {
        return ts.subsumes(targetType, anno.getType());
    }

    private static String formatAsPercentage(float value) {
        return String.format("%.1f%%", value * 100);
    }

    private static String formatAsFloating(float value) {
        return String.format("%.2f", value);
    }
}