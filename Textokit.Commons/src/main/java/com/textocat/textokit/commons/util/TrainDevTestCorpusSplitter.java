
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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Rinat Gareev
 */
public class TrainDevTestCorpusSplitter {

    @Parameter(names = "-p", required = true)
    private int partitionsNum;
    @Parameter(names = {"-s", "--corpus-file-suffix"})
    private String corpusFileSuffix;
    @Parameter(names = {"-r", "-R"})
    private boolean includeSubDirectores = true;
    @Parameter(names = {"-c", "--corpus-dir"}, required = true)
    private File corpusDir;
    // output to current dir
    private File outputDir = new File(".");
    private TrainDevTestCorpusSplitter() {
    }

    public static void main(String[] args) throws Exception {
        TrainDevTestCorpusSplitter launcher = new TrainDevTestCorpusSplitter();
        new JCommander(launcher, args);
        launcher.run();
    }

    private static <T> T getAndRemove(List<T> list, int index) {
        T result = list.get(index);
        list.remove(index);
        return result;
    }

    private void run() throws Exception {
        IOFileFilter corpusFileFilter;
        if (corpusFileSuffix == null) {
            corpusFileFilter = FileFilterUtils.trueFileFilter();
        } else {
            corpusFileFilter = FileFilterUtils.suffixFileFilter(corpusFileSuffix);
        }
        IOFileFilter corpusSubDirFilter = includeSubDirectores ? TrueFileFilter.INSTANCE : null;
        List<Set<File>> partitions = Lists.newArrayList(CorpusUtils.partitionCorpusByFileSize(
                corpusDir, corpusFileFilter, corpusSubDirFilter, partitionsNum));
        if (partitions.size() != partitionsNum) {
            throw new IllegalStateException();
        }
        // make dev partition from the last because it is a little bit smaller
        Set<File> devFiles = getAndRemove(partitions, partitions.size() - 1);
        Set<File> testFiles = getAndRemove(partitions, partitions.size() - 1);
        Set<File> trainFiles = Sets.newLinkedHashSet();
        for (Set<File> s : partitions) {
            trainFiles.addAll(s);
        }
        // write files
        File devPartFile = new File(outputDir, CorpusUtils.getDevPartitionFilename(0));
        FileUtils.writeLines(devPartFile, "utf-8", CorpusUtils.toRelativePaths(corpusDir, devFiles));
        File testPartFile = new File(outputDir, CorpusUtils.getTestPartitionFilename(0));
        FileUtils.writeLines(testPartFile, "utf-8", CorpusUtils.toRelativePaths(corpusDir, testFiles));
        File trainPartFile = new File(outputDir, CorpusUtils.getTrainPartitionFilename(0));
        FileUtils.writeLines(trainPartFile, "utf-8", CorpusUtils.toRelativePaths(corpusDir, trainFiles));
    }
}