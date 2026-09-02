package com.riigiluup.initiative;

/**
 * Thrown when the rahvaalgatus source emits a phase / parliament-decision slug we do not model.
 * A structural source change must fail the import run loudly (visible in /admin) rather than be
 * silently skipped as an ordinary bad row. Extends {@link IllegalArgumentException} so the API
 * layer keeps mapping it to a 400 with the same message.
 */
public class UnknownSourceSlugException extends IllegalArgumentException {
    public UnknownSourceSlugException(String message) {
        super(message);
    }
}
