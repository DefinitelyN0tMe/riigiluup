import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ApiError } from "../api/client";
import LoadFailed from "./LoadFailed";

/**
 * What a detail page shows when its entity cannot be loaded: a proper "not found" block with a way
 * back for a 404 (a mistyped or stale link), the usual error message otherwise. Replaces the bare
 * red line that unknown MPs, votes and bills used to get.
 */
export default function EntityLoadError({ error, backTo, backLabel }: {
  error: unknown; backTo: string; backLabel: string;
}) {
  const { t } = useTranslation();
  const notFound = error instanceof ApiError && error.status === 404;
  if (!notFound) {
    return (
      <div className="max-w-[900px] mx-auto px-5 sm:px-8 py-10 sm:py-14">
        <LoadFailed error={error} className="text-hot-deep" />
      </div>
    );
  }
  return (
    <section className="px-5 sm:px-8 md:px-10 py-16 sm:py-24 max-w-[900px] mx-auto text-center">
      <div className="font-mono text-[11px] tracking-[0.2em] uppercase text-blue font-bold mb-4">
        404 · {t("notFound.kicker")}
      </div>
      <h1 className="font-display font-bold text-[28px] sm:text-[36px] tracking-[-0.02em] mb-4">{t("notFound.title")}</h1>
      <p className="text-lg text-slate-600 mb-8 max-w-[52ch] mx-auto">{t("apiErrors.notFound")}</p>
      <div className="flex flex-wrap gap-3 justify-center">
        <Link to={backTo} className="inline-flex items-center px-5 py-3 bg-ink text-white rounded-full font-semibold text-[15px]">
          {backLabel}
        </Link>
        <Link to="/" className="inline-flex items-center px-5 py-3 border-[1.5px] border-ink rounded-full font-semibold text-[15px]">
          {t("notFound.home")}
        </Link>
      </div>
    </section>
  );
}
