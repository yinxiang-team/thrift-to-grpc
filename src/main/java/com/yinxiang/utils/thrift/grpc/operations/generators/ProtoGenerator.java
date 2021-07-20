package com.yinxiang.utils.thrift.grpc.operations.generators;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.yinxiang.utils.thrift.grpc.operations.Operation;
import com.yinxiang.utils.thrift.grpc.infos.*;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperation;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The gRPC proto generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see FilesOperation
 */
public class ProtoGenerator implements FilesOperation {
  /** The base path of proto. */
  private final String path;
  /** The package of gRPC proto. */
  private final String pkg;

  /** The map of {@link FileInfo}, key is file name. */
  private Map<String, FileInfo> fileInfos;

  public ProtoGenerator(String path, String pkg) {
    this.path = path;
    this.pkg = pkg;
  }

  @Override
  public void execute(Map<String, FileInfo> fileInfos) throws Exception {
    // check circle dependencies
    fileInfos.forEach((k, v) -> v.getImports().forEach(im -> check(fileInfos, im, k)));
    // make output path
    String outputPath = path + pkg.substring(pkg.indexOf('/') + 1);
    // mkdir
    System.out.print(new File(outputPath).mkdirs() ? "[mkdir]" : "");
    // generate all proto
    for (Map.Entry<String, FileInfo> entry : fileInfos.entrySet()) {
      File file = new File(outputPath, entry.getKey() + ".proto");
      if (!file.exists()) {
        System.out.print(file.createNewFile() ? "[create]" : "[override]");
      }
      // output content
      try (FileWriter writer = new FileWriter(file)) {
        writer.append(generateFile(entry.getValue()));
        System.out.println("generator proto: " + file.getName());
      }
    }
    this.fileInfos = fileInfos;
  }

  @Override
  public void outputLog() {
    // create a proto full name list
    List<String> protoList = fileInfos.keySet()
            .stream()
            .map(n -> "\"" + pkg + "/" + n + ".proto\"")
            .collect(Collectors.toList());
    // log all proto name
    System.out.println("[" + Joiner.on(',').join(protoList) + "]");
  }

  /**
   * Check circle dependencies.
   * @param fileInfos {@link #fileInfos}
   * @param im        import
   * @param name      proto name
   */
  private void check(Map<String, FileInfo> fileInfos, String im, String name) {
    checkArgument(!fileInfos.get(im).getImports().contains(name), name + " and " + im);
  }

  /**
   * Generate proto content.
   * @param fileInfo  proto file info
   * @return  proto content
   */
  private String generateFile(FileInfo fileInfo) {
    StringBuilder builder = new StringBuilder("syntax = \"proto");
    builder.append(fileInfo.getVersion()).append("\";\n\n");
    builder.append("import \"google/api/annotations.proto\";\n");
    String importPkg = pkg.substring(pkg.indexOf('/') + 1) + "/";
    fileInfo.getImports().forEach(i -> builder.append("import \"").append(importPkg).append(i).append(".proto\";\n"));
    builder.append("\noption java_multiple_files = true;\n");
    appendPackage(builder);
    String protoPkg = Operation.formatPkg(importPkg.substring(0, importPkg.length() - 1));
    builder.append("package ").append(protoPkg).append(";\n\n");
    builder.append("// message count: ").append(fileInfo.getStructInfos().size()).append(";\n\n");
    fileInfo.getServiceInfos().forEach(serviceInfo -> generateService(builder, serviceInfo));
    fileInfo.getStructInfos().forEach((name, structInfo) -> generateStruct(builder, structInfo));
    builder.setLength(builder.length() - 1);
    return builder.toString();
  }

  /**
   * Append package to a {@link StringBuilder} of proto content.
   * @param builder a {@link StringBuilder} of proto content
   */
  private void appendPackage(StringBuilder builder) {
    builder.append("option java_package = \"").append(Operation.formatPkg(pkg)).append("\";\n\n");
  }

  /**
   * Generate service content.
   * @param builder     a {@link StringBuilder} of proto content
   * @param serviceInfo service info
   */
  private void generateService(StringBuilder builder, ServiceInfo serviceInfo) {
    builder.append("service ").append(serviceInfo.getName()).append(" {\n");
    serviceInfo.getRpcInfos().forEach(rpcInfo -> generateRpc(builder, rpcInfo));
    builder.setLength(builder.length() - 1);
    builder.append("}\n\n");
  }

  /**
   * Generate rpc method.
   * @param builder a {@link StringBuilder} of proto content
   * @param rpcInfo rpc info
   */
  private void generateRpc(StringBuilder builder, RpcInfo rpcInfo) {
    builder.append("\trpc ")
            .append(rpcInfo.getRpc()) // name
            .append(" (")
            .append(rpcInfo.getRequest().getName()) // request
            .append(") returns (")
            .append(rpcInfo.getResponse().getName()) // response
            .append(") {\n\t\toption (google.api.http) = {\n\t\t\tpost: \"/")
            .append(rpcInfo.getRpc())
            .append("\"\n\t\t};\n\t}\n\n");
  }

  /**
   * Generate struct.
   * @param builder     a {@link StringBuilder} of proto content
   * @param structInfo  struct info
   */
  private void generateStruct(StringBuilder builder, StructInfo structInfo) {
    // skip byte[] and enum(thrift enum can be negative and repeated name with other enum in same proto)
    if (structInfo.getName().equals("bytes") || structInfo.isEnum()) {
      return;
    }
    // begin
    builder.append("message ").append(structInfo.getName()).append(" {\n");
    // body
    for (int i = 0;i < structInfo.getFieldInfos().size();i++) {
      FieldInfo fieldInfo = structInfo.getFieldInfos().get(i);
      builder.append("\t");
      // proto2 has default value
      String defaultValue = fieldInfo.getDefaultValue();
      if (!Strings.isNullOrEmpty(defaultValue)) {
        builder.append("optional ");
      }
      // field
      boolean isMap = fieldInfo.isGrpcMap();
      builder.append(fieldInfo.isRepeated() && !isMap ? "repeated " : "")
              .append(isMap ? "map<" : "")
              .append(getProtoType(fieldInfo.getFirstType().type))
              .append(isMap ? ", " + getProtoType(fieldInfo.getSecondType().type) + ">" : "")
              .append(" ")
              .append(fieldInfo.getName())
              .append(" = ")
              .append(i + 1);
      // proto2 has default value
      if (!Strings.isNullOrEmpty(defaultValue)) {
        builder.append(" [default = ").append(defaultValue).append("]");
      }
      builder.append(";\n");
    }
    // end
    builder.append("}\n\n");
  }

  /**
   * Get proto type from class name.
   * @param className class name
   * @return  proto type
   */
  private static String getProtoType(String className) {
    switch (className) {
      case "String":
      case "string":
        return "string";
      case "Boolean":
      case "boolean":
      case "bool":
        return "bool";
      case "Integer":
      case "int":
        return "int32";
      case "Long":
      case "long":
        return "int64";
      case "Double":
        return "double";
      case "Float":
        return "float";
      case "byte[]":
      case "bytes":
        return "bytes";
      default:
        StructInfo structInfo = StructInfo.getStructInfo(className);
        return structInfo != null && structInfo.isEnum() ? "int32" : FilesOperation.getLastDot(className);
    }
  }
}
