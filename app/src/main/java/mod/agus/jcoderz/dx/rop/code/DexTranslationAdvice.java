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

package mod.agus.jcoderz.dx.rop.code;

import mod.agus.jcoderz.dx.rop.type.Type;
import mod.agus.jcoderz.dx.rop.cst.CstInteger;

/**
 * Implementation of {@link TranslationAdvice} which represents what
 * the dex format will be able to represent.
 */
public final class DexTranslationAdvice
        implements TranslationAdvice {
    /** {@code non-null;} standard instance of this class */
    public static final DexTranslationAdvice THE_ONE =
        new DexTranslationAdvice();

    /** debug advice for disabling invoke-range optimization */
    public static final DexTranslationAdvice NO_SOURCES_IN_ORDER =
        new DexTranslationAdvice(true);

    /**
     * The minimum source width, in register units, for an invoke
     * instruction that requires its sources to be in order and contiguous.
     */
    private static final int MIN_INVOKE_IN_ORDER = 6;

    /** when true: always returns false for requiresSourcesInOrder */
    private final boolean disableSourcesInOrder;

    /**
     * This class is not publicly instantiable. Use {@link #THE_ONE}.
     */
    private DexTranslationAdvice() {
        disableSourcesInOrder = false;
    }

    private DexTranslationAdvice(boolean disableInvokeRange) {
        this.disableSourcesInOrder = disableInvokeRange;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasConstantOperation(mod.agus.jcoderz.dx.rop.code.Rop opcode,
                                        RegisterSpec sourceA, RegisterSpec sourceB) {
        if (sourceA.getType() != Type.INT) {
            return false;
        }

        // Return false if second source isn't a constant
        if (! (sourceB.getTypeBearer() instanceof mod.agus.jcoderz.dx.rop.cst.CstInteger)) {
            // Except for rsub-int (reverse sub) where first source is constant
            if (sourceA.getTypeBearer() instanceof mod.agus.jcoderz.dx.rop.cst.CstInteger &&
                    opcode.getOpcode() == RegOps.SUB) {
                mod.agus.jcoderz.dx.rop.cst.CstInteger cst = (mod.agus.jcoderz.dx.rop.cst.CstInteger) sourceA.getTypeBearer();
                return cst.fitsIn16Bits();
            } else {
                return false;
            }
        }

        mod.agus.jcoderz.dx.rop.cst.CstInteger cst = (mod.agus.jcoderz.dx.rop.cst.CstInteger) sourceB.getTypeBearer();

        return switch (opcode.getOpcode()) {
            // These have 8 and 16 bit cst representations
            case RegOps.REM, RegOps.ADD, RegOps.MUL, RegOps.DIV, RegOps.AND, RegOps.OR,
                 RegOps.XOR -> cst.fitsIn16Bits();
            // These only have 8 bit cst reps
            case RegOps.SHL, RegOps.SHR, RegOps.USHR -> cst.fitsIn8Bits();
            // No sub-const insn, so check if equivalent add-const fits
            case RegOps.SUB -> {
                CstInteger cst2 = CstInteger.make(-cst.getValue());
                yield cst2.fitsIn16Bits();
            }
            default -> false;
        };
    }

    /** {@inheritDoc} */
    @Override
    public boolean requiresSourcesInOrder(Rop opcode,
                                          mod.agus.jcoderz.dx.rop.code.RegisterSpecList sources) {

        return !disableSourcesInOrder && opcode.isCallLike()
                && totalRopWidth(sources) >= MIN_INVOKE_IN_ORDER;
    }

    /**
     * Calculates the total rop width of the list of SSA registers
     *
     * @param sources {@code non-null;} list of SSA registers
     * @return {@code >= 0;} rop-form width in register units
     */
    private int totalRopWidth(RegisterSpecList sources) {
        int sz = sources.size();
        int total = 0;

        for (int i = 0; i < sz; i++) {
            total += sources.get(i).getCategory();
        }

        return total;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOptimalRegisterCount() {
        return 16;
    }
}
