package com.yinxiang.utils.thrift.grpc.entrances;

import com.google.common.collect.Maps;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperations;
import com.yinxiang.utils.thrift.grpc.operations.generators.*;

/**
 * The maker of gRPC service.
 * <p>
 *   The gRPC service need a thrift stub to apply thrift client.
 * </p>
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class ServiceMaker extends Maker {
  private final String grpcPkg;
  /** The base path of marshaller module. */
  private final String marshallerPath;
  /** The package of thrift marshaller. */
  private final String thriftMarshallerPkg;
  /** The package of gRPC marshaller. */
  private final String grpcMarshallerPkg;
  /** The package of thrift stub. */
  private final String stubPkg;
  /** The base path of gRPC service module. */
  private final String servicePath;
  /** The package of gRPC service. */
  private final String servicePkg;

  private ServiceMaker(
          String thriftPackage,
          String scans,
          String drops,
          String grpcPkg,
          String marshallerPath,
          String thriftMarshallerPkg,
          String grpcMarshallerPkg,
          String stubPkg,
          String servicePath,
          String servicePkg
  ) {
    super(thriftPackage, scans, drops);
    this.grpcPkg = grpcPkg;
    this.marshallerPath = marshallerPath;
    this.thriftMarshallerPkg = thriftMarshallerPkg;
    this.grpcMarshallerPkg = grpcMarshallerPkg;
    this.stubPkg = stubPkg;
    this.servicePath = servicePath;
    this.servicePkg = servicePkg;
  }

  @Override
  protected void addFilesOperations(FilesOperations filesOperations) {
    AbstractMarshallerGenerator thriftMarshallerCreator =
            new ThriftMarshallerGenerator(marshallerPath, thriftMarshallerPkg, grpcPkg);
    GrpcMarshallerGenerator grpcMarshallerCreator =
            new GrpcMarshallerGenerator(marshallerPath, grpcMarshallerPkg, grpcPkg);
    filesOperations.addFilesOperation(
            new GrpcServiceGenerator(servicePath, servicePkg, stubPkg, thriftMarshallerCreator, grpcMarshallerCreator));
  }

  public static void main(String[] args) throws Exception {
    new ServiceMaker(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9])
            .execute(Maps.newLinkedHashMap());
  }
}
