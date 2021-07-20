package com.yinxiang.utils.thrift.grpc.operations;

/**
 * A generic type operation interface.
 * @param <T> generic type
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public interface Operation<T> {
  /**
   * Execute operation
   * @param t source
   * @throws Exception  exception
   */
  void execute(T t) throws Exception;

  /**
   * Log.
   */
  void outputLog();

  /**
   * Format the package.
   * @param pkg the package
   * @return  result
   */
  static String formatPkg(String pkg) {
    return pkg.replaceAll("/", ".");
  }
}
