package com.yinxiang.utils.thrift.grpc.operations.generators;

/**
 * Code generator.
 * @param <T> code info type
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public interface CodeGenerator<T> {
  /** CODE: Collectors.to */
  String TO = ".collect(java.util.stream.Collectors.to";
  /** CODE: Collectors.toList */
  String TO_LIST = makeToCode("List");

  /**
   * Generate code from a {@link T} to {@link StringBuilder}.
   * @param builder {@link StringBuilder}
   * @param info    code info
   * @return  result
   */
  StringBuilder generate(StringBuilder builder, T info);

  /**
   * Append some <code>'\t'</code> to {@link StringBuilder}.
   * @param builder {@link StringBuilder}
   * @param tab     tab count
   * @return  result
   */
  static StringBuilder appendTabs(StringBuilder builder, int tab) {
    for (int i = 0;i < tab;i++) {
      builder.append("\t");
    }
    return builder;
  }

  /**
   * Make <code>Collectors.to</code> code.
   * @param collectionName  collection name
   * @param content         content
   * @return  result
   */
  static String makeToCode(String collectionName, String content) {
    return TO + collectionName + "(" + content + "))";
  }

  /**
   * Make <code>Collectors.toMap</code> code.
   * @param keyCode   key code
   * @param valueCode value code
   * @return  result
   */
  static String makeToMapCode(String keyCode, String valueCode) {
    return makeToCode("Map", keyCode + ", " + valueCode);
  }

  /**
   * Make <code>Collectors.to</code> code.
   * @param collectionName  collection name
   * @return  result
   */
  static String makeToCode(String collectionName) {
    return makeToCode(collectionName, "");
  }
}
