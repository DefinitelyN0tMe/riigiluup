import { apiGet } from "./client";
import type { PageResponse, PoliticianVote, VoteDetail, VoteListItem } from "../types";

export function fetchVotes(params: {
  from?: string;
  to?: string;
  type?: string;
  page?: number;
  size?: number;
}) {
  const q = new URLSearchParams();
  if (params.from) q.set("from", params.from);
  if (params.to) q.set("to", params.to);
  if (params.type) q.set("type", params.type);
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
