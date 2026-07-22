import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { fetchLegislation } from "../api/legislation";
import LegislationRow from "../components/LegislationRow";
import DataFreshnessBadge from "../components/DataFreshnessBadge";
import SearchInput from "../components/SearchInput";
import LoadFailed from "../components/LoadFailed";

const PHASE_CODES = ["", "SUBMITTED", "IN_COMMITTEE", "IN_READINGS", "ADOPTED", "REJECTED", "WITHDRAWN", "OTHER"] as const;

export default function LegislationPage() {
  const { t } = useTranslation();
  const [sp, setSp] = useSearchParams();

  const q = sp.get("q") ?? "";
  const phase = sp.get("phase") ?? "";
  const topicEdid = sp.get("topicEdid") ? Number(sp.get("topicEdid")) : undefined;
  const topicLabel = sp.get("topicLabel") ?? undefined;   // frontend-only hint from drill-down
  const minDays = sp.get("minDays") ? Number(sp.get("minDays")) : undefined;
  const maxDays = sp.get("maxDays") ? Number(sp.get("maxDays")) : undefined;
  const velocityLabel = sp.get("velocityLabel") ?? undefined;
  const committee = sp.get("committee") ?? "";
  const committeeName = sp.get("committeeName") ?? undefined;   // frontend-only hint from the committee page
  const page = sp.get("page") ? Number(sp.get("page")) : 0;

  function updateParams(next: Record<string, string | number | undefined | null>) {
    const nextSp = new URLSearchParams(sp);
    for (const [k, v] of Object.entries(next)) {
      if (v === undefined || v === null || v === "" ) nextSp.delete(k);
      else nextSp.set(k, String(v));
    }
    setSp(nextSp, { replace: false });
  }
  const removeFilter = (keys: string[]) => {
    const nextSp = new URLSearchParams(sp);
    keys.forEach((k) => nextSp.delete(k));
    nextSp.delete("page");
    setSp(nextSp);
  };

  const phaseLabel = (code: string) =>
    code === "" ? t("legislation.allPhases") : t(`phase.${code}` as const, { defaultValue: code });

  const { data, isLoading, error } = useQuery({
    queryKey: ["legislation", q, phase, topicEdid, minDays, maxDays, committee, page],
    queryFn: () => fetchLegislation({
      q: q || undefined,
      phase: phase || undefined,
      topicEdid,
      minDays,
      maxDays,
      committee: committee || undefined,
      page,
      size: 50,
    }),
    placeholderData: (prev) => prev,
  });

  const chips = useMemo(() => {
    const out: { key: string; label: string; removeKeys: string[] }[] = [];
    if (topicEdid !== undefined) out.push({
      key: "topic",
      label: `${t("legislation.chip.topic")}: ${topicLabel ?? `#${topicEdid}`}`,
      removeKeys: ["topicEdid", "topicLabel"],
    });
    if (minDays !== undefined || maxDays !== undefined) out.push({
      key: "velocity",
      label: `${t("legislation.chip.velocity")}: ${velocityLabel ?? `${minDays ?? 0}–${maxDays ?? "∞"}d`}`,
      removeKeys: ["minDays", "maxDays", "velocityLabel"],
    });
    if (committee) out.push({
      key: "committee",
      label: `${t("legislation.chip.committee")}: ${committeeName ?? committee}`,
      removeKeys: ["committee", "committeeName"],
    });
    return out;
  }, [topicEdid, topicLabel, minDays, maxDays, velocityLabel, committee, committeeName, t]);

  return (
    <div className="px-5 sm:px-8 md:px-10 py-10 sm:py-14 md:py-16 max-w-[1440px] mx-auto w-full">
      <div className="mb-8 md:mb-12">
        <div className="font-mono text-[11px] tracking-[0.2em] uppercase text-blue font-bold flex items-center gap-2.5 mb-4">
          <span className="bg-blue text-white px-2 py-0.5 rounded font-bold tracking-[0.14em]">III.</span>
          {t("legislation.title")}
        </div>
        <div className="flex items-end justify-between flex-wrap gap-4">
          <h1 className="font-display font-bold h-display-lg">
            {t("legislation.title")}
          </h1>
          <DataFreshnessBadge />
        </div>
      </div>

      <div className="flex flex-wrap gap-3 items-center mb-4 sm:mb-5">
        <SearchInput
          value={q}
          onChange={(v) => {
            const nextSp = new URLSearchParams(sp);
            if (v) nextSp.set("q", v); else nextSp.delete("q");
            nextSp.delete("page");
            setSp(nextSp, { replace: true });
          }}
          placeholder={t("legislation.searchPlaceholder")}
          ariaLabel={t("a11y.searchBills")}
        />
        <label htmlFor="legislation-phase-filter" className="sr-only">{t("a11y.filterByPhase")}</label>
        <select
          id="legislation-phase-filter"
          value={phase}
          onChange={(e) => updateParams({ phase: e.target.value || undefined, page: undefined })}
          className="bg-white border border-rule rounded-full px-4 py-2.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-blue min-w-[220px]"
        >
          {PHASE_CODES.map((code) => <option key={code} value={code}>{phaseLabel(code)}</option>)}
        </select>
      </div>

      {chips.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-4 sm:mb-5">
          <span className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted self-center">
            {t("legislation.chip.filters")}:
          </span>
          {chips.map((c) => (
            <button key={c.key} type="button" onClick={() => removeFilter(c.removeKeys)}
                    className="inline-flex items-center gap-2 bg-blue text-white text-[12px] font-medium rounded-full pl-3 pr-2 py-1.5 hover:bg-blue-deep transition-colors">
              {c.label}
              <span className="w-4 h-4 rounded-full bg-white/25 grid place-items-center text-[10px] font-bold">×</span>
            </button>
          ))}
        </div>
      )}

      {isLoading && <p className="text-muted font-mono text-sm tracking-[0.06em]" role="status">{t("common.loading")}</p>}
      {error && <LoadFailed error={error} className="text-hot-deep font-mono text-sm" />}

      {data && (
        <>
          <p className="font-mono text-[11px] tracking-[0.1em] uppercase text-muted mb-4 sm:mb-5" aria-live="polite" aria-atomic="true">
            {t("legislation.showing", { shown: data.items.length, total: data.totalElements })}
          </p>
          <ul className="grid grid-cols-1 gap-3 sm:gap-4 list-none p-0">
            {data.items.map((i) => (
              <li key={i.id}>
                <LegislationRow i={i} />
              </li>
            ))}
          </ul>
          {data.totalPages > 1 && (
            <nav className="flex gap-3 items-center pt-8 justify-center" aria-label={t("a11y.pagination")}>
              <button
                onClick={() => updateParams({ page: Math.max(0, page - 1) })}
                disabled={page === 0}
                aria-label={t("a11y.prevPage")}
                className="border-[1.5px] border-ink rounded-full px-4 py-2 text-sm font-bold disabled:opacity-40 hover:bg-ink hover:text-white transition-colors"
              >{t("common.prev")}</button>
              <span className="font-mono text-[11px] tracking-[0.1em] text-muted" aria-live="polite">
                {t("common.pageOf", { page: data.page + 1, total: data.totalPages })}
              </span>
              <button
                onClick={() => updateParams({ page: page + 1 < data.totalPages ? page + 1 : page })}
                disabled={page + 1 >= data.totalPages}
                aria-label={t("a11y.nextPage")}
                className="border-[1.5px] border-ink rounded-full px-4 py-2 text-sm font-bold disabled:opacity-40 hover:bg-ink hover:text-white transition-colors"
              >{t("common.next")}</button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}
