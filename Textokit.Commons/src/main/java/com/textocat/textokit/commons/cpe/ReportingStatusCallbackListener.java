
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


package com.textocat.textokit.commons.cpe;

import com.google.common.collect.Sets;
import com.textocat.textokit.commons.util.DocumentUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;

import java.util.List;
import java.util.Set;

/**
 * Source code has been borrowed from UIMA examples - SimpleRunCPE.java
 *
 * @author Rinat Gareev
 */
public class ReportingStatusCallbackListener implements StatusCallbackListener {

    private CollectionProcessingEngine cpe;
    /**
     * Start time of CPE initialization
     */
    private long mStartTime;
    /**
     * Start time of the processing
     */
    private long mInitCompleteTime;
    private int entityCount = 0;
    private int entityReportingInterval = 0;
    private long size = 0;
    private int docsProcessedWithException = 0;
    private Set<String> docsWithException = Sets.newHashSet();

    public ReportingStatusCallbackListener(CollectionProcessingEngine cpe) {
        this(cpe, 0);
    }
    public ReportingStatusCallbackListener(CollectionProcessingEngine cpe,
                                           int entityReportingInterval) {
        this.cpe = cpe;
        this.entityReportingInterval = entityReportingInterval;
    }

    /**
     * Called when the initialization is completed.
     *
     * @see org.apache.uima.collection.processing.StatusCallbackListener#initializationComplete()
     */
    public void initializationComplete() {
        System.out.println("CPM Initialization Complete");
        mInitCompleteTime = System.currentTimeMillis();
    }

    /**
     * Called when the batchProcessing is completed.
     *
     * @see org.apache.uima.collection.processing.StatusCallbackListener#batchProcessComplete()
     */
    public void batchProcessComplete() {
        System.out.print("Completed " + entityCount + " documents");
        if (size > 0) {
            System.out.print("; " + size + " characters");
        }
        System.out.println();
        long elapsedTime = System.currentTimeMillis() - mStartTime;
        System.out.println("Time Elapsed : " + elapsedTime + " ms ");
    }

    /**
     * Called when the collection processing is completed.
     *
     * @see org.apache.uima.collection.processing.StatusCallbackListener#collectionProcessComplete()
     */
    public void collectionProcessComplete() {
        long time = System.currentTimeMillis();
        System.out.print("Completed " + entityCount + " documents");
        if (size > 0) {
            System.out.print("; " + size + " characters");
        }
        System.out.println();
        long initTime = mInitCompleteTime - mStartTime;
        long processingTime = time - mInitCompleteTime;
        long elapsedTime = initTime + processingTime;
        System.out.println("Total Time Elapsed: " + elapsedTime + " ms ");
        System.out.println("Initialization Time: " + initTime + " ms");
        System.out.println("Processing Time: " + processingTime + " ms");

        System.out.println("\n\n ------------------ PERFORMANCE REPORT ------------------\n");
        System.out.println(cpe.getPerformanceReport().toString());
        if (docsProcessedWithException > 0) {
            System.out.println(String.format(
                    "There are %s entities that caused exceptions:\n%s\n"
                            + "Check previous output for details",
                    docsProcessedWithException, docsWithException));
        }
    }

    /**
     * Called when the CPM is paused.
     *
     * @see org.apache.uima.collection.processing.StatusCallbackListener#paused()
     */
    public void paused() {
        System.out.println("Paused");
    }

    /**
     * Called when the CPM is resumed after a pause.
     *
     * @see org.apache.uima.collection.processing.StatusCallbackListener#resumed()
     */
    public void resumed() {
        System.out.println("Resumed");
    }

    /**
     * Called when the CPM is stopped abruptly due to errors.
     *
     * @see org.apache.uima.collection.processing.StatusCallbackListener#aborted()
     */
    public void aborted() {
        System.out.println("Aborted");
    }

    /**
     * Called when the processing of a Document is completed. <br>
     * The process status can be looked at and corresponding actions taken.
     *
     * @param aCas    CAS corresponding to the completed processing
     * @param aStatus EntityProcessStatus that holds the status of all the events
     *                for aEntity
     */
    public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
        if (aStatus.isException()) {
            String docURI = getDocumentURI(aCas);
            docsWithException.add(docURI);
            docsProcessedWithException++;
            System.err.println(String.format("During the processing of %s", docURI));
            List<Exception> exceptions = aStatus.getExceptions();
            for (int i = 0; i < exceptions.size(); i++) {
                ((Throwable) exceptions.get(i)).printStackTrace();
            }
            return;
        }
        entityCount++;
        String docText = aCas.getDocumentText();
        if (docText != null) {
            size += docText.length();
        }
        if (entityReportingInterval != 0 && entityCount % entityReportingInterval == 0) {
            System.out.println(String.format("%s entities have been processed", entityCount));
        }
    }

    // TO OVERRIDE
    protected String getDocumentURI(CAS cas) {
        return DocumentUtils.getDocumentUri(cas);
    }
}