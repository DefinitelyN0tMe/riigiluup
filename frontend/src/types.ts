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
export type ExternalAffiliation = {
  organization: string;
  orgKind: "PARTY" | "MOVEMENT" | "FRACTION_ORIGINAL" | "INDEPENDENT" | string;
  role: string | null;
  validFrom: string;             // ISO date
  validTo: string | null;         // ISO date or null (= current)
  sourceUrl: string;
  sourceLabel: string;
  verifiedBy: string;
  verifiedAt: string;
  note: string | null;
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
  active: boolean;
  wikidataQid: string | null;
  wikipediaUrlEn: string | null;
  wikipediaUrlEt: string | null;
  wikipediaUrlRu: string | null;
  faction: Faction | null;
  party: Party | null;
  committees: CommitteeMembership[];
  participation: ParticipationStats;
  attendanceChecks?: ParticipationStats;
  voting: VotingStats;
  groupAlignment: GroupAlignmentDto;
  biographyHtml: string | null;
  sourceUrl: string;
  externalAffiliations: ExternalAffiliation[];
  election?: ElectionInfo | null;
};
export type ElectionInfo = {
  electionCode: string;
  personalVotes: number;
  mandateType: "PERSONAL" | "DISTRICT" | "COMPENSATION" | string;
  districtNumber: number | null;
  partyName: string | null;
  ballotNumber: number | null;
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
  linkedBill: {
    id: string;
    externalId: string;
    mark: number | null;
    title: string;
    phase: string;
  } | null;
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

export type LegislationListItem = {
  id: string;
  externalId: string;
  mark: number | null;
  draftTypeCode: string | null;
  title: string;
  phase: "SUBMITTED" | "IN_COMMITTEE" | "IN_READINGS" | "ADOPTED" | "REJECTED" | "WITHDRAWN" | "OTHER";
  activeStageSourceCode: string | null;
  initiatedDate: string | null;
  acceptedDate: string | null;
  leadingCommitteeName: string | null;
  sourceUrl: string;
};

export type LegislationStage = {
  readingCode: string | null;
  statusCode: string | null;
  occurredAt: string | null;
  sequence: number;
};

export type LegislationSponsor = {
  kind: "PLENARY_MEMBER" | "FACTION" | "COMMITTEE" | "ORGAN" | "OTHER";
  displayName: string | null;
  memberSlug: string | null;
  memberFullName: string | null;
  externalId: string | null;
};

export type LegislationTopic = { edid: number; text: string };

export type LegislationDetail = LegislationListItem & {
  membership: number | null;
  initialTitle: string | null;
  activeStatusSourceCode: string | null;
  proceedingStatus: string | null;
  activeStatusDate: string | null;
  amendmentsDeadline: string | null;
  introduction: string | null;
  stages: LegislationStage[];
  sponsors: LegislationSponsor[];
  topics: LegislationTopic[];
};

export type PoliticianLegislationResponse = {
  totalSponsored: number;
  items: PageResponse<LegislationListItem>;
};

export type AdminStatus = {
  jobs: Array<{
    sourceName: string;
    jobName: string;
    lastRunStatus: string;
    lastRunAt: string | null;
    recordsSeen: number;
    recordsUpserted: number;
    errorMessage: string | null;
  }>;
  snapshotsByEntity: Array<{ entityType: string; count: number }>;
  counts: {
    plenaryMembers: number;
    groups: number;
    voteEvents: number;
    individualVotes: number;
    legislativeItems: number;
  };
  generatedAt: string;
};
