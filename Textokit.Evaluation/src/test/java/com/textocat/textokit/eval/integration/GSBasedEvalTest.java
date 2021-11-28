
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

package com.textocat.textokit.eval.integration;

import com.textocat.textokit.eval.GoldStandardBasedEvaluation;
import com.textocat.textokit.eval.event.EvaluationListener;
import com.textocat.textokit.eval.event.LoggingEvaluationListener;
import com.textocat.textokit.eval.event.SoftPrecisionRecallListener;
import com.textocat.textokit.eval.event.StrictPrecisionRecallListener;
import com.textocat.textokit.eval.measure.RecognitionMeasures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;

/**
 * @author Rinat Gareev
 */
@ContextConfiguration(classes = GSBasedEvalTest.AppContext.class)
public class GSBasedEvalTest extends AbstractJUnit4SpringContextTests {

    @PropertySource("classpath:GSBasedEvalTest.properties")
    @Configuration
    @ImportResource("classpath:com/textocat/textokit/eval/app-context.xml")
    public static class AppContext {
        @Bean
        public EvaluationListener softEvalListener() {
            SoftPrecisionRecallListener listener = new SoftPrecisionRecallListener();
            listener.setTargetTypeName("test.TestFirst");
            return listener;
        }

        @Bean
        public EvaluationListener strictEvalListener1() {
            StrictPrecisionRecallListener listener = new StrictPrecisionRecallListener();
            listener.setTargetTypeName("test.TestFirst");
            return listener;
        }

        @Bean
        public EvaluationListener strictEvalListener2() {
            StrictPrecisionRecallListener listener = new StrictPrecisionRecallListener();
            listener.setTargetTypeName("test.TestSecond");
            return listener;
        }

        @Bean
        public EvaluationListener strictEvalListenerOverall() {
            return new StrictPrecisionRecallListener();
        }

        @Bean
        public EvaluationListener loggingListener() {
            LoggingEvaluationListener list = new LoggingEvaluationListener();
            list.setStripDocumentUri(true);
            return list;
        }
    }

    @Autowired
    private GoldStandardBasedEvaluation evaluator;
    @Resource(name = "softEvalListener")
    private SoftPrecisionRecallListener softEvalListener;
    @Resource(name = "strictEvalListener1")
    private StrictPrecisionRecallListener strictEvalListener1;
    @Resource(name = "strictEvalListener2")
    private StrictPrecisionRecallListener strictEvalListener2;
    @Resource(name = "strictEvalListenerOverall")
    private StrictPrecisionRecallListener strictEvalListenerOverall;

    @Test
    @DirtiesContext
    public void test() throws Exception {
        evaluator.run();

        {
            RecognitionMeasures softMetrics = softEvalListener.getMeasures();
            assertEquals(4.152f, softMetrics.getMatchedScore(), 0.001f);
            assertEquals(12.083f, softMetrics.getSpuriousScore(), 0.001f);
            assertEquals(4.848f, softMetrics.getMissedScore(), 0.001f);

            assertEquals(0.256f, softMetrics.getPrecision(), 0.001f);
            assertEquals(0.461f, softMetrics.getRecall(), 0.001f);
            assertEquals(0.329f, softMetrics.getF1(), 0.001f);
        }

        {
            RecognitionMeasures strictMeasures = strictEvalListener1.getMeasures();
            assertEquals(2f, strictMeasures.getMatchedScore(), 0.001f);
            assertEquals(7f, strictMeasures.getSpuriousScore(), 0.001f);
            assertEquals(7f, strictMeasures.getMissedScore(), 0.001f);

            assertEquals(0.22f, strictMeasures.getPrecision(), 0.01f);
            assertEquals(0.22f, strictMeasures.getRecall(), 0.01f);
            assertEquals(0.222f, strictMeasures.getF1(), 0.001f);
        }

        {
            RecognitionMeasures strictMeasures = strictEvalListener2.getMeasures();
            assertEquals(2f, strictMeasures.getMatchedScore(), 0.001f);
            assertEquals(3f, strictMeasures.getSpuriousScore(), 0.001f);
            assertEquals(1f, strictMeasures.getMissedScore(), 0.001f);

            assertEquals(0.4f, strictMeasures.getPrecision(), 0.01f);
            assertEquals(0.67f, strictMeasures.getRecall(), 0.01f);
            assertEquals(0.501f, strictMeasures.getF1(), 0.001f);
        }

        {
            RecognitionMeasures strictMeasures = strictEvalListenerOverall.getMeasures();
            assertEquals(4f, strictMeasures.getMatchedScore(), 0.001f);
            assertEquals(10f, strictMeasures.getSpuriousScore(), 0.001f);
            assertEquals(8f, strictMeasures.getMissedScore(), 0.001f);

            assertEquals(0.286f, strictMeasures.getPrecision(), 0.001f);
            assertEquals(0.333f, strictMeasures.getRecall(), 0.001f);
            assertEquals(0.308f, strictMeasures.getF1(), 0.001f);
        }
    }
}