import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchVotes } from "../api/votes";
import VoteRow from "../components/VoteRow";
import DataFreshnessBadge from "../components/DataFreshnessBadge";

const TYPES: Array<{ code: string; label: string }> = [
  { code: "", label: "All types" },
  { code: "OPEN", label: "Roll-call" },
  { code: "ATTENDANCE_CHECK", label: "Attendance check" },
  { code: "SECRET", label: "Secret" },
  { code: "OTHER", label: "Other" },
];

export default function VotesPage() {
  const [type, setType] = useState<string>("");
  const [page, setPage] = useState(0);

  const { data, isLoading, error } = useQuery({
    queryKey: ["votes", type, page],
    queryFn: () => fetchVotes({ type: type || undefined, page, size: 50 }),
    placeholderData: (prev) => prev,
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-semibold text-ink">Recent votes</h1>
        <DataFreshnessBadge />
      </div>

      <div className="flex flex-wrap gap-3 items-center">
        <select
          value={type}
          onChange={(e) => { setPage(0); setType(e.target.value); }}
          className="border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
        >
          {TYPES.map((t) => <option key={t.code} value={t.code}>{t.label}</option>)}
        </select>
      </div>

      {isLoading && <p className="text-slate-500">Loading…</p>}
      {error && <p className="text-red-600">Failed to load. {(error as Error).message}</p>}

      {data && (
        <>
          <p className="text-sm text-slate-600">
            Showing {data.items.length} of {data.totalElements} votes.
          </p>
          <div className="grid grid-cols-1 gap-3">
            {data.items.map((v) => <VoteRow key={v.id} v={v} />)}
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
