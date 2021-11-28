
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

import com.textocat.textokit.eval.TypeSystemInitializer;
import com.textocat.textokit.eval.matching.TypeBasedMatcherDispatcher.Builder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.*;

import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertEquals;

/**
 * @author Rinat Gareev
 */
@ContextConfiguration(classes = MatchingConfigurationInitializerTest.AppContext.class)
public class MatchingConfigurationInitializerTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @PropertySource("classpath:MatchingConfigurationInitializerTest.properties")
    public static class AppContext {
        @Bean
        public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        public TypeSystemInitializer typeSystemInitializer() {
            return new TypeSystemInitializer();
        }
    }

    @Autowired
    private TypeSystem ts;

    @Test
    public void testChunkMatcher() {
        Map<String, Object> properties = newHashMap();
        properties.put("check.targetTypes", "test.Chunk");
        properties
                .put("check.Chunk",
                        "checkType,feature.chunkType={primitive},feature.head={checkBoundaries},feature.dependents={ordered&ref:Word},feature.subChunks={unordered&ref:Chunk}");
        properties.put("check.Word", "checkBoundaries,feature.uid={primitive}");
        PropertyResolver propResolver = makePropertyResolver(properties);
        TypeBasedMatcherDispatcher<AnnotationFS> actualMatcher =
                new MatchingConfigurationInitializer(ts, propResolver).create();