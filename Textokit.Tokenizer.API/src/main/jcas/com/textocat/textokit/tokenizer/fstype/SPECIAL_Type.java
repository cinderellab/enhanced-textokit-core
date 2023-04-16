/* First created by JCasGen Thu Nov 12 00:10:20 MSK 2015 */
package com.textocat.textokit.tokenizer.fstype;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/**
 * Updated by JCasGen Thu Nov 12 00:10:20 MSK 2015
 *
 * @generated
 */
public class SPECIAL_Type extends Token_Type {
    /**
     * @generated
     */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = SPECIAL.typeIndexID;
    /**
     * @generated
     * @modifiable
     */
    @SuppressWarnings("hiding")
    public final static boolean featOkTst = JCasRegistry.getFeatOkTst("com.textocat.textokit.tokenizer.fstype.SPECIAL");
    /**
     * @generated
     */
    private final FSGenerator fsGenerator =
            new FSGenerator() {
                public FeatureStructure createFS(int addr, CASImpl cas) {
                    if (SPECIAL_Type.this.useExistingInstance) {
                        // Return eq fs instance if already created
                        FeatureStructure fs = SPECIAL_Type.this.jcas.getJfsFromCaddr(addr);
                        if (null == fs) {
                            fs = new 