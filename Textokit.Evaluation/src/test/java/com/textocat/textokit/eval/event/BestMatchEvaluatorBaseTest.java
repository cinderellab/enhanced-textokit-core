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

import com.textocat.textokit.eval.TypeSystemInitializer;
import com.textocat.textokit.eval.measure.RecognitionMeasures;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import static org.junit.Assert.assertEquals;

/**
 * @author Rinat Gareev
 */
@ContextConfiguration(classes = BestMatchEvaluatorBaseTest.AppContext.class)
public class BestMatchEvaluatorBaseTest extends AbstractJUnit4SpringContextTests {

    @PropertySource("classpath:BestMatchEvaluatorBaseTest.properties")
    @Configuration
    public static class AppContext {
        @Bean
        public BestMatchEvaluatorBase evaluator() {
            return new BestMatchEvaluatorBase() {
                @Override
                protected RecognitionMeasures evaluateAnno(AnnotationFS goldAnno,
                                                           AnnotationFS sysAnno) {
                    RecognitionMeasures result = new RecognitionMeasures();
                    int overlapBegin = Math.max(goldAnno.getBegin(), sysAnno.getBegin());
                    int overlapEnd = Math.min(goldAnno.getEnd(), sysAnno.getEn