package com.yinxiang.utils.thrift.grpc.operations;

import com.yinxiang.utils.thrift.grpc.infos.FieldInfo;
import com.yinxiang.utils.thrift.grpc.infos.FileInfo;
import com.yinxiang.utils.thrift.grpc.infos.StructInfo;

import java.util.List;
import java.util.Map;

@Deprecated
public class RefsRecorder implements FilesOperation {
  @Override
  public void execute(Map<String, FileInfo> fileInfos) {
    FileInfo originFile = new FileInfo(ORIGIN_FILE);
    originFile.setVersion((byte) 2);
    fileInfos.forEach((fileName, fileInfo) -> {
      StructInfo fileStruct = originFile.getStructInfos().computeIfAbsent(fileName, StructInfo::new);
      fileStruct.setFileInfo(fileInfo);
      List<FieldInfo> fieldInfos = fileStruct.getFieldInfos();
      fileInfo.getStructInfos()
              .values()
              .stream()
              .filter(s -> s.getRefInfo() != null)
              .map(StructInfo::getRefInfo)
              .forEach(fieldInfos::add);
    });
    fileInfos.put(ORIGIN_FILE, originFile);
  }

  @Override
  public void outputLog() {}
}
