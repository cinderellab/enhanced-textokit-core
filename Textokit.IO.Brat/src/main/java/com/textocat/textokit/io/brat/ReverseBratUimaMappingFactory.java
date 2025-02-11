

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

package com.textocat.textokit.io.brat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ResourceCreationSpecifierFactory;
import org.apache.uima.fit.factory.initializable.Initializable;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.nlplab.brat.configuration.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.textocat.textokit.io.brat.UIMA2BratAnnotator.*;

/**
 * @author Rinat Gareev
 */
public class ReverseBratUimaMappingFactory extends BratUimaMappingFactoryBase implements
        Initializable {

    public static final String PARAM_U2B_DESC_PATH = "Uima2BratDescriptorPath";
    public static final String PARAM_U2B_DESC_NAME = "Uima2BratDescriptorName";

    @ConfigurationParameter(name = PARAM_U2B_DESC_PATH, mandatory = false)
    private String u2bDescPath;
    @ConfigurationParameter(name = PARAM_U2B_DESC_NAME, mandatory = false)
    private String u2bDescName;

    @Override
    public void initialize(UimaContext ctx) throws ResourceInitializationException {
        ConfigurationParameterInitializer.initialize(this, ctx);
        // validate parameters
        if ((u2bDescName == null && u2bDescPath == null)
                || (u2bDescName != null && u2bDescPath != null)) {
            throw new IllegalStateException(String.format(
                    "Illegal parameter settings: %s=%s; %s=%s",
                    PARAM_U2B_DESC_NAME, u2bDescName,
                    PARAM_U2B_DESC_PATH, u2bDescPath));
        }
    }

    @Override
    public BratUimaMapping getMapping() throws ResourceInitializationException {
        UimaBratMapping u2bMapping;
        try {
            u2bMapping = createU2BMapping();
        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
        return BratUimaMapping.reverse(u2bMapping);
    }

    private UimaBratMapping createU2BMapping() throws UIMAException, IOException {
        AnalysisEngineDescription u2bDesc;
        if (u2bDescName != null) {
            u2bDesc = AnalysisEngineFactory.createEngineDescription(u2bDescName);
        } else {
            u2bDesc = (AnalysisEngineDescription) ResourceCreationSpecifierFactory
                    .createResourceCreationSpecifier(u2bDescPath, null);
        }
        ConfigurationParameterSettings u2bParamSettings =
                u2bDesc.getAnalysisEngineMetaData().getConfigurationParameterSettings();
        String[] entityToBratStrings = (String[]) u2bParamSettings
                .getParameterValue(ENTITIES_TO_BRAT);
        List<EntityDefinitionValue> entitiesToBrat;
        if (entityToBratStrings == null) {
            entitiesToBrat = ImmutableList.of();
        } else {
            entitiesToBrat = Lists.newLinkedList();
            for (String s : entityToBratStrings) {
                entitiesToBrat.add(EntityDefinitionValue.fromString(s));
            }
        }
        String[] relationToBratStrings = (String[]) u2bParamSettings
                .getParameterValue(RELATIONS_TO_BRAT);
        List<StructureDefinitionValue> relationsToBrat;
        if (relationToBratStrings == null) {
            relationsToBrat = ImmutableList.of();
        } else {
            relationsToBrat = Lists.newLinkedList();
            for (String s : relationToBratStrings) {
                relationsToBrat.add(StructureDefinitionValue.fromString(s));
            }
        }
        String[] eventToBratStrings = (String[]) u2bParamSettings
                .getParameterValue(EVENTS_TO_BRAT);
        List<StructureDefinitionValue> eventsToBrat;
        if (eventToBratStrings == null) {
            eventsToBrat = ImmutableList.of();
        } else {
            eventsToBrat = Lists.newLinkedList();
            for (String s : eventToBratStrings) {
                eventsToBrat.add(StructureDefinitionValue.fromString(s));
            }
        }
        String[] noteMapperDefStrings = (String[]) u2bParamSettings
                .getParameterValue(BRAT_NOTE_MAPPERS);
        List<NoteMapperDefinitionValue> noteMapperDefs;
        if (noteMapperDefStrings == null) {
            noteMapperDefs = ImmutableList.of();
        } else {
            noteMapperDefs = Lists.newLinkedList();
            for (String s : noteMapperDefStrings) {
                noteMapperDefs.add(NoteMapperDefinitionValue.fromString(s));
            }
        }
        UimaBratMappingInitializer initializer = new UimaBratMappingInitializer(ts,
                entitiesToBrat, relationsToBrat, eventsToBrat, noteMapperDefs) {
            @Override
            protected BratEntityType getEntityType(String typeName) {
                return bratTypesCfg.getType(typeName, BratEntityType.class);
            }

            @Override
            protected BratRelationType getRelationType(String typeName,
                                                       Map<String, String> argTypeNames) {
                BratRelationType result = bratTypesCfg.getType(typeName, BratRelationType.class);
                checkRoleMappings(result, argTypeNames);
                return result;
            }

            @Override
            protected BratEventType getEventType(String typeName,
                                                 Map<String, String> roleTypeNames,
                                                 Set<String> multiValuedRoles) {
                BratEventType result = bratTypesCfg.getType(typeName, BratEventType.class);
                checkRoleMappings(result, roleTypeNames);
                for (String roleName : roleTypeNames.keySet()) {
                    EventRole role = result.getRole(roleName);
                    if (role.getCardinality().allowsMultipleValues() != multiValuedRoles
                            .contains(roleName)) {
                        throw new IllegalStateException(String.format(
                                "Incompatible cardinality in mapping for role %s in type %s",
                                roleName, typeName));
                    }
                }
                return result;
            }
        };
        return initializer.create();
    }

    private void checkRoleMappings(HasRoles targetType, Map<String, String> mpRoleTypeNames) {
        for (String mpRoleName : mpRoleTypeNames.keySet()) {
            String mpRoleTypeName = mpRoleTypeNames.get(mpRoleName);
            BratType mpRoleType = bratTypesCfg.getType(mpRoleTypeName);
            if (!targetType.isLegalAssignment(mpRoleName, mpRoleType)) {
                throw new IllegalStateException(String.format(
                        "Incompatible type %s for role %s in %s:",
                        mpRoleTypeName, mpRoleName, targetType));
            }
        }
    }
}