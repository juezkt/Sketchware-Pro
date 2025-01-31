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

package mod.agus.jcoderz.dx.ssa;

import java.util.ArrayList;
import java.util.List;
import mod.agus.jcoderz.dx.rop.code.Insn;
import mod.agus.jcoderz.dx.rop.code.LocalItem;
import mod.agus.jcoderz.dx.rop.code.RegisterSpec;
import mod.agus.jcoderz.dx.rop.code.RegisterSpecList;
import mod.agus.jcoderz.dx.rop.code.Rop;
import mod.agus.jcoderz.dx.rop.code.SourcePosition;
import mod.agus.jcoderz.dx.rop.type.Type;
import mod.agus.jcoderz.dx.rop.type.TypeBearer;
import mod.agus.jcoderz.dx.util.Hex;

/**
 * A Phi instruction (magical post-control-flow-merge) instruction in SSA form. Will be converted to
 * moves in predecessor blocks before conversion back to ROP form.
 */
public final class PhiInsn extends mod.agus.jcoderz.dx.ssa.SsaInsn {
  /**
   * result register. The original result register of the phi insn is needed during the renaming
   * process after the new result register has already been chosen.
   */
  private final int ropResultReg;

  /** {@code non-null;} operands of the instruction; built up by {@link #addPhiOperand} */
  private final ArrayList<Operand> operands = new ArrayList<Operand>();

  /** {@code null-ok;} source registers; constructed lazily */
  private mod.agus.jcoderz.dx.rop.code.RegisterSpecList sources;

  /**
   * Constructs a new phi insn with no operands.
   *
   * @param resultReg the result reg for this phi insn
   * @param block block containing this insn.
   */
  public PhiInsn(
      mod.agus.jcoderz.dx.rop.code.RegisterSpec resultReg,
      mod.agus.jcoderz.dx.ssa.SsaBasicBlock block) {
    super(resultReg, block);
    ropResultReg = resultReg.getReg();
  }

  /**
   * Makes a phi insn with a void result type.
   *
   * @param resultReg the result register for this phi insn.
   * @param block block containing this insn.
   */
  public PhiInsn(final int resultReg, final mod.agus.jcoderz.dx.ssa.SsaBasicBlock block) {
    /*
     * The result type here is bogus: The type depends on the
     * operand and will be derived later.
     */
    super(mod.agus.jcoderz.dx.rop.code.RegisterSpec.make(resultReg, Type.VOID), block);
    ropResultReg = resultReg;
  }

  /** {@inheritDoc} */
  @Override
  public PhiInsn clone() {
    throw new UnsupportedOperationException("can't clone phi");
  }

  /**
   * Updates the TypeBearers of all the sources (phi operands) to be the current TypeBearer of the
   * register-defining instruction's result. This is used during phi-type resolution.
   *
   * <p>Note that local association of operands are preserved in this step.
   *
   * @param ssaMeth method that contains this insn
   */
  public void updateSourcesToDefinitions(mod.agus.jcoderz.dx.ssa.SsaMethod ssaMeth) {
    for (Operand o : operands) {
      mod.agus.jcoderz.dx.rop.code.RegisterSpec def =
          ssaMeth.getDefinitionForRegister(o.regSpec.getReg()).getResult();

      o.regSpec = o.regSpec.withType(def.getType());
    }

    sources = null;
  }

  /**
   * Changes the result type. Used during phi type resolution
   *
   * @param type {@code non-null;} new TypeBearer
   * @param local {@code null-ok;} new local info, if available
   */
  public void changeResultType(TypeBearer type, LocalItem local) {
    setResult(
        mod.agus.jcoderz.dx.rop.code.RegisterSpec.makeLocalOptional(
            getResult().getReg(), type, local));
  }

  /**
   * Gets the original rop-form result reg. This is useful during renaming.
   *
   * @return the original rop-form result reg
   */
  public int getRopResultReg() {
    return ropResultReg;
  }

  /**
   * Adds an operand to this phi instruction.
   *
   * @param registerSpec register spec, including type and reg of operand
   * @param predBlock predecessor block to be associated with this operand
   */
  public void addPhiOperand(
      mod.agus.jcoderz.dx.rop.code.RegisterSpec registerSpec,
      mod.agus.jcoderz.dx.ssa.SsaBasicBlock predBlock) {
    operands.add(new Operand(registerSpec, predBlock.getIndex(), predBlock.getRopLabel()));

    // Un-cache sources, in case someone has already called getSources().
    sources = null;
  }

  /**
   * Removes all operand uses of a register from this phi instruction.
   *
   * @param registerSpec register spec, including type and reg of operand
   */
  public void removePhiRegister(mod.agus.jcoderz.dx.rop.code.RegisterSpec registerSpec) {
    ArrayList<Operand> operandsToRemove = new ArrayList<Operand>();
    for (Operand o : operands) {
      if (o.regSpec.getReg() == registerSpec.getReg()) {
        operandsToRemove.add(o);
      }
    }

    operands.removeAll(operandsToRemove);

    // Un-cache sources, in case someone has already called getSources().
    sources = null;
  }

  /**
   * Gets the index of the pred block associated with the RegisterSpec at the particular
   * getSources() index.
   *
   * @param sourcesIndex index of source in getSources()
   * @return block index
   */
  public int predBlockIndexForSourcesIndex(int sourcesIndex) {
    return operands.get(sourcesIndex).blockIndex;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Always returns null for {@code PhiInsn}s.
   */
  @Override
  public Rop getOpcode() {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Always returns null for {@code PhiInsn}s.
   */
  @Override
  public mod.agus.jcoderz.dx.rop.code.Insn getOriginalRopInsn() {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Always returns false for {@code PhiInsn}s.
   */
  @Override
  public boolean canThrow() {
    return false;
  }

  /**
   * Gets sources. Constructed lazily from phi operand data structures and then cached.
   *
   * @return {@code non-null;} sources list
   */
  @Override
  public mod.agus.jcoderz.dx.rop.code.RegisterSpecList getSources() {
    if (sources != null) {
      return sources;
    }

    if (operands.isEmpty()) {
      // How'd this happen? A phi insn with no operand?
      return mod.agus.jcoderz.dx.rop.code.RegisterSpecList.EMPTY;
    }

    int szSources = operands.size();
    sources = new RegisterSpecList(szSources);

    for (int i = 0; i < szSources; i++) {
      Operand o = operands.get(i);

      sources.set(i, o.regSpec);
    }

    sources.setImmutable();
    return sources;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isRegASource(int reg) {
    /*
     * Avoid creating a sources list in case it has not already been
     * created.
     */

    for (Operand o : operands) {
      if (o.regSpec.getReg() == reg) {
        return true;
      }
    }

    return false;
  }

  /**
   * @return true if all operands use the same register
   */
  public boolean areAllOperandsEqual() {
    if (operands.isEmpty()) {
      // This should never happen.
      return true;
    }

    int firstReg = operands.get(0).regSpec.getReg();
    for (Operand o : operands) {
      if (firstReg != o.regSpec.getReg()) {
        return false;
      }
    }

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public final void mapSourceRegisters(RegisterMapper mapper) {
    for (Operand o : operands) {
      mod.agus.jcoderz.dx.rop.code.RegisterSpec old = o.regSpec;
      o.regSpec = mapper.map(old);
      if (old != o.regSpec) {
        getBlock().getParent().onSourceChanged(this, old, o.regSpec);
      }
    }
    sources = null;
  }

  /**
   * Always throws an exeption, since a phi insn may not be converted back to rop form.
   *
   * @return always throws exception
   */
  @Override
  public Insn toRopInsn() {
    throw new IllegalArgumentException("Cannot convert phi insns to rop form");
  }

  /**
   * Returns the list of predecessor blocks associated with all operands that have {@code reg} as an
   * operand register.
   *
   * @param reg register to look up
   * @param ssaMeth method we're operating on
   * @return list of predecessor blocks, empty if none
   */
  public List<mod.agus.jcoderz.dx.ssa.SsaBasicBlock> predBlocksForReg(int reg, SsaMethod ssaMeth) {
    ArrayList<mod.agus.jcoderz.dx.ssa.SsaBasicBlock> ret = new ArrayList<SsaBasicBlock>();

    for (Operand o : operands) {
      if (o.regSpec.getReg() == reg) {
        ret.add(ssaMeth.getBlocks().get(o.blockIndex));
      }
    }

    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isPhiOrMove() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasSideEffect() {
    return Optimizer.getPreserveLocals() && getLocalAssignment() != null;
  }

  /** {@inheritDoc} */
  @Override
  public void accept(SsaInsn.Visitor v) {
    v.visitPhiInsn(this);
  }

  /** {@inheritDoc} */
  @Override
  public String toHuman() {
    return toHumanWithInline(null);
  }

  /**
   * Returns human-readable string for listing dumps. This method allows sub-classes to specify
   * extra text.
   *
   * @param extra {@code null-ok;} the argument to print after the opcode
   * @return human-readable string for listing dumps
   */
  protected final String toHumanWithInline(String extra) {
    StringBuilder sb = new StringBuilder(80);

    sb.append(SourcePosition.NO_INFO);
    sb.append(": phi");

    if (extra != null) {
      sb.append("(");
      sb.append(extra);
      sb.append(")");
    }

    mod.agus.jcoderz.dx.rop.code.RegisterSpec result = getResult();

    if (result == null) {
      sb.append(" .");
    } else {
      sb.append(" ");
      sb.append(result.toHuman());
    }

    sb.append(" <-");

    int sz = getSources().size();
    if (sz == 0) {
      sb.append(" .");
    } else {
      for (int i = 0; i < sz; i++) {
        sb.append(" ");
        sb.append(sources.get(i).toHuman() + "[b=" + Hex.u2(operands.get(i).ropLabel) + "]");
      }
    }

    return sb.toString();
  }

  /** A single phi operand, consiting of source register and block index for move. */
  private static class Operand {
    public mod.agus.jcoderz.dx.rop.code.RegisterSpec regSpec;
    public final int blockIndex;
    public final int ropLabel; // only used for debugging

    public Operand(RegisterSpec regSpec, int blockIndex, int ropLabel) {
      this.regSpec = regSpec;
      this.blockIndex = blockIndex;
      this.ropLabel = ropLabel;
    }
  }

  /** Visitor interface for instances of this (outer) class. */
  public static interface Visitor {
    public void visitPhiInsn(PhiInsn insn);
  }
}
