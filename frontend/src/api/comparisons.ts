import { apiGet } from "./client";
import type { ComparisonResponse } from "../types";

export function fetchComparison(leftSlug: string, rightSlug: string) {
  const q = new URLSearchParams({ leftSlug, rightSlug });
  return apiGet<ComparisonResponse>(`/api/v1/comparisons/politicians?${q.toString()}`);
}
