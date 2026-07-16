import { apiGet } from "./client";

export type FactionCell = {
  externalId: string;
  name: string;
  shortName: string;
  colorHex: string | null;
  seats: number;
};
export type FactionAgreementMatrix = {
  factions: FactionCell[];
  matrix: (number | null)[][];
  support: number[][];
  totalVotesConsidered: number;
  computedAt: string;
};

export type DisciplineBreaker = {
  memberSlug: string;
  memberName: string;
  factionExternalId: string | null;
  factionName: string | null;
  factionShortName: string | null;
  factionColorHex: string | null;
  deviations: number;
  eligible: number;
  deviationRate: number;
  exampleVoteDescription: string | null;
  exampleVoteDate: string | null;
  exampleVoteId: string | null;
};
export type DisciplineBreakers = { items: DisciplineBreaker[]; computedAt: string };

export type MemberActivityItem = {
  memberSlug: string;
  memberName: string;
  factionShortName: string | null;
  factionColorHex: string | null;
  speeches: number;
  questions: number;
  interpellations: number;
  writtenQuestions: number;
};
export type MemberActivityBoard = { items: MemberActivityItem[]; computedAt: string };
export type ActivityMetric = "speeches" | "questions" | "interpellations" | "writtenQuestions";

export type BillFlowNode = { id: string; label: string; count: number };
export type BillFlowLink = { source: string; target: string; count: number };
export type BillFlow = { nodes: BillFlowNode[]; links: BillFlowLink[]; totalBills: number; computedAt: string };

export type AttendanceRow = {
  slug: string;
  shortName: string;
  factionShortName: string | null;
  factionColorHex: string | null;
  presentCount: number;
  totalChecks: number;
};
export type AttendanceCol = { sittingExternalId: string; date: string; label: string };
export type AttendanceMatrix = {
  members: AttendanceRow[];
  sittings: AttendanceCol[];
  cells: string[];   // row-major, "P" | "A" | "-"
  membersCount: number;
  sittingsCount: number;
};

export type VoteTimingHeatmap = { cells: number[][]; totalVotes: number; maxCell: number };

export type TopicSlice = { edid: number; label: string; billCount: number; adoptedCount: number };
export type TopicTreemap = { items: TopicSlice[]; totalBills: number };

export type VelocityBucket = { label: string; daysMax: number; count: number };
export type BillVelocity = {
  buckets: VelocityBucket[];
  totalAdopted: number;
  medianDays: number;
  p90Days: number;
  fastestDays: number;
  slowestDays: number;
};

export type MpPoint = {
  slug: string;
  name: string;
  factionShortName: string | null;
  factionColorHex: string | null;
  x: number;
  y: number;
  totalComparableVotes: number;
  deviations: number;
};
export type MpSimilarity = { points: MpPoint[]; computedAt: string };

export type CoSponsorNode = {
  slug: string;
  name: string;
  factionShortName: string | null;
  factionColorHex: string | null;
  billsSponsored: number;
};
export type CoSponsorEdge = { source: string; target: string; weight: number };
export type CoSponsorship = { nodes: CoSponsorNode[]; edges: CoSponsorEdge[]; totalBillsConsidered: number };

export type PartyOfDay = {
  factionExternalId: string;
  factionName: string;
  factionShortName: string | null;
  colorHex: string | null;
  unity: number;
  votesConsidered: number;
};
export type AttendanceStreak = {
  memberSlug: string;
  memberName: string;
  factionShortName: string | null;
  consecutivePresent: number;
  totalRecent: number;
};
export type VoteMargin = {
  voteId: string;
  voteNumber: number | null;
  description: string | null;
  startedAt: string | null;
  forCount: number;
  againstCount: number;
  margin: number;
};
export type HighlightsBundle = {
  partyOfWeek: PartyOfDay | null;
  streaks: AttendanceStreak[];
  tightVotes: VoteMargin[];
};

export type MpTopicRadar = { slug: string; topics: TopicSlice[]; totalBills: number };

export type NightVoteItem = {
  voteId: string;
  voteNumber: number | null;
  description: string | null;
  startedAt: string | null;
  hourOfDay: number;
  dayOfWeek: number;      // 1=Mon..7=Sun
  weekend: boolean;
  lateNight: boolean;
  forCount: number;
  againstCount: number;
  margin: number;
  linkedBillId: string | null;
  linkedBillTitle: string | null;
};
export type NightHourBucket = { hourOfDay: number; total: number; weekday: number; weekend: number };
export type NightVotes = {
  totalVotes: number;
  nightVotes: number;
  weekendVotes: number;
  lateNightVotes: number;
  nightRatio: number;
  windowStartHour: number;
  windowEndHour: number;
  items: NightVoteItem[];
  hourDistribution: NightHourBucket[];
  computedAt: string;
};

const B = "/api/v1/analytics";

export const fetchFactionAgreement = () => apiGet<FactionAgreementMatrix>(`${B}/faction-agreement`);
export const fetchDisciplineBreakers = (limit = 24) =>
  apiGet<DisciplineBreakers>(`${B}/discipline-breakers?limit=${limit}`);
export const fetchMemberActivity = () => apiGet<MemberActivityBoard>(`${B}/member-activity`);
export const fetchBillFlow = () => apiGet<BillFlow>(`${B}/bill-flow`);
export const fetchAttendanceMatrix = (sittings = 40) =>
  apiGet<AttendanceMatrix>(`${B}/attendance-matrix?sittings=${sittings}`);
export const fetchVoteTiming = () => apiGet<VoteTimingHeatmap>(`${B}/vote-timing`);
export const fetchTopicTreemap = (limit = 24) => apiGet<TopicTreemap>(`${B}/topic-treemap?limit=${limit}`);
export const fetchBillVelocity = () => apiGet<BillVelocity>(`${B}/bill-velocity`);
export const fetchMpSimilarity = () => apiGet<MpSimilarity>(`${B}/mp-similarity`);
export const fetchCoSponsorship = (minWeight = 2) =>
  apiGet<CoSponsorship>(`${B}/co-sponsorship?minWeight=${minWeight}`);
export const fetchHighlights = () => apiGet<HighlightsBundle>(`${B}/highlights`);
export const fetchMpTopicRadar = (slug: string, limit = 8) =>
  apiGet<MpTopicRadar>(`${B}/mp-topic-radar/${encodeURIComponent(slug)}?limit=${limit}`);
export const fetchNightVotes = (limit = 20) =>
  apiGet<NightVotes>(`${B}/night-votes?limit=${limit}`);

export type DeviationDayCell = { date: string; deviations: number; eligible: number };
export type MpDeviationsTimeline = {
  slug: string;
  days: DeviationDayCell[];
  totalDeviations: number;
  totalEligible: number;
  rangeFrom: string;
  rangeTo: string;
};
export const fetchMpDeviationsTimeline = (slug: string, months = 12) =>
  apiGet<MpDeviationsTimeline>(`${B}/mp-deviations-timeline/${encodeURIComponent(slug)}?months=${months}`);

export type PeerAgreement = {
  slug: string;
  name: string;
  factionShortName: string | null;
  factionColorHex: string | null;
  agreementRate: number;
  overlap: number;
  sameCount: number;
  diffCount: number;
};
export type MpSimilarPeers = {
  slug: string;
  mostSimilar: PeerAgreement[];
  mostOpposite: PeerAgreement[];
  minOverlap: number;
  computedAt: string;
};
export const fetchMpSimilarPeers = (slug: string, limit = 5) =>
  apiGet<MpSimilarPeers>(`${B}/mp-similar-peers/${encodeURIComponent(slug)}?limit=${limit}`);
