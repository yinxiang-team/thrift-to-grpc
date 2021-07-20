package com.yinxiang.utils.thrift.grpc.operations.generators.code;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * The type code data.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see TabCode
 */
public class TypeCode extends TabCode {
  /** Modifier(public/final/static/void/...) or modifier group(pubic static final...). */
  public final String mod;
  /** Type(class name or primitive type). */
  public final String type;
  /** Name. */
  public final String name;
  /** The list of annotations. */
  public final List<String> annotations = Lists.newLinkedList();

  public TypeCode(int tab, String mod, String type, String name) {
    super(tab);
    this.mod = mod;
    this.type = type;
    this.name = name;
  }
}
