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
  const legend = `For ${inFavor}, Against ${against}, Abstained ${abstained}, Did not vote ${didNotVote}, Absent ${absent}, total ${total}`;
  return (
    <div className="w-full" role="img" aria-label={legend}>
      <div className="flex h-3 rounded overflow-hidden bg-slate-100">
        {seg(inFavor, COLORS.inFavor, "FOR")}
        {seg(against, COLORS.against, "AGAINST")}
        {seg(abstained, COLORS.abstained, "ABSTAINED")}
        {seg(didNotVote, COLORS.didNotVote, "DID NOT VOTE")}
        {seg(absent, COLORS.absent, "ABSENT")}
      </div>
      <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-600">
        <span><span className={`${COLORS.inFavor} inline-block w-2 h-2 mr-1`}></span>For {inFavor}</span>
        <span><span className={`${COLORS.against} inline-block w-2 h-2 mr-1`}></span>Against {against}</span>
        <span><span className={`${COLORS.abstained} inline-block w-2 h-2 mr-1`}></span>Abstained {abstained}</span>
        <span><span className={`${COLORS.didNotVote} inline-block w-2 h-2 mr-1`}></span>Did not vote {didNotVote}</span>
        <span><span className={`${COLORS.absent} inline-block w-2 h-2 mr-1`}></span>Absent {absent}</span>
      </div>
    </div>
  );
}
