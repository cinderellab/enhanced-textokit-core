
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

package org.nlplab.brat.ann;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.nlplab.brat.configuration.BratRelationType;

import java.util.Map;

/**
 * @author Rinat Gareev
 */
public class BratRelation extends BratStructureAnnotation<BratRelationType> {

    public BratRelation(BratRelationType type, BratEntity arg1, BratEntity arg2) {
        super(type, ImmutableMultimap.<String, BratEntity>of());
        if (!checkArgVal(arg1) || !checkArgVal(arg2)) {
            throw new IllegalArgumentException(String.format