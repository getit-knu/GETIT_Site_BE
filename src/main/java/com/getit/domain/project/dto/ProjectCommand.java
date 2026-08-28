package com.getit.domain.project.dto;

import java.util.List;

public record ProjectCommand(
    String title,
    String teamName,
    String semester,
    String description,
    List<String> techStacks,
    String codeUrl,
    String demoUrl,
    boolean isFeatured,
    Long fileId
) { }
