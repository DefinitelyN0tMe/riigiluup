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
