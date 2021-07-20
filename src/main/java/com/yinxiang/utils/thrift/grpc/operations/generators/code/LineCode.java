package com.yinxiang.utils.thrift.grpc.operations.generators.code;

/**
 * The line code data.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see TabCode
 */
public class LineCode extends TabCode {
  /** The line content. */
  public String content;

  public LineCode(int tab, String content) {
    super(tab);
    this.content = content;
  }
}
