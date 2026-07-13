import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchPoliticians } from "../api/politicians";
import PoliticianCard from "../components/PoliticianCard";
import SearchInput from "../components/SearchInput";
import DataFreshnessBadge from "../components/DataFreshnessBadge";

export default function PoliticiansPage() {
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);

  const { data, isLoading, error } = useQuery({
    queryKey: ["politicians", q, page],
    queryFn: () => fetchPoliticians({ q: q || undefined, page, size: 50, activeOnly: true }),
    placeholderData: (previous) => previous,
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-semibold text-ink">Members of the Riigikogu</h1>
        <DataFreshnessBadge />
      </div>

      <SearchInput value={q} onChange={(v) => { setPage(0); setQ(v); }} placeholder="Search by name…" />

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
