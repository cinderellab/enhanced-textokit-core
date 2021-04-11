
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


package com.textocat.textokit.commons.util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Rinat Gareev
 */
public class CrossValidationCorpusSplitter {

    @Parameter(names = "-f", required = true)
    private int foldNum;
    @Parameter(names = {"-s", "--corpus-file-suffix"})
    private String corpusFileSuffix;
    @Parameter(names = {"-r", "-R"})
    private boolean includeSubDirectores = true;
    @Parameter(names = {"-c", "--corpus-dir"}, required = true)
    private File corpusDir;
    // output to current dir
    private File outputDir = new File(".");
    private CrossValidationCorpusSplitter() {
    }

    public static void main(String[] args) throws Exception {
        CrossValidationCorpusSplitter launcher = new CrossValidationCorpusSplitter();
        new JCommander(launcher, args);
        launcher.run();
    }

    public static File getTrainingListFile(File dir, int fold) {
        return new File(dir, CorpusUtils.getTrainPartitionFilename(fold));
    }

    public static File getTestingListFile(File dir, int fold) {
        return new File(dir, CorpusUtils.getTestPartitionFilename(fold));
    }

    private void run() throws Exception {
        IOFileFilter corpusFileFilter;
        if (corpusFileSuffix == null) {
            corpusFileFilter = FileFilterUtils.trueFileFilter();
        } else {
            corpusFileFilter = FileFilterUtils.suffixFileFilter(corpusFileSuffix);
        }
        IOFileFilter corpusSubDirFilter = includeSubDirectores ? TrueFileFilter.INSTANCE : null;
        List<CorpusSplit> corpusSplits = CorpusUtils.createCrossValidationSplits(corpusDir,
                corpusFileFilter, corpusSubDirFilter, foldNum);
        for (int i = 0; i < corpusSplits.size(); i++) {
            writeFileLists(outputDir, i, corpusSplits.get(i));
        }
    }

    private void writeFileLists(File outputDir, int i, CorpusSplit corpusSplit)
            throws IOException {
        File trainingList = getTrainingListFile(outputDir, i);
        File testingList = getTestingListFile(outputDir, i);
        FileUtils.writeLines(trainingList, "utf-8", corpusSplit.getTrainingSetPaths());
        FileUtils.writeLines(testingList, "utf-8", corpusSplit.getTestingSetPaths());
    }
}