export type Politician = {
  id: string;
  slug: string;
  fullName: string;
  firstName: string;
  lastName: string;
  photoUrl: string | null;
  officialProfileUrl: string | null;
  active: boolean;
  factionName: string | null;
  externalId: string;
  sourceUrl: string;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type DataStatus = {
  sourceName: string;
  jobName: string;
  lastRunAt: string | null;
  lastRunStatus: string;
  lastRunRecords: number;
};

export type Faction = { externalId: string; name: string };
export type Party = {
  shortName: string;
  fullName: string;
  colorHex: string | null;
  officialUrl: string | null;
};
export type CommitteeMembership = {
  name: string;
  shortName: string | null;
  colorHex: string | null;
  role: "MEMBER" | "CHAIR" | "VICE_CHAIR" | "REPRESENTATIVE" | "OTHER";
  active: boolean;
};
export type ParticipationStats = {
  totalSittings: number;
  attended: number;
  participationRate: number | null;
  methodologyNote: string;
  sourceUrl: string;
};
export type VotingStats = {
  totalVotings: number;
  participated: number;
  participationRate: number | null;
  methodologyNote: string;
  sourceUrl: string;
};
export type PoliticianProfile = {
  id: string;
  slug: string;
  fullName: string;
  firstName: string;
  lastName: string;
  photoUrl: string | null;
  officialProfileUrl: string | null;
  email: string | null;
  gender: string | null;
  dateOfBirth: string | null;
  electoralDistrict: string | null;
  parliamentSeniorityDays: number | null;
  faction: Faction | null;
  party: Party | null;
  committees: CommitteeMembership[];
  participation: ParticipationStats;
  voting: VotingStats;
  biographyHtml: string | null;
  sourceUrl: string;
};
export type FactionOption = { externalId: string; name: string; memberCount: number };
