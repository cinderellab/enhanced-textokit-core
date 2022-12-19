
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

package com.textocat.textokit.shaltef

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}

import com.textocat.textokit.morph.fs.{Word, Wordform}
import com.textocat.textokit.phrrecog.cas.{NounPhrase, Phrase}
import com.textocat.textokit.segmentation.SentenceSplitterAPI
import com.textocat.textokit.segmentation.fstype.Sentence
import com.textocat.textokit.shaltef.util.{CasTestUtils, NprCasBuilder}
import com.textocat.textokit.tokenizer.TokenizerAPI
import org.apache.uima.cas.{CAS, TypeSystem}
import org.apache.uima.cas.impl.XmiCasDeserializer
import org.apache.uima.cas.text.{AnnotationFS, AnnotationIndex}
import org.apache.uima.fit.util.CasUtil._
import org.apache.uima.fit.util.FSCollectionFactory
import org.apache.uima.jcas.JCas
import org.apache.uima.util.CasCreationUtils
import org.scalatest.FunSuite

import scala.collection.JavaConversions.iterableAsScalaIterable

/**
 * @author Rinat Gareev
 */
class PhraseIndexTestSuite extends FunSuite with CasTestUtils {

  private val ts = loadTypeSystem("com.textocat.textokit.phrrecog.ts-phrase-recognizer",
    TokenizerAPI.TYPESYSTEM_TOKENIZER,
    SentenceSplitterAPI.TYPESYSTEM_SENTENCES)

  private val sentenceType = ts.getType(classOf[Sentence].getName)

  test("Create phrase index over segment without NPs") {
    val cas = readCAS("src/test/resources/phr-idx/no-np.xmi")
    val jCas = cas.getJCas
    val segment = selectSingle(cas, sentenceType).asInstanceOf[AnnotationFS]
    val idx = new PhraseIndex(segment, selectWord(jCas, "перенесен"), ts)
    assert(idx.phraseSeq.isEmpty)
    assert(idx.refWordIndex === -1)
  }

  test("Create phrase index over segment with NPs and refWord is not inside an NP") {
    val cas = readCAS("src/test/resources/phr-idx/9-np.xmi")
    val jCas = cas.getJCas
    val segment = selectSingle(cas, sentenceType).asInstanceOf[AnnotationFS]
    val idx = new PhraseIndex(segment, selectWord(jCas, "перенесен"), ts)
    assert(idx.phraseSeq.toList.map(toTestNP(_)) ===
      TestNP("матч", depWords = "Отборочный" :: Nil,
        depNPs = TestNP("чемпионата", depNPs = TestNP("мира") :: Nil) :: Nil) ::
        TestNP("футболу", "по") ::
        TestNP("года", depWords = "2014" :: Nil) ::
        TestNP("сборными", "между",
          depNPs = TestNP("Ирландии", depWords = "Северной" :: Nil) :: TestNP("России") :: Nil) ::
        TestNP("срок", "на", depWords = "более" :: "поздний" :: Nil) :: Nil)
    assert(idx.refWordIndex == 3)
  }

  test("Create phrase index over segment with NPs and refWord is head of an NP") {
    val cas = readCAS("src/test/resources/phr-idx/9-np.xmi")
    val jCas = cas.getJCas()
    val segment = selectSingle(cas, sentenceType).asInstanceOf[AnnotationFS]
    val idx = new PhraseIndex(segment, selectWord(jCas, "сборными"), ts)
    assert(idx.phraseSeq.toList.map(toTestNP(_)) ===
      TestNP("матч", depWords = "Отборочный" :: Nil,
        depNPs = TestNP("чемпионата", depNPs = TestNP("мира") :: Nil) :: Nil) ::
        TestNP("футболу", "по") ::
        TestNP("года", depWords = "2014" :: Nil) ::
        TestNP("Ирландии", depWords = "Северной" :: Nil) ::
        TestNP("России") ::
        TestNP("срок", "на", depWords = "более" :: "поздний" :: Nil) :: Nil)
    assert(idx.refWordIndex == 2)
  }

  test("Create phrase index over segment with NPs and refWord is head of a sub-NP") {
    val cas = readCAS("src/test/resources/phr-idx/9-np.xmi")
    val jCas = cas.getJCas()
    val segment = selectSingle(cas, sentenceType).asInstanceOf[AnnotationFS]
    val idx = new PhraseIndex(segment, selectWord(jCas, "чемпионата"), ts)
    assert(idx.phraseSeq.toList.map(toTestNP(_)) ===
      TestNP("мира") ::
        TestNP("футболу", "по") ::
        TestNP("года", depWords = "2014" :: Nil) ::
        TestNP("сборными", "между",
          depNPs = TestNP("Ирландии", depWords = "Северной" :: Nil) :: TestNP("России") :: Nil) ::
        TestNP("срок", "на", depWords = "более" :: "поздний" :: Nil) :: Nil)
    assert(idx.refWordIndex == -1)
  }

  test("Create phrase index over segment with NPs and refWord is head of a sub-NP - 2") {
    val cas = readCAS("src/test/resources/phr-idx/9-np.xmi")
    val jCas = cas.getJCas()
    val segment = selectSingle(cas, sentenceType).asInstanceOf[AnnotationFS]
    val idx = new PhraseIndex(segment, selectWord(jCas, "мира"), ts)
    assert(idx.phraseSeq.toList.map(toTestNP(_)) ===
      TestNP("футболу", "по") ::
        TestNP("года", depWords = "2014" :: Nil) ::
        TestNP("сборными", "между",
          depNPs = TestNP("Ирландии", depWords = "Северной" :: Nil) :: TestNP("России") :: Nil) ::
        TestNP("срок", "на", depWords = "более" :: "поздний" :: Nil) :: Nil)
    assert(idx.refWordIndex == -1)
  }

  private def toTestNP(p: Phrase): TestNP = {
    assert(p.isInstanceOf[NounPhrase])
    val np = p.asInstanceOf[NounPhrase]
    val testDepWords =
      if (np.getDependentWords != null)
        FSCollectionFactory.create(np.getDependentWords, classOf[Wordform]).toList
      else Nil
    TestNP(np.getHead.getWord.getCoveredText,
      toTestWord(np.getPreposition),
      toTestWord(np.getParticle),
      testDepWords.map(toTestWord),
      if (np.getDependentPhrases != null)
        FSCollectionFactory.create(np.getDependentPhrases, classOf[Phrase]).toList.map(toTestNP)
      else Nil)
  }

  test("Create phrase index over segment with NPs and refWord is head of last NP") {
    val cas = readCAS("src/test/resources/phr-idx/9-np.xmi")
    val jCas = cas.getJCas
    val segment = selectSingle(cas, sentenceType).asInstanceOf[AnnotationFS]
    val idx = new PhraseIndex(segment, selectWord(jCas, "срок"), ts)
    assert(idx.phraseSeq.toList.map(toTestNP(_)) ===
      TestNP("матч", depWords = "Отборочный" :: Nil,
        depNPs = TestNP("чемпионата", depNPs = TestNP("мира") :: Nil) :: Nil) ::
        TestNP("футболу", "по") ::
        TestNP("года", depWords = "2014" :: Nil) ::
        TestNP("сборными", "между",
          depNPs = TestNP("Ирландии", depWords = "Северной" :: Nil) :: TestNP("России") :: Nil) ::
        Nil)
    assert(idx.refWordIndex == 3)
  }

  private def toTestWord(w: Wordform): String =
    if (w == null) null
    else w.getWord.getCoveredText

  private def selectWord(jCas: JCas, wordTxt: String): Word = {
    val wordIdx: Iterable[Word] = jCas.getAnnotationIndex(Word.typeIndexID).asInstanceOf[AnnotationIndex[Word]]
    wordIdx.find(_.getCoveredText == wordTxt) match {
      case Some(w) => w
      case None => throw new IllegalStateException("Can't find word '%s'".format(wordTxt))
    }
  }

  private def readCAS(filePath: String): CAS = readCAS(filePath, ts)

  private def readCAS(filePath: String, ts: TypeSystem): CAS = {
    val file = new File(filePath)
    if (!file.exists()) throw new IllegalArgumentException(
      "File %s does not exist".format(file))
    val xmiIS = new BufferedInputStream(new FileInputStream(file))
    try {
      readCAS(xmiIS, ts)
    } finally {
      xmiIS.close()
    }
  }

  private def readCAS(xmiIS: InputStream, ts: TypeSystem): CAS = {
    val cas = CasCreationUtils.createCas(ts, null, null, null)
    XmiCasDeserializer.deserialize(xmiIS, cas)
    cas
  }
}

private[shaltef] case class TestNP(head: String, prepOpt: String = null, particleOpt: String = null,
                                   depWords: List[String] = Nil, depNPs: List[TestNP] = Nil)

object PhraseIndexTestSuiteResources extends App {
  private val text = "Отборочный матч чемпионата мира по футболу 2014 года между сборными Северной Ирландии и России перенесен на более поздний срок."

  {
    // no-np.xmi
    val cb = new NprCasBuilder(text, Nil)
    preprocess(cb)
    cb.serialize("src/test/resources/phr-idx/no-np.xmi")
  }
  {
    // 9-np.xmi
    val cb = new NprCasBuilder(text, Nil)
    preprocess(cb)
    import cb._
    val np1 = np("мира")
    val np2 = np("чемпионата", depNPs = np1 :: Nil)
    val np3 = np("матч", depWordIds = "Отборочный" :: Nil, depNPs = np2 :: Nil, index = true)
    val np4 = np("футболу", "по", index = true)
    val np5 = np("года", depWordIds = "2014" :: Nil, index = true)
    val np6 = np("Ирландии", depWordIds = "Северной" :: Nil)
    val np7 = np("России")
    val np8 = np("сборными", "между", depNPs = np6 :: np7 :: Nil, index = true)
    val np9 = np("срок", "на", depWordIds = "более" :: "поздний" :: Nil, index = true)
    serialize("src/test/resources/phr-idx/9-np.xmi")
  }

  private def preprocess(cb: NprCasBuilder) {
    import cb._
    w(0, 10)
    w(11, 15)
    w(16, 26)
    w(27, 31)
    w(32, 34)
    w(35, 42)
    w(43, 47)
    w(48, 52)
    w(53, 58)
    w(59, 67)
    w(68, 76)
    w(77, 85)
    w(86, 87)
    w(88, 94)
    w(95, 104)
    w(105, 107)
    w(108, 113)
    w(114, 121)
    w(122, 126)
    sent(0, 127)
  }
}