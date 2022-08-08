/* First created by JCasGen Thu Nov 12 17:05:14 MSK 2015 */
package com.textocat.textokit.segmentation.fstype;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;


/**
 * Annotate content between two line separators
 * Updated by JCasGen Thu Nov 12 17:05:14 MSK 2015
 * XML source: src/main/resources/com/textocat/textokit/segmentation/segmentation-TypeSystem.xml
 *
 * @generated
 */
public class Paragraph extends Annotation {
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = JCasRegistry.register(Paragraph.class);
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int type = typeIndexID;

    /**
     * @return index of the type
     * @generated
     */
    @Override
    public int getTypeIndexID() {
        return typeIndexID;
    }

    /**
     * Never called.  Disable default constructor
     *
