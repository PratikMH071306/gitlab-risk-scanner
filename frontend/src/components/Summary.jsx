export default function Summary({ repositories, issues, high, medium, low }) {
  const items = [
    ["Repositories", repositories],
    ["Issues", issues],
    ["High", high],
    ["Medium", medium],
    ["Low", low],
  ];

  return (
    <section className="rounded-xl border border-slate-800 bg-slate-900 p-6">
      <h2 className="text-lg font-semibold">Summary</h2>
      <dl className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-5">
        {items.map(([label, value]) => (
          <div key={label}>
            <dt className="text-sm text-slate-400">{label}</dt>
            <dd className="text-2xl font-semibold">{value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
