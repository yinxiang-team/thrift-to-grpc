package com.yinxiang.utils.thrift.grpc.operations;

import com.google.common.collect.Sets;
import com.yinxiang.utils.thrift.grpc.infos.FieldType;
import com.yinxiang.utils.thrift.grpc.infos.FileInfo;
import com.yinxiang.utils.thrift.grpc.infos.StructInfo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The gRPC proto circle dependencies resolver.
 * <p>
 *   Thrift can delimit a import b and b import a, but gRPC can not.
 *   The resolver will change b import a's struct to 'common.proto'.
 * </p>
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see FilesOperation
 */
public class CircleDependenciesResolver implements FilesOperation {
  private int resolveCount;

  @Override
  public void execute(Map<String, FileInfo> fileInfos) {
    resolveCount = 0;
    fileInfos.values().stream().filter(CircleDependenciesResolver::isThriftFile).forEach(fileInfo -> {
      // get dependencies set
      Set<String> dependencies = Sets.newHashSet(fileInfo.getDependencies().keySet());
      // retain the dependency this proto's set
      dependencies.retainAll(fileInfo.getDependencyMe());
      // change all be depended struct to common file
      Consumer<String> resolver = dependencyMe -> addDependencyToCommon(fileInfos, dependencyMe, fileInfo);
      dependencies.forEach(dependency -> {
        fileInfos.get(dependency).getDependencies().get(fileInfo.getName()).forEach(resolver);
        fileInfos.get(dependency).getImports().remove(fileInfo.getName());
      });
      // add import
      fileInfo.getImports().add(COMMON_FILE);
      // counter
      if (dependencies.size() > 0) {
        resolveCount++;
      }
    });
  }

  @Override
  public void outputLog() {
    System.out.println("Resolve circle dependencies: " + resolveCount);
  }

  /**
   * Check is origin thrift file.
   * @param fileInfo  proto file info
   * @return  true if is origin thrift file else false
   */
  private static boolean isThriftFile(FileInfo fileInfo) {
    return !fileInfo.getName().equals(COMMON_FILE) && !fileInfo.getName().equals(ORIGIN_FILE)
            && !fileInfo.getName().equals(REQUEST_FILE) && !fileInfo.getName().equals(RESPONSE_FILE);
  }

  /**
   * Add dependency to common file.
   * @param fileInfos   map of file info
   * @param structName  dependency struct name
   * @param sourceFile  struct's parent file
   */
  private void addDependencyToCommon(Map<String, FileInfo> fileInfos, String structName, FileInfo sourceFile) {
    if (sourceFile == null) {
      return;
    }
    // get and remove from source proto file
    StructInfo structInfo = sourceFile.getStructInfos().remove(structName);
    // get the common file
    FileInfo commonFile = fileInfos.get(COMMON_FILE);
    // get the map of struct from common file
    Map<String, StructInfo> structInfos = commonFile.getStructInfos();
    // put to common file
    if (structInfo == null) {
      structInfo = checkNotNull(structInfos.get(structName), structName + " is null.");
    } else {
      structInfos.put(structName, structInfo);
    }
    // process non-enum
    if (!structInfo.isEnum()) {
      // process all field
      structInfo.getFieldInfos().forEach(fieldInfo -> {
        addFieldDependencyToCommon(fileInfos, fieldInfo.getFirstType());
        addFieldDependencyToCommon(fileInfos, fieldInfo.getSecondType());
      });
    }
  }

  /**
   * Add a field dependency to common file.
   * @param fileInfos map of file info
   * @param fieldType field type
   */
  private void addFieldDependencyToCommon(Map<String, FileInfo> fileInfos, FieldType fieldType) {
    if (fieldType == null) {
      return;
    }
    String type = fieldType.type;
    Optional.ofNullable(StructInfo.getFileInfo(type)).ifPresent(file -> addDependencyToCommon(fileInfos, type, file));
  }
}
