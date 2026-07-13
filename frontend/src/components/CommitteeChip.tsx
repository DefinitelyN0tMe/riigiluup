import type { CommitteeMembership } from "../types";

const ROLE_LABEL: Record<CommitteeMembership["role"], string> = {
  CHAIR: "Chair",
  VICE_CHAIR: "Vice-chair",
  MEMBER: "Member",
  REPRESENTATIVE: "Representative",
  OTHER: "Other",
};

export default function CommitteeChip({ c }: { c: CommitteeMembership }) {
  const border = c.colorHex ?? "#94a3b8";
  return (
    <li
      className="border rounded-full px-3 py-1 text-sm bg-white text-ink"
      style={{ borderColor: border }}
    >
      <span className="font-medium">{c.name}</span>
      {c.role !== "MEMBER" && (
        <span className="text-slate-500"> — {ROLE_LABEL[c.role]}</span>
      )}
    </li>
  );
}
