package com.yinxiang.utils.thrift.grpc.infos;

/**
 * Record a rpc infos of gRPC.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class RpcInfo {
  /** The rpc name. */
  private final String rpc;
  /** The request of rpc. */
  private StructInfo request;
  /** The response of rpc. */
  private StructInfo response;

  public RpcInfo(String rpc) {
    this.rpc = rpc;
  }

  /** @see #request */
  public StructInfo getRequest() {
    return request;
  }

  /** @see #request */
  public void setRequest(StructInfo request) {
    this.request = request;
  }

  /** @see #response */
  public StructInfo getResponse() {
    return response;
  }

  /** @see #response */
  public void setResponse(StructInfo response) {
    this.response = response;
  }

  /** @see #rpc */
  public String getRpc() {
    return rpc;
  }
}
