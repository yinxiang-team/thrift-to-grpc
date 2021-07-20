package com.yinxiang.utils.thrift.grpc.infos;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.yinxiang.utils.thrift.grpc.TType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Record a message infos of gRPC.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class StructInfo {
  /** The set of struct name, use to make alias name. */
  private static final Set<String> NAMES = Sets.newHashSet();
  /** The map of all proto, key is {@link FieldInfo#getName()}. */
  private static final Map<String, FileInfo> PARENTS = Maps.newHashMap();

  /** The default empty struct. */
  public static final StructInfo EMPTY = new StructInfo("Empty");

  /** The origin name of struct. */
  private String originName;
  /** The name of struct, this will the alias name if this name is repeated. */
  private String name;
  /** Note if a enum. */
  private boolean isEnum;
  /** The list of fields. */
  private List<FieldInfo> fieldInfos = Lists.newLinkedList();
  /** The reference info. */
  private FieldInfo refInfo;

  public StructInfo(String name) {
    setName(name);
  }

  /** @see #fieldInfos */
  public List<FieldInfo> getFieldInfos() {
    return fieldInfos;
  }

  /** @see #name */
  public String getName() {
    return name;
  }

  /** @see #name */
  private void setName(String name) {
    originName = name;
    if (!name.equals("bytes")) {
      int index = 0;
      while (NAMES.contains(name + (index == 0 ? "" : index))) {
        index++;
      }
      name += (index == 0 ? "" : index);
      NAMES.add(name);
    }
    this.name = name;
  }

  /** @see #isEnum */
  public boolean isEnum() {
    return isEnum;
  }

  /** @see #isEnum */
  public void setEnum(boolean anEnum) {
    isEnum = anEnum;
  }

  /**
   * Get a parent proto file of a struct.
   * @param structName  the name of struct
   * @return  parent proto file
   */
  public static FileInfo getFileInfo(String structName) {
    return PARENTS.get(structName);
  }

  /**
   * @return  the common proto file
   */
  public static FileInfo safeGetFileInfo(String structName) {
    FileInfo fileInfo = PARENTS.get(structName);
    return (fileInfo == null || !fileInfo.getStructInfos().containsKey(structName) ? getCommonFile() : fileInfo);
  }

  /**
   * @return  the common proto file
   */
  public static FileInfo getCommonFile() {
    return PARENTS.get(EMPTY.getName());
  }

  /**
   * Get a struct.
   * @param structName  the name of struct
   * @return  struct
   */
  public static StructInfo getStructInfo(String structName) {
    return safeGetFileInfo(structName).getStructInfos().get(structName);
  }

  /**
   * Record parent.
   * @param fileInfo  the parent proto file
   */
  public void setFileInfo(FileInfo fileInfo) {
    PARENTS.put(name, fileInfo);
  }

  /** @see #originName */
  String getOriginName() {
    return originName;
  }

  /** @see #refInfo */
  public FieldInfo getRefInfo() {
    return refInfo;
  }

  /**
   * Record thrift type.
   * @param refName   reference name
   * @param refClass  thrift class
   */
  public void setRefInfo(String refName, Class<?> refClass) {
    FieldInfo refInfo = new FieldInfo();
    refInfo.setFirstType(new FieldType("string", TType.STRING));
    refInfo.setName(refName);
    refInfo.setDefaultValue("\"" + refClass.getName() + "\"");
    this.refInfo = refInfo;
  }

  /**
   * @return  the thrift type
   */
  public String getRefThriftType() {
    return getRefInfo().getRefThriftType();
  }
}
