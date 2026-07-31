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

function generateNewIdempotencyKey() {
  const period = document.getElementById('batch-period').value || '2026-07';
  const newKey = 'IDEM-' + period + '-' + Math.floor(1000 + Math.random() * 9000);
  document.getElementById('batch-idempotency').value = newKey;
  return newKey;
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
      const msg = err.message || JSON.stringify(err);
      
      if (msg.includes('Duplicate batch request') || msg.includes('idempotency')) {
        const nextKey = generateNewIdempotencyKey();
        alert(`🔒 Idempotency Safety Catch:\n\nBatch request for '${idempotencyKey}' was already created and processed.\n\nA fresh key '${nextKey}' has been generated. Click 'Calculate Batch & Tax Deductions' again to proceed.`);
      } else {
        alert('Failed to create batch: ' + msg);
      }
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
    
    // Auto-generate fresh key for next batch
    generateNewIdempotencyKey();

    alert(`Batch ${batch.batchReference} calculated successfully!\n\nNet Payout Total: €${formatNumber(batch.totalNet)}`);
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

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerText = message;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

async function loadAuditLogs() {
  const container = document.getElementById('audit-logs-container');
  try {
    const res = await fetch(`${API_BASE}/audit-logs`);
    if (!res.ok) throw new Error('Failed to fetch audit logs');
    const logs = await res.json();

    if (logs.length === 0) {
      container.innerHTML = `<div style="color:var(--text-muted); text-align:center; padding:2rem;">No financial audit entries recorded yet. Create or disburse a batch to generate cryptographic audit seals.</div>`;
      return;
    }

    container.innerHTML = logs.map(log => `
      <div class="glass-panel" style="margin-bottom:1rem; padding:1.25rem;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.6rem;">
          <div>
            <span class="badge ${log.verified ? 'badge-success' : 'badge-warning'}">
              ${log.verified ? 'VERIFIED INTEGRITY' : 'TAMPER DETECTED'}
            </span>
            <strong style="margin-left:0.6rem; color:var(--text-main); font-family:var(--font-mono);">${escapeHtml(log.action)}</strong>
            <span style="color:var(--text-muted); font-size:0.8rem; margin-left:0.5rem;">[${escapeHtml(log.entityName)} ID: ${escapeHtml(log.entityId)}]</span>
          </div>
          <span style="font-family:var(--font-mono); font-size:0.75rem; color:var(--text-dim);">${new Date(log.createdAt).toLocaleString()}</span>
        </div>
        <div class="code-block" style="font-size:0.8rem; padding:0.75rem;">
<span style="color:var(--accent-emerald);">HMAC-SHA256:</span> ${escapeHtml(log.hmacSignature)}
<span style="color:#a5b4fc;">Payload:</span> ${escapeHtml(log.payloadJson)}
        </div>
      </div>
    `).join('');

  } catch (err) {
    console.error('Error loading audit logs:', err);
    container.innerHTML = `<div style="color:var(--accent-rose); text-align:center;">Failed to load audit logs: ${escapeHtml(err.message)}</div>`;
  }
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
