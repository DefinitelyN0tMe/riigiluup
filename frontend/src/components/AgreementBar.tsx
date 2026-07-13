import { useTranslation } from "react-i18next";

type Props = {
  same: number;
  diff: number;
  oneNotParticipating: number;
};

export default function AgreementBar({ same, diff, oneNotParticipating }: Props) {
  const { t } = useTranslation();
  const comparable = same + diff;
  if (comparable === 0) {
    return <div className="text-sm text-slate-500">{t("agreement.noOverlap")}</div>;
  }
  const samePct = (same / comparable) * 100;
  const legend = `${t("agreement.same")} ${same}, ${t("agreement.different")} ${diff}, ${t("agreement.oneNotParticipating")} ${oneNotParticipating}`;
  return (
    <div role="img" aria-label={legend}>
      <div className="flex h-3 rounded overflow-hidden bg-slate-100">
        <span className="bg-emerald-500 h-3 block" style={{ width: `${samePct}%` }} title={t("agreement.sameCount", { count: same })} />
        <span className="bg-orange-500 h-3 block" style={{ width: `${100 - samePct}%` }} title={t("agreement.differentCount", { count: diff })} />
      </div>
      <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-600">
        <span><span className="bg-emerald-500 inline-block w-2 h-2 mr-1"></span>{t("agreement.sameCount", { count: same })}</span>
        <span><span className="bg-orange-500 inline-block w-2 h-2 mr-1"></span>{t("agreement.differentCount", { count: diff })}</span>
        <span className="text-slate-500">{t("agreement.oneNotParticipatingCount", { count: oneNotParticipating })}</span>
      </div>
    </div>
  );
}
