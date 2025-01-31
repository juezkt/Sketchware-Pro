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
 * Standard implementation of {@link mod.agus.jcoderz.dx.cf.iface.AttributeList}, which directly
 * stores an array of {@link mod.agus.jcoderz.dx.cf.iface.Attribute} objects and can be made
 * immutable.
 */
public final class StdAttributeList extends FixedSizeList implements AttributeList {
  /**
   * Constructs an instance. All indices initially contain {@code null}.
   *
   * @param size the size of the list
   */
  public StdAttributeList(int size) {
    super(size);
  }

  /** {@inheritDoc} */
  @Override
  public mod.agus.jcoderz.dx.cf.iface.Attribute get(int n) {
    return (mod.agus.jcoderz.dx.cf.iface.Attribute) get0(n);
  }

  /** {@inheritDoc} */
  @Override
  public int byteLength() {
    int sz = size();
    int result = 2; // u2 attributes_count

    for (int i = 0; i < sz; i++) {
      result += get(i).byteLength();
    }

    return result;
  }

  /** {@inheritDoc} */
  @Override
  public mod.agus.jcoderz.dx.cf.iface.Attribute findFirst(String name) {
    int sz = size();

    for (int i = 0; i < sz; i++) {
      mod.agus.jcoderz.dx.cf.iface.Attribute att = get(i);
      if (att.getName().equals(name)) {
        return att;
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public mod.agus.jcoderz.dx.cf.iface.Attribute findNext(
      mod.agus.jcoderz.dx.cf.iface.Attribute attrib) {
    int sz = size();
    int at;

    outer:
    {
      for (at = 0; at < sz; at++) {
        mod.agus.jcoderz.dx.cf.iface.Attribute att = get(at);
        if (att == attrib) {
          break outer;
        }
      }

      return null;
    }

    String name = attrib.getName();

    for (at++; at < sz; at++) {
      mod.agus.jcoderz.dx.cf.iface.Attribute att = get(at);
      if (att.getName().equals(name)) {
        return att;
      }
    }

    return null;
  }

  /**
   * Sets the attribute at the given index.
   *
   * @param n {@code >= 0, < size();} which attribute
   * @param attribute {@code null-ok;} the attribute object
   */
  public void set(int n, Attribute attribute) {
    set0(n, attribute);
  }
}
