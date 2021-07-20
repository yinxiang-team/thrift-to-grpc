package com.yinxiang.utils.thrift.grpc.infos;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Record a service infos of gRPC.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class ServiceInfo {
  /** The list of rpc. */
  private List<RpcInfo> rpcInfos = Lists.newLinkedList();
  /** Service name. */
  private String name;
  /** The thrift interface. */
  private Class clz;

  public ServiceInfo(String name) {
    this.name = name;
  }

  /** @see #rpcInfos */
  public List<RpcInfo> getRpcInfos() {
    return rpcInfos;
  }

  /** @see #name */
  public String getName() {
    return name;
  }

  /** @see #clz */
  public Class getClz() {
    return clz;
  }

  /** @see #clz */
  public void setClz(Class clz) {
    this.clz = clz;
  }
}
