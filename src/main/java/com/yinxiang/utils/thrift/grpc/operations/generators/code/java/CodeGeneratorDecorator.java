package com.yinxiang.utils.thrift.grpc.operations.generators.code.java;

import com.yinxiang.utils.thrift.grpc.operations.generators.CodeGenerator;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.LineCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.TabCode;

/**
 * The decorator of code generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see CodeGenerator
 */
public abstract class CodeGeneratorDecorator<T> implements CodeGenerator<T> {
  /** Delegate. */
  protected final CodeGenerator<T> delegate;

  public CodeGeneratorDecorator(CodeGenerator<T> delegate) {
    this.delegate = delegate;
  }

  /**
   * Inline with "()" code generate.
   * @param <T> generic type
   */
  public static class SmallCloseGenerator<T> extends CodeGeneratorDecorator<T> {
    public SmallCloseGenerator(CodeGenerator<T> delegate) {
      super(delegate);
    }

    @Override
    public StringBuilder generate(StringBuilder builder, T info) {
      return delegate.generate(builder.append('('), info).append(')');
    }
  }

  /**
   * Inline with "{}" code generate.
   * @param <T> TabCode
   */
  public static class BigCloseGenerator<T extends TabCode> extends CodeGeneratorDecorator<T> {
    public BigCloseGenerator(CodeGenerator<T> delegate) {
      super(delegate);
    }

    @Override
    public StringBuilder generate(StringBuilder builder, T info) {
      return CodeGenerator.appendTabs(delegate.generate(builder.append(" {\n"), info), info.tab).append("}\n");
    }
  }

  /**
   * End with '\n' code generate.
   * @param <T> TabCode
   */
  public static class LineGenerator<T extends TabCode> extends CodeGeneratorDecorator<T> {
    public LineGenerator(CodeGenerator<T> delegate) {
      super(delegate);
    }

    @Override
    public StringBuilder generate(StringBuilder builder, T info) {
      return delegate.generate(builder, info).append("\n");
    }
  }

  /** Start with some '\t' code generate. */
  public static class WithTabCodeGenerator extends LineGenerator<LineCode> {
    public WithTabCodeGenerator() {
      super((builder, info) -> CodeGenerator.appendTabs(builder, info.tab).append(info.content));
    }
  }

  /**
   * End with ';' code generate.
   * @param <T> TabCode
   */
  public static class SentenceGenerator<T extends TabCode> extends LineGenerator<T> {
    public SentenceGenerator(CodeGenerator<T> delegate) {
      super((builder, info) -> delegate.generate(builder, info).append(';'));
    }
  }
}
