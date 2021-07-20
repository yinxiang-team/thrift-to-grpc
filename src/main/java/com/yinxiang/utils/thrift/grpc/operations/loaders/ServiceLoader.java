package com.yinxiang.utils.thrift.grpc.operations.loaders;

import com.google.common.base.Strings;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.yinxiang.utils.thrift.grpc.TType;
import com.yinxiang.utils.thrift.grpc.infos.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.Collator;
import java.util.*;
import java.util.function.Function;

import static com.yinxiang.utils.thrift.grpc.utils.StringUtils.firstUpper;
import static com.yinxiang.utils.thrift.grpc.infos.StructInfo.EMPTY;
import static com.yinxiang.utils.thrift.grpc.operations.FilesOperation.*;

/**
 * The thrift service loader.
 * <p>
 *   1. 0 arg will create a {@link StructInfo} named <code>Empty</code> which does not have anyone field.
 *   2. 1 arg struct will create a gRPC mapping message.
 *   3. 1 arg primitive type will create a gRPC message which has only one field to record this arg.
 *   4. More than 1 args will create a gRPC message which has same amount field to record these args.
 *   5. <code>void</code> method is same with 1.
 *   6. Has a struct return type method is same with 2.
 *   7. Has a primitive type return type method is same with 3.
 * </p>
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see Loader
 */
public class ServiceLoader implements Loader {
  /** The response default field name. */
  private static final String RETURN_DATA = "data";

  private final StructLoader structLoader;
  /** The map for alias of struct, key is alias, value is origin name. */
  private final Map<String, String> alias = Maps.newHashMap();
  /** The map for all kinds of args count method counter, ket is args count, value is a set of method name. */
  private final Multimap<Integer, String> counter = LinkedListMultimap.create();

  /** The count of services. */
  private int serviceCount;
  /** The count of rpc. */
  private int rpcCount;

  public ServiceLoader(StructLoader structLoader) {
    this.structLoader = structLoader;
  }

  @Override
  public void execute(Class<?> clz) throws Exception {
    FileInfo fileInfo = structLoader.apply(Loader.formatPackage(clz));
    FileInfo requestFile = getFileInfo(fileInfo, REQUEST_FILE);
    FileInfo responseFile = getFileInfo(fileInfo, RESPONSE_FILE);
    // create ServiceInfo
    ServiceInfo serviceInfo = new ServiceInfo(formatInterfaceName(clz.getSimpleName()));
    serviceInfo.setClz(clz);
    fileInfo.getServiceInfos().add(serviceInfo);
    // get classes of args
    Map<String, Class> argsClasses = Maps.newHashMap();
    for (Class argsClass : Class.forName(formatInterfaceName(clz.getName())).getClasses()) {
      argsClasses.put(argsClass.getSimpleName(), argsClass);
    }
    // generate all methods
    for (Method method : getRpcList(clz)) {
      // create RpcInfo
      RpcInfo rpcInfo = new RpcInfo(method.getName());
      // request
      rpcInfo.setRequest(createRequest(fileInfo, requestFile, argsClasses.get(method.getName() + "_args"), method));
      fileInfo.getDependencies().put(REQUEST_FILE, rpcInfo.getRequest().getName());
      // response
      rpcInfo.setResponse(createResponse(fileInfo, responseFile, method));
      fileInfo.getDependencies().put(RESPONSE_FILE, rpcInfo.getResponse().getName());
      // cache
      serviceInfo.getRpcInfos().add(rpcInfo);
      rpcCount++;
    }
    // counter
    serviceCount++;
  }

  @Override
  public void outputLog() {
    counter.asMap().forEach((count, list) -> System.out.println(count + " args RPC: " + list.size()));
    System.out.println("Create service: " + serviceCount);
    System.out.println("Create rpc: " + rpcCount);
  }

  /**
   * Format the interface name.
   * @param name  interface name
   * @return  code
   */
  private static String formatInterfaceName(String name) {
    return name.substring(0, name.length() - 5);
  }

  /**
   * Get a list of rpc from a thrift interface class.
   * @param clz a thrift interface class
   * @return  a list of rpc
   */
  private List<Method> getRpcList(Class<?> clz) {
    List<Method> rpcList = Arrays.asList(clz.getMethods());
    rpcList.sort((m1, m2) -> Collator.getInstance().compare(m1.getName(), m2.getName()));
    return rpcList;
  }

  /**
   * Get a file info and record import.
   * @param fileInfo  a file info which need record import
   * @param fileName  file name
   * @return  a file
   */
  private FileInfo getFileInfo(FileInfo fileInfo, String fileName) {
    fileInfo.getImports().add(fileName);
    FileInfo ret = structLoader.apply(fileName);
    ret.getImports().add(COMMON_FILE);
    return ret;
  }

  /**
   * Create response struct.
   * @param fileInfo    parent file info
   * @param argFileInfo arg file info
   * @param method      rpc method
   * @return  response struct
   * @throws Exception  exception
   */
  private StructInfo createResponse(FileInfo fileInfo, FileInfo argFileInfo, Method method) throws Exception {
    Class<?> returnType = method.getReturnType();
    // no response
    if (returnType == null || returnType.equals(void.class)) {
      fileInfo.getImports().add(COMMON_FILE);
      return EMPTY;
    }
    // create response
    Type type = method.getGenericReturnType();
    String structName = makeRequestName(returnType, type, "_Response");
    return createStructForOneType(fileInfo, argFileInfo, structName, RETURN_DATA, returnType, type);
  }

  /**
   * Create request struct.
   * @param fileInfo    parent file info
   * @param argFileInfo arg file info
   * @param argsClass   can get args class
   * @param method      rpc method
   * @return  request struct
   * @throws Exception  exception
   */
  private StructInfo createRequest(FileInfo fileInfo, FileInfo argFileInfo, Class<?> argsClass, Method method)
          throws Exception {
    Parameter[] parameters = method.getParameters();
    counter.put(parameters.length, method.getName());
    // do not have parameter
    if (parameters.length == 0) {
      fileInfo.getImports().add(COMMON_FILE);
      return EMPTY;
    }
    // one parameter
    if (parameters.length == 1) {
      String fieldName = getFieldNames(argsClass).get(0);
      Type type = parameters[0].getParameterizedType();
      String structName = makeRequestName(parameters[0].getType(), type, firstUpper(fieldName + "_Request"));
      return createStructForOneType(fileInfo, argFileInfo, structName, fieldName, parameters[0].getType(), type);
    }
    // more then one parameter
    return createRequest(method.getName(), argFileInfo, argsClass, parameters);
  }

  /**
   * Create request struct.
   * @param methodName  method name
   * @param argFileInfo arg file info
   * @param argsClass   can get args class
   * @param parameters  rpc method's parameters
   * @return  request struct
   * @throws Exception  exception
   */
  private StructInfo createRequest(String methodName, FileInfo argFileInfo, Class<?> argsClass, Parameter[] parameters)
          throws Exception {
    List<FieldInfo> fieldInfos = Lists.newLinkedList();
    String aliasName = collectParameters(argsClass, parameters, (parameter, fieldName) -> {
      Type parameterizedType = parameter.getParameterizedType();
      // add a field to list
      fieldInfos.add(structLoader.createFieldInfo(argFileInfo, fieldName, parameter.getType(), parameterizedType));
    });
    // return if exists
    if (alias.containsKey(aliasName)) {
      return argFileInfo.getStructInfos().get(alias.get(aliasName));
    }
    // make name
    String requestName = firstUpper(methodName + "_Request");
    // create
    StructInfo ret = Loader.createStruct(argFileInfo, requestName, false);
    ret.getFieldInfos().addAll(fieldInfos);
    // record alias
    if (!Strings.isNullOrEmpty(aliasName)) {
      alias.put(aliasName, ret.getName());
    }
    return ret;
  }

  /**
   * Collect parameters info of a request and make it's alias.
   * @param argsClass   can get args class
   * @param parameters  rpc method's parameters
   * @param consumer    parameter info consumer
   * @return  the request alias
   * @throws Exception
   */
  private String collectParameters(Class<?> argsClass, Parameter[] parameters, ParameterConsumer consumer)
          throws Exception {
    StringBuilder aliasBuilder = new StringBuilder();
    // get a map of field names, key is parameter's index.
    Map<Integer, String> fieldNames = getFieldNames(argsClass);
    boolean needFindExist = true;
    for (int i = 0;i < parameters.length;i++) {
      Parameter parameter = parameters[i];
      Type parameterizedType = parameter.getParameterizedType();
      // get field name for this parameter
      String fieldName = fieldNames.get(i);
      if (needFindExist) {
        // append alias and check is need exist same name struct
        needFindExist = appendParameter(aliasBuilder, parameter.getType(), parameterizedType, fieldName);
      }
      consumer.accept(parameter, fieldName);
    }
    aliasBuilder.setLength(needFindExist ? aliasBuilder.length() - 1 : 0);
    return aliasBuilder.toString().toLowerCase();
  }

  /** Consume parameter info. */
  protected interface ParameterConsumer {
    /**
     * Consume parameter info.
     * @param parameter a parameter
     * @param name      parameter's real name
     * @throws Exception
     */
    void accept(Parameter parameter, String name) throws Exception;
  }

  /**
   * Append parameter to a alias {@link StringBuilder}.
   * @param aliasBuilder  a alias {@link StringBuilder}
   * @param clz           parameter type
   * @param type          parameter parameterized type
   * @param fieldName     parameter name
   * @return  true if need find exist else false
   */
  private static boolean appendParameter(StringBuilder aliasBuilder, Class clz, Type type, String fieldName) {
    aliasBuilder.append(clz.getName());
    // process ParameterizedType
    if (type instanceof ParameterizedType) {
      Type[] types = ((ParameterizedType) type).getActualTypeArguments();
      aliasBuilder.append('<');
      for (Type actualType : types) {
        if (actualType instanceof Class) {
          aliasBuilder.append(((Class) actualType).getName()).append(',');
        } else { // nested container
          return false;
        }
      }
      aliasBuilder.setLength(aliasBuilder.length() - 1);
      aliasBuilder.append('>');
    }
    // append field name
    aliasBuilder.append(" ").append(fieldName).append(',');
    return true;
  }

  /**
   * Get the field names from a args class.
   * @param argsClass a args class
   * @return  map of field names, key is field index
   */
  private SortedMap<Integer, String> getFieldNames(Class argsClass) {
    SortedMap<Integer, String> fieldNames = Maps.newTreeMap();
    Loader.forEachThriftFields(argsClass, (index, field) -> fieldNames.put(index++, field.getName()));
    return fieldNames;
  }

  /**
   * Adapt class.
   * @param clz class
   * @return  result
   */
  private static Class adaptClass(Class clz) {
    return Enum.class.isAssignableFrom(clz) ? int.class : clz;
  }

  /**
   * Make request name.
   * @param clz   class
   * @param type  {@link ParameterizedType}
   * @param name  base name
   * @return  request name
   */
  private static String makeRequestName(Class clz, Type type, String name) {
    String ret = firstUpper(name);
    if (clz.equals(List.class) || clz.equals(Set.class)) { // list or set
      Type subType = ((ParameterizedType) type).getActualTypeArguments()[0];
      ret = adaptName(subType, subClass -> makeRequestName(subClass, subType, "")) + "List" + ret;
    } else if (clz.equals(Map.class)) { // map
      Type[] actualTypes = ((ParameterizedType) type).getActualTypeArguments();
      String key = adaptName(actualTypes[0], subClass -> makeRequestName(subClass, actualTypes[0], ""));
      String value = adaptName(actualTypes[1], subClass -> makeRequestName(subClass, actualTypes[1], ""));
      ret = key + value + "Map" + ret;
    } else { // others
      ret = Loader.formatPackage(clz) + clz.getSimpleName() + ret;
    }
    return firstUpper(ret).replace("[]", "s");
  }

  /**
   * Adapt a {@link Type} name.
   * @param type      a {@link Type}
   * @param function  process if type is a {@link ParameterizedType}
   * @return  adapt name
   */
  private static String adaptName(Type type, Function<Class, String> function) {
    return firstUpper(type instanceof Class ?
            Loader.formatPackage((Class) type) + adaptClass(((Class) type)).getSimpleName() :
            function.apply((Class) ((ParameterizedType) type).getRawType()));
  }

  /**
   * Create a {@link StructInfo} for one type.
   * <p>
   *   This method use to create 1 arg request or not void response
   * </p>
   * @param fileInfo    parent file info
   * @param argFileInfo arg file info
   * @param structName  struct name
   * @param fieldName   the only one field name
   * @param clz         the only one field class
   * @param type        the only one field type
   * @return  {@link StructInfo}
   * @throws Exception  exception
   */
  private StructInfo createStructForOneType(
          FileInfo fileInfo,
          FileInfo argFileInfo,
          String structName,
          String fieldName,
          Class<?> clz,
          Type type
  ) throws Exception {
    FieldInfo fieldInfo = structLoader.createFieldInfo(argFileInfo, fieldName, clz, type);
    // struct
    if (fieldInfo.isStruct() && !clz.equals(byte[].class)) {
      return structLoader.createReference(fileInfo, clz);
    }
    // process alias
    if (needCheckAlias(fieldInfo, type)) {
      // return if exists
      if (alias.containsKey(structName.toLowerCase())) {
        return argFileInfo.getStructInfos().get(alias.get(structName.toLowerCase()));
      }
      alias.put(structName.toLowerCase(), structName);
    }
    // create a struct for primitive type
    StructInfo structInfo = Loader.createStruct(argFileInfo, structName, false);
    structInfo.getFieldInfos().add(fieldInfo);
    return structInfo;
  }

  /**
   * Determine is need check alias.
   * @param fieldInfo field info
   * @param type      field type
   * @return  true if need check alias else false
   */
  private static boolean needCheckAlias(FieldInfo fieldInfo, Type type) {
    FieldType containerType = fieldInfo.getContainerType(), firstType = fieldInfo.getFirstType();
    if (containerType == null) {
      return firstType.thriftType != TType.ENUM;
    } else {
      return containerType.thriftType != TType.MAP
              && isNormalType(((ParameterizedType) type).getActualTypeArguments()[0]);
    }
  }

  /**
   * Check is a normal type.
   * @param type  type
   * @return  is a normal type
   */
  private static boolean isNormalType(Type type) {
    return type instanceof Class && !Enum.class.isAssignableFrom((Class) type);
  }
}
