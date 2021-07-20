package com.yinxiang.utils.thrift.grpc.operations.generators;

import com.google.common.base.Joiner;
import com.yinxiang.utils.thrift.grpc.operations.Operation;
import com.yinxiang.utils.thrift.grpc.infos.*;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperation;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.ClassCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.FieldCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.LineCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.MethodCode;
import com.yinxiang.utils.thrift.grpc.operations.generators.code.java.ClassGenerator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.yinxiang.utils.thrift.grpc.operations.generators.AbstractMarshallerGenerator.*;
import static com.yinxiang.utils.thrift.grpc.utils.StringUtils.firstUpper;

/**
 * The gRPC service generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see FilesOperation
 */
public class GrpcServiceGenerator implements FilesOperation {
  /** The default class generator. */
  private static final ClassGenerator CLASS_GENERATOR = new ClassGenerator();
  /** The base path of service module. */
  private final String servicePath;
  /** The package of gRPC service. */
  private final String servicePkg;
  /** The package of thrift stub. */
  private final String stubPkg;
  private final AbstractMarshallerGenerator thriftMarshallerCreator;
  private final GrpcMarshallerGenerator grpcMarshallerCreator;

  public GrpcServiceGenerator(
          String servicePath, String servicePkg,
          String stubPkg,
          AbstractMarshallerGenerator thriftMarshallerCreator,
          GrpcMarshallerGenerator grpcMarshallerCreator
  ) {
    this.servicePath = servicePath;
    this.servicePkg = servicePkg;
    this.stubPkg = stubPkg;
    this.thriftMarshallerCreator = thriftMarshallerCreator;
    this.grpcMarshallerCreator = grpcMarshallerCreator;
  }

  @Override
  public void execute(Map<String, FileInfo> fileInfos) {
    fileInfos.forEach((fileName, fileInfo) -> fileInfo.getServiceInfos().forEach(serviceInfo -> {
      ClassCode classCode = createClassCode(serviceInfo);
      String content = CLASS_GENERATOR.generate(new StringBuilder(), classCode).toString();
      FilesOperation.createFile(servicePath + servicePkg, classCode.name, content);
    }));
  }

  /**
   * Create a {@link ClassCode} from a {@link ServiceInfo}.
   * @param serviceInfo service info
   * @return  {@link ClassCode}
   */
  protected ClassCode createClassCode(ServiceInfo serviceInfo) {
    String javaPackage = Operation.formatPkg(thriftMarshallerCreator.grpcPkg);
    String serviceName = serviceInfo.getName(), className = serviceName + "GrpcService";
    // create
    ClassCode classCode = new ClassCode(
            "public final",
            "class",
            className,
            Operation.formatPkg(servicePkg),
            Joiner.on('.').join(javaPackage, serviceName + "Grpc", serviceName + "ImplBase")
    );
    // imports
    classCode.imports.add(Operation.formatPkg(stubPkg) + ".ThriftStub");
    classCode.imports.add(Operation.formatPkg(thriftMarshallerCreator.marshallerPkg) + ".*");
    classCode.imports.add(Operation.formatPkg(grpcMarshallerCreator.marshallerPkg) + ".*");
    classCode.imports.addAll(IMPORTS);
    // fields
    classCode.fieldCodes.addAll(FIELD_CODES);
    classCode.fieldCodes.add(new FieldCode(TAB, PF_MOD, "ThriftStub", "stub"));
    String log = "log = org.slf4j.LoggerFactory.getLogger(" + className + ".class)";
    classCode.fieldCodes.add(new FieldCode(TAB, PF_MOD, "org.slf4j.Logger", log));
    // constructor
    MethodCode constructor = new MethodCode(TAB, "public", "", className);
    constructor.parameters.add(new FieldCode("ThriftStub", "stub"));
    constructor.lineCodes.add(new LineCode(TAB + 1, "this.stub = stub;"));
    classCode.methodCodes.add(constructor);
    // methods
    classCode.methodCodes.addAll(
            serviceInfo.getRpcInfos()
                    .stream()
                    .map(rpcInfo -> createMethodCode(rpcInfo, serviceName, TAB))
                    .collect(Collectors.toList())
    );
    createCheckers(classCode.methodCodes::add);
    return classCode;
  }

  /**
   * Create a {@link MethodCode} from a {@link RpcInfo}.
   * @param rpcInfo     rpc info
   * @param serviceName parent service name
   * @param tab         tab count
   * @return  {@link MethodCode}
   */
  protected MethodCode createMethodCode(RpcInfo rpcInfo, String serviceName, int tab) {
    String javaPackage = Operation.formatPkg(thriftMarshallerCreator.grpcPkg);
    // make name
    String methodName = rpcInfo.getRpc();
    // get request and response
    StructInfo request = rpcInfo.getRequest(), response = rpcInfo.getResponse();
    // create
    MethodCode methodCode = new MethodCode(tab, "public", V_MOD, adaptGrpcName(methodName));
    methodCode.annotations.add("Override");
    // parameters
    methodCode.parameters.add(new FieldCode(javaPackage + "." + request.getName(), "r"));
    String type = "io.grpc.stub.StreamObserver<" + javaPackage + "." + response.getName() + ">";
    methodCode.parameters.add(new FieldCode(type, "o"));
    // body
    methodCode.lineCodes.add(new LineCode(tab + 1, "try {"));
    String call = "stub.get" + serviceName + "()." + methodName + "(" + makeParameters(request) + ")";
    methodCode.lineCodes.add(new LineCode(tab + 2, makeReturn(response, call) + ";"));
    methodCode.lineCodes.add(new LineCode(tab + 2, "o.onCompleted();"));
    methodCode.lineCodes.add(new LineCode(tab + 1, "} catch (Exception e) {"));
    methodCode.lineCodes.add(new LineCode(tab + 2, "log.warn(\"\", e);"));
    methodCode.lineCodes.add(new LineCode(tab + 2, "o.onError(e);"));
    methodCode.lineCodes.add(new LineCode(tab + 1, "}"));
    return methodCode;
  }

  @Override
  public void outputLog() {}

  /**
   * Make return code.
   * @param response  struct for return
   * @param call      base code
   * @return  code
   */
  protected String makeReturn(StructInfo response, String call) {
    // void
    if (response.equals(StructInfo.EMPTY)) {
      return call + ";\n\t\t\to.onNext(" + makeBuilder(StructInfo.EMPTY) + BUILD + ")";
    }
    // primitive type
    FieldInfo ref = response.getRefInfo();
    if (ref == null) {
      FieldInfo fieldInfo = response.getFieldInfos().get(0);
      String set = (fieldInfo.isRepeated() ? fieldInfo.isGrpcMap() ? ".putAll" : ".addAll" : ".set");
      return "o.onNext(" + makeBuilder(response) + set + "Data(" + makeResponse(fieldInfo, call) + ")" + BUILD + ")";
    }
    // struct
    return "o.onNext(" + grpcMarshallerCreator.makeToType(call, response) + ")";
  }

  /**
   * Make gRPC marshaller name.
   * @param structInfo  struct info
   * @return  gRPC marshaller name
   */
  protected String makeGrpcMarshallerName(StructInfo structInfo, String getter) {
    return grpcMarshallerCreator.makeToType(getter, structInfo.getName());
  }

  /**
   * Make parameters code.
   * @param request struct info
   * @return  code
   */
  protected String makeParameters(StructInfo request) {
    List<FieldInfo> fields = request.getFieldInfos();
    // none parameter
    if (fields.size() == 0) {
      return "";
    }
    // more than one parameter or a primitive type parameter
    if (request.getRefInfo() == null) {
      return Joiner.on(',').join(fields.stream().map(this::makeRequest).collect(Collectors.toList()));
    }
    // a struct parameter
    return thriftMarshallerCreator.makeToType("r", request);
  }

  /**
   * Make request code.
   * @param fieldInfo filed info of request
   * @return  code
   */
  private String makeRequest(FieldInfo fieldInfo) {
    return thriftMarshallerCreator.makeGetter(fieldInfo, "r.get" + firstUpper(fieldInfo.getName()));
  }

  /**
   * Make response code.
   * @param fieldInfo filed info of response
   * @param call      base code
   * @return  code
   */
  protected String makeResponse(FieldInfo fieldInfo, String call) {
    return grpcMarshallerCreator.makeGetter(fieldInfo, call);
  }

  /**
   * Make request code.
   * @param structInfo  struct info
   * @return  code
   */
  protected String makeBuilder(StructInfo structInfo) {
    return grpcMarshallerCreator.makeBuilder(structInfo.getName());
  }

  /**
   * Adapt gRPC name.
   * @param name  name
   * @return  name
   */
  private static String adaptGrpcName(String name) {
    boolean nextUpper = false;
    StringBuilder builder = new StringBuilder();
    for (int i = 0;i < name.length();i++) {
      char c = name.charAt(i);
      if (nextUpper) {
        if (c >= 97 && c <= 122) {
          c -= 32;
        }
        nextUpper = false;
      } else if (c == 95) {
        nextUpper = true;
        continue;
      }
      builder.append(c);
    }
    return builder.toString();
  }
}
