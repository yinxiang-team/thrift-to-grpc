package com.yinxiang.utils.thrift.grpc.operations;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.yinxiang.utils.thrift.grpc.infos.*;
import com.yinxiang.utils.thrift.grpc.operations.loaders.EnumLoader;
import com.yinxiang.utils.thrift.grpc.operations.loaders.ServiceLoader;
import com.yinxiang.utils.thrift.grpc.operations.loaders.StructLoader;
import com.yinxiang.utils.thrift.grpc.utils.ScanUtils;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.yinxiang.utils.thrift.grpc.infos.StructInfo.EMPTY;

/**
 * The thrift classes info collector.
 * <p>
 *   Thrift can delimit a import b and b import a, but gRPC can not.
 *   The resolver will change b import a's struct to 'common.proto'.
 * </p>
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see FilesOperation
 */
public class ThriftClassesCollector implements FilesOperation {
  /** The package of thrift. */
  private final String packageName;
  /** The set of need scan class names. */
  private final Set<String> scans;
  /** The set of need skip class names. */
  private final Set<String> skips;

  private EnumLoader enumLoader;
  private StructLoader structLoader;
  private Operation<Class<?>> rpcLoader;

  /** The count of interfaces. */
  private int interfaceCount;
  /** The count of classes. */
  private int classCount;
  /** The count of enums. */
  private int enumCount;

  public ThriftClassesCollector(String packageName, String scans, String skips) {
    this.packageName = packageName;
    this.scans = isNullOrEmpty(scans) ? Sets.newHashSet() : Sets.newHashSet(Splitter.on(',').split(scans.trim()));
    this.skips = isNullOrEmpty(skips) ? Sets.newHashSet() : Sets.newHashSet(Splitter.on(',').split(skips.trim()));
  }

  @Override
  public void execute(Map<String, FileInfo> fileInfos) throws Exception {
    // create common file info
    FileInfo fileInfo = fileInfos.computeIfAbsent(COMMON_FILE, FileInfo::new);
    fileInfo.setImports(ImmutableSet.of());
    fileInfo.getStructInfos().put(EMPTY.getName(), EMPTY);
    EMPTY.setFileInfo(fileInfo);
    // create loaders
    enumLoader = new EnumLoader(name -> fileInfos.computeIfAbsent(name, FileInfo::new));
    structLoader = new StructLoader(enumLoader);
    rpcLoader = new ServiceLoader(structLoader);
    // load classes
    for (Class<?> clz : ScanUtils.scanClass(packageName, this::filterClass)) {
      if (clz.isInterface()) {
        rpcLoader.execute(clz);
        interfaceCount++;
      } else if (clz.getInterfaces().length > 1) {
        structLoader.execute(clz);
        classCount++;
      } else if (clz.getInterfaces().length > 0) {
        enumLoader.execute(clz);
        enumCount++;
      }
    }
  }

  @Override
  public void outputLog() {
    System.out.println("Load interface: " + interfaceCount);
    System.out.println("Load class: " + classCount);
    System.out.println("Load enum: " + enumCount);
    enumLoader.outputLog();
    structLoader.outputLog();
    rpcLoader.outputLog();
  }

  /**
   * Check class is need to load.
   * @param clz a thrift class
   * @return  true if is need to load else false
   */
  private boolean filterClass(Class<?> clz) {
    String name = clz.getSimpleName();
    return !clz.isMemberClass() && (scans.size() == 0 || scans.contains(name)) && !skips.contains(name);
  }
}
