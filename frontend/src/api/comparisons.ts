import { apiGet } from "./client";
import type { ComparisonResponse, FactionComparison } from "../types";

export function fetchComparison(leftSlug: string, rightSlug: string) {
  const q = new URLSearchParams({ leftSlug, rightSlug });
  return apiGet<ComparisonResponse>(`/api/v1/comparisons/politicians?${q.toString()}`);
}

export function fetchFactionComparison(left: string, right: string) {
  const q = new URLSearchParams({ left, right });
  return apiGet<FactionComparison>(`/api/v1/comparisons/factions?${q.toString()}`);
}
