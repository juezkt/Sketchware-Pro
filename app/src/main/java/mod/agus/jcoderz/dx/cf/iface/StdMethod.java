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

package mod.agus.jcoderz.dx.cf.iface;

import mod.agus.jcoderz.dx.rop.code.AccessFlags;
import mod.agus.jcoderz.dx.rop.cst.CstNat;
import mod.agus.jcoderz.dx.rop.cst.CstType;
import mod.agus.jcoderz.dx.rop.type.Prototype;

/**
 * Standard implementation of {@link mod.agus.jcoderz.dx.cf.iface.Method}, which directly stores all
 * the associated data.
 */
public final class StdMethod extends StdMember implements Method {
  /** {@code non-null;} the effective method descriptor */
  private final mod.agus.jcoderz.dx.rop.type.Prototype effectiveDescriptor;

  /**
   * Constructs an instance.
   *
   * @param definingClass {@code non-null;} the defining class
   * @param accessFlags access flags
   * @param nat {@code non-null;} member name and type (descriptor)
   * @param attributes {@code non-null;} list of associated attributes
   */
  public StdMethod(CstType definingClass, int accessFlags, CstNat nat, AttributeList attributes) {
    super(definingClass, accessFlags, nat, attributes);

    String descStr = getDescriptor().getString();
    effectiveDescriptor =
        mod.agus.jcoderz.dx.rop.type.Prototype.intern(
            descStr,
            definingClass.getClassType(),
            AccessFlags.isStatic(accessFlags),
            nat.isInstanceInit());
  }

  /** {@inheritDoc} */
  @Override
  public Prototype getEffectiveDescriptor() {
    return effectiveDescriptor;
  }
}
