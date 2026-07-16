import { useTranslation } from "react-i18next";
import MethodologyParticipationPage from "./MethodologyParticipationPage";
import MethodologyAlignmentPage from "./MethodologyAlignmentPage";
import MethodologyAgreementPage from "./MethodologyAgreementPage";
import MethodologyAnalyticsPage from "./MethodologyAnalyticsPage";

/**
 * Methodology hub — shows every section's full description inline (with an
 * anchored table of contents) instead of linking out. The individual routes
 * (/methodology/participation …) still work for deep links.
 */
export default function MethodologyIndexPage() {
  const { t } = useTranslation();
  const sections = [
    { id: "participation", label: t("methodology.index.participation"), Comp: MethodologyParticipationPage },
    { id: "alignment", label: t("methodology.index.alignment"), Comp: MethodologyAlignmentPage },
    { id: "agreement", label: t("methodology.index.agreement"), Comp: MethodologyAgreementPage },
    { id: "analytics", label: t("methodology.index.analytics", "Analytics dashboard — every chart's formula"), Comp: MethodologyAnalyticsPage },
  ];
  return (
    <div>
      <article className="prose max-w-none text-slate-700">
        <h1 className="text-3xl font-semibold text-ink">{t("methodology.index.title")}</h1>
        <p>{t("methodology.index.intro")}</p>
        <nav aria-label={t("methodology.index.title")}>
          <ul>
            {sections.map((s) => (
              <li key={s.id}>
                <a href={`#${s.id}`} className="text-estonia hover:underline">{s.label}</a>
              </li>
            ))}
          </ul>
        </nav>
      </article>

      {sections.map((s) => (
        <section key={s.id} id={s.id} className="scroll-mt-6 border-t border-slate-200 mt-10 pt-2">
          <s.Comp embedded />
        </section>
      ))}
    </div>
  );
}
