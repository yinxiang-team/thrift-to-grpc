package com.yinxiang.utils.thrift.grpc.operations.generators.code.java;

import com.yinxiang.utils.thrift.grpc.operations.generators.CodeGenerator;

/**
 * The import code generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see CodeGenerator
 */
public class ImportGenerator implements CodeGenerator<String> {
  @Override
  public StringBuilder generate(StringBuilder builder, String info) {
    return builder.append("import ").append(info).append(";\n");
  }
}
