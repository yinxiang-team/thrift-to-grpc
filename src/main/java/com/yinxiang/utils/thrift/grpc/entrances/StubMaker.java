package com.yinxiang.utils.thrift.grpc.entrances;

import com.google.common.collect.Maps;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperations;
import com.yinxiang.utils.thrift.grpc.operations.generators.ThriftStubGenerator;

/**
 * The maker of thrift stub.
 * <p>
 *   When have many thrift service, can use this maker to generate the stub interface.
 * </p>
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class StubMaker extends Maker {
  /** The base path of gRPC service module. */
  private final String path;
  /** The package of thrift stub. */
  private final String pkg;

  private StubMaker(String thriftPackage, String scans, String drops, String path, String pkg) {
    super(thriftPackage, scans, drops);
    this.path = path;
    this.pkg = pkg;
  }

  public static void main(String[] args) throws Exception {
    new StubMaker(args[0], args[1], args[2], args[3], args[4]).execute(Maps.newLinkedHashMap());
  }

  @Override
  protected void addFilesOperations(FilesOperations filesOperations) {
    filesOperations.addFilesOperation(new ThriftStubGenerator(path, pkg));
  }
}
