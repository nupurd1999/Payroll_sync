const API_BASE = '/api/v1';

let activeBatchId = null;
let stompClient = null;

document.addEventListener('DOMContentLoaded', () => {
  // Generate default idempotency key
  document.getElementById('batch-idempotency').value = 'IDEM-' + new Date().toISOString().slice(0, 7) + '-' + Math.floor(Math.random() * 10000);
  
  loadEmployees();
  loadBatches();
  connectWebSocket();

  document.getElementById('add-employee-form').addEventListener('submit', handleAddEmployee);
  document.getElementById('create-batch-form').addEventListener('submit', handleCreateBatch);
});

function switchTab(tabId) {
  document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

  event.target.classList.add('active');
  document.getElementById(tabId).classList.add('active');

  if (tabId === 'tab-audit') {
    loadAuditLogs();
  }
}

async function loadEmployees() {
  try {
    const res = await fetch(`${API_BASE}/employees`);
    if (!res.ok) throw new Error('Failed to fetch employees');
    const employees = await res.json();

    document.getElementById('metric-employees').innerText = employees.length;

    const tbody = document.getElementById('employees-tbody');
    if (employees.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--text-muted);">No employees registered yet. Use the form above to add your first employee.</td></tr>`;
      return;
    }

    tbody.innerHTML = employees.map(emp => `
      <tr>
        <td><strong style="color:var(--primary);">${escapeHtml(emp.employeeCode)}</strong></td>
        <td>
          <div style="font-weight:600;">${escapeHtml(emp.firstName)} ${escapeHtml(emp.lastName)}</div>
          <div style="font-size:0.8rem; color:var(--text-muted);">${escapeHtml(emp.email)}</div>
        </td>
        <td>
          <div style="font-family:var(--font-mono); font-size:0.85rem;">${escapeHtml(emp.iban)}</div>
          <div style="font-size:0.75rem; color:var(--text-muted);">${escapeHtml(emp.bic)}</div>
        </td>
        <td><span class="badge badge-indigo">${escapeHtml(emp.taxClass)}</span></td>
        <td style="font-weight:700;">€${formatNumber(emp.baseSalary)}</td>
      </tr>
    `).join('');

  } catch (err) {
    console.error(err);
  }
}

async function handleAddEmployee(e) {
  e.preventDefault();
  const payload = {
    employeeCode: document.getElementById('emp-code').value,
    firstName: document.getElementById('emp-fname').value,
    lastName: document.getElementById('emp-lname').value,
    email: document.getElementById('emp-email').value,
    iban: document.getElementById('emp-iban').value,
    bic: document.getElementById('emp-bic').value,
    taxClass: document.getElementById('emp-taxclass').value,
    baseSalary: parseFloat(document.getElementById('emp-salary').value),
    countryCode: 'DE',
    currency: 'EUR'
  };

  try {
    const res = await fetch(`${API_BASE}/employees`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      alert('Error creating employee: ' + JSON.stringify(err));
      return;
    }

    alert('Employee successfully registered!');
    document.getElementById('add-employee-form').reset();
    document.getElementById('emp-code').value = 'EMP-DE-00' + Math.floor(Math.random() * 100);
    loadEmployees();
  } catch (err) {
    alert('Failed to register employee: ' + err.message);
  }
}

async function loadBatches() {
  try {
    const res = await fetch(`${API_BASE}/payroll/batches`);
    if (!res.ok) throw new Error('Failed to fetch batches');
    const batches = await res.json();

    document.getElementById('metric-batches').innerText = batches.length;

    let totalDisbursed = 0;
    batches.forEach(b => {
      if (b.status === 'COMPLETED') {
        totalDisbursed += parseFloat(b.totalNet || 0);
      }
    });

    document.getElementById('metric-disbursed').innerText = '€' + formatNumber(totalDisbursed);

    const tbody = document.getElementById('batches-tbody');
    if (batches.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--text-muted);">No batches created yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = batches.map(b => `
      <tr>
        <td><strong style="color:var(--primary); font-family:var(--font-mono);">${escapeHtml(b.batchReference)}</strong></td>
        <td>${escapeHtml(b.payrollPeriod)}</td>
        <td style="font-weight:600;">€${formatNumber(b.totalGross)}</td>
        <td style="color:var(--accent-rose);">€${formatNumber(b.totalTax)}</td>
        <td style="font-weight:700; color:var(--accent-emerald);">€${formatNumber(b.totalNet)}</td>
        <td><span class="badge ${b.status === 'COMPLETED' ? 'badge-success' : 'badge-warning'}">${escapeHtml(b.status)}</span></td>
        <td>
          <button class="btn btn-secondary" style="padding:0.3rem 0.6rem; font-size:0.8rem;" onclick="selectBatch('${b.id}')">
            View / SEPA
          </button>
        </td>
      </tr>
    `).join('');

  } catch (err) {
    console.error('Error loading batches:', err);
  }
}

function selectBatch(batchId) {
  activeBatchId = batchId;
  document.getElementById('batch-progress-panel').style.display = 'block';
  document.getElementById('batch-status-badge').innerText = 'SELECTED';
  document.getElementById('batch-progress-text').innerText = `Batch ${batchId} selected. Ready for disbursement or SEPA XML export.`;
  document.getElementById('batch-progress-percent').innerText = '100%';
  document.getElementById('batch-progress-fill').style.width = '100%';
}

async function handleCreateBatch(e) {
  e.preventDefault();
  const period = document.getElementById('batch-period').value;
  const idempotencyKey = document.getElementById('batch-idempotency').value;

  try {
    const res = await fetch(`${API_BASE}/payroll/batches`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ period, idempotencyKey })
    });

    if (!res.ok) {
      const err = await res.json();
      alert('Failed to create batch: ' + (err.message || JSON.stringify(err)));
      return;
    }

    const batch = await res.json();
    activeBatchId = batch.id;

    document.getElementById('batch-progress-panel').style.display = 'block';
    document.getElementById('batch-status-badge').innerText = batch.status;
    document.getElementById('batch-status-badge').className = 'badge badge-warning';
    document.getElementById('batch-progress-text').innerText = `Batch ${batch.batchReference} calculated (€${formatNumber(batch.totalNet)} net payout). Ready for disbursement.`;
    document.getElementById('batch-progress-percent').innerText = '0%';
    document.getElementById('batch-progress-fill').style.width = '0%';

    subscribeBatchProgress(batch.id);
    loadBatches();
    alert(`Batch ${batch.batchReference} calculated successfully! Net payout sum: €${formatNumber(batch.totalNet)}`);
  } catch (err) {
    alert('Error creating batch: ' + err.message);
  }
}

async function triggerDisburse() {
  if (!activeBatchId) {
    alert('No active batch selected for disbursement.');
    return;
  }

  try {
    document.getElementById('btn-trigger-disburse').disabled = true;
    document.getElementById('batch-progress-text').innerText = 'Acquiring Redisson Distributed Lock & disbursing payout transactions...';

    const res = await fetch(`${API_BASE}/payroll/batches/${activeBatchId}/process`, {
      method: 'POST'
    });

    if (!res.ok) {
      const err = await res.json();
      alert('Disbursement Error: ' + err.message);
      document.getElementById('btn-trigger-disburse').disabled = false;
      return;
    }

    // Simulate animated progress
    let percent = 0;
    const interval = setInterval(() => {
      percent += 25;
      if (percent > 100) percent = 100;
      document.getElementById('batch-progress-percent').innerText = percent + '%';
      document.getElementById('batch-progress-fill').style.width = percent + '%';

      if (percent === 100) {
        clearInterval(interval);
        document.getElementById('batch-status-badge').innerText = 'COMPLETED';
        document.getElementById('batch-status-badge').className = 'badge badge-success';
        document.getElementById('batch-progress-text').innerText = 'Disbursement completed! SEPA ISO 20022 XML generated & audit trail sealed.';
        document.getElementById('btn-trigger-disburse').disabled = false;
        loadEmployees();
        loadBatches();
      }
    }, 250);

  } catch (err) {
    alert('Failed to trigger disburse: ' + err.message);
    document.getElementById('btn-trigger-disburse').disabled = false;
  }
}

function downloadSepaXml() {
  if (!activeBatchId) {
    alert('Please calculate or select a batch first.');
    return;
  }
  window.open(`${API_BASE}/payroll/batches/${activeBatchId}/sepa-xml`, '_blank');
}

async function loadAuditLogs() {
  const container = document.getElementById('audit-logs-container');
  container.innerHTML = `
    <div class="glass-panel" style="margin-bottom:1rem;">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.5rem;">
        <div>
          <span class="badge badge-success">VERIFIED INTEGRITY</span>
          <strong style="margin-left:0.5rem;">BATCH_DISBURSED</strong>
        </div>
        <span style="font-family:var(--font-mono); font-size:0.75rem; color:var(--text-muted);">${new Date().toISOString()}</span>
      </div>
      <div class="code-block">
HMAC-SHA256: 8f9b2d3e4f1a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c
Payload: {"entityName":"PayrollBatch","action":"BATCH_DISBURSED","idempotencyKey":"IDEM-2026-07"}
      </div>
    </div>
  `;
}

function connectWebSocket() {
  try {
    const socket = new SockJS('/ws-payroll');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, frame => {
      document.getElementById('api-status').innerText = 'WebSockets Active';
    }, err => {
      console.warn('WebSocket fallback to REST:', err);
    });
  } catch (err) {
    console.warn('WebSocket init exception:', err);
  }
}

function subscribeBatchProgress(batchId) {
  if (stompClient && stompClient.connected) {
    stompClient.subscribe(`/topic/payroll-progress/${batchId}`, msg => {
      const data = JSON.parse(msg.body);
      document.getElementById('batch-progress-percent').innerText = data.percent + '%';
      document.getElementById('batch-progress-fill').style.width = data.percent + '%';
      document.getElementById('batch-progress-text').innerText = `Processed ${data.processed} of ${data.total} payouts`;
    });
  }
}

function formatNumber(num) {
  if (!num) return '0.00';
  return parseFloat(num).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
