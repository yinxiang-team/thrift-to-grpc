package com.yinxiang.utils.thrift.grpc.infos;

/**
 * Record a field type.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class FieldType {
  /** The simple name of thrift class. */
  public final String type;
  /** {@link com.yinxiang.utils.thrift.grpc.TType} */
  public final byte thriftType;

  public FieldType(String type, byte thriftType) {
    this.type = type;
    this.thriftType = thriftType;
  }
}
