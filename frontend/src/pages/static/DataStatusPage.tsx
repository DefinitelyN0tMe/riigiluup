import { useQuery } from "@tanstack/react-query";
import { fetchDataStatus } from "../../api/politicians";

export default function DataStatusPage() {
  const { data } = useQuery({ queryKey: ["data-status"], queryFn: fetchDataStatus });
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">Data freshness</h1>
      <p>
        Politico refreshes upstream Riigikogu data daily at 03:05 Europe/Tallinn.
        The table below lists the most recent successful runs of each ingestion
        job.
      </p>
      {(!data || data.length === 0) ? (
        <p className="text-slate-500">No successful runs recorded yet.</p>
      ) : (
        <table className="table-auto border border-slate-200">
          <thead>
            <tr className="bg-slate-50 text-left text-sm">
              <th className="px-3 py-2">Source</th>
              <th className="px-3 py-2">Job</th>
              <th className="px-3 py-2">Last run</th>
              <th className="px-3 py-2">Status</th>
              <th className="px-3 py-2 text-right">Records</th>
            </tr>
          </thead>
          <tbody>
            {data.map((s) => (
              <tr key={`${s.sourceName}:${s.jobName}`} className="border-t border-slate-200 text-sm">
                <td className="px-3 py-2">{s.sourceName}</td>
                <td className="px-3 py-2">{s.jobName}</td>
                <td className="px-3 py-2">{s.lastRunAt ? new Date(s.lastRunAt).toLocaleString() : "—"}</td>
                <td className="px-3 py-2">{s.lastRunStatus}</td>
                <td className="px-3 py-2 text-right">{s.lastRunRecords}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <h2>Known limitations</h2>
      <ul>
        <li>Riigikogu itself sometimes publishes vote or bill records with a delay of a few hours.</li>
        <li>Riigikogu's statistics endpoints require a date window; the site queries them lazily and caches results per MP × date range.</li>
        <li>Secret votes have no per-MP records upstream and appear only as aggregate counts.</li>
      </ul>
    </article>
  );
}
