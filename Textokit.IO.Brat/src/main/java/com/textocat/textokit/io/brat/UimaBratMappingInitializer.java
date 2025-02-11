

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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.nlplab.brat.configuration.BratEntityType;
import org.nlplab.brat.configuration.BratEventType;
import org.nlplab.brat.configuration.BratRelationType;
import org.nlplab.brat.configuration.BratType;
import org.nlplab.brat.util.StringParser;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.textocat.textokit.commons.util.AnnotatorUtils.annotationTypeExist;
import static com.textocat.textokit.commons.util.AnnotatorUtils.featureExist;
import static com.textocat.textokit.io.brat.PUtils.getCollectionElementType;
import static com.textocat.textokit.io.brat.PUtils.hasCollectionRange;

/**
 * syntax for EntitiesToBrat strings:
 * <p/>
 * <pre>
 * &lt;UIMA_TYPE_NAME&gt; ("=>" &lt;BRAT_TYPE_NAME&gt;)?
 * </pre>
 * <p/>
 * syntax for RelationsToBrat strings:
 * <p/>
 * <pre>
 * &lt;UIMA_TYPE_NAME&gt; ("=>" &lt;BRAT_TYPE_NAME&gt;)? ":" &lt;Arg1FeatureName&gt; (" as " &lt;UIMA_TYPE_SHORT_NAME&gt;)? "," &lt;Arg2FeatureName&gt; (" as " &lt;UIMA_TYPE_SHORT_NAME&gt;)? <br/>
 * </pre>
 *
 * @author Rinat Gareev
 */
abstract class UimaBratMappingInitializer {

    // config fields
    private TypeSystem ts;
    private List<EntityDefinitionValue> entityDefinitions;
    private List<StructureDefinitionValue> relationDefinitions;
    private List<StructureDefinitionValue> eventDefinitions;
    private Map<Type, BratNoteMapper> type2NoteMapper;

    @SuppressWarnings("unchecked")
    public UimaBratMappingInitializer(TypeSystem ts,
                                      List<EntityDefinitionValue> entityDefinitions,
                                      List<StructureDefinitionValue> relationDefinitions,
                                      List<StructureDefinitionValue> eventDefinitions,
                                      List<NoteMapperDefinitionValue> noteMapperDefinitions)
            throws AnalysisEngineProcessException {
        this.ts = ts;
        this.entityDefinitions = entityDefinitions;
        this.relationDefinitions = relationDefinitions;
        this.eventDefinitions = eventDefinitions;
        //
        type2NoteMapper = Maps.newHashMap();
        for (NoteMapperDefinitionValue ndv : noteMapperDefinitions) {
            Type type = ts.getType(ndv.uimaType);
            annotationTypeExist(ndv.uimaType, type);
            if (type2NoteMapper.containsKey(type)) {
                throw new IllegalStateException(String.format(
                        "Duplicate note mapper declaration for %s", type));
            }
            Class<? extends BratNoteMapper> noteMapperClass;
            try {
                noteMapperClass = (Class<? extends BratNoteMapper>) Class
                        .forName(ndv.mapperClassName);
                // create instance
                BratNoteMapper mapperInstance = noteMapperClass.newInstance();
                // initialize via interface method
                mapperInstance.typeSystemInit(ts);
                // memorize
                type2NoteMapper.put(type, mapperInstance);
            } catch (Exception e) {
                throw new IllegalStateException(String.format(
                        "Can't initialize note mapper for %s", type), e);
            }
        }
    }

    protected abstract BratEntityType getEntityType(String typeName);

    protected abstract BratRelationType getRelationType(String typeName,
                                                        Map<String, String> argTypeNames);

    protected abstract BratEventType getEventType(String typeName,
                                                  Map<String, String> roleTypeNames,
                                                  Set<String> multiValuedRoles);

    UimaBratMapping create() throws AnalysisEngineProcessException {
        // mapping builder
        UimaBratMapping.Builder mpBuilder = UimaBratMapping.builder();
        // configure entity types
        for (EntityDefinitionValue entityDef : entityDefinitions) {
            Type uimaType = ts.getType(entityDef.uimaTypeName);
            annotationTypeExist(entityDef.uimaTypeName, uimaType);
            String bratTypeName = entityDef.bratTypeName;
            if (bratTypeName == null) {
                bratTypeName = uimaType.getShortName();
            }
            BratEntityType bratType = getEntityType(bratTypeName);
            checkBratType(bratType, bratTypeName);
            BratNoteMapper noteMapper = type2NoteMapper.get(uimaType);
            mpBuilder.addEntityMapping(uimaType, bratType, noteMapper);
        }
        // configure relation types
        for (StructureDefinitionValue relationDef : relationDefinitions) {
            String uimaTypeName = relationDef.uimaTypeName;
            Type uimaType = ts.getType(uimaTypeName);
            annotationTypeExist(uimaTypeName, uimaType);
            String bratTypeName = relationDef.bratTypeName;
            if (bratTypeName == null) {
                bratTypeName = uimaType.getShortName();
            }
            Map<String, Feature> argFeatures = Maps.newHashMap();
            Map<String, String> argTypeNames = Maps.newLinkedHashMap();
            for (RoleDefinitionValue rdv : relationDef.roleDefinitions) {
                String argFeatName = rdv.featureName;
                Feature argFeat = featureExist(uimaType, argFeatName);
                argFeatures.put(argFeatName, argFeat);
                Type argUimaType = detectRoleUimaType(mpBuilder, argFeat, rdv.asTypeName);
                BratEntityType argBratType = mpBuilder.getEntityType(argUimaType);
                argTypeNames.put(argFeatName, argBratType.getName());
            }

            BratRelationType brt = getRelationType(bratTypeName, argTypeNames);
            checkBratType(brt, bratTypeName);
            BratNoteMapper noteMapper = type2NoteMapper.get(uimaType);
            mpBuilder.addRelationMapping(uimaType, brt, argFeatures, noteMapper);
        }
        // configure event types
        for (StructureDefinitionValue eventDef : eventDefinitions) {
            String uimaTypeName = eventDef.uimaTypeName;
            Type uimaType = ts.getType(uimaTypeName);
            annotationTypeExist(uimaTypeName, uimaType);
            String bratTypeName = eventDef.bratTypeName;
            if (bratTypeName == null) {
                bratTypeName = uimaType.getShortName();
            }
            Map<String, Feature> roleFeatures = Maps.newHashMap();
            Map<String, String> roleTypeNames = Maps.newLinkedHashMap();
            Set<String> multiValuedRoles = Sets.newHashSet();

            for (RoleDefinitionValue rdv : eventDef.roleDefinitions) {
                String roleFeatName = rdv.featureName;
                Feature roleFeat = featureExist(uimaType, roleFeatName);
                roleFeatures.put(roleFeatName, roleFeat);
                Type roleUimaType = detectRoleUimaType(mpBuilder, roleFeat, rdv.asTypeName);
                BratType roleBratType = mpBuilder.getType(roleUimaType);
                roleTypeNames.put(roleFeatName, roleBratType.getName());
                if (hasCollectionRange(roleFeat)) {
                    multiValuedRoles.add(roleFeatName);
                }
            }

            BratEventType bet = getEventType(bratTypeName, roleTypeNames, multiValuedRoles);
            checkBratType(bet, bratTypeName);
            BratNoteMapper noteMapper = type2NoteMapper.get(uimaType);
            mpBuilder.addEventMapping(uimaType, bet, roleFeatures, noteMapper);
        }
        return mpBuilder.build();
    }

    private void checkBratType(BratType type, String typeName) {
        if (type == null) {
            throw new IllegalStateException(String.format(
                    "Can't make mapping to not existing Brat type %s", typeName));
        }
    }

    private Type detectRoleUimaType(UimaBratMapping.Builder mpBuilder, Feature roleFeat,
                                    String shortTypeNameHint) {
        Type uRoleType;
        if (shortTypeNameHint == null) {
            if (hasCollectionRange(roleFeat)) {
                uRoleType = getCollectionElementType(roleFeat);
            } else {
                uRoleType = roleFeat.getRange();
            }
        } else {
            uRoleType = mpBuilder.getUimaTypeByShortName(shortTypeNameHint);
            if (!ts.subsumes(roleFeat.getRange(), uRoleType)) {
                throw new IllegalStateException(String.format(
                        "%s is not subtype of %s", uRoleType, roleFeat.getRange()));
            }
        }
        return uRoleType;
    }
}

/**
 * this is just auxiliary interface to hold pattern constants
 */
interface MappingConfigurationPatterns {
    Pattern JAVA_CLASS_PATTERN = Pattern.compile("[._\\p{Alnum}]+");
    Pattern UIMA_TYPE_PATTERN = JAVA_CLASS_PATTERN;
    Pattern OPTIONAL_SPACE_PATTERN = Pattern.compile("\\s*");
    Pattern MAP_DELIM_PATTERN = Pattern.compile("=>");
    Pattern BRAT_TYPE_PATTERN = Pattern.compile("[_\\p{Alnum}]+");
    Pattern BEFORE_ROLES_PATTERN = Pattern.compile("\\s*:\\s*");
    String ROLE_SEP_PATTERN_STRING = "\\s*,\\s*";
    Pattern FEATURE_NAME_PATTERN = Pattern.compile("\\p{Alnum}+");
    Pattern FEATURE_CAST_PATTERN = Pattern.compile("\\s+as\\s+([_\\p{Alnum}]+)");
    Pattern BEFORE_NOTE_MAPPER_PATTERN = Pattern.compile("\\s*:\\s*");
}

class EntityDefinitionValue implements MappingConfigurationPatterns {

    static EntityDefinitionValue fromString(String str) {
        StringParser p = new StringParser(str);
        try {
            String uimaTypeName = p.consume1(UIMA_TYPE_PATTERN);
            p.skip(OPTIONAL_SPACE_PATTERN);
            String bratTypeName = null;
            if (!StringUtils.isBlank(p.getCurrentString())) {
                p.skip(MAP_DELIM_PATTERN);
                p.skip(OPTIONAL_SPACE_PATTERN);
                bratTypeName = p.consume1(BRAT_TYPE_PATTERN);
            }
            p.ensureBlank();
            return new EntityDefinitionValue(uimaTypeName, bratTypeName);
        } catch (Exception e) {
            throw new IllegalStateException(String.format(
                    "Can't parse entity mapping param value:\n%s", str), e);
        }
    }

    final String uimaTypeName;
    final String bratTypeName;

    EntityDefinitionValue(String uimaTypeName, String bratTypeName) {
        this.uimaTypeName = uimaTypeName;
        this.bratTypeName = bratTypeName;
    }
}

// base for events and relations
class StructureDefinitionValue implements MappingConfigurationPatterns {

    static StructureDefinitionValue fromString(String src) {
        StringParser p = new StringParser(src);
        try {
            String uimaTypeName = p.consume1(UIMA_TYPE_PATTERN);
            p.skip(OPTIONAL_SPACE_PATTERN);
            String bratTypeName = null;
            if (p.consumeOptional(MAP_DELIM_PATTERN) != null) {
                p.skip(OPTIONAL_SPACE_PATTERN);
                bratTypeName = p.consume1(BRAT_TYPE_PATTERN);
            }
            p.skip(BEFORE_ROLES_PATTERN);
            String roleDeclarationsSrc = p.getCurrentString();
            // parse role declarations
            String[] roleDeclStrings = roleDeclarationsSrc.split(ROLE_SEP_PATTERN_STRING);
            // trim trailing whitespace in last string
            roleDeclStrings[roleDeclStrings.length - 1] =
                    roleDeclStrings[roleDeclStrings.length - 1].trim();

            List<RoleDefinitionValue> roleDefs = Lists.newLinkedList();
            for (String roleDeclStr : roleDeclStrings) {
                roleDefs.add(RoleDefinitionValue.fromString(roleDeclStr));
            }
            return new StructureDefinitionValue(uimaTypeName, bratTypeName, roleDefs);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format(
                    "Can't parse structure mapping param value:\n%s", src), e);
        }
    }

    final String uimaTypeName;
    final String bratTypeName;
    final List<RoleDefinitionValue> roleDefinitions;

    StructureDefinitionValue(String uimaTypeName, String bratTypeName,
                             List<RoleDefinitionValue> roleDefinitions) {
        this.uimaTypeName = uimaTypeName;
        this.bratTypeName = bratTypeName;
        this.roleDefinitions = ImmutableList.copyOf(roleDefinitions);
    }

}

class RoleDefinitionValue implements MappingConfigurationPatterns {

    static RoleDefinitionValue fromString(String src) {
        StringParser p = new StringParser(src);
        try {
            String featureName = p.consume1(FEATURE_NAME_PATTERN);
            String[] castDecl = p.consumeOptional(FEATURE_CAST_PATTERN);
            String asTypeName = null;
            if (castDecl != null) {
                asTypeName = castDecl[1];
            }
            p.ensureBlank();
            return new RoleDefinitionValue(featureName, asTypeName);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format(
                    "Can't parse role definition: %s", src), e);
        }
    }

    final String featureName;
    final String asTypeName;

    RoleDefinitionValue(String featureName, String asTypeName) {
        this.featureName = featureName;
        this.asTypeName = asTypeName;
    }

    @Override
    public int hashCode() {
        return featureName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RoleDefinitionValue)) {
            return false;
        }
        RoleDefinitionValue that = (RoleDefinitionValue) obj;
        return Objects.equal(this.featureName, that.featureName)
                && Objects.equal(this.asTypeName, that.asTypeName);
    }
}

class NoteMapperDefinitionValue implements MappingConfigurationPatterns {

    static NoteMapperDefinitionValue fromString(String str) {
        StringParser p = new StringParser(str);
        String uimaType = p.consume1(UIMA_TYPE_PATTERN);
        p.skip(BEFORE_NOTE_MAPPER_PATTERN);
        String mapperClassName = p.consume1(JAVA_CLASS_PATTERN);
        return new NoteMapperDefinitionValue(uimaType, mapperClassName);
    }

    final String uimaType;
    final String mapperClassName;

    public NoteMapperDefinitionValue(String uimaType, String mapperClassName) {
        this.uimaType = uimaType;
        this.mapperClassName = mapperClassName;
    }
}