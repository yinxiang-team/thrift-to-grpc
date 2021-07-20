package com.yinxiang.utils.thrift.grpc.operations.generators;

import com.yinxiang.utils.thrift.grpc.operations.Operation;
import com.yinxiang.utils.thrift.grpc.infos.FileInfo;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperation;

import java.util.Map;

/**
 * The thrift stub generator.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see FilesOperation
 */
public class ThriftStubGenerator implements FilesOperation {
  /** The base path of thrift stub. */
  private final String path;
  /** The package of thrift stub. */
  private final String pkg;

  public ThriftStubGenerator(String path, String pkg) {
    this.path = path;
    this.pkg = pkg;
  }

  @Override
  public void execute(Map<String, FileInfo> fileInfos) {
    StringBuilder builder = new StringBuilder("package ");
    builder.append(Operation.formatPkg(pkg)).append(";\n\n");
    builder.append("public interface ThriftStub {\n");
    // methods
    fileInfos.forEach((fileName, fileInfo) -> fileInfo.getServiceInfos().forEach(serviceInfo -> {
      builder.append("\t")
              .append(serviceInfo.getClz().getName())
              .append(" get")
              .append(serviceInfo.getName())
              .append("() throws Exception;\n\n");
    }));
    // end
    builder.setLength(builder.length() - 1);
    builder.append("}");
    FilesOperation.createFile(path + pkg, "ThriftStub", builder.toString());
  }

  @Override
  public void outputLog() {}
}
