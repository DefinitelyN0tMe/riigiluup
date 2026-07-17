import { apiGet } from "./client";
import type { InitiativeListItem } from "./initiatives";

export type CommitteeListItem = {
  externalId: string;
  name: string;
  shortName: string | null;
  colorHex: string | null;
  secretariat: string | null;
  memberCount: number;
  ledBillCount: number;
  initiativeCount: number;
};

export type CommitteeMemberRef = {
  slug: string;
  name: string;
  role: string;
  factionName: string | null;
};

export type CommitteeBillRef = {
  id: string;
  mark: number | null;
  title: string;
  phase: string | null;
  initiatedDate: string | null;
};

export type CommitteeDetail = {
  externalId: string;
  name: string;
  shortName: string | null;
  colorHex: string | null;
  secretariat: string | null;
  members: CommitteeMemberRef[];
  ledBills: { total: number; recent: CommitteeBillRef[] };
  initiatives: InitiativeListItem[];
};

export function fetchCommittees(): Promise<CommitteeListItem[]> {
  return apiGet<CommitteeListItem[]>("/api/v1/committees");
}

export function fetchCommittee(externalId: string): Promise<CommitteeDetail> {
  return apiGet<CommitteeDetail>(`/api/v1/committees/${encodeURIComponent(externalId)}`);
}
