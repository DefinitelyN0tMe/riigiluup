import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchFactions, fetchPoliticians } from "../api/politicians";
import PoliticianCard from "../components/PoliticianCard";
import SearchInput from "../components/SearchInput";
import DataFreshnessBadge from "../components/DataFreshnessBadge";

export default function PoliticiansPage() {
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
        <h1 className="text-2xl font-semibold text-ink">Members of the Riigikogu</h1>
        <DataFreshnessBadge />
      </div>

      <div className="flex flex-wrap gap-3 items-center">
        <SearchInput
          value={q}
          onChange={(v) => { setPage(0); setQ(v); }}
          placeholder="Search by name…"
        />
        <select
          value={faction}
          onChange={(e) => { setPage(0); setFaction(e.target.value); }}
          className="border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
        >
          <option value="">All factions</option>
          {factions.data?.map((f) => (
            <option key={f.externalId} value={f.externalId}>
              {f.name} ({f.memberCount})
            </option>
          ))}
        </select>
      </div>

      {isLoading && <p className="text-slate-500">Loading…</p>}
      {error && <p className="text-red-600">Failed to load. {(error as Error).message}</p>}

      {data && (
        <>
          <p className="text-sm text-slate-600">
            Showing {data.items.length} of {data.totalElements} MPs.
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {data.items.map((p) => <PoliticianCard key={p.id} p={p} />)}
          </div>
          {data.totalPages > 1 && (
            <div className="flex gap-2 items-center pt-4">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="border border-slate-300 rounded px-3 py-1 disabled:opacity-50"
              >
                Prev
              </button>
              <span className="text-sm text-slate-600">Page {data.page + 1} of {data.totalPages}</span>
              <button
                onClick={() => setPage((p) => (p + 1 < data.totalPages ? p + 1 : p))}
                disabled={page + 1 >= data.totalPages}
                className="border border-slate-300 rounded px-3 py-1 disabled:opacity-50"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
