import type { AdminAnalytics, AdminStatus } from "../types";

const BASE = import.meta.env.VITE_API_BASE_URL ?? "";

/** Thrown when the admin API returns 401 — SPA renders the sign-in button. */
export class AdminUnauthorizedError extends Error {
  constructor(msg = "Unauthorized") {
    super(msg);
    this.name = "AdminUnauthorizedError";
  }
}

async function adminFetch(path: string, init: RequestInit = {}) {
  const res = await fetch(`${BASE}${path}`, {
    credentials: "include",
    headers: { Accept: "application/json", ...(init.body ? { "Content-Type": "application/json" } : {}), ...init.headers },
    ...init,
  });
  if (res.status === 401) throw new AdminUnauthorizedError();
  if (!res.ok) {
    let msg = `Admin API ${res.status}`;
    try {
      const body = await res.json();
      if (body?.message) msg = body.message;
    } catch { /* ignore */ }
    throw new Error(msg);
  }
  return res;
}

export async function fetchAdminStatus(): Promise<AdminStatus> {
  const res = await adminFetch("/api/v1/admin/status");
  return res.json();
}

/** Visitor analytics from the self-hosted Umami, proxied through the admin API. */
export async function fetchAdminAnalytics(range: "24h" | "7d" | "30d"): Promise<AdminAnalytics> {
  const res = await adminFetch(`/api/v1/admin/analytics/summary?range=${range}`);
  return res.json();
}

export async function triggerAdminImport(path: string): Promise<unknown> {
  const res = await adminFetch(path, { method: "POST" });
  return res.json();
}

/* ============ Full historical seed (multi-source backfill) ============ */
export type BackfillRunStatus = {
  runId: string;
  status: "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED" | string;
  fromDate: string;
  toDate: string;
  currentWindowStart: string;
  kinds: string;
  phase: string | null;
  startedAt: string | null;
  endedAt: string | null;
  billsImported: number;
  votesImported: number;
  windowsCompleted: number;
  windowsTotal: number;
  stepCounts: string | null;
  errorMessage: string | null;
};

/** Kick off the dependency-ordered seed. `from` is YYYY-MM-DD; `kinds` defaults to ALL. */
export async function startFullBackfill(from: string, kinds = "ALL"): Promise<BackfillRunStatus> {
  const q = `?from=${encodeURIComponent(from)}&kinds=${encodeURIComponent(kinds)}`;
  const res = await adminFetch(`/api/v1/admin/backfill/full${q}`, { method: "POST" });
  return res.json();
}

/** Latest seed run for polling progress; null when none has ever run (404). */
export async function fetchLatestBackfill(): Promise<BackfillRunStatus | null> {
  try {
    const res = await adminFetch(`/api/v1/admin/backfill/full/latest`);
    return res.json();
  } catch (e) {
    if (e instanceof Error && e.message.includes("404")) return null;
    throw e;
  }
}

/* ============ External affiliations CRUD ============ */
export type AdminAffiliation = {
  id: string;
  memberSlug: string;
  organization: string;
  orgKind: "PARTY" | "MOVEMENT" | "FRACTION_ORIGINAL" | "INDEPENDENT" | string;
  role: string | null;
  validFrom: string;
  validTo: string | null;
  sourceUrl: string;
  sourceLabel: string;
  verifiedBy: string;
  verifiedAt: string;
  note: string | null;
  createdAt: string;
};

export type AdminAffiliationUpsert = {
  memberSlug: string;
  organization: string;
  orgKind: string;
  role?: string | null;
  validFrom: string;
  validTo?: string | null;
  sourceUrl: string;
  sourceLabel: string;
  verifiedBy?: string | null;
  verifiedAt?: string | null;
  note?: string | null;
};

export async function listAffiliations(slug?: string): Promise<AdminAffiliation[]> {
  const q = slug ? `?slug=${encodeURIComponent(slug)}` : "";
  const res = await adminFetch(`/api/v1/admin/external-affiliations${q}`);
  return res.json();
}

export async function createAffiliation(body: AdminAffiliationUpsert): Promise<AdminAffiliation> {
  const res = await adminFetch(`/api/v1/admin/external-affiliations`, {
    method: "POST",
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function updateAffiliation(id: string, body: AdminAffiliationUpsert): Promise<AdminAffiliation> {
  const res = await adminFetch(`/api/v1/admin/external-affiliations/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function deleteAffiliation(id: string): Promise<void> {
  await adminFetch(`/api/v1/admin/external-affiliations/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

/* ============ Initiative → bill link ============ */
export async function linkInitiativeToBill(id: number, legislativeItemId: string): Promise<void> {
  await adminFetch(`/api/v1/admin/initiatives/${id}/legislative-item`, {
    method: "PUT",
    body: JSON.stringify({ legislativeItemId }),
  });
}

export async function unlinkInitiativeFromBill(id: number): Promise<void> {
  await adminFetch(`/api/v1/admin/initiatives/${id}/legislative-item`, {
    method: "DELETE",
  });
}
