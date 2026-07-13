import { useState } from "react";
import { useTranslation } from "react-i18next";
import { fetchAdminStatus, triggerAdminImport } from "../api/admin";
import type { AdminStatus } from "../types";

const IMPORT_JOBS = [
  { path: "/api/v1/admin/import/plenary-members", labelKey: "admin.jobs.plenaryMembers" },
  { path: "/api/v1/admin/import/usergroups", labelKey: "admin.jobs.usergroups" },
  { path: "/api/v1/admin/import/plenary-member-details", labelKey: "admin.jobs.plenaryMemberDetails" },
  { path: "/api/v1/admin/import/votes", labelKey: "admin.jobs.votes" },
  { path: "/api/v1/admin/import/recompute-alignments", labelKey: "admin.jobs.recomputeAlignments" },
  { path: "/api/v1/admin/import/legislation", labelKey: "admin.jobs.legislation" },
  { path: "/api/v1/admin/import/link-votes-to-bills", labelKey: "admin.jobs.linkVotesToBills" },
] as const;

export default function AdminPage() {
  const { t } = useTranslation();
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
    if (!confirm(t("admin.confirmRun", { label }))) return;
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
        <h1 className="text-2xl font-semibold text-ink">{t("admin.title")}</h1>
        <label className="block text-sm">
          {t("admin.username")}
          <input value={u} onChange={(e) => setU(e.target.value)} autoComplete="username"
                 className="w-full border border-slate-300 rounded-md px-3 py-2 mt-1"/>
        </label>
        <label className="block text-sm">
          {t("admin.password")}
          <input type="password" value={p} onChange={(e) => setP(e.target.value)} autoComplete="current-password"
                 className="w-full border border-slate-300 rounded-md px-3 py-2 mt-1"/>
        </label>
        <button type="submit" className="px-4 py-2 rounded-md bg-estonia text-white hover:bg-blue-700">
          {t("admin.signIn")}
        </button>
        {error && <p className="text-red-600 text-sm" role="alert">{error}</p>}
      </form>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center flex-wrap gap-3">
        <h1 className="text-2xl font-semibold text-ink">{t("admin.title")}</h1>
        <button onClick={refresh} className="text-sm text-estonia hover:underline">{t("admin.refresh")}</button>
      </div>

      {error && <p className="text-red-600 text-sm" role="alert">{error}</p>}

      <section aria-label="Counts">
        <h2 className="text-lg font-semibold text-ink mb-2">{t("admin.domainCounts")}</h2>
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
        <h2 className="text-lg font-semibold text-ink mb-2">{t("admin.snapshotsByEntity")}</h2>
        <ul className="grid grid-cols-2 md:grid-cols-4 gap-2 text-sm">
          {status.snapshotsByEntity.map((s) => (
            <li key={s.entityType} className="border border-slate-200 rounded p-2">
              {s.entityType}: <span className="font-medium">{s.count}</span>
            </li>
          ))}
        </ul>
      </section>

      <section aria-label="Trigger">
        <h2 className="text-lg font-semibold text-ink mb-2">{t("admin.triggerJob")}</h2>
        <ul className="grid grid-cols-1 md:grid-cols-2 gap-2">
          {IMPORT_JOBS.map((j) => {
            const label = t(j.labelKey);
            return (
              <li key={j.path}>
                <button
                  onClick={() => trigger(j.path, label)}
                  disabled={busy !== null}
                  className="w-full text-left border border-slate-200 rounded p-3 text-sm hover:border-estonia disabled:opacity-50"
                >
                  {label}
                  {busy === label && <span className="text-slate-500">{t("admin.running")}</span>}
                </button>
              </li>
            );
          })}
        </ul>
      </section>

      <section aria-label="Recent runs">
        <h2 className="text-lg font-semibold text-ink mb-2">{t("admin.recentRuns")}</h2>
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
                {t("admin.seenUpserted", {
                  when: j.lastRunAt ? new Date(j.lastRunAt).toLocaleString() : "—",
                  seen: j.recordsSeen,
                  upserted: j.recordsUpserted,
                })}
                {j.errorMessage && <span className="text-red-600 block mt-1">{j.errorMessage}</span>}
              </div>
            </li>
          ))}
        </ul>
      </section>

      <p className="text-xs text-slate-400">{t("admin.generatedAt", { when: new Date(status.generatedAt).toLocaleString() })}</p>
    </div>
  );
}
