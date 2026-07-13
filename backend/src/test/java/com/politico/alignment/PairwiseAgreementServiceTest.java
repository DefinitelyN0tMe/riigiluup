package com.politico.alignment;

import com.politico.vote.VoteChoice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PairwiseAgreementServiceTest {

    @Test
    void counts_same_diff_and_one_not_participating_correctly() {
        // (leftChoice, rightChoice) tuples across 10 votes:
        List<PairwiseAgreementService.Pair> pairs = List.of(
                pair(VoteChoice.FOR,       VoteChoice.FOR),        // same
                pair(VoteChoice.FOR,       VoteChoice.FOR),        // same
                pair(VoteChoice.AGAINST,   VoteChoice.AGAINST),    // same
                pair(VoteChoice.ABSTAINED, VoteChoice.ABSTAINED),  // same
                pair(VoteChoice.FOR,       VoteChoice.AGAINST),    // diff
                pair(VoteChoice.AGAINST,   VoteChoice.ABSTAINED),  // diff
                pair(VoteChoice.FOR,       VoteChoice.DID_NOT_VOTE),// one non-part
                pair(VoteChoice.ABSENT,    VoteChoice.FOR),         // one non-part
                pair(VoteChoice.ABSENT,    VoteChoice.ABSENT),      // one non-part (both actually)
                pair(VoteChoice.PRESENT,   VoteChoice.PRESENT)      // one non-part (attendance)
        );

        PairwiseAgreementService.Result r = PairwiseAgreementService.count(pairs);

        assertThat(r.sameCount()).isEqualTo(4);
        assertThat(r.diffCount()).isEqualTo(2);
        assertThat(r.oneNotParticipatingCount()).isEqualTo(4);
        assertThat(r.totalOverlap()).isEqualTo(10);
        assertThat(r.agreementRate()).isEqualTo(4.0 / 6.0);
    }

    @Test
    void empty_pairs_yields_null_rate() {
        PairwiseAgreementService.Result r = PairwiseAgreementService.count(List.of());
        assertThat(r.totalOverlap()).isZero();
        assertThat(r.sameCount()).isZero();
        assertThat(r.diffCount()).isZero();
        assertThat(r.agreementRate()).isNull();
    }

    @Test
    void only_non_participating_pairs_yields_null_rate_but_nonzero_overlap() {
        List<PairwiseAgreementService.Pair> pairs = List.of(
                pair(VoteChoice.DID_NOT_VOTE, VoteChoice.ABSENT),
                pair(VoteChoice.ABSENT, VoteChoice.PRESENT)
        );

        PairwiseAgreementService.Result r = PairwiseAgreementService.count(pairs);

        assertThat(r.totalOverlap()).isEqualTo(2);
        assertThat(r.oneNotParticipatingCount()).isEqualTo(2);
        assertThat(r.agreementRate()).isNull();
    }

    private static PairwiseAgreementService.Pair pair(VoteChoice l, VoteChoice r) {
        return new PairwiseAgreementService.Pair(l, r);
    }
}
