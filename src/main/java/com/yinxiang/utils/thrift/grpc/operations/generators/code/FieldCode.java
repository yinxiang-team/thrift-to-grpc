package com.yinxiang.utils.thrift.grpc.operations.generators.code;

/**
 * The field code data.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see TypeCode
 */
public class FieldCode extends TypeCode {
  public FieldCode(int tab, String mod, String type, String name) {
    super(tab, mod, type, name);
  }

  public FieldCode(String mod, String type, String name) {
    this(0, mod, type, name);
  }

  public FieldCode(String type, String name) {
    this(0, "", type, name);
  }
}
