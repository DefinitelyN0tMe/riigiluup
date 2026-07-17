import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import type { CommitteeMembership } from "../types";

export default function CommitteeChip({ c }: { c: CommitteeMembership }) {
  const { t } = useTranslation();
  const border = c.colorHex ?? "#94a3b8";
  return (
    <li
      className="border rounded-full px-3 py-1 text-sm bg-white text-ink"
      style={{ borderColor: border }}
    >
      {c.externalId ? (
        <Link to={`/committees/${c.externalId}`} className="font-medium hover:underline">
          {c.name}
        </Link>
      ) : (
        <span className="font-medium">{c.name}</span>
      )}
      {c.role !== "MEMBER" && (
        <span className="text-slate-500"> — {t(`committeeRole.${c.role}` as const, { defaultValue: c.role })}</span>
      )}
    </li>
  );
}
