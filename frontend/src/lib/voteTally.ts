export type VoteTally = {
  inFavor: number;
  against: number;
  abstained: number;
  didNotVote: number;
  /** In-hall count of an attendance check (kohaloleku kontroll); 0 for every real vote. */
  present: number;
  absent: number;
};

type VoteResultFields = {
  resultInFavor: number;
  resultAgainst: number;
  resultNeutral: number;
  resultPresent: number;
  resultAbsent: number;
  type?: string | null;
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
 *
 * An attendance check (type ATTENDANCE_CHECK) is not a vote: nobody votes for or against, the
 * source only records who pressed the button. Its in-hall count is `present`, never "did not vote".
 */
export function voteTally(v: VoteResultFields): VoteTally {
  if (v.type === "ATTENDANCE_CHECK") {
    return { inFavor: 0, against: 0, abstained: 0, didNotVote: 0, present: v.resultPresent, absent: v.resultAbsent };
  }
  return {
    inFavor: v.resultInFavor,
    against: v.resultAgainst,
    abstained: v.resultNeutral,
    didNotVote: Math.max(0, v.resultPresent - v.resultInFavor - v.resultAgainst - v.resultNeutral),
    present: 0,
    absent: v.resultAbsent,
  };
}
