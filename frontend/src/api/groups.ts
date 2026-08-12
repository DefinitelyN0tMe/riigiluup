import { apiGet } from "./client";
import type { GroupDirectoryItem, GroupDirectoryDetail } from "../types";

export function fetchGroups(): Promise<GroupDirectoryItem[]> {
  return apiGet<GroupDirectoryItem[]>("/api/v1/groups");
}

export function fetchGroup(externalId: string): Promise<GroupDirectoryDetail> {
  return apiGet<GroupDirectoryDetail>(`/api/v1/groups/${encodeURIComponent(externalId)}`);
}
