
/* First created by JCasGen Thu Nov 12 17:05:14 MSK 2015 */
package com.textocat.textokit.segmentation.fstype;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation_Type;

/**
 * Annotate content between two line separators
 * Updated by JCasGen Thu Nov 12 17:05:14 MSK 2015
 *
 * @generated
 */
public class Paragraph_Type extends Annotation_Type {
    /**
     * @return the generator for this type
     * @generated
     */
    @Override
    protected FSGenerator getFSGenerator() {
        return fsGenerator;
    }

    /**
     * @generated
     */
    private final FSGenerator fsGenerator =
            new FSGenerator() {
                public FeatureStructure createFS(int addr, CASImpl cas) {
                    if (Paragraph_Type.this.useExistingInstance) {
                        // Return eq fs instance if already created
                        FeatureStructure fs = Paragraph_Type.this.jcas.getJfsFromCaddr(addr);
                        if (null == fs) {
                            fs = new Paragraph(addr, Paragraph_Type.this);
                            Paragraph_Type.this.jcas.putJfsFromCaddr(addr, fs);
                            return fs;
                        }
                        return fs;
                    } else return new Paragraph(addr, Paragraph_Type.this);
                }
            };
    /**
     * @generated
     */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = Paragraph.typeIndexID;
    /**
     * @generated
     * @modifiable
     */
    @SuppressWarnings("hiding")
    public final static boolean featOkTst = JCasRegistry.getFeatOkTst("com.textocat.textokit.segmentation.fstype.Paragraph");


    /**
     * initialize variables to correspond with Cas Type and Features
     *
     * @param jcas    JCas
     * @param casType Type
     * @generated
     */
    public Paragraph_Type(JCas jcas, Type casType) {
        super(jcas, casType);
        casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    }
}



    