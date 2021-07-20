package com.yinxiang.utils.thrift.grpc.operations.generators.code.java;

import com.yinxiang.utils.thrift.grpc.operations.generators.CodeGenerator;

import java.util.List;

/**
 * A composite {@link CodeGenerator}.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see CodeGenerator
 */
public class CodeGenerators<T> implements CodeGenerator<T> {
  /** The list of {@link CodeGenerator}. */
  private final List<CodeGenerator<T>> list;

  public CodeGenerators(List<CodeGenerator<T>> list) {
    this.list = list;
  }

  @Override
  public StringBuilder generate(StringBuilder builder, T info) {
    list.forEach(e -> e.generate(builder, info));
    return builder;
  }
}
