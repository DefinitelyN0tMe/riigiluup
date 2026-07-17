package com.riigiluup.initiative;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** @see InitiativePhaseConverter — same rationale: the DB keeps the source's slug. */
@Converter
public class ParliamentDecisionConverter
        implements AttributeConverter<ParliamentDecision, String> {

    @Override
    public String convertToDatabaseColumn(ParliamentDecision decision) {
        return decision == null ? null : decision.slug();
    }

    @Override
    public ParliamentDecision convertToEntityAttribute(String slug) {
        return ParliamentDecision.fromSlug(slug);
    }
}
