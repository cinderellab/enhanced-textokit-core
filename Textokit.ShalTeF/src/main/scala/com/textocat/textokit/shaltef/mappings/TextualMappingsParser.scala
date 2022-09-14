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

import java.io.{BufferedReader, InputStreamReader, Reader}
import java.net.URL

import com.textocat.textokit.shaltef.mappings.impl.DefaultDepToArgMapping
import com.textocat.textokit.shaltef.mappings.pattern._
import org.apache.uima.cas.{Feature, Type}

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * @author Rinat Gareev
 */
private[mappings] class TextualMappingsParser(config: MappingsParserConfig) extends MappingsParser {

  import config._

  override def parse(url: URL, templateAnnoType: Type, mappingsHolder: DepToArgMappingsBuilder) {
    val is = url.openStream()
    val reader = new BufferedReader(new InputStreamReader(is, "utf-8"))
    try {
      val mappings = new DocParsers(templateAnnoType, url).parseDoc(reader)
      mappings.foreach(mappingsHolder.add)
    } finally {
      reader.close()
    }
  }

  private class DocParsers(templateType: Type, url: URL) extends JavaTokenParsers {
    def parseDoc(reader: Reader): List[DepToArgMapping] = parseAll(mappingsDoc, reader) match {
      case Success(mappings, _) => mappings
      case NoSuccess(msg, remainingInput) => throw new IllegalStateException(
        "Unsuccessful parsing of %s at line %s & column %s:\n%s".format(
          url, remainingInput.pos.line, remainingInput.pos.column, msg))
    }

    private def mappingsDoc: Parser[List[DepToArgMapping]] =
      rep(rep(comment) ~> mappingDecl <~ rep(comment))

    private def comment: Parser[String] = """#[^\n]*""".r

    private def mappingDecl: Parser[DepToArgMapping] = triggerDecl ~ rep1(slotMapping) ^^ {
      case lemmaIdSet ~ slotMappings => new DefaultDepToArgMapping(templateType,
        lemmaIdSet, slotMappings)
    }

    private def triggerDecl = "[" ~> triggerConstraint <~ "]"

    private def triggerConstraint = "lemma(" ~> rep1sep(identifiedWordformLiteral, ",") <~ ")" ^^ {
      _.toSet.flatten
    }

    private def identifiedWordformLiteral = stringLiteral ^? {
      case strLiteral =>
        val str = strLiteral.substring(1, strLiteral.length - 1)
        val lemmaIdSet = morphDict.getEntries(str) match {
          case null => Set.empty[Int]
          case wfSet => wfSet.filter(_.getLemmaId >= 0).map(_.getLemmaId).toSet
        }
        if (lemmaIdSet.isEmpty)
          throw new IllegalArgumentException("Can't find lemmaId for word '%s'".format(str))
        else lemmaIdSet
    }

    private def slotMapping: Parser[SlotMapping] =
      slotPattern ~ slotMappingOptionality ~ (templateFeatureName | templateFeatureStub) ^^ {
        case pattern ~ optionality ~ slotFeatureOpt =>
         