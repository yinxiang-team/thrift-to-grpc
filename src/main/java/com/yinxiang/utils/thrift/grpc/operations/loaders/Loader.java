package com.yinxiang.utils.thrift.grpc.operations.loaders;

import com.yinxiang.utils.thrift.grpc.infos.FileInfo;
import com.yinxiang.utils.thrift.grpc.infos.StructInfo;
import com.yinxiang.utils.thrift.grpc.operations.Operation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;

import static com.yinxiang.utils.thrift.grpc.utils.StringUtils.firstUpper;

/**
 * The thrift class loader interface.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see Operation
 */
public interface Loader extends Operation<Class<?>> {
  /**
   * Format the package.
   * @param clz a class
   * @return  replaceAll '.' to '_'
   */
  static String formatPackage(Class<?> clz) {
    Package pkg = clz.getPackage();
    return pkg == null ? "" : pkg.getName().replaceAll("\\.", "_");
  }

  /**
   * Check is thrift field.
   * @param mod modifier
   * @return  true if thrift field else false
   * @see Modifier
   */
  static boolean isThriftField(int mod) {
    return !Modifier.isStatic(mod) && !Modifier.isFinal(mod);
  }

  /**
   * Traversal all thrift fields.
   * @param clz       a class
   * @param consumer  field consumer
   */
  static void forEachThriftFields(Class clz, BiConsumer<Integer, Field> consumer) {
    int index = 0;
    for (Field field : clz.getDeclaredFields()) {
      Class type = field.getType();
      if (isThriftField(field.getModifiers()) && (!type.isArray() || type.equals(byte[].class))) {
        consumer.accept(index++, field);
      }
    }
  }

  /**
   * Create a {@link StructInfo}.
   * @param fileInfo  parent proto file info
   * @param name      struct name
   * @param isEnum    is enum
   * @return  {@link StructInfo}
   */
  static StructInfo createStruct(FileInfo fileInfo, String name, boolean isEnum) {
    StructInfo structInfo = new StructInfo(firstUpper(name));
    structInfo.setEnum(isEnum);
    fileInfo.saveStruct(structInfo);
    return structInfo;
  }
}
