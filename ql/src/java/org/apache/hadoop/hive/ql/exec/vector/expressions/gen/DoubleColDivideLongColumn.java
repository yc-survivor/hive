/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.apache.hadoop.hive.ql.exec.vector.expressions.gen;

import org.apache.hadoop.hive.ql.exec.vector.expressions.VectorExpression;
import org.apache.hadoop.hive.ql.exec.vector.expressions.NullUtil;
import org.apache.hadoop.hive.ql.exec.vector.*;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;

public class DoubleColDivideLongColumn extends VectorExpression {
  int colNum1;
  int colNum2;
  int outputColumn;

  public DoubleColDivideLongColumn(int colNum1, int colNum2, int outputColumn) {
    this.colNum1 = colNum1;
    this.colNum2 = colNum2;
    this.outputColumn = outputColumn;
  }

  @Override
  public void evaluate(VectorizedRowBatch batch) {

    if (childExpressions != null) {
      super.evaluateChildren(batch);
    }

    DoubleColumnVector inputColVector1 = (DoubleColumnVector) batch.cols[colNum1];
    LongColumnVector inputColVector2 = (LongColumnVector) batch.cols[colNum2];
    DoubleColumnVector outputColVector = (DoubleColumnVector) batch.cols[outputColumn];
    int[] sel = batch.selected;
    int n = batch.size;
    double[] vector1 = inputColVector1.vector;
    long[] vector2 = inputColVector2.vector;
    double[] outputVector = outputColVector.vector;
    
    // return immediately if batch is empty
    if (n == 0) {
      return;
    }
    
    outputColVector.isRepeating = inputColVector1.isRepeating && inputColVector2.isRepeating;
    
    // Handle nulls first  
    NullUtil.propagateNullsColCol(
      inputColVector1, inputColVector2, outputColVector, sel, n, batch.selectedInUse);
          
    /* Disregard nulls for processing. In other words,
     * the arithmetic operation is performed even if one or 
     * more inputs are null. This is to improve speed by avoiding
     * conditional checks in the inner loop.
     */ 
    if (inputColVector1.isRepeating && inputColVector2.isRepeating) { 
      outputVector[0] = vector1[0] / vector2[0];
    } else if (inputColVector1.isRepeating) {
      if (batch.selectedInUse) {
        for(int j = 0; j != n; j++) {
          int i = sel[j];
          outputVector[i] = vector1[0] / vector2[i];
        }
      } else {
        for(int i = 0; i != n; i++) {
          outputVector[i] = vector1[0] / vector2[i];
        }
      }
    } else if (inputColVector2.isRepeating) {
      if (batch.selectedInUse) {
        for(int j = 0; j != n; j++) {
          int i = sel[j];
          outputVector[i] = vector1[i] / vector2[0];
        }
      } else {
        for(int i = 0; i != n; i++) {
          outputVector[i] = vector1[i] / vector2[0];
        }
      }
    } else {
      if (batch.selectedInUse) {
        for(int j = 0; j != n; j++) {
          int i = sel[j];
          outputVector[i] = vector1[i] / vector2[i];
        }
      } else {
        for(int i = 0; i != n; i++) {
          outputVector[i] = vector1[i] /  vector2[i];
        }
      }
    }
    
    /* For the case when the output can have null values, follow 
     * the convention that the data values must be 1 for long and 
     * NaN for double. This is to prevent possible later zero-divide errors
     * in complex arithmetic expressions like col2 / (col1 - 1)
     * in the case when some col1 entries are null.
     */
    NullUtil.setNullDataEntriesDouble(outputColVector, batch.selectedInUse, sel, n);
  }

  @Override
  public int getOutputColumn() {
    return outputColumn;
  }

  @Override
  public String getOutputType() {
    return "double";
  }
}
