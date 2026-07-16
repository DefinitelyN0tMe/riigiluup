import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <section className="px-5 sm:px-8 md:px-10 py-16 sm:py-24 max-w-[900px] mx-auto text-center">
      <div className="font-mono text-[11px] tracking-[0.2em] uppercase text-blue font-bold mb-4">
        404 · {t("notFound.kicker")}
      </div>
      <h1 className="font-display font-bold h-display-lg mb-4">{t("notFound.title")}</h1>
      <p className="text-lg text-slate-600 mb-8 max-w-[52ch] mx-auto">{t("notFound.body")}</p>
      <div className="flex flex-wrap gap-3 justify-center">
        <Link
          to="/"
          className="inline-flex items-center px-5 py-3 sm:px-6 sm:py-3.5 bg-ink text-white rounded-full font-semibold text-[15px]"
        >
          {t("notFound.home")}
        </Link>
        <Link
          to="/politicians"
          className="inline-flex items-center px-5 py-3 sm:px-6 sm:py-3.5 border-[1.5px] border-ink rounded-full font-semibold text-[15px]"
        >
          {t("nav.mps")}
        </Link>
        <Link
          to="/votes"
          className="inline-flex items-center px-5 py-3 sm:px-6 sm:py-3.5 border-[1.5px] border-ink rounded-full font-semibold text-[15px]"
        >
          {t("nav.votes")}
        </Link>
      </div>
    </section>
  );
}
