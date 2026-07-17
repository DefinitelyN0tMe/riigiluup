import { apiGet } from "./client";

export type CommitteeRef = {
  slug: string;
  name: string | null;
  groupId: string | null;
};

export type InitiativeListItem = {
  id: number;
  externalId: string;
  title: string | null;
  authors: string | null;
  phase: string | null;
  signatureCount: number | null;
  decision: string | null;
  sentToParliamentAt: string | null;
  committees: CommitteeRef[];
  sourceUrl: string;
};

export type InitiativeListPage = {
  items: InitiativeListItem[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export type InitiativeDetail = {
  id: number;
  externalId: string;
  title: string | null;
  authors: string | null;
  phase: string | null;
  signatureCount: number | null;
  threshold: number | null;
  publishedAt: string | null;
  signingStartedAt: string | null;
  signingEndsAt: string | null;
  lastSignedAt: string | null;
  sentToParliamentAt: string | null;
  decision: string | null;
  finishedInParliamentAt: string | null;
  sentToGovernmentAt: string | null;
  finishedInGovernmentAt: string | null;
  committees: CommitteeRef[];
  linkedBill: { id: string; title: string; linkedBy: string | null; linkedAt: string | null } | null;
  sourceUrl: string;
};

export type FunnelStep = { key: string; count: number; shareOfTargeted: number | null };

export type InitiativeFunnel = {
  steps: FunnelStep[];
  decisions: { decision: string; count: number }[];
  committees: { slug: string; name: string | null; groupId: string | null; count: number }[];
  medianDaysToDecision: number | null;
  medianSampleSize: number;
  reachedThresholdButNeverSent: number;
  sentBelowThreshold: number;
};

export function fetchInitiatives(params: {
  q?: string;
  phase?: string;
  decision?: string;
  committee?: string;
  page?: number;
  size?: number;
}): Promise<InitiativeListPage> {
  const sp = new URLSearchParams();
  if (params.q) sp.set("q", params.q);
  if (params.phase) sp.set("phase", params.phase);
  if (params.decision) sp.set("decision", params.decision);
  if (params.committee) sp.set("committee", params.committee);
  if (params.page) sp.set("page", String(params.page));
  if (params.size) sp.set("size", String(params.size));
  const qs = sp.toString();
  return apiGet<InitiativeListPage>(`/api/v1/initiatives${qs ? `?${qs}` : ""}`);
}

export function fetchInitiative(id: string): Promise<InitiativeDetail> {
  return apiGet<InitiativeDetail>(`/api/v1/initiatives/${id}`);
}

export function fetchInitiativeFunnel(): Promise<InitiativeFunnel> {
  return apiGet<InitiativeFunnel>("/api/v1/analytics/initiative-funnel");
}
