package com.trina.visiontask.service;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class TaskQueryOptions {

    private List<UUID> taskIds;
    private List<Integer> states;
    private List<String> taskTypes;

    private Integer pageNum;
    private Integer pageSize;

}
