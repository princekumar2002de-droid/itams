package com.princekumar.itams.asset;

import com.princekumar.itams.asset.dto.AssetResponse;

final class AssetMapper {
    private AssetMapper() {}
    static AssetResponse toResponse(Asset a) {
        var m = a.getModel();
        return new AssetResponse(
            a.getId(), a.getAssetTag(),
            m.getId(), m.getManufacturer(), m.getModelName(), m.getCategory().getCode(),
            a.getSerialNumber(), a.getStatus(),
            a.getPurchaseDate(), a.getPurchasePrice(),
            a.getWarrantyEndsOn(), a.getNotes(),
            a.getRetiredAt(), a.getRetiredReason(),
            a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
