import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import DataFreshnessBadge from "../components/DataFreshnessBadge";

export default function HomePage() {
  const { t } = useTranslation();
  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-semibold text-ink">{t("home.title")}</h1>
      <p className="max-w-2xl text-slate-700 leading-relaxed">
        {t("home.intro")}
      </p>
      <div>
        <Link
          to="/politicians"
          className="inline-flex items-center px-4 py-2 rounded-md bg-estonia text-white hover:bg-blue-700"
        >
          {t("home.browseMps")}
        </Link>
      </div>
      <DataFreshnessBadge />
    </div>
  );
}
