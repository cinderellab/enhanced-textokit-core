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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.textocat.textokit.commons.DocumentMetadata;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.*;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.CasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.factory.initializable.InitializableFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.nlplab.brat.ann.*;
import org.nlplab.brat.configuration.*;
import org.nlplab.brat.configuration.EventRole.Cardinality;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.nlplab.brat.BratConstants.*;

/**
 * TODO adjust this top javadoc
 * <p/>
 * UIMA Annotator is CAS Annotator to convert UIMA annotations to brat standoff
 * format annotations. 1) defines input, ouput files directories 2) reading
 * annotator descriptor file and converts parameters to brat configuration file
 * saved as annotation.conf 3) saves annotations text file using specified file
 * name parameter in DocumentMetadata annotations. 4) reading UIMA annotations
 * and converts them to brat annotation (*.ann files)
 * <p/>
 * T: text-bound annotation R: relation E: event A: attribute M: modification
 * (alias for attribute, for backward compatibility) N: normalization #: note
 * <p/>
 * For event annotation you have to add additional info about event entities
 * into desc file
 *
 * @author Rinat Gareev
 * @author pathfinder
 */
@OperationalProperties(modifiesCas = false, multipleDeploymentAllowed = false)
public class UIMA2BratAnnotator extends CasAnnotator_ImplBase {

    public final static String BRAT_OUT = "BratOutputDir";
    public final static String ENTITIES_TO_BRAT = "EntitiesToBrat";
    public final static String RELATIONS_TO_BRAT = "RelationsToBrat";
    public final static String EVENTS_TO_BRAT = "EventsToBrat";
    public final static String BRAT_NOTE_MAPPERS = "BratNoteMappers";
    public static final String PARAM_OUTPUT_PATH_FUNCTION = "outputPathFunction";

    // annotator configuration fields
    @ConfigurationParameter(name = BRAT_OUT, mandatory = true)
    private File bratDirectory;
    @ConfigurationParameter(name = ENTITIES_TO_BRAT, mandatory = false)
    private String[] entitiesToBratRaw;
    private List<EntityDefinitionValue> entitiesToBrat;
    @ConfigurationParameter(name = RELATIONS_TO_BRAT, mandatory = false)
    private String[] relationsToBratRaw;
    private List<StructureDefinitionValue> relationsToBrat;
    @ConfigurationParameter(name = EVENTS_TO_BRAT, mandatory = false)
    private String[] eventsToBratRaw;
    private List<StructureDefinitionValue> eventsToBrat;
    @ConfigurationParameter(name = BRAT_NOTE_MAPPERS, mandatory = false)
    private String[] noteMappersDefinitionsRaw;
    private List<NoteMapperDefinitionValue> noteMappersDefinitions;
    @ConfigurationParameter(name = PARAM_OUTPUT_PATH_FUNCTION, mandatory = false,
            defaultValue = "com.textocat.textokit.consumer.DefaultSourceURI2OutputFilePathFunction")
    private Class<? extends Function> outPathFuncClass;

    // derived configuration fields
    private BratTypesConfiguration bratTypesConfig;
    private UimaBratMapping mapping;
    private Function<DocumentMetadata, Path> outPathFunc;

    // state fields
    private TypeSystem ts;

    // per-CAS state fields
    private String currentDocName;
    private BratAnnotationContainer bac;
    private ToBratMappingContext context;

    @Override
    public void initialize(UimaContext ctx)
            throws ResourceInitializationException {
        super.initialize(ctx);

        getLogger().info("Annotator is initializing ...");
        if (entitiesToBratRaw == null) {
            entitiesToBrat = ImmutableList.of();
        } else {
            entitiesToBrat = Lists.newLinkedList();
            for (String valStr : entitiesToBratRaw) {
                entitiesToBrat.add(EntityDefinitionValue.fromString(valStr));
            }
            entitiesToBrat = ImmutableList.copyOf(entitiesToBrat);
        }
        if (relationsToBratRaw == null) {
            relationsToBrat = ImmutableList.of();
        } else {
            relationsToBrat = Lists.newLinkedList();
            for (String valStr : relationsToBratRaw) {
                StructureDefinitionValue val = StructureDefinitionValue.fromString(valStr);
                if (val.roleDefinitions.size() != 2) {
                    throw new IllegalArgumentException(String.format(
                            "Illegal relation definition: %s", valStr));
                }
                relationsToBrat.add(val);
            }
            relationsToBrat = ImmutableList.copyOf(relationsToBrat);
        }
        if (eventsToBratRaw == null) {
            eventsToBrat = ImmutableList.of();
        } else {
            eventsToBrat = Lists.newLinkedList();
            for (String valStr : 