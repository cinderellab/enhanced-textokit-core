

/* First created by JCasGen Tue Nov 17 01:38:30 MSK 2015 */
package com.textocat.textokit.phrrecog.cas;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** Represents Verb Phrase
 * Updated by JCasGen Tue Nov 17 01:38:30 MSK 2015
 * XML source: src/main/resources/com/textocat/textokit/phrrecog/ts-phrase-recognizer.xml
 * @generated */
public class VerbPhrase extends Phrase {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(VerbPhrase.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected VerbPhrase() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public VerbPhrase(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public VerbPhrase(JCas j