import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { AdminUnauthorizedError, fetchAdminStatus, triggerAdminImport } from "../api/admin";
import AdminAffiliationsSection from "../components/admin/AdminAffiliationsSection";
import AdminInitiativeLinksSection from "../components/admin/AdminInitiativeLinksSection";
import type { AdminStatus } from "../types";
import { formatDateTime } from "../lib/formatDate";

const IMPORT_JOBS = [
  { path: "/api/v1/admin/import/plenary-members", labelKey: "admin.jobs.plenaryMembers" },
  { path: "/api/v1/admin/import/usergroups", labelKey: "admin.jobs.usergroups" },
  { path: "/api/v1/admin/import/plenary-member-details", labelKey: "admin.jobs.plenaryMemberDetails" },
  { path: "/api/v1/admin/import/votes", labelKey: "admin.jobs.votes" },
  { path: "/api/v1/admin/import/recompute-alignments", labelKey: "admin.jobs.recomputeAlignments" },
  { path: "/api/v1/admin/import/legislation", labelKey: "admin.jobs.legislation" },
  { path: "/api/v1/admin/import/link-votes-to-bills", labelKey: "admin.jobs.linkVotesToBills" },
] as const;

const BASE = import.meta.env.VITE_API_BASE_URL ?? "";
const OIDC_LOGIN_URL = `${BASE}/oauth2/authorization/google`;

type AuthState = "loading" | "unauthorized" | "authorized";

export default function AdminPage() {
  const { t } = useTranslation();
  const [authState, setAuthState] = useState<AuthState>("loading");
  const [status, setStatus] = useState<AdminStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  async function refresh() {
    setError(null);
    try {
      const s = await fetchAdminStatus();
      setStatus(s);
      setAuthState("authorized");
    } catch (e) {
      if (e instanceof AdminUnauthorizedError) {
        setAuthState("unauthorized");
        setStatus(null);
      } else {
        setError((e as Error).message);
      }
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  async function trigger(path: string, label: string) {
    if (!confirm(t("admin.confirmRun", { label }))) return;
    setBusy(label);
    setError(null);
    try {
      await triggerAdminImport(path);
      await refresh();
    } catch (e) {
      if (e instanceof AdminUnauthorizedError) {
        setAuthState("unauthorized");
        setStatus(null);
      } else {
        setError((e as Error).message);
      }
    } finally {
      setBusy(null);
    }
  }

  if (authState === "loading") {
    return (
      <div className="max-w-sm space-y-3 px-5 sm:px-8 md:px-10 py-10">
        <h1 className="text-2xl font-semibold text-ink">{t("admin.title")}</h1>
        <p className="text-muted text-sm">{t("admin.running")}</p>
      </div>
    );
  }

  if (authState === "unauthorized" || !status) {
    return (
      <div className="max-w-sm space-y-4 px-5 sm:px-8 md:px-10 py-10">
        <h1 className="text-2xl font-semibold text-ink">{t("admin.title")}</h1>
        <p className="text-sm text-muted">{t("admin.signInWithGoogleHelp", { defaultValue: "Sign in with your authorised Google account." })}</p>
        <a
          href={OIDC_LOGIN_URL}
          className="inline-block px-4 py-2 rounded-md bg-blue text-white hover:bg-blue-deep"
        >
          {t("admin.signInWithGoogle", { defaultValue: "Sign in with Google" })}
        </a>
        {error && <p className="text-hot-deep text-sm" role="alert">{error}</p>}
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-[1200px] mx-auto w-full px-5 sm:px-8 md:px-10 py-8 sm:py-12">
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

      <AdminAffiliationsSection onUnauthorized={() => setAuthState("unauthorized")} />

      <AdminInitiativeLinksSection onUnauthorized={() => setAuthState("unauthorized")} />

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
                  when: formatDateTime(j.lastRunAt),
                  seen: j.recordsSeen,
                  upserted: j.recordsUpserted,
                })}
                {j.errorMessage && <span className="text-red-600 block mt-1">{j.errorMessage}</span>}
              </div>
            </li>
          ))}
        </ul>
      </section>

      <p className="text-xs text-slate-400">{t("admin.generatedAt", { when: formatDateTime(status.generatedAt) })}</p>
    </div>
  );
}
