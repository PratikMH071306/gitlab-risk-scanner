export default function ScanForm({ type, name, loading, onTypeChange, onNameChange, onSubmit }) {
  return (
    <form
      className="rounded-xl border border-slate-800 bg-slate-900 p-6"
      onSubmit={onSubmit}
    >
      <h2 className="text-lg font-semibold">Scan Target</h2>
      <div className="mt-4 flex gap-6 text-sm">
        <label className="flex items-center gap-2">
          <input
            type="radio"
            name="type"
            value="USER"
            checked={type === "USER"}
            onChange={(event) => onTypeChange(event.target.value)}
          />
          User
        </label>
        <label className="flex items-center gap-2">
          <input
            type="radio"
            name="type"
            value="GROUP"
            checked={type === "GROUP"}
            onChange={(event) => onTypeChange(event.target.value)}
          />
          Group
        </label>
      </div>
      <label className="mt-4 block text-sm text-slate-400">
        Username / Group
        <input
          className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100"
          value={name}
          onChange={(event) => onNameChange(event.target.value)}
          placeholder="gitlab-user or group-path"
          required
        />
      </label>
      <button
        type="submit"
        disabled={loading}
        className="mt-4 rounded-lg bg-sky-500 px-4 py-2 font-medium text-slate-950 hover:bg-sky-400 disabled:opacity-60"
      >
        {loading ? "Scanning..." : "Start Scan"}
      </button>
    </form>
  );
}
