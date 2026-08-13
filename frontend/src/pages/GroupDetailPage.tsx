import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { fetchGroup } from "../api/groups";
import LoadFailed from "../components/LoadFailed";

export default function GroupDetailPage() {
  const { t } = useTranslation();
  const { externalId } = useParams<{ externalId: string }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ["group", externalId],
    queryFn: () => fetchGroup(externalId as string),
    enabled: !!externalId,
  });

  return (
    <div className="max-w-[860px] mx-auto px-5 sm:px-8 py-8 sm:py-10">
      <Link to="/groups" className="font-mono text-[12px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
        ← {t("groups.backToList")}
      </Link>

      {isLoading && !data && (
        <p className="mt-6 text-muted font-mono text-sm" role="status">
          {t("common.loading")}
        </p>
      )}
      {error && <LoadFailed error={error} className="mt-6 text-hot-deep font-mono text-sm" />}

      {data && (
        <>
          <div className="mt-4 mb-6">
            <span className="font-mono text-[10px] tracking-[0.14em] uppercase px-2 py-0.5 rounded bg-blue/10 text-blue">
              {t(`groups.category.${data.category}`, { defaultValue: data.category })}
            </span>
            <h1 className="font-display font-bold text-[30px] sm:text-[38px] tracking-[-0.03em] leading-[1.05] mt-3">
              {data.name}
            </h1>
            <p className="font-mono text-[12px] text-ink-2 mt-2">
              {t("groups.members", { count: data.members.length })}
            </p>
          </div>

          <ul className="flex flex-col divide-y divide-rule border border-rule rounded-[16px] overflow-hidden">
            {data.members.map((m) => (
              <li key={m.slug} className="p-4 flex items-baseline justify-between gap-3">
                <Link to={`/politicians/${m.slug}`} className="font-medium text-ink text-[15px] hover:underline">
                  {m.name}
                </Link>
                {m.factionName && <span className="text-[12px] text-muted text-right shrink-0">{m.factionName}</span>}
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
