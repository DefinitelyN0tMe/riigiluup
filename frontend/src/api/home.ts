import { apiGet } from "./client";
import type { HomeSummary } from "../types";

/** Live figures for the homepage stat strip (deltas, sparklines, sync times). */
export function fetchHomeSummary() {
  return apiGet<HomeSummary>("/api/v1/home/summary");
}
