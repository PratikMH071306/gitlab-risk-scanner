export async function startScan(data) {
  const response = await fetch("http://localhost:8082/api/scans", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || "Scan failed");
  }

  return response.json();
}

export async function getScanResults(scanId) {
  const response = await fetch(`http://localhost:8082/api/scans/${scanId}/results`);

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || "Could not load scan results");
  }

  return response.json();
}
