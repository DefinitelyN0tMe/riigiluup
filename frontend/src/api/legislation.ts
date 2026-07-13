import { apiGet } from "./client";
import type {
  LegislationDetail,
  LegislationListItem,
  PageResponse,
  PoliticianLegislationResponse,
} from "../types";

export function fetchLegislation(params: {
  q?: string;
  phase?: string;
  membership?: number;
  page?: number;
  size?: number;
}) {
  const q = new URLSearchParams();
  if (params.q) q.set("q", params.q);
  if (params.phase) q.set("phase", params.phase);
  if (params.membership !== undefined) q.set("membership", String(params.membership));
  if (params.page !== undefined) q.set("page", String(params.page));
  if (params.size !== undefined) q.set("size", String(params.size));
  const suffix = q.toString() ? `?${q.toString()}` : "";
  return apiGet<PageResponse<LegislationListItem>>(`/api/v1/legislation${suffix}`);
}

export function fetchLegislationDetail(id: string) {
  return apiGet<LegislationDetail>(`/api/v1/legislation/${encodeURIComponent(id)}`);
}

export function fetchPoliticianLegislation(slug: string, page = 0, size = 20) {
  return apiGet<PoliticianLegislationResponse>(
    `/api/v1/politicians/${encodeURIComponent(slug)}/legislation?page=${page}&size=${size}`
  );
}
