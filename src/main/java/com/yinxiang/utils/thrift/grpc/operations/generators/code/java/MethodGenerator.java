package com.yinxiang.utils.thrift.grpc.operations.generators.code.java;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.yinxiang.utils.thrift.grpc.operations.generators.CodeGenerator;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.MethodCode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The method code generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see CodeGenerator
 */
public class MethodGenerator extends CodeGeneratorDecorator<MethodCode> {
  /** The filed code generator. */
  private static final FieldGenerator TYPE_GENERATOR = new FieldGenerator();
  /** The parameters code generator. */
  private static final CodeGenerator<MethodCode> PARAMETERS_GENERATOR = (builder, info) -> {
    List<String> parameters = info.parameters.stream()
            .map(fieldCode -> TYPE_GENERATOR.generate(new StringBuilder(), fieldCode).toString())
            .collect(Collectors.toList());
    return builder.append(Joiner.on(',').join(parameters));
  };
  /** The small close code generator. */
  private static final CodeGenerator<MethodCode> SMALL_CLOSE_GENERATOR
          = new SmallCloseGenerator<>(PARAMETERS_GENERATOR);
  /** The method head code generator. */
  private static final CodeGenerator<MethodCode> HEAD_GENERATOR =
          (builder, info) -> SMALL_CLOSE_GENERATOR.generate(TYPE_GENERATOR.generate(builder, info), info);
  /** The every line begin code generator. */
  private static final WithTabCodeGenerator LINE_BEGIN_GENERATOR = new WithTabCodeGenerator();
  /** The method body code generator. */
  private static final CodeGenerator<MethodCode> BODY_GENERATOR = (builder, info) -> {
    info.lineCodes.forEach(lineCode -> LINE_BEGIN_GENERATOR.generate(builder, lineCode));
    return builder;
  };
  /** The big close code generator. */
  private static final CodeGenerator<MethodCode> BIG_CLOSE_GENERATOR = new BigCloseGenerator<>(BODY_GENERATOR);

  public MethodGenerator() {
    super(new CodeGenerators<>(Lists.newArrayList(HEAD_GENERATOR, BIG_CLOSE_GENERATOR)));
  }

  @Override
  public StringBuilder generate(StringBuilder builder, MethodCode info) {
    return delegate.generate(builder, info).append('\n');
  }
}
