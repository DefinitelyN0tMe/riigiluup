import { Link, Route, Routes } from "react-router-dom";
import HomePage from "./pages/HomePage";
import PoliticiansPage from "./pages/PoliticiansPage";
import PoliticianProfilePage from "./pages/PoliticianProfilePage";

export default function App() {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b border-slate-200 px-6 py-4 flex items-center gap-6">
        <Link to="/" className="text-lg font-semibold text-ink">Politico</Link>
        <nav className="text-sm text-slate-600 flex gap-4">
          <Link to="/politicians" className="hover:text-estonia">MPs</Link>
        </nav>
      </header>
      <main className="flex-1 max-w-6xl mx-auto w-full px-6 py-8">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/politicians" element={<PoliticiansPage />} />
          <Route path="/politicians/:slug" element={<PoliticianProfilePage />} />
        </Routes>
      </main>
      <footer className="border-t border-slate-200 px-6 py-4 text-xs text-slate-500">
        Data © Riigikogu, CC BY-SA 3.0.
      </footer>
    </div>
  );
}
