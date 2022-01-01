
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
            throw new IllegalArgumentException(String.format(
                    "Relation %s arguments: %s, %s", type, arg1, arg2));
        }
        Builder<String, BratEntity> b = ImmutableMultimap.builder();
        b.put(type.getArg1Name(), arg1);
        b.put(type.getArg2Name(), arg2);
        setRoleAnnotations(b.build());
    }

    /**
     * auxiliary constructor
     *
     * @param type
     * @param arg1
     * @param arg2
     */
    public BratRelation(BratRelationType type, Map<String, ? extends BratAnnotation<?>> argMap) {
        super(type, Multimaps.forMap(argMap));
        boolean checked = checkArgVal(argMap.get(type.getArg1Name()))
                && checkArgVal(argMap.get(type.getArg2Name()))
                && argMap.size() == 2;
        if (!checked) {
            throw new IllegalArgumentException(String.format(
                    "Relation %s arguments: %s", type, argMap));
        }
    }

    public BratEntity getArg1() {
        return (BratEntity) getRoleAnnotations().get(getType().getArg1Name()).iterator().next();
    }

    public BratEntity getArg2() {
        return (BratEntity) getRoleAnnotations().get(getType().getArg2Name()).iterator().next();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", getId()).append("type", getType())
                .append("roleAnnotations", getRoleAnnotations()).toString();
    }

    private boolean checkArgVal(BratAnnotation<?> anno) {
        return anno instanceof BratEntity;
    }
}