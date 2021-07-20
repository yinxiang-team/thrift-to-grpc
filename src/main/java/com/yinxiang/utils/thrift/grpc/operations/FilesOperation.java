package com.yinxiang.utils.thrift.grpc.operations;

import com.yinxiang.utils.thrift.grpc.infos.FileInfo;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

/**
 * Support proto files operation interface.
 * @author Huiyuan Fu
 * @since 1.0.0
 * @see FilesOperation
 */
public interface FilesOperation extends Operation<Map<String, FileInfo>> {
  /** File name: common. */
  String COMMON_FILE = "common";
  /** File name: origin. */
  String ORIGIN_FILE = "origin";
  /** File name: request. */
  String REQUEST_FILE = "request";
  /** File name: response. */
  String RESPONSE_FILE = "response";
  /** First tab. */
  int TAB = 1;

  /**
   * Get last part of a split result.
   * @param str  to split str
   * @return  last part of a split result
   */
  static String getLastDot(String str) {
    String[] names = str.split("\\.");
    return names[names.length - 1];
  }

  /**
   * Create file.
   * @param path    file path
   * @param name    file name
   * @param content file content
   */
  static void createFile(String path, String name, String content) {
    try {
      System.out.print(new File(path).mkdirs() ? "[mkdir]" : "");
      createFile(path + "/" + name + ".java", content);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Create file.
   * @param path    file path (include file name)
   * @param content file content
   * @throws Exception  exception
   */
  static void createFile(String path, String content) throws Exception {
    File file = new File(path);
    System.out.println("File: " + file.getName() + (file.exists() ? "." : ("[" + file.createNewFile() + "].")));
    // output content
    try (FileWriter writer = new FileWriter(file)) {
      writer.append(content);
    }
  }
}
