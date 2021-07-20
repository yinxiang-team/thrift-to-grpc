package com.yinxiang.utils.thrift.grpc.operations.loaders;

import com.yinxiang.utils.thrift.grpc.TType;
import com.yinxiang.utils.thrift.grpc.infos.FieldInfo;
import com.yinxiang.utils.thrift.grpc.infos.FieldType;
import com.yinxiang.utils.thrift.grpc.infos.FileInfo;
import com.yinxiang.utils.thrift.grpc.infos.StructInfo;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.yinxiang.utils.thrift.grpc.utils.StringUtils.firstUpper;

/**
 * The thrift struct loader.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see Loader
 */
public class StructLoader implements Loader, Function<String, FileInfo> {
  private final EnumLoader enumLoader;

  /** Count of thrift struct. */
  private int count;

  public StructLoader(EnumLoader enumLoader) {
    this.enumLoader = enumLoader;
  }

  @Override
  public void execute(Class<?> clz) {
    FileInfo fileInfo = apply(Loader.formatPackage(clz));
    if (!fileInfo.getOriginNames().contains(clz.getSimpleName())) {
      createStruct(fileInfo, clz);
    }
    count++;
  }

  @Override
  public void outputLog() {
    System.out.println("Create message: " + count);
  }

  /**
   * Create a {@link StructInfo} for a thrift class.
   * @param fileInfo  parent proto file info
   * @param clz       a thrift class
   * @return  {@link StructInfo}
   */
  private StructInfo createStruct(FileInfo fileInfo, Class<?> clz) {
    StructInfo structInfo = Loader.createStruct(fileInfo, clz.getSimpleName(), false);
    // process all fields
    Loader.forEachThriftFields(clz, (index, field) -> {
       try {
        FieldInfo fieldInfo = createFieldInfo(fileInfo, field.getName(), field.getType(), field.getGenericType());
        structInfo.getFieldInfos().add(fieldInfo);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    // add reference info
    structInfo.setRefInfo(structInfo.getName(), clz);
    return structInfo;
  }

  /**
   * Create a {@link FieldInfo}.
   * @param fileInfo  parent proto file info
   * @param name      field name
   * @param clz       field class
   * @param type      field type
   * @return  {@link FieldInfo}
   * @throws Exception  exception
   */
  FieldInfo createFieldInfo(FileInfo fileInfo, String name, Class clz, Type type) throws Exception {
    FieldInfo fieldInfo = new FieldInfo();
    fieldInfo.setName(name);
    if (clz.equals(List.class)) {
      fieldInfo.setFirstType(createSubType(fileInfo, name, type, 0));
      fieldInfo.setContainerType(new FieldType(clz.getSimpleName(), TType.LIST));
    } else if (clz.equals(Set.class)) {
      fieldInfo.setFirstType(createSubType(fileInfo, name, type, 0));
      fieldInfo.setContainerType(new FieldType(clz.getSimpleName(), TType.SET));
    } else if (clz.equals(Map.class)) {
      FieldType firstType = createSubType(fileInfo, name, type, 0);
      if (firstType.thriftType == TType.STRUCT) {
        StructInfo entry = new StructInfo(firstUpper(name));
        entry.getFieldInfos().add(createSubFiled(fileInfo, "key", type, 0));
        entry.getFieldInfos().add(createSubFiled(fileInfo, "value", type, 1));
        fileInfo.saveStruct(entry);
        fieldInfo.setFirstType(new FieldType(entry.getName(), TType.STRUCT));
      } else {
        fieldInfo.setFirstType(firstType);
        fieldInfo.setSecondType(createSubType(fileInfo, name, type, 1));
      }
      fieldInfo.setContainerType(new FieldType(clz.getSimpleName(), TType.MAP));
    } else if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
      fieldInfo.setFirstType(new FieldType("bool", TType.BOOL));
    } else if (clz.equals(byte.class) || clz.equals(Byte.class)) {
      fieldInfo.setFirstType(new FieldType("int", TType.BYTE));
    } else if (clz.equals(short.class) || clz.equals(Short.class)) {
      fieldInfo.setFirstType(new FieldType("int", TType.I16));
    } else if (clz.equals(int.class) || clz.equals(Integer.class)) {
      fieldInfo.setFirstType(new FieldType("int", TType.I32));
    } else if (clz.equals(long.class) || clz.equals(Long.class)) {
      fieldInfo.setFirstType(new FieldType("long", TType.I64));
    } else if (clz.equals(double.class) || clz.equals(Double.class)) {
      fieldInfo.setFirstType(new FieldType("double", TType.DOUBLE));
    } else if (clz.equals(String.class)) {
      fieldInfo.setFirstType(new FieldType("string", TType.STRING));
    } else if (clz.equals(byte[].class)) {
      fieldInfo.setFirstType(new FieldType("bytes", TType.STRUCT));
    } else if (Enum.class.isAssignableFrom(clz)) {
      fieldInfo.setFirstType(new FieldType(createReference(fileInfo, clz).getName(), TType.ENUM));
    } else {
      fieldInfo.setFirstType(new FieldType(createReference(fileInfo, clz).getName(), TType.STRUCT));
    }
    return fieldInfo;
  }

  /**
   * Create a {@link FieldType} for sub type.
   * @param fileInfo    parent proto file info
   * @param name        sub field name
   * @param genericType a {@link ParameterizedType} to supplier sub type
   * @param index       sub type index
   * @return  {@link FieldType}
   * @throws Exception  exception
   */
  private FieldType createSubType(FileInfo fileInfo, String name, Type genericType, int index) throws Exception {
    // get sub type
    Type subType = ((ParameterizedType) genericType).getActualTypeArguments()[index];
    // get sub class
    Class subClass = subType instanceof Class ? (Class) subType : (Class) ((ParameterizedType) subType).getRawType();
    // create field info
    FieldInfo info = createSubFiled(fileInfo, name, genericType, index);
    // repeated struct
    if (info.isRepeated()) {
      StructInfo subList = new StructInfo(firstUpper(name));
      subList.getFieldInfos().add(info);
      subList.setRefInfo(subList.getName(), subClass);
      fileInfo.saveStruct(subList);
      return new FieldType(subList.getName(), TType.STRUCT);
    }
    return info.getFirstType();
  }

  /**
   * Create a {@link FieldInfo} for sub type.
   * @param fileInfo    parent proto file info
   * @param name        sub field name
   * @param genericType a {@link ParameterizedType} to supplier sub type
   * @param index       sub type index
   * @return  {@link FieldInfo}
   * @throws Exception  exception
   */
  private FieldInfo createSubFiled(FileInfo fileInfo, String name, Type genericType, int index) throws Exception {
    // get sub type
    Type subType = ((ParameterizedType) genericType).getActualTypeArguments()[index];
    // get sub class
    Class subClass = subType instanceof Class ? (Class) subType : (Class) ((ParameterizedType) subType).getRawType();
    // get sub generic type
    Type subGenericType = subType instanceof Class ? null : subType;
    // create field info
    return createFieldInfo(fileInfo, name, subClass, subGenericType);
  }

  /**
   * Create a {@link StructInfo} for reference class.
   * @param fileInfo  parent proto file info
   * @param refClass  the reference class
   * @return  {@link StructInfo}
   */
  StructInfo createReference(FileInfo fileInfo, Class refClass) {
    // byte[]
    if (refClass.equals(byte[].class)) {
      return new StructInfo("bytes");
    }
    // get reference class name
    String name = refClass.getSimpleName();
    // get reference file info
    FileInfo refFile = StructInfo.safeGetFileInfo(name);
    // try to get the reference struct info
    StructInfo ref = refFile.getStructInfos().get(name);
    // same name and not same package
    if (ref != null && !ref.getRefThriftType().equals(refClass.getName())) {
      ref = null;
    }
    // get proto name
    String protoName = ref == null ? Loader.formatPackage(refClass) : refFile.getName();
    // create reference struct if not found
    if (ref == null) {
      // get reference file info
      refFile = apply(protoName);
      if (refFile.getOriginNames().contains(name)) { // exist struct
        int index = 0;
        while (!refFile.getStructInfos().containsKey(name + (index == 0 ? "" : index))) {
          index++;
        }
        name += (index == 0 ? "" : index);
        ref = refFile.getStructInfos().get(name);
      } else { // create new struct
        ref = Enum.class.isAssignableFrom(refClass) ?
                enumLoader.createEnum(refClass, refFile) : createStruct(refFile, refClass);
      }
    }
    // process dependency
    if (!fileInfo.getName().equals(refFile.getName())) {
      fileInfo.getImports().add(protoName);
      refFile.getDependencyMe().add(fileInfo.getName());
      fileInfo.getDependencies().put(protoName, ref.getName());
    }
    return ref;
  }

  @Override
  public FileInfo apply(String protoName) {
    return enumLoader.fileInfos.apply(protoName);
  }
}
