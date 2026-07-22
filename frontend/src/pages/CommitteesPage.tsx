import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { fetchCommittees, type CommitteeListItem } from "../api/committees";
import LoadFailed from "../components/LoadFailed";

function CommitteeCard({ c }: { c: CommitteeListItem }) {
  const { t } = useTranslation();
  const accent = c.colorHex ?? "#94a3b8";
  return (
    <Link
      to={`/committees/${c.externalId}`}
      className="block bg-white border border-rule rounded-[20px] p-5 hover:border-blue transition-colors"
      style={{ borderLeft: `4px solid ${accent}` }}
    >
      <h2 className="font-display font-bold text-[18px] leading-[1.2] tracking-[-0.02em]">{c.name}</h2>
      {c.shortName && (
        <p className="font-mono text-[11px] text-muted tracking-[0.06em] mt-1">{c.shortName}</p>
      )}
      <div className="flex flex-wrap gap-x-4 gap-y-1 mt-4 font-mono text-[12px] text-ink-2">
        <span>{t("committees.members", { count: c.memberCount })}</span>
        <span>{t("committees.ledBills", { count: c.ledBillCount })}</span>
        <span>{t("committees.initiatives", { count: c.initiativeCount })}</span>
      </div>
    </Link>
  );
}

export default function CommitteesPage() {
  const { t } = useTranslation();
  const { data, isLoading, error } = useQuery({
    queryKey: ["committees"],
    queryFn: fetchCommittees,
  });

  return (
    <div className="max-w-[1040px] mx-auto px-5 sm:px-8 py-8 sm:py-10">
      <h1 className="font-display font-bold text-[34px] sm:text-[44px] tracking-[-0.03em] leading-none mb-2">
        {t("committees.title")}
      </h1>
      <p className="font-serif italic text-[15px] text-ink-2 mb-6 max-w-[70ch]">{t("committees.lede")}</p>

      {isLoading && !data && (
        <p className="mt-2 text-muted font-mono text-sm" role="status">
          {t("common.loading")}
        </p>
      )}
      {error && <LoadFailed error={error} className="mt-2 text-hot-deep font-mono text-sm" />}

      {data && (
        <ul className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 list-none p-0">
          {data.map((c) => (
            <li key={c.externalId}>
              <CommitteeCard c={c} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
