package com.yinxiang.utils.thrift.grpc.operations.generators.code;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * The class code data.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see TypeCode
 */
public class ClassCode extends TypeCode {
  /** The package of class. */
  public final String pkg;
  /** The parent class. */
  public final String extend;
  /** The list of imports. */
  public final List<String> imports = Lists.newLinkedList();
  /** The list of {@link FieldCode}. */
  public final List<FieldCode> fieldCodes = Lists.newLinkedList();
  /** The list of {@link MethodCode}. */
  public final List<MethodCode> methodCodes = Lists.newLinkedList();

  public ClassCode(int tab, String mod, String type, String name, String pkg, String extend) {
    super(tab, mod, type, name);
    this.pkg = pkg;
    this.extend = extend;
  }

  public ClassCode(String mod, String type, String name, String pkg, String extend) {
    this(0, mod, type, name, pkg, extend);
  }

  public ClassCode(String mod, String type, String name, String pkg) {
    this(0, mod, type, name, pkg, "");
  }
}
