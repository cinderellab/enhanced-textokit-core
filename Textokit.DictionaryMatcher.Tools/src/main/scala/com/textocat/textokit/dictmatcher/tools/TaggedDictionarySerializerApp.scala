
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

package com.textocat.textokit.dictmatcher.tools

import java.io.{BufferedOutputStream, File}

import com.textocat.textokit.chunk.ChunkerBuilder
import com.textocat.textokit.commons.util.PipelineDescriptorUtils
import com.textocat.textokit.morph.commons.SimplyWordAnnotator
import com.textocat.textokit.morph.dictionary.MorphDictionaryAPIFactory
import com.textocat.textokit.morph.fs.SimplyWord
import com.textocat.textokit.morph.lemmatizer.LemmatizerAPI
import com.textocat.textokit.postagger.PosTaggerAPI
import com.textocat.textokit.segmentation.SingleSentenceAnnotator
import com.textocat.textokit.tokenizer.TokenizerAPI
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SerializationUtils
import org.apache.uima.analysis_engine.AnalysisEngine
import org.apache.uima.fit.factory.AnalysisEngineFactory
import org.apache.uima.fit.util.JCasUtil

import scala.collection.JavaConversions._
import scala.collection.immutable.ListMap
import scala.io.Source

/**
 * TODO NOT FINISHED, as there are no serializable Chunker implementations yet
 * <p>
 * An app that takes a set of input files in the format specified below and produces
 * a serialized instance of [[com.textocat.textokit.chunk.Chunker]] which
 * can be later instantiated by [[com.textocat.textokit.resource.SerializedResourceLocator]].
 * </p>
 * <h2>Input (text) file format:</h2>
 * <p>One dictionary entry per line.
 * </p>
 * <h2>Entry normalization</h2>
 * Each entry is normalized using a lemmatizer pipeline.
 * <h2>Dictionary metadata</h2>
 * An entry is assigned a tag (see [[com.textocat.textokit.dictmatcher.TaggedChunkAnnotationAdapter]]) given by its containing file. A tag of file
 * is defined in app arguments like <code>foo.txt=>foo_tag</code>, i.e. all entries from <code>foo.txt</code> get
 * tag <code>foo_tag</code>.
 *
 * @author Rinat Gareev
 */
object TaggedDictionarySerializerApp {

  val loggingPeriodicity = 1000

  def main(args: Array[String]): Unit = {
    val (inputBaseDir, outputFile, fileTagTuples) = args.toList match {
      case inputBasePath :: outputPath :: fileTagMappingStrs if fileTagMappingStrs.nonEmpty =>
        (new File(inputBasePath), new File(outputPath),
          fileTagMappingStrs.map(_.split("=>")).map {
            case Array(filename, tag) => (filename, tag.trim.intern())
            case _ =>
              System.err.println("Can't mapping argument")
              sys.exit(1)
          })
      case _ =>
        System.err.println("Usage: <input-dir> <ser-output-file> < filename => tag >+")
        sys.exit(1)
    }
    // TODO
    val builder: ChunkerBuilder[String] = ???
    //
    val lemmatizerAE: AnalysisEngine = AnalysisEngineFactory.createEngine(lemmatizerAEDesc)
    val jCas = lemmatizerAE.newJCas()
    var entryCounter = 0
    def addToBuilder(file: File, tag: String): Unit = {
      val fileSrc = Source.fromFile(file, "UTF-8")
      try {
        for (line <- fileSrc.getLines() if line.trim.nonEmpty) {
          jCas.setDocumentText(line)
          lemmatizerAE.process(jCas)
          // TODO refactor this strategy and save with chunker
          val tokens = JCasUtil.select(jCas, classOf[SimplyWord]).map(sw => sw.getLemma match {
            case null => sw.getCoveredText
            case lemma => lemma
          })
          builder.addEntry(asJavaIterable(tokens), tag)
          // TODO implement debug option when normalized tokens are written into a separate plain text file
          entryCounter += 1
          if (entryCounter % loggingPeriodicity == 0) {
            println(s"$entryCounter entries have been added...")
          }
          jCas.reset()
        }
      } finally {
        fileSrc.close()
      }
    }
    //
    for ((filename, tag) <- fileTagTuples) {
      addToBuilder(new File(inputBaseDir, filename), tag)
    }
    val chunker = builder.build()
    //
    val out = new BufferedOutputStream(FileUtils.openOutputStream(outputFile))
    SerializationUtils.serialize(chunker.asInstanceOf[java.io.Serializable], out)
    println(s"Finished serialization. The result file size is ${outputFile.length() / 1024} Kb")
  }

  private val lemmatizerAEDesc = {
    val desc = PipelineDescriptorUtils.createAggregateDescription(ListMap(
      "tokenizer" -> TokenizerAPI.getAEImport,
      "sentence-splitter" -> SingleSentenceAnnotator.createDescription(),
      "pos-tagger" -> PosTaggerAPI.getAEImport,
      "lemmatizer" -> LemmatizerAPI.getAEImport,
      "sw-make" -> SimplyWordAnnotator.createDescription()
    ))
    val morphDictDesc = MorphDictionaryAPIFactory.getMorphDictionaryAPI.getResourceDescriptionForCachedInstance
    morphDictDesc.setName(PosTaggerAPI.MORPH_DICTIONARY_RESOURCE_NAME)
    PipelineDescriptorUtils.getResourceManagerConfiguration(desc).addExternalResource(morphDictDesc)
    desc
  }
}