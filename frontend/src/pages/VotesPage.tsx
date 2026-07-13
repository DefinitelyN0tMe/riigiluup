import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchVotes } from "../api/votes";
import VoteRow from "../components/VoteRow";
import DataFreshnessBadge from "../components/DataFreshnessBadge";

const TYPE_CODES = ["", "OPEN", "ATTENDANCE_CHECK", "SECRET", "OTHER"] as const;

export default function VotesPage() {
  const { t } = useTranslation();
  const [type, setType] = useState<string>("");
  const [page, setPage] = useState(0);
  const typeLabel = (code: string) =>
    code === "" ? t("voteType.allTypes") : t(`voteType.${code}` as const, { defaultValue: code });

  const { data, isLoading, error } = useQuery({
    queryKey: ["votes", type, page],
    queryFn: () => fetchVotes({ type: type || undefined, page, size: 50 }),
    placeholderData: (prev) => prev,
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-semibold text-ink">{t("votes.title")}</h1>
        <DataFreshnessBadge />
      </div>

      <div className="flex flex-wrap gap-3 items-center">
        <label htmlFor="votes-type-filter" className="sr-only">Filter by vote type</label>
        <select
          id="votes-type-filter"
          value={type}
          onChange={(e) => { setPage(0); setType(e.target.value); }}
          className="border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
        >
          {TYPE_CODES.map((c) => <option key={c} value={c}>{typeLabel(c)}</option>)}
        </select>
      </div>

      {isLoading && <p className="text-slate-500" role="status">{t("common.loading")}</p>}
      {error && <p className="text-red-600" role="alert">{t("common.failedToLoad")} {(error as Error).message}</p>}

      {data && (
        <>
          <p className="text-sm text-slate-600" aria-live="polite" aria-atomic="true">
            {t("votes.showing", { shown: data.items.length, total: data.totalElements })}
          </p>
          <ul className="grid grid-cols-1 gap-3 list-none p-0">
            {data.items.map((v) => (
              <li key={v.id}>
                <VoteRow v={v} />
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
