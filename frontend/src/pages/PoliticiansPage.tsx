import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchFactions, fetchPoliticians } from "../api/politicians";
import PoliticianCard from "../components/PoliticianCard";
import SearchInput from "../components/SearchInput";
import DataFreshnessBadge from "../components/DataFreshnessBadge";

export default function PoliticiansPage() {
  const { t } = useTranslation();
  const [q, setQ] = useState("");
  const [faction, setFaction] = useState<string | "">("");
  const [page, setPage] = useState(0);

  const factions = useQuery({ queryKey: ["factions"], queryFn: fetchFactions });

  const { data, isLoading, error } = useQuery({
    queryKey: ["politicians", q, faction, page],
    queryFn: () =>
      fetchPoliticians({
        q: q || undefined,
        faction: faction || undefined,
        page,
        size: 50,
        activeOnly: true,
      }),
    placeholderData: (previous) => previous,
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-semibold text-ink">{t("politicians.title")}</h1>
        <DataFreshnessBadge />
      </div>

      <div className="flex flex-wrap gap-3 items-center">
        <SearchInput
          value={q}
          onChange={(v) => { setPage(0); setQ(v); }}
          placeholder={t("politicians.searchPlaceholder")}
          ariaLabel="Search MPs by name"
        />
        <label htmlFor="politicians-faction-filter" className="sr-only">Filter by faction</label>
        <select
          id="politicians-faction-filter"
          value={faction}
          onChange={(e) => { setPage(0); setFaction(e.target.value); }}
          className="border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
        >
          <option value="">{t("politicians.allFactions")}</option>
          {factions.data?.map((f) => (
            <option key={f.externalId} value={f.externalId}>
              {t("politicians.factionOption", { name: f.name, count: f.memberCount })}
            </option>
          ))}
        </select>
      </div>

      {isLoading && <p className="text-slate-500" role="status">{t("common.loading")}</p>}
      {error && <p className="text-red-600" role="alert">{t("common.failedToLoad")} {(error as Error).message}</p>}

      {data && (
        <>
          <p className="text-sm text-slate-600" aria-live="polite" aria-atomic="true">
            {t("politicians.showing", { shown: data.items.length, total: data.totalElements })}
          </p>
          <ul className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 list-none p-0">
            {data.items.map((p) => (
              <li key={p.id}>
                <PoliticianCard p={p} />
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
              >
                {t("common.prev")}
              </button>
              <span className="text-sm text-slate-600" aria-live="polite">{t("common.pageOf", { page: data.page + 1, total: data.totalPages })}</span>
              <button
                onClick={() => setPage((p) => (p + 1 < data.totalPages ? p + 1 : p))}
                disabled={page + 1 >= data.totalPages}
                aria-label="Next page"
                className="border border-slate-300 rounded px-3 py-1 disabled:opacity-50"
              >
                {t("common.next")}
              </button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}
