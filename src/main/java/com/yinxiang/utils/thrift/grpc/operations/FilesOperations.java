package com.yinxiang.utils.thrift.grpc.operations;

import com.google.common.collect.Lists;
import com.yinxiang.utils.thrift.grpc.infos.FileInfo;

import java.util.List;
import java.util.Map;

/**
 * A composite {@link FilesOperation}.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see FilesOperation
 */
public class FilesOperations implements FilesOperation {
  /** The list of {@link FilesOperation}. */
  private final List<FilesOperation> operations = Lists.newLinkedList();

  /**
   * Add a operation.
   * @param filesOperation  {@link FilesOperation}
   */
  public void addFilesOperation(FilesOperation filesOperation) {
    operations.add(filesOperation);
  }

  /**
   * Clear all operations.
   */
  public void clear() {
    operations.clear();
  }

  /**
   * Get size of operations.
   * @return  size of operations
   */
  public int size() {
    return operations.size();
  }

  /**
   * Remove a operation.
   * @param index operation index
   */
  public void remove(int index) {
    operations.remove(index);
  }

  @Override
  public void execute(Map<String, FileInfo> fileInfos) throws Exception {
    for (FilesOperation filesOperation : operations) {
      filesOperation.execute(fileInfos);
    }
  }

  @Override
  public void outputLog() {
    operations.forEach(Operation::outputLog);
  }
}
