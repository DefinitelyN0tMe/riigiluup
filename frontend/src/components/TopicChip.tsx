import type { LegislationTopic } from "../types";

export default function TopicChip({ t }: { t: LegislationTopic }) {
  return (
    <span className="inline-block border border-slate-200 rounded-full px-2 py-0.5 text-xs bg-slate-50 text-slate-700 mr-1 mb-1">
      {t.text}
    </span>
  );
}
