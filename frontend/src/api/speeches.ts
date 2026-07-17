import { apiGet } from "./client";

export type SpeechItem = {
  id: number;
  speakerRaw: string;
  memberSlug: string | null;
  memberName: string | null;
  spokenAt: string;
  sittingTitle: string | null;
  agendaItemTitle: string | null;
  /** Search hits are wrapped in [[ ]] delimiters; render via <Excerpt/>, never innerHTML. */
  excerpt: string;
  sourceUrl: string;
};

export type SpeechPage = {
  items: SpeechItem[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export function fetchSpeeches(params: {
  q?: string;
  member?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}): Promise<SpeechPage> {
  const sp = new URLSearchParams();
  if (params.q) sp.set("q", params.q);
  if (params.member) sp.set("member", params.member);
  if (params.from) sp.set("from", params.from);
  if (params.to) sp.set("to", params.to);
  if (params.page) sp.set("page", String(params.page));
  if (params.size) sp.set("size", String(params.size));
  const qs = sp.toString();
  return apiGet<SpeechPage>(`/api/v1/speeches${qs ? `?${qs}` : ""}`);
}
