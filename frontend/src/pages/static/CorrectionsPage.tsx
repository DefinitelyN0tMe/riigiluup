import { useTranslation } from "react-i18next";
import { CORRECTIONS_EMAIL } from "../../config";

export default function CorrectionsPage() {
  const { t } = useTranslation();
  const subject = encodeURIComponent(t("corrections.mailSubject"));
  const body = encodeURIComponent(t("corrections.mailTemplate"));
  const mailto = `mailto:${CORRECTIONS_EMAIL}?subject=${subject}&body=${body}`;
  return (
    <article className="prose max-w-[820px] mx-auto px-5 sm:px-8 md:px-10 py-10 sm:py-14 text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("corrections.title")}</h1>
      <p>{t("corrections.intro")}</p>
      <h2>{t("corrections.sendHeading")}</h2>
      <p>
        {t("corrections.emailPrefix")}
        <a href={mailto} className="text-estonia hover:underline">{CORRECTIONS_EMAIL}</a>
        {t("corrections.emailSuffix")}
      </p>
      <ol>
        <li>{t("corrections.step1")}</li>
        <li>{t("corrections.step2")}</li>
        <li>{t("corrections.step3")}</li>
      </ol>
      <p>
        {t("corrections.buttonExplainerA")}
        <em>{t("corrections.buttonExplainerB")}</em>
        {t("corrections.buttonExplainerC")}
      </p>
      <p>
        <a
          href={mailto}
          className="inline-flex items-center px-4 py-2 rounded-md bg-estonia text-white hover:bg-blue-700 no-underline"
        >
          {t("corrections.sendButton")}
        </a>
      </p>
      <h2>{t("corrections.whatWeDoHeading")}</h2>
      <p>{t("corrections.whatWeDo")}</p>
      <h2>{t("corrections.whatWeDontHeading")}</h2>
      <ul>
        <li>{t("corrections.wnd1")}</li>
        <li>{t("corrections.wnd2")}</li>
        <li>{t("corrections.wnd3")}</li>
      </ul>
    </article>
  );
}
