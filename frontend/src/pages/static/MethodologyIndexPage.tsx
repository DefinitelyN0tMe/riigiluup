import { Link } from "react-router-dom";

export default function MethodologyIndexPage() {
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">Methodology</h1>
      <p>
        Each metric on this site follows an explicit formula. The pages below
        describe how numbers are computed, what is counted, what is excluded,
        and what the numbers do NOT tell you.
      </p>
      <ul>
        <li><Link to="/methodology/participation" className="text-estonia hover:underline">Voting participation and attendance-check presence</Link></li>
        <li><Link to="/methodology/alignment" className="text-estonia hover:underline">Group alignment</Link></li>
        <li><Link to="/methodology/agreement" className="text-estonia hover:underline">Pairwise vote agreement</Link></li>
      </ul>
    </article>
  );
}
