import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function MethodologyIndexPage() {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("methodology.index.title")}</h1>
      <p>{t("methodology.index.intro")}</p>
      <ul>
        <li><Link to="/methodology/participation" className="text-estonia hover:underline">{t("methodology.index.participation")}</Link></li>
        <li><Link to="/methodology/alignment" className="text-estonia hover:underline">{t("methodology.index.alignment")}</Link></li>
        <li><Link to="/methodology/agreement" className="text-estonia hover:underline">{t("methodology.index.agreement")}</Link></li>
      </ul>
    </article>
  );
}
