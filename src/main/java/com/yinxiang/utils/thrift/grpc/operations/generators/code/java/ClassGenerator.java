package com.yinxiang.utils.thrift.grpc.operations.generators.code.java;

import com.google.common.base.Strings;
import com.yinxiang.utils.thrift.grpc.operations.generators.CodeGenerator;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.ClassCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.TypeCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.java.CodeGeneratorDecorator.*;

/**
 * The class code generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see CodeGenerator
 */
public class ClassGenerator implements CodeGenerator<ClassCode> {
  /** The import code generator. */
  private static final ImportGenerator IMPORT_GENERATOR = new ImportGenerator();
  /** The filed code generator. */
  private static final FieldGenerator TYPE_GENERATOR = new FieldGenerator();
  /** The one line filed code generator. */
  private static final CodeGenerator<TypeCode> FIELD_GENERATOR = new SentenceGenerator<>(TYPE_GENERATOR);
  /** The method code generator. */
  private static final MethodGenerator METHOD_GENERATOR = new MethodGenerator();
  /** The class body code generator. */
  private static final CodeGenerator<ClassCode> BODY_GENERATOR = (builder, info) -> {
    // generate fields
    info.fieldCodes.forEach(fieldCode -> FIELD_GENERATOR.generate(builder, fieldCode));
    builder.append('\n');
    // generate methods
    info.methodCodes.forEach(methodCode -> METHOD_GENERATOR.generate(builder, methodCode));
    builder.setLength(builder.length() - 1);
    return builder;
  };
  /** The import code generator. */
  private static final CodeGenerator<ClassCode> BIG_CLOSE_GENERATOR = new BigCloseGenerator<>(BODY_GENERATOR);

  @Override
  public StringBuilder generate(StringBuilder builder, ClassCode info) {
    // generate package
    builder.append("package ").append(info.pkg).append(";\n\n");
    // generate imports
    info.imports.forEach(im -> IMPORT_GENERATOR.generate(builder, im));
    // generate class head
    TYPE_GENERATOR.generate(builder.append('\n'), info)
            .append(Strings.isNullOrEmpty(info.extend) ? "" : (" extends " + info.extend));
    // generate class body
    return BIG_CLOSE_GENERATOR.generate(builder, info);
  }
}
