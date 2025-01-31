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

import mod.agus.jcoderz.dx.util.FixedSizeList;

/**
 * Standard implementation of {@link mod.agus.jcoderz.dx.cf.iface.MethodList}, which directly stores
 * an array of {@link mod.agus.jcoderz.dx.cf.iface.Method} objects and can be made immutable.
 */
public final class StdMethodList extends FixedSizeList implements MethodList {
  /**
   * Constructs an instance. All indices initially contain {@code null}.
   *
   * @param size the size of the list
   */
  public StdMethodList(int size) {
    super(size);
  }

  /** {@inheritDoc} */
  @Override
  public mod.agus.jcoderz.dx.cf.iface.Method get(int n) {
    return (mod.agus.jcoderz.dx.cf.iface.Method) get0(n);
  }

  /**
   * Sets the method at the given index.
   *
   * @param n {@code >= 0, < size();} which method
   * @param method {@code null-ok;} the method object
   */
  public void set(int n, Method method) {
    set0(n, method);
  }
}
