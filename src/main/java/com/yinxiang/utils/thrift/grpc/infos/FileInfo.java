package com.yinxiang.utils.thrift.grpc.infos;

import com.google.common.collect.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Record a proto file infos.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class FileInfo {
  /** The name of file. */
  private String name;
  /** The map of this proto's all struct, key is {@link StructInfo#getName()}. */
  private Map<String, StructInfo> structInfos = Maps.newLinkedHashMap();
  /** The list of this proto's all services. */
  private List<ServiceInfo> serviceInfos = Lists.newLinkedList();
  /** The set of this proto's all import. */
  private Set<String> imports = Sets.newHashSet();
  /** The proto version, default 3. */
  private byte version = 3;

  /**
   * The set of all struct origin name.
   * <p>
   *   In a proto file some struct will have same name, so with create struct the name will replace by a alias name,
   *   so need record the origin name to tell struct need replace it's name when has same name.
   * </p>
   */
  private transient Set<String> originNames = Sets.newHashSet();
  /** The set of import this proto's proto name. */
  private transient Set<String> dependencyMe = Sets.newHashSet();
  /** The map of dependencies, key is struct name, value is a set of dependencies proto name. */
  private transient SetMultimap<String, String> dependencies = HashMultimap.create();

  public FileInfo(String name) {
    this.name = name;
  }

  /** @see #name */
  public String getName() {
    return name;
  }

  /** @see #structInfos */
  public Map<String, StructInfo> getStructInfos() {
    return structInfos;
  }

  /** @see #serviceInfos */
  public List<ServiceInfo> getServiceInfos() {
    return serviceInfos;
  }

  /** @see #imports */
  public Set<String> getImports() {
    return imports;
  }

  /** @see #imports */
  public void setImports(Set<String> imports) {
    this.imports = imports;
  }

  /** @see #originNames */
  public Set<String> getOriginNames() {
    return originNames;
  }

  /** @see #dependencyMe */
  public Set<String> getDependencyMe() {
    return dependencyMe;
  }

  /** @see #dependencies */
  public SetMultimap<String, String> getDependencies() {
    return dependencies;
  }

  /** @see #version */
  public byte getVersion() {
    return version;
  }

  /** @see #version */
  public void setVersion(byte version) {
    this.version = version;
  }

  /**
   * Record a struct.
   * @param structInfo  struct
   */
  public void saveStruct(StructInfo structInfo) {
    structInfos.put(structInfo.getName(), structInfo);
    originNames.add(structInfo.getOriginName());
    structInfo.setFileInfo(this);
  }
}
