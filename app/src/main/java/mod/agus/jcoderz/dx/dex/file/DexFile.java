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

package mod.agus.jcoderz.dx.dex.file;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Adler32;
import mod.agus.jcoderz.dex.DexFormat;
import mod.agus.jcoderz.dex.util.ExceptionWithContext;
import mod.agus.jcoderz.dx.dex.DexOptions;
import mod.agus.jcoderz.dx.rop.cst.Constant;
import mod.agus.jcoderz.dx.rop.cst.CstBaseMethodRef;
import mod.agus.jcoderz.dx.rop.cst.CstCallSiteRef;
import mod.agus.jcoderz.dx.rop.cst.CstEnumRef;
import mod.agus.jcoderz.dx.rop.cst.CstFieldRef;
import mod.agus.jcoderz.dx.rop.cst.CstMethodHandle;
import mod.agus.jcoderz.dx.rop.cst.CstProtoRef;
import mod.agus.jcoderz.dx.rop.cst.CstString;
import mod.agus.jcoderz.dx.rop.cst.CstType;
import mod.agus.jcoderz.dx.rop.type.Type;
import mod.agus.jcoderz.dx.util.ByteArrayAnnotatedOutput;

/**
 * Representation of an entire {@code .dex} (Dalvik EXecutable) file, which itself consists of a set
 * of Dalvik classes.
 */
public final class DexFile {
  /** options controlling the creation of the file */
  private final mod.agus.jcoderz.dx.dex.DexOptions dexOptions;

  /** {@code non-null;} word data section */
  private final MixedItemSection wordData;

  /**
   * {@code non-null;} type lists section. This is word data, but separating it from {@link
   * #wordData} helps break what would otherwise be a circular dependency between the that and
   * {@link #protoIds}.
   */
  private final MixedItemSection typeLists;

  /**
   * {@code non-null;} map section. The map needs to be in a section by itself for the
   * self-reference mechanics to work in a reasonably straightforward way. See {@link
   * mod.agus.jcoderz.dx.dex.file.MapItem#addMap} for more detail.
   */
  private final MixedItemSection map;

  /** {@code non-null;} string data section */
  private final MixedItemSection stringData;

  /** {@code non-null;} string identifiers section */
  private final mod.agus.jcoderz.dx.dex.file.StringIdsSection stringIds;

  /** {@code non-null;} type identifiers section */
  private final mod.agus.jcoderz.dx.dex.file.TypeIdsSection typeIds;

  /** {@code non-null;} prototype identifiers section */
  private final mod.agus.jcoderz.dx.dex.file.ProtoIdsSection protoIds;

  /** {@code non-null;} field identifiers section */
  private final mod.agus.jcoderz.dx.dex.file.FieldIdsSection fieldIds;

  /** {@code non-null;} method identifiers section */
  private final MethodIdsSection methodIds;

  /** {@code non-null;} class definitions section */
  private final mod.agus.jcoderz.dx.dex.file.ClassDefsSection classDefs;

  /** {@code non-null;} class data section */
  private final MixedItemSection classData;

  /** {@code null-ok;} call site identifiers section, only exists for SDK >= 26 */
  private final mod.agus.jcoderz.dx.dex.file.CallSiteIdsSection callSiteIds;

  /** {@code null-ok;} method handles section, only exists for SDK >= 26 */
  private final mod.agus.jcoderz.dx.dex.file.MethodHandlesSection methodHandles;

  /** {@code non-null;} byte data section */
  private final MixedItemSection byteData;

  /** {@code non-null;} file header */
  private final HeaderSection header;

  /** {@code non-null;} array of sections in the order they will appear in the final output file */
  private final Section[] sections;

  /** {@code >= -1;} total file size or {@code -1} if unknown */
  private int fileSize;

  /** {@code >= 40;} maximum width of the file dump */
  private int dumpWidth;

  /**
   * Constructs an instance. It is initially empty.
   *
   * @param dexOptions {@code non-null;} DEX file generations options to apply
   */
  public DexFile(final mod.agus.jcoderz.dx.dex.DexOptions dexOptions) {
    this.dexOptions = dexOptions;

    header = new HeaderSection(this);
    typeLists = new MixedItemSection(null, this, 4, MixedItemSection.SortType.NONE);
    wordData = new MixedItemSection("word_data", this, 4, MixedItemSection.SortType.TYPE);
    stringData = new MixedItemSection("string_data", this, 1, MixedItemSection.SortType.INSTANCE);
    classData = new MixedItemSection(null, this, 1, MixedItemSection.SortType.NONE);
    byteData = new MixedItemSection("byte_data", this, 1, MixedItemSection.SortType.TYPE);
    stringIds = new mod.agus.jcoderz.dx.dex.file.StringIdsSection(this);
    typeIds = new mod.agus.jcoderz.dx.dex.file.TypeIdsSection(this);
    protoIds = new mod.agus.jcoderz.dx.dex.file.ProtoIdsSection(this);
    fieldIds = new mod.agus.jcoderz.dx.dex.file.FieldIdsSection(this);
    methodIds = new MethodIdsSection(this);
    classDefs = new mod.agus.jcoderz.dx.dex.file.ClassDefsSection(this);
    map = new MixedItemSection("map", this, 4, MixedItemSection.SortType.NONE);

    /*
     * Prepare the list of sections in the order they appear in
     * the final output.
     */
    if (dexOptions.apiIsSupported(DexFormat.API_METHOD_HANDLES)) {
      /*
       * Method handles and call sites only visible in DEX files
       * from SDK version 26 onwards. Do not create or add sections unless
       * version true. This is conditional to avoid churn in the dx tests
       * that look at annotated output.
       */
      callSiteIds = new mod.agus.jcoderz.dx.dex.file.CallSiteIdsSection(this);
      methodHandles = new mod.agus.jcoderz.dx.dex.file.MethodHandlesSection(this);

      sections =
          new Section[] {
            header,
            stringIds,
            typeIds,
            protoIds,
            fieldIds,
            methodIds,
            classDefs,
            callSiteIds,
            methodHandles,
            wordData,
            typeLists,
            stringData,
            byteData,
            classData,
            map
          };
    } else {
      callSiteIds = null;
      methodHandles = null;

      sections =
          new Section[] {
            header,
            stringIds,
            typeIds,
            protoIds,
            fieldIds,
            methodIds,
            classDefs,
            wordData,
            typeLists,
            stringData,
            byteData,
            classData,
            map
          };
    }

    fileSize = -1;
    dumpWidth = 79;
  }

  /** Returns true if this dex doesn't contain any class defs. */
  public boolean isEmpty() {
    return classDefs.items().isEmpty();
  }

  /** Gets the dex-creation options object. */
  public DexOptions getDexOptions() {
    return dexOptions;
  }

  /**
   * Adds a class to this instance. It is illegal to attempt to add more than one class with the
   * same name.
   *
   * @param clazz {@code non-null;} the class to add
   */
  public void add(ClassDefItem clazz) {
    classDefs.add(clazz);
  }

  /**
   * Gets the class definition with the given name, if any.
   *
   * @param name {@code non-null;} the class name to look for
   * @return {@code null-ok;} the class with the given name, or {@code null} if there is no such
   *     class
   */
  public ClassDefItem getClassOrNull(String name) {
    try {
      mod.agus.jcoderz.dx.rop.type.Type type = Type.internClassName(name);
      return (ClassDefItem) classDefs.get(new mod.agus.jcoderz.dx.rop.cst.CstType(type));
    } catch (IllegalArgumentException ex) {
      // Translate exception, per contract.
      return null;
    }
  }

  /**
   * Writes the contents of this instance as either a binary or a human-readable form, or both.
   *
   * @param out {@code null-ok;} where to write to
   * @param humanOut {@code null-ok;} where to write human-oriented output to
   * @param verbose whether to be verbose when writing human-oriented output
   */
  public void writeTo(OutputStream out, Writer humanOut, boolean verbose) throws IOException {
    writeTo(out, null /* storage */, humanOut, verbose);
  }

  /**
   * Writes the contents of this instance as either a binary or a human-readable form, or both.
   *
   * @param out {@code null-ok;} where to write to
   * @param storage temporary storage for storing dexing.
   * @param humanOut {@code null-ok;} where to write human-oriented output to
   * @param verbose whether to be verbose when writing human-oriented output
   */
  public void writeTo(OutputStream out, Storage storage, Writer humanOut, boolean verbose)
      throws IOException {
    boolean annotate = (humanOut != null);
    mod.agus.jcoderz.dx.util.ByteArrayAnnotatedOutput result = toDex0(annotate, verbose, storage);

    if (out != null) {
      out.write(result.getArray());
    }

    if (annotate) {
      result.writeAnnotationsTo(humanOut);
    }
  }

  /**
   * Writes the contents of this instance as a binary.
   *
   * @param storage temporary storage for storing dexing.
   * @return the stored content.
   */
  public mod.agus.jcoderz.dx.util.ByteArrayAnnotatedOutput writeTo(Storage storage) {
    return toDex0(false, false, storage);
  }

  /**
   * Returns the contents of this instance as a {@code .dex} file, in {@code byte[]} form.
   *
   * @param humanOut {@code null-ok;} where to write human-oriented output to
   * @param verbose whether to be verbose when writing human-oriented output
   * @return {@code non-null;} a {@code .dex} file for this instance
   */
  public byte[] toDex(Writer humanOut, boolean verbose) throws IOException {
    boolean annotate = (humanOut != null);
    mod.agus.jcoderz.dx.util.ByteArrayAnnotatedOutput result = toDex0(annotate, verbose, null);

    if (annotate) {
      result.writeAnnotationsTo(humanOut);
    }

    return result.getArray();
  }

  /**
   * Sets the maximum width of the human-oriented dump of the instance.
   *
   * @param dumpWidth {@code >= 40;} the width
   */
  public void setDumpWidth(int dumpWidth) {
    if (dumpWidth < 40) {
      throw new IllegalArgumentException("dumpWidth < 40");
    }

    this.dumpWidth = dumpWidth;
  }

  /**
   * Gets the total file size, if known.
   *
   * <p>This is package-scope in order to allow the {@link HeaderSection} to set itself up properly.
   *
   * @return {@code >= 0;} the total file size
   * @throws RuntimeException thrown if the file size is not yet known
   */
  public int getFileSize() {
    if (fileSize < 0) {
      throw new RuntimeException("file size not yet known");
    }

    return fileSize;
  }

  /**
   * Gets the string data section.
   *
   * <p>This is package-scope in order to allow the various {@link Item} instances to add items to
   * the instance.
   *
   * @return {@code non-null;} the string data section
   */
  /*package*/ MixedItemSection getStringData() {
    return stringData;
  }

  /**
   * Gets the word data section.
   *
   * <p>This is package-scope in order to allow the various {@link Item} instances to add items to
   * the instance.
   *
   * @return {@code non-null;} the word data section
   */
  /*package*/ MixedItemSection getWordData() {
    return wordData;
  }

  /**
   * Gets the type lists section.
   *
   * <p>This is package-scope in order to allow the various {@link Item} instances to add items to
   * the instance.
   *
   * @return {@code non-null;} the word data section
   */
  /*package*/ MixedItemSection getTypeLists() {
    return typeLists;
  }

  /**
   * Gets the map section.
   *
   * <p>This is package-scope in order to allow the header section to query it.
   *
   * @return {@code non-null;} the map section
   */
  /*package*/ MixedItemSection getMap() {
    return map;
  }

  /**
   * Gets the string identifiers section.
   *
   * <p>This is package-scope in order to allow the various {@link Item} instances to add items to
   * the instance.
   *
   * @return {@code non-null;} the string identifiers section
   */
  /*package*/ StringIdsSection getStringIds() {
    return stringIds;
  }

  /**
   * Gets the class definitions section.
   *
   * <p>This is package-scope in order to allow the various {@link Item} instances to add items to
   * the instance.
   *
   * @return {@code non-null;} the class definitions section
   */
  public ClassDefsSection getClassDefs() {
    return classDefs;
  }

  /**
   * Gets the class data section.
   *
   * <p>This is package-scope in order to allow the various {@link Item} instances to add items to
   * the instance.
   *
   * @return {@code non-null;} the class data section
   */
  /*package*/ MixedItemSection getClassData() {
    return classData;
  }

  /**
   * Gets the type identifiers section.
   *
   * <p>This is public in order to allow the various {@link Item} instances to add items to the
   * instance and help early counting of type ids.
   *
   * @return {@code non-null;} the class identifiers section
   */
  public TypeIdsSection getTypeIds() {
    return typeIds;
  }

  /**
   * Gets the prototype identifiers section.
   *
   * <p>This is package-scope in order to allow the various {@link Item} instances to add items to
   * the instance.
   *
   * @return {@code non-null;} the prototype identifiers section
   */
  /*package*/ ProtoIdsSection getProtoIds() {
    return protoIds;
  }

  /**
   * Gets the field identifiers section.
   *
   * <p>This is public in order to allow the various {@link Item} instances to add items to the
   * instance and help early counting of field ids.
   *
   * @return {@code non-null;} the field identifiers section
   */
  public FieldIdsSection getFieldIds() {
    return fieldIds;
  }

  /**
   * Gets the method identifiers section.
   *
   * <p>This is public in order to allow the various {@link Item} instances to add items to the
   * instance and help early counting of method ids.
   *
   * @return {@code non-null;} the method identifiers section
   */
  public MethodIdsSection getMethodIds() {
    return methodIds;
  }

  /**
   * Gets the method handles section.
   *
   * <p>This is public in order to allow the various {@link Item} instances to add items to the
   * instance and help early counting of method handles.
   *
   * @return {@code non-null;} the method handles section
   */
  public MethodHandlesSection getMethodHandles() {
    return methodHandles;
  }

  /**
   * Gets the call site identifiers section.
   *
   * <p>This is public in order to allow the various {@link Item} instances to add items to the
   * instance and help early counting of call site identifiers.
   *
   * @return {@code non-null;} the call site identifiers section
   */
  public CallSiteIdsSection getCallSiteIds() {
    return callSiteIds;
  }

  /**
   * Gets the byte data section.
   *
   * <p>This is package-scope in order to allow the various {@link Item} instances to add items to
   * the instance.
   *
   * @return {@code non-null;} the byte data section
   */
  /*package*/ MixedItemSection getByteData() {
    return byteData;
  }

  /**
   * Gets the first section of the file that is to be considered part of the data section.
   *
   * <p>This is package-scope in order to allow the header section to query it.
   *
   * @return {@code non-null;} the section
   */
  /*package*/ Section getFirstDataSection() {
    return wordData;
  }

  /**
   * Gets the last section of the file that is to be considered part of the data section.
   *
   * <p>This is package-scope in order to allow the header section to query it.
   *
   * @return {@code non-null;} the section
   */
  /*package*/ Section getLastDataSection() {
    return map;
  }

  /**
   * Interns the given constant in the appropriate section of this instance, or do nothing if the
   * given constant isn't the sort that should be interned.
   *
   * @param cst {@code non-null;} constant to possibly intern
   */
  /*package*/ void internIfAppropriate(mod.agus.jcoderz.dx.rop.cst.Constant cst) {
    if (cst == null) {
      throw new NullPointerException("cst == null");
    }

    if (cst instanceof mod.agus.jcoderz.dx.rop.cst.CstString) {
      stringIds.intern((mod.agus.jcoderz.dx.rop.cst.CstString) cst);
    } else if (cst instanceof mod.agus.jcoderz.dx.rop.cst.CstType) {
      typeIds.intern((mod.agus.jcoderz.dx.rop.cst.CstType) cst);
    } else if (cst instanceof mod.agus.jcoderz.dx.rop.cst.CstBaseMethodRef) {
      methodIds.intern((mod.agus.jcoderz.dx.rop.cst.CstBaseMethodRef) cst);
    } else if (cst instanceof mod.agus.jcoderz.dx.rop.cst.CstFieldRef) {
      fieldIds.intern((mod.agus.jcoderz.dx.rop.cst.CstFieldRef) cst);
    } else if (cst instanceof mod.agus.jcoderz.dx.rop.cst.CstEnumRef) {
      fieldIds.intern(((mod.agus.jcoderz.dx.rop.cst.CstEnumRef) cst).getFieldRef());
    } else if (cst instanceof mod.agus.jcoderz.dx.rop.cst.CstProtoRef) {
      protoIds.intern(((mod.agus.jcoderz.dx.rop.cst.CstProtoRef) cst).getPrototype());
    } else if (cst instanceof mod.agus.jcoderz.dx.rop.cst.CstMethodHandle) {
      methodHandles.intern((mod.agus.jcoderz.dx.rop.cst.CstMethodHandle) cst);
    }
  }

  /**
   * Gets the {@link mod.agus.jcoderz.dx.dex.file.IndexedItem} corresponding to the given constant,
   * if it is a constant that has such a correspondence, or return {@code null} if it isn't such a
   * constant. This will throw an exception if the given constant <i>should</i> have been found but
   * wasn't.
   *
   * @param cst {@code non-null;} the constant to look up
   * @return {@code null-ok;} its corresponding item, if it has a corresponding item, or {@code
   *     null} if it's not that sort of constant
   */
  /*package*/ IndexedItem findItemOrNull(Constant cst) {
    if (cst instanceof CstString) {
      return stringIds.get(cst);
    } else if (cst instanceof CstType) {
      return typeIds.get(cst);
    } else if (cst instanceof CstBaseMethodRef) {
      return methodIds.get(cst);
    } else if (cst instanceof CstFieldRef) {
      return fieldIds.get(cst);
    } else if (cst instanceof mod.agus.jcoderz.dx.rop.cst.CstEnumRef) {
      return fieldIds.intern(((CstEnumRef) cst).getFieldRef());
    } else if (cst instanceof CstProtoRef) {
      return protoIds.get(cst);
    } else if (cst instanceof CstMethodHandle) {
      return methodHandles.get(cst);
    } else if (cst instanceof CstCallSiteRef) {
      return callSiteIds.get(cst);
    } else {
      return null;
    }
  }

  /** Holder for a byte[] that can grow on demand. */
  public static final class Storage {
    byte[] storage;

    public Storage(byte[] storage) {
      this.storage = storage;
    }

    public byte[] getStorage(int requestedLength) {
      if (storage.length < requestedLength) {
        Logger.getAnonymousLogger()
            .log(
                Level.FINER,
                "DexFile storage too small  " + storage.length + " vs " + requestedLength);
        storage = new byte[requestedLength];
      }
      return storage;
    }
  }

  /**
   * Returns the contents of this instance as a {@code .dex} file, in a {@link
   * mod.agus.jcoderz.dx.util.ByteArrayAnnotatedOutput} instance.
   *
   * @param annotate whether or not to keep annotations
   * @param verbose if annotating, whether to be verbose
   * @return {@code non-null;} a {@code .dex} file for this instance
   */
  private mod.agus.jcoderz.dx.util.ByteArrayAnnotatedOutput toDex0(
      boolean annotate, boolean verbose, Storage storage) {
    /*
     * The following is ordered so that the prepare() calls which
     * add items happen before the calls to the sections that get
     * added to.
     */

    classDefs.prepare();
    classData.prepare();
    wordData.prepare();
    if (dexOptions.apiIsSupported(DexFormat.API_METHOD_HANDLES)) {
      // Prepare call site ids before byteData where the call site items are placed.
      callSiteIds.prepare();
    }
    byteData.prepare();
    if (dexOptions.apiIsSupported(DexFormat.API_METHOD_HANDLES)) {
      // Prepare method handles after call site items placed in byteData.
      methodHandles.prepare();
    }
    methodIds.prepare();
    fieldIds.prepare();
    protoIds.prepare();
    typeLists.prepare();
    typeIds.prepare();
    stringIds.prepare();
    stringData.prepare();
    header.prepare();

    // Place the sections within the file.

    int count = sections.length;
    int offset = 0;

    for (int i = 0; i < count; i++) {
      Section one = sections[i];
      if ((one == callSiteIds || one == methodHandles) && one.items().isEmpty()) {
        /*
         * Skip call site or method handles sections if they have no items as
         * they may change the alignment of what follows even if empty as
         * Section.setFileOffset() always ensures appropriate alignment for the section.
         */
        continue;
      }

      int placedAt = one.setFileOffset(offset);
      if (placedAt < offset) {
        throw new RuntimeException("bogus placement for section " + i);
      }

      try {
        if (one == map) {
          /*
           * Inform the map of all the sections, and add it
           * to the file. This can only be done after all
           * the other items have been sorted and placed.
           */
          MapItem.addMap(sections, map);
          map.prepare();
        }

        if (one instanceof MixedItemSection) {
          /*
           * Place the items of a MixedItemSection that just
           * got placed.
           */
          ((MixedItemSection) one).placeItems();
        }

        offset = placedAt + one.writeSize();
      } catch (RuntimeException ex) {
        throw ExceptionWithContext.withContext(ex, "...while writing section " + i);
      }
    }

    // Write out all the sections.

    fileSize = offset;
    byte[] barr = storage == null ? new byte[fileSize] : storage.getStorage(fileSize);

    mod.agus.jcoderz.dx.util.ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(barr);

    if (annotate) {
      out.enableAnnotations(dumpWidth, verbose);
    }

    for (int i = 0; i < count; i++) {
      try {
        final Section one = sections[i];
        if ((one == callSiteIds || one == methodHandles) && one.items().isEmpty()) {
          continue;
        }
        final int zeroCount = one.getFileOffset() - out.getCursor();
        if (zeroCount < 0) {
          throw new ExceptionWithContext("excess write of " + (-zeroCount));
        }
        out.writeZeroes(zeroCount);
        one.writeTo(out);
      } catch (RuntimeException ex) {
        ExceptionWithContext ec;
        if (ex instanceof ExceptionWithContext) {
          ec = (ExceptionWithContext) ex;
        } else {
          ec = new ExceptionWithContext(ex);
        }
        ec.addContext("...while writing section " + i);
        throw ec;
      }
    }

    if (out.getCursor() != fileSize) {
      throw new RuntimeException("foreshortened write");
    }

    // Perform final bookkeeping.

    calcSignature(barr, out.getCursor());
    calcChecksum(barr, out.getCursor());

    if (annotate) {
      wordData.writeIndexAnnotation(out, ItemType.TYPE_CODE_ITEM, "\nmethod code index:\n\n");
      getStatistics().writeAnnotation(out);
      out.finishAnnotating();
    }

    return out;
  }

  /**
   * Generates and returns statistics for all the items in the file.
   *
   * @return {@code non-null;} the statistics
   */
  public mod.agus.jcoderz.dx.dex.file.Statistics getStatistics() {
    mod.agus.jcoderz.dx.dex.file.Statistics stats = new Statistics();

    for (Section s : sections) {
      stats.addAll(s);
    }

    return stats;
  }

  /**
   * Calculates the signature for the {@code .dex} file in the given array, and modify the array to
   * contain it.
   *
   * @param bytes {@code non-null;} the bytes of the file
   * @param len length of {@code .dex} file encoded in the array
   */
  private static void calcSignature(byte[] bytes, int len) {
    MessageDigest md;

    try {
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }

    md.update(bytes, 32, len - 32);

    try {
      int amt = md.digest(bytes, 12, 20);
      if (amt != 20) {
        throw new RuntimeException("unexpected digest write: " + amt + " bytes");
      }
    } catch (DigestException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Calculates the checksum for the {@code .dex} file in the given array, and modify the array to
   * contain it.
   *
   * @param bytes {@code non-null;} the bytes of the file
   * @param len length of {@code .dex} file encoded in the array
   */
  private static void calcChecksum(byte[] bytes, int len) {
    Adler32 a32 = new Adler32();

    a32.update(bytes, 12, len - 12);

    int sum = (int) a32.getValue();

    bytes[8] = (byte) sum;
    bytes[9] = (byte) (sum >> 8);
    bytes[10] = (byte) (sum >> 16);
    bytes[11] = (byte) (sum >> 24);
  }
}
