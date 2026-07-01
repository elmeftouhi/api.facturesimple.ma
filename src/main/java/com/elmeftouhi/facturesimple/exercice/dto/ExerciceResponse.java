package com.elmeftouhi.facturesimple.exercice.dto;

import com.elmeftouhi.facturesimple.exercice.ExerciceStatus;
import java.time.LocalDate;

public record ExerciceResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        ExerciceStatus status,
        Long tenantId
) {
}
