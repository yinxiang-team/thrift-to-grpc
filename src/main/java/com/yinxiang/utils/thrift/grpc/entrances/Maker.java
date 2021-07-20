package com.yinxiang.utils.thrift.grpc.entrances;

import com.yinxiang.utils.thrift.grpc.infos.FileInfo;
import com.yinxiang.utils.thrift.grpc.operations.CircleDependenciesResolver;
import com.yinxiang.utils.thrift.grpc.operations.ThriftClassesCollector;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperation;
import com.yinxiang.utils.thrift.grpc.operations.FilesOperations;

import java.util.Map;

/**
 * Base maker of files.
 * <p>
 *   Implement method {@link #addFilesOperations(FilesOperations)} to add all need operations.
 *   Add <code>public static void main(String[] args)</code> method to execute it.
 * </p>
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public abstract class Maker implements FilesOperation {
  /** The collections of {@link FilesOperation}. */
  private final FilesOperations filesOperations = new FilesOperations();
  /** The init size of {@link #filesOperations}. */
  private final int size;

  public Maker(String thriftPackage, String scans, String drops) {
    filesOperations.addFilesOperation(new ThriftClassesCollector(thriftPackage, scans, drops));
    filesOperations.addFilesOperation(new CircleDependenciesResolver());
    size = filesOperations.size();
  }

  @Override
  public void execute(Map<String, FileInfo> fileInfos) throws Exception {
    System.out.println(getClass().getName() + ".execute");
    // remove if more than init size
    while (filesOperations.size() > size) {
      filesOperations.remove(size);
    }
    // add all extend operations
    addFilesOperations(filesOperations);
    // execute all
    filesOperations.execute(fileInfos);
    // log
    outputLog();
  }

  @Override
  public void outputLog() {
    filesOperations.outputLog();
  }

  /**
   * Add all extend operations.
   * @param filesOperations filesOperations
   */
  protected abstract void addFilesOperations(FilesOperations filesOperations);
}
