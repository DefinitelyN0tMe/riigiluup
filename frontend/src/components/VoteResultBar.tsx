import { useTranslation } from "react-i18next";

type Props = {
  inFavor: number;
  against: number;
  abstained: number;
  didNotVote: number;
  absent: number;
};

const COLORS = {
  inFavor: "bg-estonia",
  against: "bg-orange-500",
  abstained: "bg-slate-400",
  didNotVote: "bg-slate-300",
  absent: "bg-slate-200",
};

export default function VoteResultBar({
  inFavor, against, abstained, didNotVote, absent,
}: Props) {
  const { t } = useTranslation();
  const total = inFavor + against + abstained + didNotVote + absent;
  if (total === 0) return <div className="h-3 bg-slate-100 rounded" />;
  const seg = (n: number, cls: string, label: string) =>
    n > 0 ? (
      <span
        key={label}
        className={`${cls} h-3 block`}
        style={{ width: `${(n / total) * 100}%` }}
        title={`${label}: ${n}`}
      />
    ) : null;
  const legend = t("voteBar.legend", { inFavor, against, abstained, didNotVote, absent, total });
  return (
    <div className="w-full" role="img" aria-label={legend}>
      <div className="flex h-3 rounded overflow-hidden bg-slate-100">
        {seg(inFavor, COLORS.inFavor, t("choice.FOR"))}
        {seg(against, COLORS.against, t("choice.AGAINST"))}
        {seg(abstained, COLORS.abstained, t("choice.ABSTAINED"))}
        {seg(didNotVote, COLORS.didNotVote, t("choice.DID_NOT_VOTE"))}
        {seg(absent, COLORS.absent, t("choice.ABSENT"))}
      </div>
      <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-600">
        <span><span className={`${COLORS.inFavor} inline-block w-2 h-2 mr-1`}></span>{t("voteBar.for", { count: inFavor })}</span>
        <span><span className={`${COLORS.against} inline-block w-2 h-2 mr-1`}></span>{t("voteBar.against", { count: against })}</span>
        <span><span className={`${COLORS.abstained} inline-block w-2 h-2 mr-1`}></span>{t("voteBar.abstained", { count: abstained })}</span>
        <span><span className={`${COLORS.didNotVote} inline-block w-2 h-2 mr-1`}></span>{t("voteBar.didNotVote", { count: didNotVote })}</span>
        <span><span className={`${COLORS.absent} inline-block w-2 h-2 mr-1`}></span>{t("voteBar.absent", { count: absent })}</span>
      </div>
    </div>
  );
}
