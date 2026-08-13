import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { fetchGroups } from "../api/groups";
import type { GroupDirectoryItem } from "../types";
import LoadFailed from "../components/LoadFailed";

const CATEGORY_ORDER = ["FRIENDSHIP", "SUPPORT", "DELEGATION"] as const;

function GroupCard({ g }: { g: GroupDirectoryItem }) {
  const { t } = useTranslation();
  return (
    <Link
      to={`/groups/${g.externalId}`}
      className="block bg-white border border-rule rounded-[18px] p-4 hover:border-blue transition-colors"
    >
      <h3 className="font-display font-bold text-[16px] leading-[1.2] tracking-[-0.015em]">{g.name}</h3>
      <p className="font-mono text-[12px] text-ink-2 mt-2">{t("groups.members", { count: g.memberCount })}</p>
    </Link>
  );
}

export default function GroupsPage() {
  const { t } = useTranslation();
  const { data, isLoading, error } = useQuery({ queryKey: ["groups"], queryFn: fetchGroups });
  const [q, setQ] = useState("");

  const byCategory = useMemo(() => {
    const needle = q.trim().toLowerCase();
    const filtered = (data ?? []).filter((g) => !needle || g.name.toLowerCase().includes(needle));
    const map = new Map<string, GroupDirectoryItem[]>();
    for (const cat of CATEGORY_ORDER) map.set(cat, []);
    for (const g of filtered) {
      const key = CATEGORY_ORDER.includes(g.category as (typeof CATEGORY_ORDER)[number]) ? g.category : "SUPPORT";
      (map.get(key) ?? map.get("SUPPORT"))!.push(g);
    }
    return map;
  }, [data, q]);

  return (
    <div className="max-w-[1040px] mx-auto px-5 sm:px-8 py-8 sm:py-10">
      <h1 className="font-display font-bold text-[34px] sm:text-[44px] tracking-[-0.03em] leading-none mb-2">
        {t("groups.title")}
      </h1>
      <p className="font-serif italic text-[15px] text-ink-2 mb-6 max-w-[70ch]">{t("groups.lede")}</p>

      {data && data.length > 0 && (
        <input
          type="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={t("groups.searchPlaceholder")}
          className="w-full sm:max-w-[420px] border border-rule rounded-full px-4 py-2 text-[14px] mb-6 focus:outline-none focus:border-blue"
        />
      )}

      {isLoading && !data && (
        <p className="mt-2 text-muted font-mono text-sm" role="status">
          {t("common.loading")}
        </p>
      )}
      {error && <LoadFailed error={error} className="mt-2 text-hot-deep font-mono text-sm" />}

      {data &&
        CATEGORY_ORDER.map((cat) => {
          const items = byCategory.get(cat) ?? [];
          if (items.length === 0) return null;
          return (
            <section key={cat} aria-label={t(`groups.category.${cat}`)} className="mb-9">
              <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">
                {t(`groups.category.${cat}`)}
              </h2>
              <p className="text-[13px] leading-snug text-ink-2 mb-4 max-w-[72ch]">
                {t(`groups.categoryLede.${cat}`)}
              </p>
              <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 list-none p-0">
                {items.map((g) => (
                  <li key={g.externalId}>
                    <GroupCard g={g} />
                  </li>
                ))}
              </ul>
            </section>
          );
        })}

      {data && CATEGORY_ORDER.every((c) => (byCategory.get(c) ?? []).length === 0) && (
        <p className="font-serif italic text-[14px] text-ink-2 py-4">{t("common.noResults")}</p>
      )}
    </div>
  );
}
