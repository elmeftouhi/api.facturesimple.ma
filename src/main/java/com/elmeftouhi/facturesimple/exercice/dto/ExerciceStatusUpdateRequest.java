package com.elmeftouhi.facturesimple.exercice.dto;

import com.elmeftouhi.facturesimple.exercice.ExerciceStatus;
import jakarta.validation.constraints.NotNull;

public record ExerciceStatusUpdateRequest(
        @NotNull ExerciceStatus status
) {
}
