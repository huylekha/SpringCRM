export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <aside className="w-64 border-r bg-white p-4">
        <nav className="space-y-2">
          <p className="text-xs font-semibold uppercase text-gray-400">CRM</p>
          <a href="/customers" className="block rounded px-3 py-2 text-sm hover:bg-gray-100">Customers</a>
          <a href="/leads" className="block rounded px-3 py-2 text-sm hover:bg-gray-100">Leads</a>
          <a href="/opportunities" className="block rounded px-3 py-2 text-sm hover:bg-gray-100">Opportunities</a>
        </nav>
      </aside>
      <main className="flex-1 p-6">{children}</main>
    </div>
  );
}
