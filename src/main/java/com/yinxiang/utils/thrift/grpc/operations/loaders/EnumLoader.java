package com.yinxiang.utils.thrift.grpc.operations.loaders;

import com.yinxiang.utils.thrift.grpc.infos.FieldInfo;
import com.yinxiang.utils.thrift.grpc.infos.FileInfo;
import com.yinxiang.utils.thrift.grpc.infos.StructInfo;

import java.lang.reflect.Field;
import java.util.function.Function;

import static com.yinxiang.utils.thrift.grpc.utils.StringUtils.firstUpper;

/**
 * The thrift enum loader.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see Loader
 */
public class EnumLoader implements Loader {
  /** The map of enum parent files, key is file name. */
  final Function<String, FileInfo> fileInfos;

  /** Count of thrift enum. */
  private int count;

  public EnumLoader(Function<String, FileInfo> fileInfos) {
    this.fileInfos = fileInfos;
  }

  @Override
  public void execute(Class<?> clz) {
    FileInfo fileInfo = fileInfos.apply(Loader.formatPackage(clz));
    if (!fileInfo.getOriginNames().contains(clz.getSimpleName())) {
      createEnum(clz, fileInfo);
    }
    count++;
  }

  @Override
  public void outputLog() {
    System.out.println("Create enum: " + count);
  }

  /**
   * Create enum struct.
   * @param clz       a thrift enum class
   * @param fileInfo  parent file info
   * @return  enum struct
   */
  StructInfo createEnum(Class<?> clz, FileInfo fileInfo) {
    StructInfo structInfo = Loader.createStruct(fileInfo, firstUpper(clz.getSimpleName()), true);
    for (Field field : clz.getFields()) {
      FieldInfo fieldInfo = new FieldInfo();
      fieldInfo.setName(field.getName());
      structInfo.getFieldInfos().add(fieldInfo);
    }
    structInfo.setRefInfo(structInfo.getName(), clz);
    return structInfo;
  }
}
