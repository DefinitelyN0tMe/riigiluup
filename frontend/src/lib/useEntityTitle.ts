import { useEffect } from "react";
import { useTranslation } from "react-i18next";

/**
 * Names the browser tab after the entity on a detail page ("Jaak Aab — Riigiluup" instead of the
 * section name), for bookmarks, history and copied links. App sets the per-section title in its own
 * effect, which runs after this one on the same commit, so the entity title is applied a frame later.
 */
export function useEntityTitle(name: string | null | undefined) {
  const { t, i18n } = useTranslation();
  useEffect(() => {
    if (!name) return;
    const title = `${name.length > 90 ? name.slice(0, 87) + "…" : name} — ${t("nav.brand")}`;
    const id = requestAnimationFrame(() => { document.title = title; });
    return () => cancelAnimationFrame(id);
  }, [name, t, i18n.resolvedLanguage]);
}
