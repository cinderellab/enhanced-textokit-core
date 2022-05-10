


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

/* First created by JCasGen Fri Nov 13 19:45:22 MSK 2015 */
package com.textocat.textokit.morph.fs;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;
import com.textocat.textokit.tokenizer.fstype.Token;
import org.apache.uima.jcas.cas.StringArray;


/** 
 * Updated by JCasGen Fri Nov 13 19:45:22 MSK 2015
 * XML source: src/main/resources/com/textocat/textokit/morph/morphology-ts.xml
 * @generated */
public class SimplyWord extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SimplyWord.class);
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
  protected SimplyWord() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SimplyWord(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SimplyWord(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SimplyWord(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: posTag

  /** getter for posTag - gets 
   * @generated
   * @return value of the feature 
   */
  public String getPosTag() {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_posTag == null)
      jcasType.jcas.throwFeatMissing("posTag", "com.textocat.textokit.morph.fs.SimplyWord");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_posTag);}
    
  /** setter for posTag - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPosTag(String v) {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_posTag == null)
      jcasType.jcas.throwFeatMissing("posTag", "com.textocat.textokit.morph.fs.SimplyWord");
    jcasType.ll_cas.ll_setStringValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_posTag, v);}    
   
    
  //*--------------*
  //* Feature: grammems

  /** getter for grammems - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getGrammems() {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_grammems == null)
      jcasType.jcas.throwFeatMissing("grammems", "com.textocat.textokit.morph.fs.SimplyWord");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_grammems)));}
    
  /** setter for grammems - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setGrammems(StringArray v) {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_grammems == null)
      jcasType.jcas.throwFeatMissing("grammems", "com.textocat.textokit.morph.fs.SimplyWord");
    jcasType.ll_cas.ll_setRefValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_grammems, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for grammems - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getGrammems(int i) {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_grammems == null)
      jcasType.jcas.throwFeatMissing("grammems", "com.textocat.textokit.morph.fs.SimplyWord");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_grammems), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_grammems), i);}

  /** indexed setter for grammems - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setGrammems(int i, String v) { 
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_grammems == null)
      jcasType.jcas.throwFeatMissing("grammems", "com.textocat.textokit.morph.fs.SimplyWord");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_grammems), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_grammems), i, v);}
   
    
  //*--------------*
  //* Feature: lemma

  /** getter for lemma - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLemma() {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_lemma == null)
      jcasType.jcas.throwFeatMissing("lemma", "com.textocat.textokit.morph.fs.SimplyWord");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_lemma);}
    
  /** setter for lemma - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLemma(String v) {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_lemma == null)
      jcasType.jcas.throwFeatMissing("lemma", "com.textocat.textokit.morph.fs.SimplyWord");
    jcasType.ll_cas.ll_setStringValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_lemma, v);}    
   
    
  //*--------------*
  //* Feature: lemmaId

  /** getter for lemmaId - gets 
   * @generated
   * @return value of the feature 
   */
  public int getLemmaId() {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_lemmaId == null)
      jcasType.jcas.throwFeatMissing("lemmaId", "com.textocat.textokit.morph.fs.SimplyWord");
    return jcasType.ll_cas.ll_getIntValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_lemmaId);}
    
  /** setter for lemmaId - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLemmaId(int v) {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_lemmaId == null)
      jcasType.jcas.throwFeatMissing("lemmaId", "com.textocat.textokit.morph.fs.SimplyWord");
    jcasType.ll_cas.ll_setIntValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_lemmaId, v);}    
   
    
  //*--------------*
  //* Feature: token

  /** getter for token - gets 
   * @generated
   * @return value of the feature 
   */
  public Token getToken() {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_token == null)
      jcasType.jcas.throwFeatMissing("token", "com.textocat.textokit.morph.fs.SimplyWord");
    return (Token)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_token)));}
    
  /** setter for token - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setToken(Token v) {
    if (SimplyWord_Type.featOkTst && ((SimplyWord_Type)jcasType).casFeat_token == null)
      jcasType.jcas.throwFeatMissing("token", "com.textocat.textokit.morph.fs.SimplyWord");
    jcasType.ll_cas.ll_setRefValue(addr, ((SimplyWord_Type)jcasType).casFeatCode_token, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    