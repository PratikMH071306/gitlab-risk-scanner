const severityClass = {
  HIGH: "text-red-400",
  Medium: "text-amber-400",
  LOW: "text-slate-300",
};

export default function ResultsTable({ results }) {
  return (
    <section className="rounded-xl border border-slate-800 bg-slate-900 p-6">
      <h2 className="text-lg font-semibold">Security Issues</h2>
      {results.length === 0 ? (
        <p className="mt-4 text-sm text-slate-400">No issues found.</p>
      ) : (
        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-slate-400">
              <tr>
                <th className="pb-2 pr-4 font-medium">Project</th>
                <th className="pb-2 pr-4 font-medium">Issue</th>
                <th className="pb-2 pr-4 font-medium">Severity</th>
                <th className="pb-2 font-medium">File</th>
              </tr>
            </thead>
            <tbody>
              {results.map((row, index) => (
                <tr key={`${row.projectName}-${row.filePath}-${index}`} className="border-t border-slate-800">
                  <td className="py-2 pr-4">{row.projectName}</td>
                  <td className="py-2 pr-4">{row.issueDescription}</td>
                  <td className={`py-2 pr-4 font-medium ${severityClass[row.severity] || ""}`}>
                    {row.severity}
                  </td>
                  <td className="py-2">{row.filePath || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
