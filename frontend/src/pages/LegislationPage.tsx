import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchLegislation } from "../api/legislation";
import LegislationRow from "../components/LegislationRow";
import DataFreshnessBadge from "../components/DataFreshnessBadge";

const PHASES: Array<{ code: string; label: string }> = [
  { code: "", label: "All phases" },
  { code: "SUBMITTED", label: "Submitted" },
  { code: "IN_COMMITTEE", label: "In committee" },
  { code: "IN_READINGS", label: "In readings" },
  { code: "ADOPTED", label: "Adopted" },
  { code: "REJECTED", label: "Rejected" },
  { code: "WITHDRAWN", label: "Withdrawn" },
  { code: "OTHER", label: "Other" },
];

export default function LegislationPage() {
  const [phase, setPhase] = useState<string>("");
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);

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
        <h1 className="text-2xl font-semibold text-ink">Bills</h1>
        <DataFreshnessBadge />
      </div>

      <div className="flex flex-wrap gap-3 items-center">
        <input
          type="search"
          value={q}
          onChange={(e) => { setPage(0); setQ(e.target.value); }}
          placeholder="Search by title…"
          className="flex-1 min-w-[240px] border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
        />
        <select
          value={phase}
          onChange={(e) => { setPage(0); setPhase(e.target.value); }}
          className="border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
        >
          {PHASES.map((p) => <option key={p.code} value={p.code}>{p.label}</option>)}
        </select>
      </div>

      {isLoading && <p className="text-slate-500">Loading…</p>}
      {error && <p className="text-red-600">Failed to load. {(error as Error).message}</p>}

      {data && (
        <>
          <p className="text-sm text-slate-600">
            Showing {data.items.length} of {data.totalElements} bills.
          </p>
          <div className="grid grid-cols-1 gap-3">
            {data.items.map((i) => <LegislationRow key={i.id} i={i} />)}
          </div>
          {data.totalPages > 1 && (
            <div className="flex gap-2 items-center pt-4">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="border border-slate-300 rounded px-3 py-1 disabled:opacity-50"
              >Prev</button>
              <span className="text-sm text-slate-600">
                Page {data.page + 1} of {data.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => (p + 1 < data.totalPages ? p + 1 : p))}
                disabled={page + 1 >= data.totalPages}
                className="border border-slate-300 rounded px-3 py-1 disabled:opacity-50"
              >Next</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
