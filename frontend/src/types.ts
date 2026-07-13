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
  groupAlignment: GroupAlignmentDto;
  biographyHtml: string | null;
  sourceUrl: string;
};
export type FactionOption = { externalId: string; name: string; memberCount: number };

export type VoteListItem = {
  id: string;
  externalId: string;
  votingNumber: number | null;
  type: "OPEN" | "ATTENDANCE_CHECK" | "SECRET" | "OTHER";
  typeSourceCode: string | null;
  description: string | null;
  sittingTitle: string | null;
  startedAt: string | null;
  resultInFavor: number;
  resultAgainst: number;
  resultAbstained: number;
  resultNeutral: number;
  resultPresent: number;
  resultAbsent: number;
  sourceUrl: string;
};

export type VoteFactionBreakdown = {
  factionExternalId: string | null;
  factionName: string;
  inFavor: number;
  against: number;
  abstained: number;
  didNotVote: number;
  absent: number;
  present: number;
  unknown: number;
  total: number;
};

export type VoteIndividual = {
  memberExternalId: string | null;
  memberSlug: string | null;
  memberFullName: string | null;
  factionExternalId: string | null;
  factionName: string | null;
  choice: "FOR" | "AGAINST" | "ABSTAINED" | "DID_NOT_VOTE" | "ABSENT" | "PRESENT" | "UNKNOWN";
  choiceSourceCode: string | null;
};

export type VoteDetail = VoteListItem & {
  sittingExternalId: string | null;
  endedAt: string | null;
  factionBreakdowns: VoteFactionBreakdown[];
  individualVotes: VoteIndividual[];
};

export type PoliticianVote = {
  voteEventId: string;
  voteEventExternalId: string;
  description: string | null;
  type: "OPEN" | "ATTENDANCE_CHECK" | "SECRET" | "OTHER" | null;
  startedAt: string | null;
  choice: VoteIndividual["choice"];
  choiceSourceCode: string | null;
};

export type ComparisonSide = {
  id: string;
  slug: string;
  fullName: string;
  factionName: string | null;
  partyShortName: string | null;
  photoUrl: string | null;
  groupAlignmentRate: number | null;
  groupAlignmentMatches: number;
  groupAlignmentEligible: number;
};

export type PairwiseAgreement = {
  sameCount: number;
  diffCount: number;
  oneNotParticipatingCount: number;
  totalOverlap: number;
  agreementRate: number | null;
  methodologyNote: string;
};

export type ComparisonDisagreement = {
  voteEventId: string;
  voteEventDescription: string | null;
  voteType: string | null;
  startedAt: string | null;
  leftChoice: string;
  rightChoice: string;
};

export type ComparisonResponse = {
  left: ComparisonSide;
  right: ComparisonSide;
  period: { from: string; to: string };
  agreement: PairwiseAgreement;
  recentDisagreements: ComparisonDisagreement[];
};

export type GroupAlignmentDto = {
  rate: number | null;
  matches: number;
  eligible: number;
  recentDeviations: Array<{
    voteEventId: string;
    voteEventDescription: string | null;
    voteType: string | null;
    startedAt: string | null;
    memberChoice: string | null;
    factionMajorityChoice: string | null;
  }>;
  methodologyNote: string;
};
