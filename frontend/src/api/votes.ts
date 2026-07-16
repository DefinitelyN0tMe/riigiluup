import { apiGet } from "./client";
import type { PageResponse, PoliticianVote, VoteDetail, VoteListItem } from "../types";

export function fetchVotes(params: {
  from?: string;
  to?: string;
  type?: string;
  hour?: number;
  dow?: number;              // 0=Mon..6=Sun
  onlyWeekend?: boolean;
  nightOnly?: boolean;
  lateOnly?: boolean;
  factionA?: string;
  factionB?: string;
  page?: number;
  size?: number;
}) {
  const q = new URLSearchParams();
  if (params.from) q.set("from", params.from);
  if (params.to) q.set("to", params.to);
  if (params.type) q.set("type", params.type);
  if (params.hour !== undefined) q.set("hour", String(params.hour));
  if (params.dow !== undefined) q.set("dow", String(params.dow));
  if (params.onlyWeekend) q.set("onlyWeekend", "true");
  if (params.nightOnly) q.set("nightOnly", "true");
  if (params.lateOnly) q.set("lateOnly", "true");
  if (params.factionA) q.set("factionA", params.factionA);
  if (params.factionB) q.set("factionB", params.factionB);
  if (params.page !== undefined) q.set("page", String(params.page));
  if (params.size !== undefined) q.set("size", String(params.size));
  const suffix = q.toString() ? `?${q.toString()}` : "";
  return apiGet<PageResponse<VoteListItem>>(`/api/v1/votes${suffix}`);
}

export function fetchVoteDetail(id: string) {
  return apiGet<VoteDetail>(`/api/v1/votes/${encodeURIComponent(id)}`);
}

export function fetchPoliticianVotes(slug: string, page = 0, size = 50) {
  return apiGet<PageResponse<PoliticianVote>>(
    `/api/v1/politicians/${encodeURIComponent(slug)}/votes?page=${page}&size=${size}`
  );
}
