package com.yinxiang.utils.thrift.grpc.operations.generators.code;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * The method code data.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see TypeCode
 */
public class MethodCode extends TypeCode {
  /** The list of {@link FieldCode}. */
  public final List<FieldCode> parameters = Lists.newLinkedList();
  /** The list of {@link LineCode}. */
  public final List<LineCode> lineCodes = Lists.newLinkedList();

  public MethodCode(int tab, String mod, String type, String name) {
    super(tab, mod, type, name);
  }
}
