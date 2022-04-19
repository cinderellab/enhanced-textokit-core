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

/**
 *
 */
package com.textocat.textokit.phrrecog.util

import java.io.File
import java.net.URI

import com.textocat.textokit.commons.DocumentMetadata
import com.textocat.textokit.commons.cas.AnnotationUtils
import com.textocat.textokit.commons.util.AnnotatorUtils._
import com.textocat.textokit.phrrecog.util.StandoffAnnotationsProcessor._
import com.textocat.textokit.segmentation.fstype.Paragraph
import com.textocat.textokit.tokenizer.fstype.Token
import org.apache.commons.io.FilenameUtils
import org.apache.uima.UimaContext
import org.apache.uima.cas.Type
import org.apache.uima.cas.text.AnnotationFS
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.{CasUtil, JCasUtil}
import org.apache.uima.jcas.JCas

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * @author Rinat Gareev
 *
 */
class StandoffAnnotationsProcessor extends JCasAnnotator_ImplBase {

  // state
  private var tokenType: Type = _
  private var annoStrParserFactory: PhraseStringParsersFactory = _

  override def initialize(ctx: UimaContext) {
    super.initialize(ctx)
    val annoStrParserFactoryClassName = ctx.getConfigParameterValue(ParamAnnotationStringParserFactoryClass).asInstanceOf[String]
    mandatoryParam(ParamAnnotationStringParserFactoryClass, annoStrParserFactoryCla