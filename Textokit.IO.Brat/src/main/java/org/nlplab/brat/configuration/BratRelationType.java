

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

package org.nlplab.brat.configuration;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Set;

/**
 * @author Rinat Gareev
 */
public class BratRelationType extends BratType implements HasRoles {

    private String arg1Name;
    private Set<BratEntityType> arg1Types;
    private String arg2Name;
    private Set<BratEntityType> arg2Types;

    public BratRelationType(String name,
                            Set<BratEntityType> arg1Types,
                            String arg1Name,
                            Set<BratEntityType> arg2Types,
                            String arg2Name) {
        super(name);
        this.arg1Name = arg1Name;
        this.arg1Types = ImmutableSet.copyOf(arg1Types);
        this.arg2Name = arg2Name;
        this.arg2Types = ImmutableSet.copyOf(arg2Types);
    }

    public Set<BratEntityType> getArg1Types() {
        return arg1Types;
    }

    public Set<BratEntityType> getArg2Types() {
        return arg2Types;
    }

    public String getArg1Name() {
        return arg1Name;
    }

    public String getArg2Name() {
        return arg2Name;
    }

    @Override
    public boolean isLegalAssignment(String roleName, BratType t) {
        Set<BratEntityType> roleTypes;
        if (arg1Name.equals(roleName)) {
            roleTypes = arg1Types;
        } else if (arg2Name.equals(roleName)) {
            roleTypes = arg2Types;
        } else {
            throw new IllegalArgumentException("Unknown role " + roleName);
        }
        for (BratEntityType roleType : roleTypes) {
            if (roleType.getName().equals(t.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BratRelationType)) {
            return false;
        }
        BratRelationType that = (BratRelationType) obj;
        return new EqualsBuilder().append(name, that.name)
                .append(arg1Types, that.arg1Types)
                .append(arg2Types, that.arg2Types)
                .append(arg1Name, that.arg1Name)
                .append(arg2Name, that.arg2Name).isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("arg1Name", arg1Name).append("arg1Types", arg1Types)
                .append("arg2Name", arg2Name).append("arg2Types", arg2Types)
                .toString();
    }
}