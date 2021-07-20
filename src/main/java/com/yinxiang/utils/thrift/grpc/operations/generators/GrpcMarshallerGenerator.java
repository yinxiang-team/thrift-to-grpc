package com.yinxiang.utils.thrift.grpc.operations.generators;

import com.google.common.collect.Lists;
import com.yinxiang.utils.thrift.grpc.TType;
import com.yinxiang.utils.thrift.grpc.infos.FieldInfo;
import com.yinxiang.utils.thrift.grpc.infos.FieldType;
import com.yinxiang.utils.thrift.grpc.infos.StructInfo;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.LineCode;

import java.util.List;
import java.util.Optional;

import static com.yinxiang.utils.thrift.grpc.operations.generators.CodeGenerator.*;
import static com.yinxiang.utils.thrift.grpc.operations.Operation.formatPkg;
import static com.yinxiang.utils.thrift.grpc.utils.StringUtils.firstUpper;
import static com.yinxiang.utils.thrift.grpc.infos.StructInfo.getStructInfo;

/**
 * The gRPC marshaller generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see AbstractMarshallerGenerator
 */
public class GrpcMarshallerGenerator extends AbstractMarshallerGenerator {
  public GrpcMarshallerGenerator(String path, String marshallerPkg, String pkg) {
    super(path, marshallerPkg, pkg);
  }

  @Override
  protected String getMarshallerType() {
    return "Grpc";
  }

  @Override
  protected List<LineCode> createLineCode(FieldInfo fieldInfo, int tab) {
    String fieldName = firstUpper(fieldInfo.getName());
    List<LineCode> ret = Lists.newLinkedList();
    List<LineCode> list = super.createLineCode(fieldInfo, tab);
    // add check 'isSet' code, because gRPC will throw NullPointerException when set a null value
    ret.add(new LineCode(tab, "if (arg.isSet" + fieldName + "()) {"));
    ret.add(new LineCode(list.get(0).tab + 1, list.get(0).content));
    ret.add(new LineCode(tab, "}"));
    return ret;
  }

  @Override
  protected String makeReturnType(StructInfo struct, String javaPackage) {
    return javaPackage + "." + struct.getName();
  }

  @Override
  protected String makeRequestType(StructInfo struct, String javaPackage) {
    return struct.getRefThriftType();
  }

  @Override
  protected String makeGetter(FieldType firstType, String name) {
    // enum
    if (firstType.thriftType == TType.ENUM) {
      return makeEnum(null, name);
    }
    // others
    switch (firstType.type) {
      case "bool":
        return name.replaceFirst("arg.get", "arg.is");
      case "int": case "double": case "long": case "string":
        return name;
      case "bytes":
        return makeBytesGetter(name);
      default:
        return makeToType(name, firstType.type);
    }
  }

  @Override
  protected String adaptGetterName(FieldInfo fieldInfo, String name) {
    return name.endsWith(")") ? name : name + "()";
  }

  @Override
  protected String makeMapListCode(String mapGetter, String type, FieldInfo key, FieldInfo value, int deep) {
    String e = "e" + deep, getValue = e + ".getValue()", valueSetter;
    // get first/second type from value field
    FieldType valueFirstType = value.getFirstType(), valueSecondType = value.getSecondType();
    String firstType = valueFirstType.type;
    // make valueSetter code
    if (value.getContainerType() == null) { // not container
      StructInfo struct = getStructInfo(firstType);
      String v = struct == null ? getValue : struct.isEnum() ? makeEnum(struct, getValue) : makeToType(getValue, struct);
      valueSetter = ".setValue(" + v + ")";
    } else { // container
      if (value.getContainerType().thriftType == TType.MAP) { // map
        valueSetter = ".putAllValue(" + makeMap(getValue, valueFirstType, valueSecondType, deep + 1) + ")";
      } else { // list or set
        valueSetter = ".addAllValue(" + makeListOrSet(getValue, firstType) + ")";
      }
    }
    // make code
    String keySetter = ".setKey(" + makeGetter(key, e + ".getKey", deep + 1) + ")";
    // TODO key is List/Set/Map
    String mapCode = e + " -> " + makeBuilder(type) + keySetter + valueSetter + BUILD;
    return mapGetter + MAP_STREAM + ".map(" + mapCode + ")" + TO_LIST;
  }

  /**
   * Make the full name builder code.
   * @param name  builder name
   * @return  the full name builder code
   */
  String makeBuilder(String name) {
    return formatPkg(grpcPkg) + "." + name + ".newBuilder()";
  }

  @Override
  protected String makeMapCode(String getter, StructInfo structInfo, int deep) {
    FieldInfo fieldInfo = structInfo.getFieldInfos().get(0);
    String map = makeMap(getter, fieldInfo.getFirstType(), fieldInfo.getSecondType(), deep + 1);
    String putAll = ".putAll" + firstUpper(fieldInfo.getName());
    return makeBuilder(structInfo.getName()) + putAll + "(" + map + ")" + BUILD;
  }

  @Override
  protected String makeListOrSetCode(String getter, StructInfo structInfo, String collection) {
    FieldInfo fieldInfo = structInfo.getFieldInfos().get(0);
    String listOrSet = makeListOrSet(getter, fieldInfo.getFirstType().type);
    String addAll = ".addAll" + firstUpper(fieldInfo.getName());
    return makeBuilder(structInfo.getName()) + addAll + "(" + listOrSet + ")" + BUILD;
  }

  @Override
  protected String makeEnum(StructInfo structInfo, String getterName) {
    return getterName + ".getValue()";
  }

  @Override
  protected String makeSetter(FieldInfo fieldInfo, String name) {
    name = adaptGrpcName(name);
    switch (Optional.ofNullable(fieldInfo.getContainerType()).map(t -> t.thriftType).orElse(TType.VOID)) {
      case TType.MAP:
        return (fieldInfo.getSecondType() == null ? "addAll" : "putAll") + name;
      case TType.LIST:
      case TType.SET:
        return "addAll" + name;
      default:
        return "set" + name;
    }
  }

  @Override
  protected String makeReturnCreatorCode(String returnType) {
    return returnType + ".Builder ret = " + returnType + ".newBuilder()";
  }

  @Override
  protected String makeReturnCode() {
    return BUILD;
  }

  /**
   * Make list or set Code
   * @param getterName    getter name
   * @param type          field type
   * @return  code
   */
  private String makeListOrSet(String getterName, String type) {
    return makeListOrSet(getterName, type, "");
  }

  @Override
  protected String makeListOrSet(String getterName, String type, String collectorName) {
    switch (type) {
      case "bool":
        return getterName.replaceFirst("get", "is");
      case "int": case "double": case "long": case "string":
        return getterName;
      case "bytes":
        return makeBytesGetter(getterName);
      default:
        StructInfo struct = getStructInfo(type);
        String map = struct.isEnum() ? struct.getRefThriftType() + "::getValue" : makeToType(type);
        return getterName + ".stream().map(" + map + ")" + TO_LIST;
    }
  }

  @Override
  protected String makeBytesGetter(String getterName) {
    return "com.google.protobuf.ByteString.copyFrom(" + getterName + ")";
  }
}
