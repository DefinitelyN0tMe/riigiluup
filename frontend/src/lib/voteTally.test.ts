import { describe, expect, it } from "vitest";
import { voteTally } from "./voteTally";

describe("voteTally", () => {
  it("partitions a roll-call into five non-overlapping parts that sum to the seats", () => {
    // Real shape: the source's resultAbstained (39) overlaps did-not-vote + absent and must not be used.
    const t = voteTally({ resultInFavor: 43, resultAgainst: 19, resultNeutral: 0, resultPresent: 93, resultAbsent: 8, type: "OPEN" });
    expect(t).toEqual({ inFavor: 43, against: 19, abstained: 0, didNotVote: 31, present: 0, absent: 8 });
    expect(t.inFavor + t.against + t.abstained + t.didNotVote + t.present + t.absent).toBe(101);
  });

  it("uses resultNeutral as the real abstentions", () => {
    const t = voteTally({ resultInFavor: 50, resultAgainst: 30, resultNeutral: 5, resultPresent: 90, resultAbsent: 11 });
    expect(t.abstained).toBe(5);
    expect(t.didNotVote).toBe(5);
  });

  it("shows an attendance check as present/absent, never as 'did not vote'", () => {
    const t = voteTally({ resultInFavor: 0, resultAgainst: 0, resultNeutral: 0, resultPresent: 91, resultAbsent: 10, type: "ATTENDANCE_CHECK" });
    expect(t).toEqual({ inFavor: 0, against: 0, abstained: 0, didNotVote: 0, present: 91, absent: 10 });
  });

  it("never goes negative on inconsistent source totals", () => {
    const t = voteTally({ resultInFavor: 60, resultAgainst: 40, resultNeutral: 5, resultPresent: 100, resultAbsent: 1 });
    expect(t.didNotVote).toBe(0);
  });
});
