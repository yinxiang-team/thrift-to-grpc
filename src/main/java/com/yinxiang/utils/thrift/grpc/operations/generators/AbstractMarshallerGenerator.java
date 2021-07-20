package com.yinxiang.utils.thrift.grpc.operations.generators;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.yinxiang.utils.thrift.grpc.TType;
import com.yinxiang.utils.thrift.grpc.infos.FieldType;
import com.yinxiang.utils.thrift.grpc.operations.Operation;
import com.yinxiang.utils.thrift.grpc.infos.FieldInfo;
import com.yinxiang.utils.thrift.grpc.infos.FileInfo;
import com.yinxiang.utils.thrift.grpc.infos.StructInfo;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperation;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.ClassCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.FieldCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.LineCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.MethodCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.java.ClassGenerator;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.yinxiang.utils.thrift.grpc.infos.StructInfo.getStructInfo;
import static com.yinxiang.utils.thrift.grpc.operations.generators.CodeGenerator.makeToMapCode;
import static com.yinxiang.utils.thrift.grpc.utils.StringUtils.firstUpper;
import static com.yinxiang.utils.thrift.grpc.infos.StructInfo.safeGetFileInfo;

/**
 * Base marshaller generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see FilesOperation
 */
public abstract class AbstractMarshallerGenerator implements FilesOperation {
  /** Modifier: private static final. */
  private static final String PSF_MOD = "private static final";
  /** Type: int. */
  private static final String INT_TYPE = "int";
  /** Type: Range. */
  private static final String RANGE_TYPE = "Range<Integer>";
  /** The class generator. */
  private static final ClassGenerator CLASS_GENERATOR = new ClassGenerator();

  /** Modifier: private final. */
  static final String PF_MOD = "private final";
  /** Modifier: void. */
  static final String V_MOD = "void";
  /** Method: getKey. */
  static final String GET_KEY = "getKey";
  /** Method: getValue. */
  static final String GET_VALUE = "getValue";
  /** CODE: .entrySet().stream(). */
  static final String MAP_STREAM = ".entrySet().stream()";
  /** The list of base imports. */
  static final ImmutableList<String> IMPORTS = ImmutableList.of(
          "com.google.common.base.Preconditions",
          "com.google.common.collect.Range"
  );
  /** The list of base fields. */
  static final ImmutableList<FieldCode> FIELD_CODES = ImmutableList.of(
          new FieldCode(TAB, PSF_MOD, INT_TYPE, "MIN_BYTE = Byte.MIN_VALUE"),
          new FieldCode(TAB, PSF_MOD, INT_TYPE, "MAX_BYTE = Byte.MAX_VALUE"),
          new FieldCode(TAB, PSF_MOD, INT_TYPE, "MIN_SHORT = Short.MIN_VALUE"),
          new FieldCode(TAB, PSF_MOD, INT_TYPE, "MAX_SHORT = Short.MAX_VALUE"),
          new FieldCode(TAB, PSF_MOD, RANGE_TYPE, "BYTE_RANGE = Range.closed(MIN_BYTE, MAX_BYTE)"),
          new FieldCode(TAB, PSF_MOD, RANGE_TYPE, "SHORT_RANGE = Range.closed(MIN_SHORT, MAX_SHORT)")
  );

  /** CODE: .build(). */
  public static final String BUILD = ".build()";

  /** The base path of marshaller module. */
  protected final String path;

  /** The package of marshaller. */
  final String marshallerPkg;
  /** The package of gRPC. */
  final String grpcPkg;

  protected AbstractMarshallerGenerator(String path, String marshallerPkg, String grpcPkg) {
    this.path = path;
    this.marshallerPkg = marshallerPkg;
    this.grpcPkg = grpcPkg;
  }

  @Override
  public void execute(Map<String, FileInfo> fileInfos) {
    fileInfos.forEach((fileName, fileInfo) -> {
      ClassCode classCode = createClassCode(fileName, fileInfo);
      String content = CLASS_GENERATOR.generate(new StringBuilder(), classCode).toString();
      FilesOperation.createFile(path + marshallerPkg, classCode.name, content);
    });
  }

  /**
   * Create a {@link ClassCode} from a proto file.
   * @param fileName  name of proto file
   * @param fileInfo  proto file
   * @return  {@link ClassCode}
   */
  private ClassCode createClassCode(String fileName, FileInfo fileInfo) {
    String className = fileName + getMarshallerType() + "Marshaller";
    ClassCode classCode = new ClassCode(
            "public final",
            "class",
            className,
            Operation.formatPkg(marshallerPkg)
    );
    // imports
    classCode.imports.addAll(IMPORTS);
    // fields
    classCode.fieldCodes.addAll(FIELD_CODES);
    // methods
    classCode.methodCodes.add(new MethodCode(TAB, "public", "", className));
    classCode.methodCodes.addAll(transform(fileInfo.getStructInfos().values()));
    createCheckers(classCode.methodCodes::add);
    return classCode;
  }

  /**
   * Transform a {@link StructInfo} collection to a list of {@link MethodCode}.
   * @param source  a {@link StructInfo} collection
   * @return  list of {@link MethodCode}
   */
  private List<MethodCode> transform(Collection<StructInfo> source) {
    return source.stream().filter(this::needMarshaller).map(this::createMethodCode).collect(Collectors.toList());
  }

  /**
   * Create a {@link MethodCode} of a marshaller method for a struct.
   * @param struct  a struct
   * @return  {@link MethodCode}
   */
  private MethodCode createMethodCode(StructInfo struct) {
    String javaPackage = Operation.formatPkg(grpcPkg);
    // get return type and request type
    String returnType = makeReturnType(struct, javaPackage), argType = makeRequestType(struct, javaPackage);
    // create MethodCode
    MethodCode methodCode = new MethodCode(TAB, "public static", returnType, "to" + getMarshallerType());
    // set parameter
    methodCode.parameters.add(new FieldCode(argType, "arg"));
    // body
    methodCode.lineCodes.add(new LineCode(TAB + 1, makeReturnCreatorCode(returnType) + ";"));
    struct.getFieldInfos().forEach(fieldInfo -> methodCode.lineCodes.addAll(createLineCode(fieldInfo, TAB + 1)));
    methodCode.lineCodes.add(new LineCode(TAB + 1, "return ret" + makeReturnCode() + ";"));
    return methodCode;
  }

  /**
   * Create a list of {@link LineCode} from a {@link FieldInfo}.
   * @param fieldInfo field
   * @param tab       tab count
   * @return  list of {@link LineCode}
   */
  protected List<LineCode> createLineCode(FieldInfo fieldInfo, int tab) {
    String fieldName = firstUpper(fieldInfo.getName()), getter = "arg.get" + fieldName;
    String content = "ret." + makeSetter(fieldInfo, fieldName) + "(" + makeGetter(fieldInfo, getter) + ");";
    return Lists.newArrayList(new LineCode(tab, content));
  }

  /**
   * Check the struct is it necessary to generate a marshaller method.
   * @param struct  struct
   * @return  true if is it necessary to generate a marshaller method else false
   */
  protected boolean needMarshaller(StructInfo struct) {
    return !struct.isEnum() && Optional.ofNullable(struct.getRefInfo())
            .map(FieldInfo::getRefThriftType)
            .map(className -> !Map.class.getName().equals(className)
                    && !List.class.getName().equals(className)
                    && !Set.class.getName().equals(className)
            )
            .orElse(false);
  }

  /**
   * @return  type of marshaller
   */
  protected abstract String getMarshallerType();

  /**
   * Make the return type of a struct's marshaller method.
   * @param struct      struct
   * @param javaPackage the gRPC package
   * @return  the return type
   */
  protected abstract String makeReturnType(StructInfo struct, String javaPackage);

  /**
   * Make the request type of a struct's marshaller method.
   * @param struct      struct
   * @param javaPackage the gRPC package
   * @return  the request type
   */
  protected abstract String makeRequestType(StructInfo struct, String javaPackage);

  /**
   * Make the getter code for a {@link FieldInfo}.
   * @param fieldInfo field
   * @param name      base name
   * @param deep      container deep
   * @return  the getter code
   */
  String makeGetter(FieldInfo fieldInfo, String name, int deep) {
    name = adaptGetterName(fieldInfo, name);
    FieldType firstType = fieldInfo.getFirstType();
    String type = firstType.type;
    if (fieldInfo.isRepeated()) {
      byte containerType = fieldInfo.getContainerType().thriftType;
      return containerType == TType.MAP ?
              makeMap(name, firstType, fieldInfo.getSecondType(), deep) :
              makeListOrSet(name, type, containerType == TType.LIST ? "List" : "Set");
    }
    return makeGetter(firstType, name);
  }

  /**
   * Make the getter code for a {@link FieldInfo}.
   * @param firstType field type
   * @param name      name
   * @return  the getter code
   */
  protected abstract String makeGetter(FieldType firstType, String name);

  /**
   * Make map transform code from key type and value type.
   * @param mapGetter getterName
   * @param keyType   the first type of field
   * @param valueType the second type of field
   * @param deep      container deep
   * @return  code
   */
  String makeMap(String mapGetter, FieldType keyType, FieldType valueType, int deep) {
    // list
    if (valueType == null) {
      StructInfo structInfo = getStructInfo(keyType.type);
      FieldInfo key = getFieldByName(structInfo, "key");
      FieldInfo value = getFieldByName(structInfo, "value");
      return makeMapListCode(mapGetter, keyType.type, key, value, deep);
    }
    // map
    return mapGetter + MAP_STREAM +
            makeToMapCode(toMapEntry(keyType, GET_KEY, deep), toMapEntry(valueType, GET_VALUE, deep));
  }

  /**
   * Make map transform code.
   * @param type        {@link FieldType}
   * @param getterName  getterName
   * @param deep        container deep
   * @return  code
   */
  private String toMapEntry(FieldType type, String getterName, int deep) {
    String lambda = "e" + deep + " -> ", getter = "e" + deep + "." + getterName + "()";
    // byte[]
    if (type.type.equals("bytes")) {
      return lambda + makeBytesGetter(getter);
    }
    StructInfo structInfo = getStructInfo(type.type);
    // primitive type
    if (structInfo == null) {
      return "java.util.Map.Entry::" + getterName;
    }
    // enum
    if (structInfo.isEnum()) {
      return lambda + makeEnum(structInfo, getter);
    }
    return toMapEntry(getter, structInfo, deep);
  }

  /**
   * Make map entry transform code.
   * @param getterName  getterName
   * @param structInfo  struct
   * @param deep        container deep
   * @return  code
   */
  private String toMapEntry(String getterName, StructInfo structInfo, int deep) {
    String lambda = "e" + deep + " -> ", refClassName = structInfo.getRefThriftType();
    // map
    if (refClassName.equals(Map.class.getName())) {
      return lambda + makeMapCode(getterName, structInfo, deep + 1);
    }
    // list or set
    boolean isList = refClassName.equals(List.class.getName());
    if (isList || refClassName.equals(Set.class.getName())) {
      return lambda + makeListOrSetCode(getterName, structInfo, isList ? "List" : "Set");
    }
    // others
    return lambda + makeToType(getterName, structInfo);
  }

  /**
   * Make to marshaller type code.
   * @param getterName  getter name
   * @param structInfo  struct
   * @return  code
   */
  String makeToType(String getterName, StructInfo structInfo) {
    return makeToType(getterName, structInfo.getName());
  }

  /**
   * Make to marshaller type code.
   * @param getterName  getter name
   * @param structName  struct name
   * @return  code
   */
  String makeToType(String getterName, String structName) {
    return makeMarshallerName(structName) + makeMethod("to" + getMarshallerType(), getterName);
  }

  /**
   * Make to marshaller type code.
   * @param structName  struct name
   * @return  code
   */
  String makeToType(String structName) {
    return makeToType(null, structName);
  }

  /**
   * Make method code.
   * @param method  method name
   * @param arg     method arg
   * @return  code
   */
  static String makeMethod(String method, String arg) {
    return Strings.isNullOrEmpty(arg) ? ("::" + method) : ("." + method + "(" + arg + ")");
  }

  /**
   * Make map transform code.
   * @param getter      getterName
   * @param structInfo  struct
   * @param deep        container deep
   * @return  code
   */
  protected abstract String makeMapCode(String getter, StructInfo structInfo, int deep);

  /**
   * Make list or set transform code.
   * @param getter      getterName
   * @param structInfo  struct
   * @param collection  collection name
   * @return  code
   */
  protected abstract String makeListOrSetCode(String getter, StructInfo structInfo, String collection);

  /**
   * Make enum getter code.
   * @param structInfo  struct
   * @param getterName  getter name
   * @return  code
   */
  protected abstract String makeEnum(StructInfo structInfo, String getterName);

  /**
   * Make byte[] getter code.
   * @param getterName  getter name
   * @return  code
   */
  protected abstract String makeBytesGetter(String getterName);

  /**
   * Make map to list or list to map Code.
   * @param mapGetter getter name
   * @param type      field type
   * @param key       key field
   * @param value     value field
   * @param deep      container deep
   * @return  code
   */
  protected abstract String makeMapListCode(String mapGetter, String type, FieldInfo key, FieldInfo value, int deep);

  /**
   * Make list or set Code.
   * @param getterName    getter name
   * @param type          field type
   * @param collectorName collector name
   * @return  code
   */
  protected abstract String makeListOrSet(String getterName, String type, String collectorName);

  /**
   * Adapt the getter name.
   * @param fieldInfo getter from field
   * @param name      getter
   * @return  getter name
   */
  protected abstract String adaptGetterName(FieldInfo fieldInfo, String name);

  /**
   * Make the getter name for a {@link FieldInfo}.
   * @param fieldInfo field
   * @param name      base name
   * @return  the getter name
   */
  String makeGetter(FieldInfo fieldInfo, String name) {
    return makeGetter(fieldInfo, name, 0);
  }

  /**
   * Make the setter name for a {@link FieldInfo}.
   * @param fieldInfo field
   * @param name      base name
   * @return  the setter name
   */
  protected abstract String makeSetter(FieldInfo fieldInfo, String name);

  /**
   * Make create the return code for a return type.
   * @param returnType  a return type
   * @return  return code
   */
  protected abstract String makeReturnCreatorCode(String returnType);

  /**
   * Make return code.
   * @return  return code
   */
  protected String makeReturnCode() { return ""; }

  /**
   * Adapt the gRPC name rule.
   * @param name  a name
   * @return  adapt result
   */
  static String adaptGrpcName(String name) {
    boolean nextUpper = false;
    StringBuilder builder = new StringBuilder();
    for (int i = 0;i < name.length();i++) {
      char c = name.charAt(i);
      if (nextUpper) {
        if (c >= 97 && c <= 122) {
          c -= 32;
        }
        nextUpper = false;
      } else if (c >= 48 && c <= 57) {
        nextUpper = true;
      }
      builder.append(c);
    }
    return builder.toString();
  }

  /**
   * Make the marshaller name.
   * @param structName  name of struct
   * @return  the marshaller name
   */
  String makeMarshallerName(String structName) {
    return safeGetFileInfo(structName).getName() + getMarshallerType() + "Marshaller";
  }

  /**
   * Get a {@link FieldInfo} by name from a {@link StructInfo}.
   * @param structInfo  struct
   * @param name        field name
   * @return  {@link FieldInfo}
   */
  private static FieldInfo getFieldByName(StructInfo structInfo, String name) {
    Predicate<FieldInfo> filter = field -> field.getName().equals(name);
    return checkNotNull(structInfo.getFieldInfos().stream().filter(filter).findFirst().orElse(null));
  }

  @Override
  public void outputLog() {}

  /**
   * Create checker method.
   * @param consumer  checker consumer
   */
  static void createCheckers(Consumer<MethodCode> consumer) {
    // checkByte
    MethodCode checkByte = new MethodCode(1, "private static", "byte", "checkByte");
    checkByte.parameters.add(new FieldCode("int", "value"));
    checkByte.lineCodes.add(new LineCode(2, "Preconditions.checkArgument(BYTE_RANGE.contains(value));"));
    checkByte.lineCodes.add(new LineCode(2, "return (byte) value;"));
    consumer.accept(checkByte);
    // checkShort
    MethodCode checkShort = new MethodCode(1, "private static", "short", "checkShort");
    checkShort.parameters.add(new FieldCode("int", "value"));
    checkShort.lineCodes.add(new LineCode(2, "Preconditions.checkArgument(SHORT_RANGE.contains(value));"));
    checkShort.lineCodes.add(new LineCode(2, "return (short) value;"));
    consumer.accept(checkShort);
    // checkEnum
    MethodCode checkEnum = new MethodCode(1, "private static <T>", "T", "checkEnum");
    checkEnum.parameters.add(new FieldCode("T", "e"));
    checkEnum.lineCodes.add(new LineCode(2, "return Preconditions.checkNotNull(e);"));
    consumer.accept(checkEnum);
  }
}
