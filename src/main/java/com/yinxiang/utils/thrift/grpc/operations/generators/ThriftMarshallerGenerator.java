package com.yinxiang.utils.thrift.grpc.operations.generators;

import com.google.common.collect.Sets;
import com.yinxiang.utils.thrift.grpc.TType;
import com.yinxiang.utils.thrift.grpc.infos.FieldInfo;
import com.yinxiang.utils.thrift.grpc.infos.FieldType;
import com.yinxiang.utils.thrift.grpc.infos.StructInfo;

import java.util.Optional;

import static com.yinxiang.utils.thrift.grpc.operations.generators.CodeGenerator.*;
import static com.yinxiang.utils.thrift.grpc.utils.StringUtils.firstUpper;
import static com.yinxiang.utils.thrift.grpc.infos.StructInfo.getStructInfo;

/**
 * The thrift marshaller generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see AbstractMarshallerGenerator
 */
public class ThriftMarshallerGenerator extends AbstractMarshallerGenerator {
  public ThriftMarshallerGenerator(String path, String marshallerPkg, String pkg) {
    super(path, marshallerPkg, pkg);
  }

  @Override
  protected String getMarshallerType() {
    return "Thrift";
  }

  @Override
  protected String makeReturnType(StructInfo struct, String javaPackage) {
    return struct.getRefThriftType();
  }

  @Override
  protected String makeRequestType(StructInfo struct, String javaPackage) {
    return javaPackage + "." + struct.getName();
  }

  @Override
  protected String adaptGetterName(FieldInfo fieldInfo, String getterName) {
    getterName = adaptGrpcName(getterName);
    switch (Optional.ofNullable(fieldInfo.getContainerType()).map(t -> t.thriftType).orElse(TType.VOID)) {
      case TType.MAP:
        if (fieldInfo.getSecondType() != null) { // map to map
          return getterName + "Map()";
        }
      case TType.LIST:
      case TType.SET:
        return getterName + "List()";
      default:
        return getterName + "()";
    }
  }

  @Override
  protected String makeGetter(FieldType firstType, String name) {
    switch (firstType.thriftType) {
      case TType.BYTE:
        return "checkByte(" + name + ")";
      case TType.I16:
        return "checkShort(" + name + ")";
      case TType.ENUM:
        return "checkEnum(" + makeEnum(getStructInfo(firstType.type), name) + ")";
      default:
        switch (firstType.type) {
          case "bool": case "int": case "double": case "long":
            return name;
          case "string":
            return getStringGetter(name);
          case "bytes":
            return makeBytesGetter(name);
          default:
            return makeToType(name, firstType.type);
        }
    }
  }

  /**
   * Get string type getter.
   * @param getterName  getter name
   * @return  string type getter
   */
  protected String getStringGetter(String getterName) {
    return getterName;
  }

  @Override
  protected String makeSetter(FieldInfo fieldInfo, String name) {
    return "set" + name;
  }

  @Override
  protected String makeReturnCreatorCode(String returnType) {
    return returnType + " ret = new " + returnType + "()";
  }

  @Override
  protected String makeEnum(StructInfo structInfo, String getterName) {
    return structInfo.getRefThriftType() + makeMethod("findByValue", getterName);
  }

  @Override
  protected String makeMapListCode(String mapGetter, String type, FieldInfo key, FieldInfo value, int deep) {
    return mapGetter + ".stream()" + makeToMapCode(toMapEntry(key, GET_KEY, deep), toMapEntry(value, GET_VALUE, deep));
  }

  @Override
  protected String makeMapCode(String getter, StructInfo structInfo, int deep) {
    FieldInfo fieldInfo = structInfo.getFieldInfos().get(0);
    FieldType keyType = fieldInfo.getFirstType(), valueType = fieldInfo.getSecondType();
    return makeMap(getter + ".get" + structInfo.getName() + "Map()", keyType, valueType, deep + 1);
  }

  @Override
  protected String makeListOrSetCode(String getter, StructInfo structInfo, String collection) {
    FieldInfo fieldInfo = structInfo.getFieldInfos().get(0);
    String fieldGetter = ".get" + firstUpper(fieldInfo.getName() + "List()");
    return makeListOrSet(getter + fieldGetter, fieldInfo.getFirstType().type, collection);
  }

  /**
   * Make map entry transform code.
   * @param fieldInfo {@link FieldType}
   * @param getter    getterName
   * @param deep      container deep
   * @return  code
   */
  private String toMapEntry(FieldInfo fieldInfo, String getter, int deep) {
    String entryName = "e" + deep, lambda = entryName + " -> ";
    FieldType type = fieldInfo.getFirstType(), secondType = fieldInfo.getSecondType();
    if (secondType != null) {
      return lambda + entryName + "." + makeMap(getter + "Map()", type, secondType, deep + 1);
    }
    String getterName = entryName + "." + getter + "()", normalBegin = lambda + getterName;
    if (type.type.equals("bytes")) {
      return makeBytesGetter(normalBegin);
    }
    StructInfo struct = getStructInfo(type.type);
    if (struct == null) {
      return normalBegin;
    }
    return lambda + (type.thriftType == TType.ENUM ? makeEnum(struct, getterName) : makeToType(getterName, type.type));
  }

  @Override
  protected String makeListOrSet(String getterName, String type, String collectorName) {
    switch (type) {
      case "bool": case "int": case "double": case "long": case "string":
        return collectorName.equals("List") ? getterName : Sets.class.getName() + ".newHashSet(" + getterName + ")";
      case "bytes":
        return makeBytesGetter(getterName);
      default:
        StructInfo struct = getStructInfo(type);
        String map = struct.isEnum() ? makeEnum(struct, null) : makeToType(type);
        return getterName + ".stream().map(" + map + ")" + makeToCode(collectorName);
    }
  }

  @Override
  protected String makeBytesGetter(String getterName) {
    return getterName + ".toByteArray()";
  }
}
