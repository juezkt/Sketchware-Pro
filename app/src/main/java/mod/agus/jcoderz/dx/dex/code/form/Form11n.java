/*
 * Copyright (C) 2007 The Android Open Source Project
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

package mod.agus.jcoderz.dx.dex.code.form;

import java.util.BitSet;
import mod.agus.jcoderz.dx.dex.code.CstInsn;
import mod.agus.jcoderz.dx.dex.code.DalvInsn;
import mod.agus.jcoderz.dx.dex.code.InsnFormat;
import mod.agus.jcoderz.dx.rop.code.RegisterSpecList;
import mod.agus.jcoderz.dx.rop.cst.Constant;
import mod.agus.jcoderz.dx.rop.cst.CstLiteralBits;
import mod.agus.jcoderz.dx.util.AnnotatedOutput;

/** Instruction format {@code 11n}. See the instruction format spec for details. */
public final class Form11n extends InsnFormat {
  /** {@code non-null;} unique instance of this class */
  public static final InsnFormat THE_ONE = new Form11n();

  /** Constructs an instance. This class is not publicly instantiable. Use {@link #THE_ONE}. */
  private Form11n() {
    // This space intentionally left blank.
  }

  /** {@inheritDoc} */
  @Override
  public String insnArgString(DalvInsn insn) {
    mod.agus.jcoderz.dx.rop.code.RegisterSpecList regs = insn.getRegisters();
    mod.agus.jcoderz.dx.rop.cst.CstLiteralBits value =
        (mod.agus.jcoderz.dx.rop.cst.CstLiteralBits) ((CstInsn) insn).getConstant();

    return regs.get(0).regString() + ", " + literalBitsString(value);
  }

  /** {@inheritDoc} */
  @Override
  public String insnCommentString(DalvInsn insn, boolean noteIndices) {
    mod.agus.jcoderz.dx.rop.cst.CstLiteralBits value =
        (mod.agus.jcoderz.dx.rop.cst.CstLiteralBits) ((CstInsn) insn).getConstant();
    return literalBitsComment(value, 4);
  }

  /** {@inheritDoc} */
  @Override
  public int codeSize() {
    return 1;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCompatible(DalvInsn insn) {
    mod.agus.jcoderz.dx.rop.code.RegisterSpecList regs = insn.getRegisters();

    if (!((insn instanceof CstInsn)
        && (regs.size() == 1)
        && unsignedFitsInNibble(regs.get(0).getReg()))) {
      return false;
    }

    CstInsn ci = (CstInsn) insn;
    Constant cst = ci.getConstant();

    if (!(cst instanceof mod.agus.jcoderz.dx.rop.cst.CstLiteralBits)) {
      return false;
    }

    mod.agus.jcoderz.dx.rop.cst.CstLiteralBits cb =
        (mod.agus.jcoderz.dx.rop.cst.CstLiteralBits) cst;

    return cb.fitsInInt() && signedFitsInNibble(cb.getIntBits());
  }

  /** {@inheritDoc} */
  @Override
  public BitSet compatibleRegs(DalvInsn insn) {
    mod.agus.jcoderz.dx.rop.code.RegisterSpecList regs = insn.getRegisters();
    BitSet bits = new BitSet(1);

    bits.set(0, unsignedFitsInNibble(regs.get(0).getReg()));
    return bits;
  }

  /** {@inheritDoc} */
  @Override
  public void writeTo(AnnotatedOutput out, DalvInsn insn) {
    RegisterSpecList regs = insn.getRegisters();
    int value = ((CstLiteralBits) ((CstInsn) insn).getConstant()).getIntBits();

    write(out, opcodeUnit(insn, makeByte(regs.get(0).getReg(), value & 0xf)));
  }
}
