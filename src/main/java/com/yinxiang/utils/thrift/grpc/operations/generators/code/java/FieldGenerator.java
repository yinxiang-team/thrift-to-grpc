package com.yinxiang.utils.thrift.grpc.operations.generators.code.java;

import com.yinxiang.utils.thrift.grpc.operations.generators.CodeGenerator;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.TypeCode;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * The field code generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see CodeGenerator
 */
public class FieldGenerator implements CodeGenerator<TypeCode> {
  @Override
  public StringBuilder generate(StringBuilder builder, TypeCode info) {
    int tab = info.tab;
    info.annotations.forEach(
            annotation -> CodeGenerator.appendTabs(builder, tab).append('@').append(annotation).append('\n'));
    return CodeGenerator.appendTabs(builder, tab)
            .append(isNullOrEmpty(info.mod) ? "" : (info.mod + ' '))
            .append(info.type)
            .append(' ')
            .append(info.name);
  }
}
