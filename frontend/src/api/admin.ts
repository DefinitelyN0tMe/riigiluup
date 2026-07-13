import type { AdminStatus } from "../types";

const BASE = import.meta.env.VITE_API_BASE_URL ?? "";

export async function fetchAdminStatus(username: string, password: string): Promise<AdminStatus> {
  const auth = btoa(`${username}:${password}`);
  const res = await fetch(`${BASE}/api/v1/admin/status`, {
    headers: { Authorization: `Basic ${auth}`, Accept: "application/json" },
  });
  if (res.status === 401) throw new Error("Invalid admin credentials.");
  if (!res.ok) throw new Error(`Admin API ${res.status}`);
  return res.json();
}

export async function triggerAdminImport(
    username: string, password: string, path: string
): Promise<unknown> {
  const auth = btoa(`${username}:${password}`);
  const res = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers: { Authorization: `Basic ${auth}`, Accept: "application/json" },
  });
  if (!res.ok) throw new Error(`Admin API ${res.status}`);
  return res.json();
}
