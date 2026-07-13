import { useTranslation } from "react-i18next";

type Props = {
  label: string;
  value: string;
  hint: string;
  sourceUrl: string;
};

export default function MetricCard({ label, value, hint, sourceUrl }: Props) {
  const { t } = useTranslation();
  return (
    <div className="border border-slate-200 rounded-lg p-4">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="text-2xl font-semibold text-ink mt-1">{value}</div>
      <div className="text-xs text-slate-500 mt-2">{hint}</div>
      <a
        href={sourceUrl}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`Source for ${label} (opens in new tab)`}
        className="text-xs text-estonia hover:underline mt-1 inline-block"
      >
        {t("common.sourceLink")}
      </a>
    </div>
  );
}
