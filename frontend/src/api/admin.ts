import type { AdminStatus } from "../types";

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

export async function triggerAdminImport(path: string): Promise<unknown> {
  const res = await adminFetch(path, { method: "POST" });
  return res.json();
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
