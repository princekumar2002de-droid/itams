package com.princekumar.itams.asset;

import com.princekumar.itams.asset.dto.AssetModelResponse;

final class AssetModelMapper {
    private AssetModelMapper() {}
    static AssetModelResponse toResponse(AssetModel m) {
        return new AssetModelResponse(
            m.getId(),
            m.getCategory().getId(),
            m.getCategory().getCode(),
            m.getManufacturer(),
            m.getModelName(),
            m.getSpecs()
        );
    }
}
