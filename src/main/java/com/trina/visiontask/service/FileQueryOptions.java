package com.trina.visiontask.service;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class FileQueryOptions {
    List<String> fileNames;
    List<UUID> fileIds;
    List<Integer> status;
    List<String> fileTypes;
    String path;
    int pageSize;
    int pageNum;
}
