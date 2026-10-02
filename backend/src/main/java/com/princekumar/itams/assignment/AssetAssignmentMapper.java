package com.princekumar.itams.assignment;

import com.princekumar.itams.assignment.dto.AssetAssignmentResponse;

final class AssetAssignmentMapper {
    private AssetAssignmentMapper() {}
    static AssetAssignmentResponse toResponse(AssetAssignment a) {
        var p = a.getAssignee();
        return new AssetAssignmentResponse(
            a.getId(),
            a.getAsset().getId(), a.getAsset().getAssetTag(),
            p.getId(), p.getFirstName() + " " + p.getLastName(),
            a.getAssignedByUserId(), a.getAssignedAt(),
            a.getExpectedReturnOn(), a.getActualReturnAt(),
            a.getReturnedByUserId(),
            a.getOutCondition(), a.getInCondition(),
            a.getNotes(),
            a.isOpen()
        );
    }
}
