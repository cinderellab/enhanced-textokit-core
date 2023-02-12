/* First created by JCasGen Thu Nov 12 00:10:20 MSK 2015 */
package com.textocat.textokit.tokenizer.fstype;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;


/**
 * Updated by JCasGen Thu Nov 12 00:10:20 MSK 2015
 * XML source: src/main/resources/com/textocat/textokit/tokenizer/tokenizer-TypeSystem.xml
 *
 * @generated
 */
public class BREAK extends WhiteSpace {
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = JCasRegistry.register(BREAK.class);
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int type = typeIndexID;

    /**
     * Never called.  Disable default constructor
     *
     * @generated
     */
    protected BREAK() {/* intentionally empty block */}

    /**
     * Internal - constructor used by generator
     *
     * @param addr low level Feature Structure reference
     * @param type the type of this Feature Structure
     * @generated
     */
    public BREAK(int addr, TOP_Type type) {
        super(addr, type);
        readObject();
    }

    /**
     * @param jcas JCas to which this Feature Structure belongs
     * @generated
     */
    public BREAK(JCas jcas) {
        super(jcas);
        readObject();
    }

    /**
     * @param jcas  JCas to which this Feature Structure belongs
