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

package mod.agus.jcoderz.dx.io.instructions;

import mod.agus.jcoderz.dx.io.IndexType;

/** A decoded Dalvik instruction which has no register arguments. */
public final class ZeroRegisterDecodedInstruction
    extends mod.agus.jcoderz.dx.io.instructions.DecodedInstruction {
  /** Constructs an instance. */
  public ZeroRegisterDecodedInstruction(
      InstructionCodec format,
      int opcode,
      int index,
      IndexType indexType,
      int target,
      long literal) {
    super(format, opcode, index, indexType, target, literal);
  }

  /** {@inheritDoc} */
  @Override
  public int getRegisterCount() {
    return 0;
  }

  /** {@inheritDoc} */
  @Override
  public DecodedInstruction withIndex(int newIndex) {
    return new ZeroRegisterDecodedInstruction(
        getFormat(), getOpcode(), newIndex, getIndexType(), getTarget(), getLiteral());
  }
}
