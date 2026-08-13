import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchCommittee } from "../api/committees";
import { ApiError } from "../api/client";
import { useLoadErrorMessage } from "../lib/useLoadErrorMessage";
import { formatDate } from "../lib/formatDate";

export default function CommitteeDetailPage() {
  const { t } = useTranslation();
  const { externalId } = useParams<{ externalId: string }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ["committee", externalId],
    queryFn: () => fetchCommittee(externalId!),
    enabled: !!externalId,
  });
  const loadErrorMessage = useLoadErrorMessage(error);

  const containerCls = "max-w-[900px] mx-auto px-5 sm:px-8 py-10 sm:py-14";

  if (isLoading) {
    return (
      <div className={containerCls}>
        <p className="text-muted font-mono text-sm tracking-[0.06em]" role="status">
          {t("common.loading")}
        </p>
      </div>
    );
  }

  if (error || !data) {
    const notFound = error instanceof ApiError && error.status === 404;
    return (
      <div className={`${containerCls} text-center`}>
        <p className="font-mono text-[11px] tracking-[0.2em] uppercase text-blue font-bold mb-3">
          {notFound ? "404" : t("common.failedToLoad")}
        </p>
        <h1 role={notFound ? "status" : "alert"} className="font-display font-bold text-2xl mb-6">
          {notFound ? t("committees.notFound") : loadErrorMessage}
        </h1>
        <Link
          to="/committees"
          className="inline-flex items-center px-5 py-3 bg-ink text-white rounded-full font-semibold text-[14px]"
        >
          {t("committees.backAll")}
        </Link>
      </div>
    );
  }

  const accent = data.colorHex ?? "#94a3b8";

  return (
    <div className={containerCls}>
      <div className="mb-6">
        <Link to="/committees" className="font-mono text-[11px] tracking-[0.08em] uppercase text-blue hover:underline">
          ← {t("committees.backAll")}
        </Link>
      </div>

      <header className="mb-10 border-l-4 pl-4" style={{ borderColor: accent }}>
        <h1 className="font-display font-bold text-[30px] sm:text-[40px] tracking-[-0.03em] leading-none">
          {data.name}
        </h1>
        {data.secretariat && (
          <p className="font-mono text-[11px] text-muted tracking-[0.06em] mt-2">
            {t("committees.secretariat")}: {data.secretariat}
          </p>
        )}
      </header>

      <section className="mb-10" aria-label={t("committees.section.members")}>
        <h2 className="font-mono text-[10px] tracking-[0.16em] uppercase text-muted mb-3">
          {t("committees.section.members")}
        </h2>
        <ul className="divide-y divide-rule border border-rule rounded-[16px] list-none p-0">
          {data.members.map((m) => (
            <li key={m.slug} className="flex items-center justify-between gap-3 px-4 py-3">
              <div className="min-w-0">
                <Link to={`/politicians/${m.slug}`} className="font-display font-semibold text-[15px] hover:underline">
                  {m.name}
                </Link>
                {m.factionName && (
                  <span className="font-mono text-[11px] text-muted tracking-[0.04em]"> · {m.factionName}</span>
                )}
              </div>
              {m.role !== "MEMBER" && (
                <span className="shrink-0 font-mono text-[10px] tracking-[0.12em] uppercase text-blue font-bold">
                  {t(`committeeRole.${m.role}` as const, { defaultValue: m.role })}
                </span>
              )}
            </li>
          ))}
        </ul>
      </section>

      <section className="mb-10" aria-label={t("committees.section.ledBills")}>
        <h2 className="font-mono text-[10px] tracking-[0.16em] uppercase text-muted mb-3">
          {t("committees.section.ledBills")}
        </h2>
        {data.ledBills.total === 0 ? (
          <p className="text-muted font-serif italic">
            {data.kind === "SPECIAL" ? t("committees.specialNoBills") : t("committees.noBills")}
          </p>
        ) : (
          <>
            <ul className="divide-y divide-rule border border-rule rounded-[16px] list-none p-0">
              {data.ledBills.recent.map((b) => (
                <li key={b.id} className="px-4 py-3">
                  <Link to={`/legislation/${b.id}`} className="text-[14px] hover:underline">
                    {b.mark != null && <span className="font-mono text-muted mr-1.5">#{b.mark}</span>}
                    {b.title}
                  </Link>
                  {b.initiatedDate && (
                    <span className="block font-mono text-[10px] text-muted tracking-[0.04em] mt-0.5">
                      {formatDate(b.initiatedDate)}
                    </span>
                  )}
                </li>
              ))}
            </ul>
            {data.ledBills.total > data.ledBills.recent.length && (
              <Link
                to={`/legislation?committee=${encodeURIComponent(data.externalId)}&committeeName=${encodeURIComponent(data.name)}`}
                className="inline-block mt-3 font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5"
              >
                {t("committees.allBills", { count: data.ledBills.total })} →
              </Link>
            )}
          </>
        )}
      </section>

      {data.initiatives.length > 0 && (
        <section className="mb-10" aria-label={t("committees.section.initiatives")}>
          <h2 className="font-mono text-[10px] tracking-[0.16em] uppercase text-muted mb-3">
            {t("committees.section.initiatives")}
          </h2>
          <ul className="divide-y divide-rule border border-rule rounded-[16px] list-none p-0">
            {data.initiatives.map((i) => (
              <li key={i.id} className="px-4 py-3">
                <Link to={`/initiatives/${i.id}`} className="text-[14px] hover:underline">
                  {i.title ?? `#${i.externalId}`}
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
