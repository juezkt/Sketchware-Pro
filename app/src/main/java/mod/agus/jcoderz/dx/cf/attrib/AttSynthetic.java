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

package mod.agus.jcoderz.dx.cf.attrib;

/** Attribute class for standard {@code Synthetic} attributes. */
public final class AttSynthetic extends BaseAttribute {
  /** {@code non-null;} attribute name for attributes of this type */
  public static final String ATTRIBUTE_NAME = "Synthetic";

  /** Constructs an instance. */
  public AttSynthetic() {
    super(ATTRIBUTE_NAME);
  }

  /** {@inheritDoc} */
  @Override
  public int byteLength() {
    return 6;
  }
}
