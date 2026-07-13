package com.politico.alignment;

import com.politico.vote.IndividualVote;
import com.politico.vote.VoteChoice;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class FactionAlignmentComputer {

    public Result compute(List<IndividualVote> votes) {
        Map<VoteChoice, Integer> tally = new EnumMap<>(VoteChoice.class);
        int comparable = 0;
        for (IndividualVote iv : votes) {
            VoteChoice c = iv.getChoice();
            if (!isComparable(c)) continue;
            comparable++;
            tally.merge(c, 1, Integer::sum);
        }
        if (comparable == 0) return new Result(null, 0, 0, false);

        VoteChoice best = null;
        int bestCount = 0;
        boolean tied = false;
        for (Map.Entry<VoteChoice, Integer> e : tally.entrySet()) {
            int n = e.getValue();
            if (n > bestCount) {
                best = e.getKey();
                bestCount = n;
                tied = false;
            } else if (n == bestCount) {
                tied = true;
            }
        }
        boolean clear = !tied && best != null;
        return new Result(clear ? best : null, bestCount, comparable, clear);
    }

    private static boolean isComparable(VoteChoice c) {
        return c == VoteChoice.FOR || c == VoteChoice.AGAINST || c == VoteChoice.ABSTAINED;
    }

    public record Result(
            VoteChoice majorityChoice,
            int majorityCount,
            int comparableCount,
            boolean hasClearMajority
    ) {}
}
