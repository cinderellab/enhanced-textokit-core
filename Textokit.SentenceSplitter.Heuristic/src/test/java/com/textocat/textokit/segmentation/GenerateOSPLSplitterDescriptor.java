
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

package com.textocat.textokit.segmentation;

import com.textocat.textokit.segmentation.heur.OneSentencePerLineSplitter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Rinat Gareev
 */
public class GenerateOSPLSplitterDescriptor {
    public static void main(String[] args) throws UIMAException, IOException, SAXException {
        String outputPath = "src/main/resources/"
                + OneSentencePerLineSplitter.class.getName().replace('.', '/') + ".xml";
        AnalysisEngineDescription desc = OneSentencePerLineSplitter.createDescription();
        FileOutputStream out = FileUtils.openOutputStream(new File(outputPath));
        try {
            desc.toXML(out);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}