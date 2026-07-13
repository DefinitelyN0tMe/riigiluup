import { useTranslation } from "react-i18next";
import type { CommitteeMembership } from "../types";

export default function CommitteeChip({ c }: { c: CommitteeMembership }) {
  const { t } = useTranslation();
  const border = c.colorHex ?? "#94a3b8";
  return (
    <li
      className="border rounded-full px-3 py-1 text-sm bg-white text-ink"
      style={{ borderColor: border }}
    >
      <span className="font-medium">{c.name}</span>
      {c.role !== "MEMBER" && (
        <span className="text-slate-500"> — {t(`committeeRole.${c.role}` as const, { defaultValue: c.role })}</span>
      )}
    </li>
  );
}
