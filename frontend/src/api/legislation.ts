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
  topicEdid?: number;
  minDays?: number;
  maxDays?: number;
  committee?: string;
  page?: number;
  size?: number;
}) {
  const q = new URLSearchParams();
  if (params.q) q.set("q", params.q);
  if (params.phase) q.set("phase", params.phase);
  if (params.committee) q.set("committee", params.committee);
  if (params.membership !== undefined) q.set("membership", String(params.membership));
  if (params.topicEdid !== undefined) q.set("topicEdid", String(params.topicEdid));
  if (params.minDays !== undefined) q.set("minDays", String(params.minDays));
  if (params.maxDays !== undefined) q.set("maxDays", String(params.maxDays));
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
