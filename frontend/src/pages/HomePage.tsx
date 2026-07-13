import { Link } from "react-router-dom";
import DataFreshnessBadge from "../components/DataFreshnessBadge";

export default function HomePage() {
  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-semibold text-ink">Politico</h1>
      <p className="max-w-2xl text-slate-700 leading-relaxed">
        Civic-tech platform aggregating open data from the Estonian Riigikogu.
        Browse Members of Parliament and follow their voting behaviour.
        Every fact links back to the official source. No opinions, no rankings.
      </p>
      <div>
        <Link
          to="/politicians"
          className="inline-flex items-center px-4 py-2 rounded-md bg-estonia text-white hover:bg-blue-700"
        >
          Browse MPs
        </Link>
      </div>
      <DataFreshnessBadge />
    </div>
  );
}
