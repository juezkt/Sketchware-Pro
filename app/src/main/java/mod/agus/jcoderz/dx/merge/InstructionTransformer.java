/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mod.agus.jcoderz.dx.merge;

import mod.agus.jcoderz.dex.DexException;
import mod.agus.jcoderz.dex.DexIndexOverflowException;
import mod.agus.jcoderz.dx.io.CodeReader;
import mod.agus.jcoderz.dx.io.Opcodes;
import mod.agus.jcoderz.dx.io.instructions.DecodedInstruction;
import mod.agus.jcoderz.dx.io.instructions.ShortArrayCodeOutput;

final class InstructionTransformer {
  private final mod.agus.jcoderz.dx.io.CodeReader reader;

  private DecodedInstruction[] mappedInstructions;
  private int mappedAt;
  private mod.agus.jcoderz.dx.merge.IndexMap indexMap;

  public InstructionTransformer() {
    this.reader = new mod.agus.jcoderz.dx.io.CodeReader();
    this.reader.setAllVisitors(new GenericVisitor());
    this.reader.setStringVisitor(new StringVisitor());
    this.reader.setTypeVisitor(new TypeVisitor());
    this.reader.setFieldVisitor(new FieldVisitor());
    this.reader.setMethodVisitor(new MethodVisitor());
    this.reader.setMethodAndProtoVisitor(new MethodAndProtoVisitor());
    this.reader.setCallSiteVisitor(new CallSiteVisitor());
  }

  public short[] transform(IndexMap indexMap, short[] encodedInstructions) throws DexException {
    DecodedInstruction[] decodedInstructions = DecodedInstruction.decodeAll(encodedInstructions);
    int size = decodedInstructions.length;

    this.indexMap = indexMap;
    mappedInstructions = new DecodedInstruction[size];
    mappedAt = 0;
    reader.visitAll(decodedInstructions);

    ShortArrayCodeOutput out = new ShortArrayCodeOutput(size);
    for (DecodedInstruction instruction : mappedInstructions) {
      if (instruction != null) {
        instruction.encode(out);
      }
    }

    this.indexMap = null;
    return out.getArray();
  }

  private class GenericVisitor implements mod.agus.jcoderz.dx.io.CodeReader.Visitor {
    @Override
    public void visit(DecodedInstruction[] all, DecodedInstruction one) {
      mappedInstructions[mappedAt++] = one;
    }
  }

  private class StringVisitor implements mod.agus.jcoderz.dx.io.CodeReader.Visitor {
    @Override
    public void visit(DecodedInstruction[] all, DecodedInstruction one) {
      int stringId = one.getIndex();
      int mappedId = indexMap.adjustString(stringId);
      boolean isJumbo = (one.getOpcode() == mod.agus.jcoderz.dx.io.Opcodes.CONST_STRING_JUMBO);
      jumboCheck(isJumbo, mappedId);
      mappedInstructions[mappedAt++] = one.withIndex(mappedId);
    }
  }

  private class FieldVisitor implements mod.agus.jcoderz.dx.io.CodeReader.Visitor {
    @Override
    public void visit(DecodedInstruction[] all, DecodedInstruction one) {
      int fieldId = one.getIndex();
      int mappedId = indexMap.adjustField(fieldId);
      boolean isJumbo = (one.getOpcode() == mod.agus.jcoderz.dx.io.Opcodes.CONST_STRING_JUMBO);
      jumboCheck(isJumbo, mappedId);
      mappedInstructions[mappedAt++] = one.withIndex(mappedId);
    }
  }

  private class TypeVisitor implements mod.agus.jcoderz.dx.io.CodeReader.Visitor {
    @Override
    public void visit(DecodedInstruction[] all, DecodedInstruction one) {
      int typeId = one.getIndex();
      int mappedId = indexMap.adjustType(typeId);
      boolean isJumbo = (one.getOpcode() == mod.agus.jcoderz.dx.io.Opcodes.CONST_STRING_JUMBO);
      jumboCheck(isJumbo, mappedId);
      mappedInstructions[mappedAt++] = one.withIndex(mappedId);
    }
  }

  private class MethodVisitor implements mod.agus.jcoderz.dx.io.CodeReader.Visitor {
    @Override
    public void visit(DecodedInstruction[] all, DecodedInstruction one) {
      int methodId = one.getIndex();
      int mappedId = indexMap.adjustMethod(methodId);
      boolean isJumbo = (one.getOpcode() == Opcodes.CONST_STRING_JUMBO);
      jumboCheck(isJumbo, mappedId);
      mappedInstructions[mappedAt++] = one.withIndex(mappedId);
    }
  }

  private class MethodAndProtoVisitor implements mod.agus.jcoderz.dx.io.CodeReader.Visitor {
    @Override
    public void visit(DecodedInstruction[] all, DecodedInstruction one) {
      int methodId = one.getIndex();
      int protoId = one.getProtoIndex();
      mappedInstructions[mappedAt++] =
          one.withProtoIndex(indexMap.adjustMethod(methodId), indexMap.adjustProto(protoId));
    }
  }

  private class CallSiteVisitor implements CodeReader.Visitor {
    @Override
    public void visit(DecodedInstruction[] all, DecodedInstruction one) {
      int callSiteId = one.getIndex();
      int mappedCallSiteId = indexMap.adjustCallSite(callSiteId);
      mappedInstructions[mappedAt++] = one.withIndex(mappedCallSiteId);
    }
  }

  private static void jumboCheck(boolean isJumbo, int newIndex) {
    if (!isJumbo && (newIndex > 0xffff)) {
      throw new DexIndexOverflowException(
          "Cannot merge new index " + newIndex + " into a non-jumbo instruction!");
    }
  }
}
