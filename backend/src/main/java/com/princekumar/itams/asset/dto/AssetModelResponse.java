package com.princekumar.itams.asset.dto;

public record AssetModelResponse(
    Long id,
    Long categoryId,
    String categoryCode,
    String manufacturer,
    String modelName,
    String specs
) {}
