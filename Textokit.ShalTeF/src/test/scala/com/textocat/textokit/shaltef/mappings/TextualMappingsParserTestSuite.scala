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

package com.textocat.textokit.shaltef.mappings

import java.io.File
import java.util.BitSet

import com.textocat.textokit.morph.dictionary.resource.{GramModel, MorphDictionary}
import com.textocat.textokit.morph.model.Wordform
import com.textocat.textokit.shaltef.mappings.impl.{DefaultDepToArgMapping, DefaultDepToArgMappingsHolder}
import com.textocat.textokit.shaltef.mappings.pattern._
import com.textocat.textokit.shaltef.util.CasTestUtils
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.mock.MockitoSugar

import scala.collection.JavaConversions.seqAsJavaList

/**
 * @author Rinat Gareev
 */
class TextualMappingsParserTestSuite extends FunSuite with MockitoSugar with CasTestUtils {

  private val ts = loadTypeSystem("mappings.ts-test")

  test("Parse mappings in release.txt") {
    val morphDict = mock[MorphDictionary]
    val gramModel = mock[GramModel]
    when(morphDict.getGramModel).thenReturn(gramModel)
    // stub invocations for trigger lemma search
    when(morphDict.getEntries("выпустил")).thenReturn(
      Wordform.builder(gramModel, 100).build() :: Nil)
    when(morphDict.getEntries("выйдет")).thenReturn(
      Wordform.builder(gramModel, 200).build :: Nil)
    // stub invocations for grammeme extractors
    val caseBS = new BitSet
    caseBS.set(100)
    val gndrBS = new BitSet
    caseBS.set(200)
    when(gramModel.getGrammemWithChildrenBits("CAse", false)).thenReturn(caseBS)
    when(gramModel.getGrammemWithChildrenBits("GNdr", false)).thenReturn(gndrBS)
    when(gramModel.toGramSet(caseBS)).thenReturn(List("nomn", "accs", "ablt"))
    when(gramModel.toGramSet(gndrBS)).thenReturn(List("masc", "femn", "neut"))

    val constrValueFactory = new ConstraintValueFactory(gramModel)
    val constrTargetFactory = new ConstraintTargetFactory(gramModel)
    val constrFactory = new PhraseConstraintFactory
    val parser = TextualMappingsParser(new MappingsParserConfig(morphDict))

    val mappingsBuilder = DepToArgMappingsBuilder()
    val templateAnnoType = ts.getType("test.Release")
    assert(templateAnnoType != null, "Can't final Release annotation type")
    parser.parse(new File("src/test/resources/mappings/release.txt").toURI.toURL,
      templateAnnoType, mappingsBuilder)

    val subjFeat = templateAnnoType.getFeatureByBaseName("subj")
    val objFeat = templateAnnoType.getFeatureByBaseName("obj")
    val dateFeat = templateAnnoType.getFeatureByBaseName("date")

    val mappings = mappingsBuilder.build().asInstanceOf[DefaultDepToArgMappingsHolder]
    assert(mappings.triggerLemmaId2Mappings.size === 2)

    import constrFactory._
    import constrTargetFactory._
    import constrValueFactory._
    val pattern1 = new ConstraintConjunctionPhrasePattern(
      phraseConstraint(headFeature("case"), Equals, constant("nomn")) ::
        phraseConstraint(headFeature("gndr"), Equals, triggerFeatureReference("gndr")) ::
        Nil)
    val pattern2 = new ConstraintConjunctionPhrasePattern(
      phraseConstraint(headFeature("case"), Equals, constant("accs"))
        :: Nil)
    val pattern3 = new ConstraintConjunctionPhrasePattern(
      phraseConstraint(prepositionTarget, Equals, constant("в"))
        :: Nil)
    assert(mappings.triggerLemmaId2Mappings(100) === new DefaultDepToArgMapping(
      templateAnnoType, Set(100),
      SlotMapping(pattern1, false, Some(subjFeat)) ::
        SlotMapping(pattern2, false, Some(objFeat)) ::
        SlotMapping(pattern3, true, Some(dateFeat)) :: Nil)
      :: Nil)

    val pattern4 = new ConstraintConjunctionPhrasePattern(
      phraseConstraint(headFeature("case"), Equals, constant("nomn")) ::
        phraseConstraint(headFeature("gndr"), Equals, triggerFeatureReference("gndr")) :: Nil)
    val pattern5 = new ConstraintConjunctionPhrasePattern(
      phraseConstraint(prepositionTarget, Equals, constant("в")) ::
        phraseConstraint(HasHeadsPath, constantCollectionAlternatives(
          Set(List("год"))))
        :: Nil)
    val pattern6 = new ConstraintConjunctionPhrasePattern(
      phraseConstraint(HasHeadsPath, constantCollectionAlternatives(
        Set(List("пресс-релиз"), List("сообщение", "сайт"))))
        :: Nil)
    val pattern7 = new ConstraintConjunctionPhrasePattern(
      phraseConstraint(prepositionTarget, Equals, constant("на"))
        :: Nil)
    assert(mappings.triggerLemmaId2Mappings(200) === new DefaultDepToArgMapping(
      templateAnnoType, Set(200),
      SlotMapping(pattern4, false, Some(objFeat)) ::
        SlotMapping(pattern5, false, Some(dateFeat)) ::
        SlotMapping(pattern6, false, Some(subjFeat)) ::
        SlotMapping(pattern7, false, None) :: Nil)
      :: Nil)
  }

}