package com.yinxiang.utils.thrift.grpc.entrances;

import com.google.common.collect.Maps;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperations;
import com.yinxiang.utils.thrift.grpc.operations.generators.ProtoGenerator;

/**
 * The maker of gRPC proto.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class ProtoMaker extends Maker {
  /** The base path of gRPC module. */
  private final String path;
  /** The package of gRPC proto. */
  private final String pkg;

  private ProtoMaker(String thriftPackage, String scans, String drops, String path, String pkg) {
    super(thriftPackage, scans, drops);
    this.path = path;
    this.pkg = pkg;
  }

  public static void main(String[] args) throws Exception {
    new ProtoMaker(args[0], args[1], args[2], args[3], args[4]).execute(Maps.newLinkedHashMap());
  }

  @Override
  protected void addFilesOperations(FilesOperations filesOperations) {
    filesOperations.addFilesOperation(new ProtoGenerator(path, pkg));
  }
}
