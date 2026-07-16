import { useTranslation } from "react-i18next";

type Props = {
  inFavor: number;
  against: number;
  abstained: number;
  didNotVote: number;
  absent: number;
};

/* B v2 palette: blue for FOR, hot-red for AGAINST, ink-black for ABSTAINED, blue-deep for PRESENT, warm-grey for ABSENT */
const COLORS = {
  inFavor: "bg-blue",
  against: "bg-hot",
  abstained: "bg-ink",
  didNotVote: "bg-blue-deep",
  absent: "bg-[#D8D6CB]",
};

export default function VoteResultBar({ inFavor, against, abstained, didNotVote, absent }: Props) {
  const { t } = useTranslation();
  const total = inFavor + against + abstained + didNotVote + absent;
  if (total === 0) return <div className="h-3.5 bg-[#F1F0EA] rounded-lg" />;
  const seg = (n: number, cls: string, label: string) =>
    n > 0 ? (
      <span
        key={label}
        className={`${cls} h-3.5 block`}
        style={{ width: `${(n / total) * 100}%` }}
        title={`${label}: ${n}`}
      />
    ) : null;
  const legend = t("voteBar.legend", { inFavor, against, abstained, didNotVote, absent, total });
  return (
    <div className="w-full" role="img" aria-label={legend}>
      <div className="flex h-3.5 rounded-lg overflow-hidden bg-[#F1F0EA]">
        {seg(inFavor, COLORS.inFavor, t("choice.FOR"))}
        {seg(against, COLORS.against, t("choice.AGAINST"))}
        {seg(abstained, COLORS.abstained, t("choice.ABSTAINED"))}
        {seg(didNotVote, COLORS.didNotVote, t("choice.DID_NOT_VOTE"))}
        {seg(absent, COLORS.absent, t("choice.ABSENT"))}
      </div>
      <div className="mt-2 flex flex-wrap gap-x-3 sm:gap-x-4 gap-y-1 font-mono text-[10px] sm:text-[11px] tracking-[0.06em] text-muted">
        <span><span className={`${COLORS.inFavor} inline-block w-2 h-2 mr-1.5 align-middle`}></span><b className="text-ink font-bold mr-0.5">{inFavor}</b>{t("choice.FOR").toLowerCase()}</span>
        <span><span className={`${COLORS.against} inline-block w-2 h-2 mr-1.5 align-middle`}></span><b className="text-ink font-bold mr-0.5">{against}</b>{t("choice.AGAINST").toLowerCase()}</span>
        <span><span className={`${COLORS.abstained} inline-block w-2 h-2 mr-1.5 align-middle`}></span><b className="text-ink font-bold mr-0.5">{abstained}</b>{t("choice.ABSTAINED").toLowerCase()}</span>
        <span><span className={`${COLORS.didNotVote} inline-block w-2 h-2 mr-1.5 align-middle`}></span><b className="text-ink font-bold mr-0.5">{didNotVote}</b>{t("choice.DID_NOT_VOTE").toLowerCase()}</span>
        <span><span className={`${COLORS.absent} inline-block w-2 h-2 mr-1.5 align-middle`}></span><b className="text-ink font-bold mr-0.5">{absent}</b>{t("choice.ABSENT").toLowerCase()}</span>
      </div>
    </div>
  );
}
