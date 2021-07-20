package com.yinxiang.utils.thrift.grpc.infos;

import com.yinxiang.utils.thrift.grpc.TType;

/**
 * Record a field infos.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class FieldInfo {
  /** Field name. */
  private String name;
  /**
   * Field:
   * - List or Set: the actual type.
   * - Map: the key actual type.
   * - others: the field type.
   */
  private FieldType firstType;
  /** When field is a map, this is the value actual type */
  private FieldType secondType;
  /** Note if field is List or Set or Map. */
  private FieldType containerType;
  /**
   * 1. gRPC proto2: default option.
   * 2. Ref info: full class name.
   */
  private String defaultValue;

  /** @see #secondType */
  public FieldType getSecondType() {
    return secondType;
  }

  /** @see #secondType */
  public void setSecondType(FieldType secondType) {
    this.secondType = secondType;
  }

  /**
   * @return  true if this is a thrift container else false.
   */
  public boolean isRepeated() {
    return containerType != null;
  }

  /** @see #name */
  public String getName() {
    return name;
  }

  /** @see #name */
  public void setName(String name) {
    this.name = name;
  }

  /** @see #firstType */
  public FieldType getFirstType() {
    return firstType;
  }

  /** @see #firstType */
  public void setFirstType(FieldType firstType) {
    this.firstType = firstType;
  }

  /**
   * @return  true if this is a thrift struct else false.
   */
  public boolean isStruct() {
    return containerType == null && this.firstType.thriftType == TType.STRUCT;
  }

  /**
   * @return  true if is gRPC map else false.
   */
  public boolean isGrpcMap() {
    return containerType != null && containerType.thriftType == TType.MAP && secondType != null;
  }

  /** @see #defaultValue */
  public String getDefaultValue() {
    return defaultValue;
  }

  /** @see #defaultValue */
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * @return  the format of thrift type reference.
   */
  public String getRefThriftType() {
    return getDefaultValue().replaceAll("\"", "");
  }

  /** @see #containerType */
  public FieldType getContainerType() {
    return containerType;
  }

  /** @see #containerType */
  public void setContainerType(FieldType containerType) {
    this.containerType = containerType;
  }
}
