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
package com.textocat.textokit.phrrecog.input

import org.apache.uima.cas.text.AnnotationFS

import scala.util.parsing.input.{Position, Reader}

/**
 * @author Rinat Gareev
 *
 */
class AnnotationSpan[A >: Null <: AnnotationFS](annoList: List[A]) {
  require(!annoList.isEmpty, "annotation list is empty")

  private val baseOffset = annoList.head.getBegin
  private lazy val endOffset = annoList.last.getEnd

  // protected val endOfSequence: A

  lazy val reader: Reader[A] = new AnnotationReader(annoList)

  private class AnnotationReader private[AnnotationSpan](inputList: List[A]) extends Reader[A] {

    override val atEnd: Boolean = inputList.isEmpty

    override val first: A =
      if (atEnd) null
      else inputList.head

    override val rest: AnnotationReader =
      if (atEnd) this
      else new AnnotationReader(inputList.tail)

    override lazy val pos: Position =
      if (atEnd) new EndOfSequencePosition
      else new AnnotationPosition(first)
  }

  class AnnotationPosition(anno: AnnotationFS) extends Position {
    require(anno.getBegin() >= baseOffset, "illegal begin of annotation")

    override val line: Int = 1
    // NOTE! Column numbers start at 1
    override val column: Int = anno.getBegin() - baseOffset + 1

    override lazy val lineContents: String = inputContentString
  }

  class EndOfSequencePosition extends Position {
    override val line: Int = 1
    // NOTE! Column numbers start at 1
    override val column: Int = endOffset

    override lazy val lineContents: String = inputContentString
  }

  private lazy val inputContentString = {
    val sb = new StringBuilder()
    appendAnnotationContent(sb, 0, annoList)
    sb.toString()
  }

  private def appendAnnotationContent(sb: StringBuilder, lastAnnoEnd: Int, list: List[A]): Unit =
    if (!list.isEmpty) {
      val anno = list.head
      lastAnnoEnd.until(anno.getBegin()).foreach(sb.append(' '))
      sb.append(anno.getCoveredText())
      appendAnnotationContent(sb, anno.getEnd(), list.tail)
    }
}