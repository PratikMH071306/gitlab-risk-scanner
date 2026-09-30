import { useState } from "react";
import ResultsTable from "./components/ResultsTable.jsx";
import ScanForm from "./components/ScanForm.jsx";
import Summary from "./components/Summary.jsx";
import { getScanResults, startScan } from "./services/scanApi.js";

function countSeverity(results, severity) {
  return results.filter((row) => row.severity === severity).length;
}

function App() {
  const [type, setType] = useState("USER");
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [summary, setSummary] = useState(null);
  const [results, setResults] = useState([]);

  const handleScan = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const scan = await startScan({ type, name });
      const issues = await getScanResults(scan.scanId);
      setSummary({
        repositories: scan.totalRepositories,
        issues: scan.totalIssues,
      });
      setResults(issues);
    } catch (err) {
      setSummary(null);
      setResults([]);
      setError(err.message || "Scan failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-950 px-6 py-10 text-slate-100">
      <div className="mx-auto flex max-w-4xl flex-col gap-6">
        <header>
          <p className="text-sm font-medium uppercase tracking-wide text-sky-400"></p>
          <h1 className="mt-1 text-3xl font-semibold">GitLab Risk Scanner</h1>
        </header>
        <ScanForm
          type={type}
          name={name}
          loading={loading}
          onTypeChange={setType}
          onNameChange={setName}
          onSubmit={handleScan}
        />
        {error && <p className="text-sm text-red-400">{error}</p>}
        {summary && (
          <>
            <Summary
              repositories={summary.repositories}
              issues={summary.issues}
              high={countSeverity(results, "HIGH")}
              medium={countSeverity(results, "MEDIUM")}
              low={countSeverity(results, "LOW")}
            />
            <ResultsTable results={results} />
          </>
        )}
      </div>
    </main>
  );
}

export default App;
