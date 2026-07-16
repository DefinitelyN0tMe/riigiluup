import { useTranslation } from "react-i18next";
import type { LegislationStage } from "../types";
import { formatDate } from "../lib/formatDate";

export default function StageTimeline({ stages }: { stages: LegislationStage[] }) {
  const { t } = useTranslation();
  if (stages.length === 0) return <p className="text-sm text-slate-500">{t("legislation.noStages")}</p>;
  return (
    <ol className="relative border-l border-slate-200 pl-4 space-y-3">
      {stages.map((s, idx) => {
        const when = s.occurredAt ? formatDate(s.occurredAt) : "—";
        const readingLabel = s.readingCode
          ? t(`reading.${s.readingCode}` as const, { defaultValue: s.readingCode })
          : t("reading.fallback");
        const statusLabel = s.statusCode
          ? t(`stageStatus.${s.statusCode}` as const, { defaultValue: s.statusCode })
          : "";
        return (
          <li key={idx} className="text-sm">
            <span className="absolute -left-1.5 mt-1 w-3 h-3 bg-estonia rounded-full" />
            <div className="text-ink font-medium">
              {readingLabel}
            </div>
            <div className="text-xs text-slate-500">
              {statusLabel} · {when}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
