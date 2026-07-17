package com.riigiluup.initiative;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores the source's own slug ('done', not 'DONE') so the column matches both the V26 CHECK
 * constraint and rahvaalgatus's public vocabulary — a hand-written SQL query against this
 * table reads the same as the source's docs.
 */
@Converter
public class InitiativePhaseConverter implements AttributeConverter<InitiativePhase, String> {

    @Override
    public String convertToDatabaseColumn(InitiativePhase phase) {
        return phase == null ? null : phase.slug();
    }

    @Override
    public InitiativePhase convertToEntityAttribute(String slug) {
        return InitiativePhase.fromSlug(slug);
    }
}
