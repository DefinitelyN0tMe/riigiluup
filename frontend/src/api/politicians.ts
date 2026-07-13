import { apiGet } from "./client";
import type { DataStatus, PageResponse, Politician } from "../types";

export function fetchPoliticians(params: { q?: string; activeOnly?: boolean; page?: number; size?: number }) {
  const query = new URLSearchParams();
  if (params.q) query.set("q", params.q);
  if (params.activeOnly !== undefined) query.set("activeOnly", String(params.activeOnly));
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  const suffix = query.toString() ? `?${query.toString()}` : "";
  return apiGet<PageResponse<Politician>>(`/api/v1/politicians${suffix}`);
}

export function fetchDataStatus() {
  return apiGet<DataStatus[]>("/api/v1/data-status");
}
