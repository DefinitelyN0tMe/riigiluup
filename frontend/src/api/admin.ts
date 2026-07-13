import type { AdminStatus } from "../types";

const BASE = import.meta.env.VITE_API_BASE_URL ?? "";

/** Thrown when the admin API returns 401 — SPA renders the sign-in button. */
export class AdminUnauthorizedError extends Error {
  constructor(msg = "Unauthorized") {
    super(msg);
    this.name = "AdminUnauthorizedError";
  }
}

export async function fetchAdminStatus(): Promise<AdminStatus> {
  const res = await fetch(`${BASE}/api/v1/admin/status`, {
    credentials: "include",
    headers: { Accept: "application/json" },
  });
  if (res.status === 401) throw new AdminUnauthorizedError();
  if (!res.ok) throw new Error(`Admin API ${res.status}`);
  return res.json();
}

export async function triggerAdminImport(path: string): Promise<unknown> {
  const res = await fetch(`${BASE}${path}`, {
    method: "POST",
    credentials: "include",
    headers: { Accept: "application/json" },
  });
  if (res.status === 401) throw new AdminUnauthorizedError();
  if (!res.ok) throw new Error(`Admin API ${res.status}`);
  return res.json();
}
