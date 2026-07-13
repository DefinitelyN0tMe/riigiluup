import { apiGet } from "./client";
import type { DataStatus, FactionOption, PageResponse, Politician, PoliticianProfile } from "../types";

export function fetchPoliticians(params: {
  q?: string;
  faction?: string;
  activeOnly?: boolean;
  page?: number;
  size?: number;
}) {
  const query = new URLSearchParams();
  if (params.q) query.set("q", params.q);
  if (params.faction) query.set("faction", params.faction);
  if (params.activeOnly !== undefined) query.set("activeOnly", String(params.activeOnly));
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  const suffix = query.toString() ? `?${query.toString()}` : "";
  return apiGet<PageResponse<Politician>>(`/api/v1/politicians${suffix}`);
}

export function fetchDataStatus() {
  return apiGet<DataStatus[]>("/api/v1/data-status");
}

export function fetchProfile(slug: string) {
  return apiGet<PoliticianProfile>(`/api/v1/politicians/${encodeURIComponent(slug)}`);
}

export function fetchFactions() {
  return apiGet<FactionOption[]>("/api/v1/politicians/factions");
}
