import { useQuery } from "@tanstack/react-query";
import { fetchDataStatus } from "../api/politicians";

export default function DataFreshnessBadge() {
  const { data } = useQuery({ queryKey: ["data-status"], queryFn: fetchDataStatus });
  const first = data?.[0];
  if (!first?.lastRunAt) return null;
  const when = new Date(first.lastRunAt).toLocaleString();
  return (
    <div className="text-xs text-slate-500">
      Last sync: <time dateTime={first.lastRunAt}>{when}</time> — {first.lastRunStatus} ({first.lastRunRecords} records)
    </div>
  );
}
