export type VoteTally = {
  inFavor: number;
  against: number;
  abstained: number;
  didNotVote: number;
  absent: number;
};

type VoteResultFields = {
  resultInFavor: number;
  resultAgainst: number;
  resultNeutral: number;
  resultPresent: number;
  resultAbsent: number;
};

/**
 * A clean, non-overlapping 5-way breakdown of a vote that sums to the seat count.
 *
 * The Riigikogu source columns overlap and must NOT be displayed together:
 *  - `resultPresent` is the whole in-hall count (for + against + abstained + did-not-vote),
 *  - `resultAbstained` double-counts (did-not-vote + absent),
 * so naively showing inFavor/against/abstained/present/absent sums to ~2× the seats.
 *
 * The real partition (verified against the individual votes for every stored voting) is:
 *  - abstained  = resultNeutral                                  (the true "erapooletu")
 *  - didNotVote = resultPresent − inFavor − against − neutral    (present but cast nothing)
 *  - absent     = resultAbsent
 */
export function voteTally(v: VoteResultFields): VoteTally {
  return {
    inFavor: v.resultInFavor,
    against: v.resultAgainst,
    abstained: v.resultNeutral,
    didNotVote: Math.max(0, v.resultPresent - v.resultInFavor - v.resultAgainst - v.resultNeutral),
    absent: v.resultAbsent,
  };
}
