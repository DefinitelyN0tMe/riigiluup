import { useTranslation } from "react-i18next";
import { ApiError } from "../api/client";

/**
 * Human-readable message for a failed data fetch. Structured {@link ApiError}s
 * carry an `apiErrors.*` i18n key (see api/client.ts) — prefer that translated
 * text over the raw HTTP message; anything else falls back to the generic line.
 */
export function useLoadErrorMessage(error: unknown): string {
  const { t } = useTranslation();
  if (error instanceof ApiError) return t(error.i18nKey);
  if (error instanceof Error && error.message) return `${t("common.failedToLoad")} ${error.message}`;
  return t("common.failedToLoad");
}
