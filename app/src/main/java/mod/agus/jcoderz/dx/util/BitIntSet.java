/*
 * Copyright (C) 2008 The Android Open Source Project
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

package mod.agus.jcoderz.dx.util;

import java.util.NoSuchElementException;

/** A set of integers, represented by a bit set */
public class BitIntSet implements mod.agus.jcoderz.dx.util.IntSet {

  /** also accessed in ListIntSet */
  int[] bits;

  /**
   * Constructs an instance.
   *
   * @param max the maximum value of ints in this set.
   */
  public BitIntSet(int max) {
    bits = mod.agus.jcoderz.dx.util.Bits.makeBitSet(max);
  }

  /** {@inheritDoc} */
  @Override
  public void add(int value) {
    ensureCapacity(value);
    mod.agus.jcoderz.dx.util.Bits.set(bits, value, true);
  }

  /**
   * Ensures that the bit set has the capacity to represent the given value.
   *
   * @param value {@code >= 0;} value to represent
   */
  private void ensureCapacity(int value) {
    if (value >= mod.agus.jcoderz.dx.util.Bits.getMax(bits)) {
      int[] newBits =
          mod.agus.jcoderz.dx.util.Bits.makeBitSet(
              Math.max(value + 1, 2 * mod.agus.jcoderz.dx.util.Bits.getMax(bits)));
      System.arraycopy(bits, 0, newBits, 0, bits.length);
      bits = newBits;
    }
  }

  /** {@inheritDoc} */
  @Override
  public void remove(int value) {
    if (value < mod.agus.jcoderz.dx.util.Bits.getMax(bits)) {
      mod.agus.jcoderz.dx.util.Bits.set(bits, value, false);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean has(int value) {
    return (value < mod.agus.jcoderz.dx.util.Bits.getMax(bits))
        && mod.agus.jcoderz.dx.util.Bits.get(bits, value);
  }

  /** {@inheritDoc} */
  @Override
  public void merge(IntSet other) {
    if (other instanceof BitIntSet) {
      BitIntSet o = (BitIntSet) other;
      ensureCapacity(mod.agus.jcoderz.dx.util.Bits.getMax(o.bits) + 1);
      mod.agus.jcoderz.dx.util.Bits.or(bits, o.bits);
    } else if (other instanceof mod.agus.jcoderz.dx.util.ListIntSet) {
      mod.agus.jcoderz.dx.util.ListIntSet o = (ListIntSet) other;
      int sz = o.ints.size();

      if (sz > 0) {
        ensureCapacity(o.ints.get(sz - 1));
      }
      for (int i = 0; i < o.ints.size(); i++) {
        mod.agus.jcoderz.dx.util.Bits.set(bits, o.ints.get(i), true);
      }
    } else {
      IntIterator iter = other.iterator();
      while (iter.hasNext()) {
        add(iter.next());
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public int elements() {
    return mod.agus.jcoderz.dx.util.Bits.bitCount(bits);
  }

  /** {@inheritDoc} */
  @Override
  public IntIterator iterator() {
    return new IntIterator() {
      private int idx = mod.agus.jcoderz.dx.util.Bits.findFirst(bits, 0);

      /** {@inheritDoc} */
      @Override
      public boolean hasNext() {
        return idx >= 0;
      }

      /** {@inheritDoc} */
      @Override
      public int next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        int ret = idx;

        idx = mod.agus.jcoderz.dx.util.Bits.findFirst(bits, idx + 1);

        return ret;
      }
    };
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append('{');

    boolean first = true;
    for (int i = mod.agus.jcoderz.dx.util.Bits.findFirst(bits, 0);
        i >= 0;
        i = Bits.findFirst(bits, i + 1)) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(i);
    }

    sb.append('}');

    return sb.toString();
  }
}
