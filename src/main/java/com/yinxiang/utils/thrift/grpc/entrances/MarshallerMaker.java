package com.yinxiang.utils.thrift.grpc.entrances;

import com.google.common.collect.Maps;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperations;
import com.yinxiang.utils.thrift.grpc.operations.generators.*;

/**
 * The maker of marshaller.
 * <p>
 *   The {@link ThriftMarshallerGenerator} will generate some gRPC to thrift method.
 *   The {@link GrpcMarshallerGenerator} will generate some thrift to gRPC method.
 * </p>
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class MarshallerMaker extends Maker {
  /** The package of gRPC. */
  private final String grpcPkg;
  /** The base path of marshaller module. */
  private final String marshallerPath;
  /** The package of thrift marshaller. */
  private final String thriftMarshallerPkg;
  /** The package of gRPC marshaller. */
  private final String grpcMarshallerPkg;

  private MarshallerMaker(
          String thriftPackage,
          String scans,
          String drops,
          String grpcPkg,
          String marshallerPath,
          String thriftMarshallerPkg,
          String grpcMarshallerPkg
  ) {
    super(thriftPackage, scans, drops);
    this.grpcPkg = grpcPkg;
    this.marshallerPath = marshallerPath;
    this.thriftMarshallerPkg = thriftMarshallerPkg;
    this.grpcMarshallerPkg = grpcMarshallerPkg;
  }

  @Override
  protected void addFilesOperations(FilesOperations filesOperations) {
    filesOperations.addFilesOperation(new ThriftMarshallerGenerator(marshallerPath, thriftMarshallerPkg, grpcPkg));
    filesOperations.addFilesOperation(new GrpcMarshallerGenerator(marshallerPath, grpcMarshallerPkg, grpcPkg));
  }

  public static void main(String[] args) throws Exception {
    new MarshallerMaker(args[0], args[1], args[2], args[3], args[4], args[5], args[6]).execute(Maps.newLinkedHashMap());
  }
}
