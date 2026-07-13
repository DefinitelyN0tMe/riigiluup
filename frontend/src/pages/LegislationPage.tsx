import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchLegislation } from "../api/legislation";
import LegislationRow from "../components/LegislationRow";
import DataFreshnessBadge from "../components/DataFreshnessBadge";

const PHASE_CODES = ["", "SUBMITTED", "IN_COMMITTEE", "IN_READINGS", "ADOPTED", "REJECTED", "WITHDRAWN", "OTHER"] as const;

export default function LegislationPage() {
  const { t } = useTranslation();
  const [phase, setPhase] = useState<string>("");
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const phaseLabel = (code: string) =>
    code === "" ? t("legislation.allPhases") : t(`phase.${code}` as const, { defaultValue: code });

  const { data, isLoading, error } = useQuery({
    queryKey: ["legislation", q, phase, page],
    queryFn: () => fetchLegislation({
      q: q || undefined,
      phase: phase || undefined,
      page,
      size: 50,
    }),
    placeholderData: (prev) => prev,
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-semibold text-ink">{t("legislation.title")}</h1>
        <DataFreshnessBadge />
      </div>

      <div className="flex flex-wrap gap-3 items-center">
        <label htmlFor="legislation-search" className="sr-only">Search bills by title</label>
        <input
          id="legislation-search"
          type="search"
          value={q}
          onChange={(e) => { setPage(0); setQ(e.target.value); }}
          placeholder={t("legislation.searchPlaceholder")}
          aria-label="Search bills by title"
          className="flex-1 min-w-[240px] border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
        />
        <label htmlFor="legislation-phase-filter" className="sr-only">Filter by phase</label>
        <select
          id="legislation-phase-filter"
          value={phase}
          onChange={(e) => { setPage(0); setPhase(e.target.value); }}
          className="border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
        >
          {PHASE_CODES.map((code) => <option key={code} value={code}>{phaseLabel(code)}</option>)}
        </select>
      </div>

      {isLoading && <p className="text-slate-500" role="status">{t("common.loading")}</p>}
      {error && <p className="text-red-600" role="alert">{t("common.failedToLoad")} {(error as Error).message}</p>}

      {data && (
        <>
          <p className="text-sm text-slate-600" aria-live="polite" aria-atomic="true">
            {t("legislation.showing", { shown: data.items.length, total: data.totalElements })}
          </p>
          <ul className="grid grid-cols-1 gap-3 list-none p-0">
            {data.items.map((i) => (
              <li key={i.id}>
                <LegislationRow i={i} />
              </li>
            ))}
          </ul>
          {data.totalPages > 1 && (
            <nav className="flex gap-2 items-center pt-4" aria-label="Pagination">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                aria-label="Previous page"
                className="border border-slate-300 rounded px-3 py-1 disabled:opacity-50"
              >{t("common.prev")}</button>
              <span className="text-sm text-slate-600" aria-live="polite">
                {t("common.pageOf", { page: data.page + 1, total: data.totalPages })}
              </span>
              <button
                onClick={() => setPage((p) => (p + 1 < data.totalPages ? p + 1 : p))}
                disabled={page + 1 >= data.totalPages}
                aria-label="Next page"
                className="border border-slate-300 rounded px-3 py-1 disabled:opacity-50"
              >{t("common.next")}</button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}
