import { useState } from "react";
import { fetchAdminStatus, triggerAdminImport } from "../api/admin";
import type { AdminStatus } from "../types";

const IMPORT_JOBS = [
  { path: "/api/v1/admin/import/plenary-members", label: "MPs (list)" },
  { path: "/api/v1/admin/import/usergroups", label: "Usergroups (fractions + committees)" },
  { path: "/api/v1/admin/import/plenary-member-details", label: "MP details (photo + committees)" },
  { path: "/api/v1/admin/import/votes", label: "Votes (last 90 days)" },
  { path: "/api/v1/admin/import/recompute-alignments", label: "Recompute faction alignments" },
  { path: "/api/v1/admin/import/legislation", label: "Bills (last 90 days)" },
  { path: "/api/v1/admin/import/link-votes-to-bills", label: "Link votes to bills" },
];

export default function AdminPage() {
  const [u, setU] = useState("");
  const [p, setP] = useState("");
  const [status, setStatus] = useState<AdminStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  async function refresh() {
    setError(null);
    try {
      const s = await fetchAdminStatus(u, p);
      setStatus(s);
    } catch (e) {
      setError((e as Error).message);
      setStatus(null);
    }
  }

  async function trigger(path: string, label: string) {
    if (!confirm(`Run: ${label}?`)) return;
    setBusy(label);
    setError(null);
    try {
      await triggerAdminImport(u, p, path);
      await refresh();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(null);
    }
  }

  if (!status) {
    return (
      <form
        className="max-w-sm space-y-3"
        onSubmit={(e) => { e.preventDefault(); refresh(); }}
      >
        <h1 className="text-2xl font-semibold text-ink">Admin</h1>
        <label className="block text-sm">
          Username
          <input value={u} onChange={(e) => setU(e.target.value)} autoComplete="username"
                 className="w-full border border-slate-300 rounded-md px-3 py-2 mt-1"/>
        </label>
        <label className="block text-sm">
          Password
          <input type="password" value={p} onChange={(e) => setP(e.target.value)} autoComplete="current-password"
                 className="w-full border border-slate-300 rounded-md px-3 py-2 mt-1"/>
        </label>
        <button type="submit" className="px-4 py-2 rounded-md bg-estonia text-white hover:bg-blue-700">
          Sign in
        </button>
        {error && <p className="text-red-600 text-sm">{error}</p>}
      </form>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center flex-wrap gap-3">
        <h1 className="text-2xl font-semibold text-ink">Admin</h1>
        <button onClick={refresh} className="text-sm text-estonia hover:underline">Refresh</button>
      </div>

      {error && <p className="text-red-600 text-sm">{error}</p>}

      <section aria-label="Counts">
        <h2 className="text-lg font-semibold text-ink mb-2">Domain counts</h2>
        <ul className="grid grid-cols-2 md:grid-cols-5 gap-3">
          {Object.entries(status.counts).map(([k, v]) => (
            <li key={k} className="border border-slate-200 rounded-lg p-3">
              <div className="text-xs uppercase text-slate-500">{k}</div>
              <div className="text-2xl font-semibold text-ink">{v}</div>
            </li>
          ))}
        </ul>
      </section>

      <section aria-label="Snapshots">
        <h2 className="text-lg font-semibold text-ink mb-2">Snapshots by entity</h2>
        <ul className="grid grid-cols-2 md:grid-cols-4 gap-2 text-sm">
          {status.snapshotsByEntity.map((s) => (
            <li key={s.entityType} className="border border-slate-200 rounded p-2">
              {s.entityType}: <span className="font-medium">{s.count}</span>
            </li>
          ))}
        </ul>
      </section>

      <section aria-label="Trigger">
        <h2 className="text-lg font-semibold text-ink mb-2">Trigger a job</h2>
        <ul className="grid grid-cols-1 md:grid-cols-2 gap-2">
          {IMPORT_JOBS.map((j) => (
            <li key={j.path}>
              <button
                onClick={() => trigger(j.path, j.label)}
                disabled={busy !== null}
                className="w-full text-left border border-slate-200 rounded p-3 text-sm hover:border-estonia disabled:opacity-50"
              >
                {j.label}
                {busy === j.label && <span className="text-slate-500"> — running…</span>}
              </button>
            </li>
          ))}
        </ul>
      </section>

      <section aria-label="Recent runs">
        <h2 className="text-lg font-semibold text-ink mb-2">Recent runs</h2>
        <ul className="divide-y divide-slate-200 border border-slate-200 rounded-lg">
          {status.jobs.map((j, idx) => (
            <li key={idx} className="p-3 text-sm">
              <div className="flex justify-between gap-3">
                <span>
                  <span className="font-medium">{j.jobName}</span>{" "}
                  <span className="text-slate-500">({j.sourceName})</span>
                </span>
                <span className={`shrink-0 text-xs ${j.lastRunStatus === "SUCCESS" ? "text-emerald-600" : "text-red-600"}`}>
                  {j.lastRunStatus}
                </span>
              </div>
              <div className="text-xs text-slate-500 mt-0.5">
                {j.lastRunAt ? new Date(j.lastRunAt).toLocaleString() : "—"} · seen {j.recordsSeen} · upserted {j.recordsUpserted}
                {j.errorMessage && <span className="text-red-600 block mt-1">{j.errorMessage}</span>}
              </div>
            </li>
          ))}
        </ul>
      </section>

      <p className="text-xs text-slate-400">Generated at {new Date(status.generatedAt).toLocaleString()}</p>
    </div>
  );
}
