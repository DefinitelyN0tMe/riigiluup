import type { LegislationStage } from "../types";

const READING_LABEL: Record<string, string> = {
  INITIATION: "Initiation",
  ESIMENE_LUGEMINE: "First reading",
  TEINE_LUGEMINE: "Second reading",
  KOLMAS_LUGEMINE: "Third reading",
  FIRST_READING: "First reading",
  SECOND_READING: "Second reading",
  THIRD_READING: "Third reading",
  EFFECTUATION: "Effectuation",
  VASTU_VOETUD: "Adopted",
  LOPETATUD: "Ended",
};

const STATUS_LABEL: Record<string, string> = {
  ALGATATUD: "Introduced",
  MENETLUSSE_VOETUD: "Accepted for procedure",
  LOPETATUD: "Concluded",
  SAADETUD_VABARIIGI_PRESIDENDILE: "Sent to the President",
  VALJAKUULUTATUD: "Promulgated",
  AVALDATUD_RIIGITEATAJAS: "Published in Riigi Teataja",
  TAGASI_LUKATUD: "Rejected",
  TAGASI_VOETUD: "Withdrawn",
};

export default function StageTimeline({ stages }: { stages: LegislationStage[] }) {
  if (stages.length === 0) return <p className="text-sm text-slate-500">No stages recorded.</p>;
  return (
    <ol className="relative border-l border-slate-200 pl-4 space-y-3">
      {stages.map((s, idx) => {
        const when = s.occurredAt ? new Date(s.occurredAt).toLocaleDateString() : "—";
        return (
          <li key={idx} className="text-sm">
            <span className="absolute -left-1.5 mt-1 w-3 h-3 bg-estonia rounded-full" />
            <div className="text-ink font-medium">
              {READING_LABEL[s.readingCode ?? ""] ?? s.readingCode ?? "Stage"}
            </div>
            <div className="text-xs text-slate-500">
              {(s.statusCode && (STATUS_LABEL[s.statusCode] ?? s.statusCode)) || ""} · {when}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
