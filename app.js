// SST Progressive Web App (PWA) - Logic Engine
// Compatível com iOS (Safari), Android (Chrome) e Navegadores Desktop

const STORAGE_KEY_OCCURRENCES = 'sst_occurrences_db_v1';
const STORAGE_KEY_EMPLOYEES = 'sst_employees_db_v1';

// Initial state
let occurrences = [];
let employees = [];
let currentPhotoBase64 = null;
let deferredInstallPrompt = null;

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
  loadData();
  setupNavigation();
  setupFormHandlers();
  setupRiskPriorityLogic();
  setupSpeechRecognition();
  setupPwaInstallation();
  renderDashboard();
  renderOccurrences();
  renderEmployees();

  // Set default date/time to now
  const now = new Date();
  const dateStr = now.toISOString().split('T')[0];
  const timeStr = now.toTimeString().slice(0, 5);
  const dateInput = document.getElementById('inputDate');
  const timeInput = document.getElementById('inputTime');
  if (dateInput) dateInput.value = dateStr;
  if (timeInput) timeInput.value = timeStr;
});

// Storage Management
function loadData() {
  try {
    const occStr = localStorage.getItem(STORAGE_KEY_OCCURRENCES);
    occurrences = occStr ? JSON.parse(occStr) : getDefaultOccurrences();

    const empStr = localStorage.getItem(STORAGE_KEY_EMPLOYEES);
    employees = empStr ? JSON.parse(empStr) : getDefaultEmployees();
    populateEmployeeSelect();
  } catch (e) {
    console.error("Erro ao carregar dados locais:", e);
  }
}

function saveData() {
  try {
    localStorage.setItem(STORAGE_KEY_OCCURRENCES, JSON.stringify(occurrences));
    localStorage.setItem(STORAGE_KEY_EMPLOYEES, JSON.stringify(employees));
  } catch (e) {
    console.error("Erro ao salvar dados locais:", e);
  }
}

function getDefaultOccurrences() {
  return [
    {
      id: 'SST-' + Date.now().toString().slice(-5),
      date: new Date().toISOString().split('T')[0],
      time: '09:30',
      type: 'Quase Acidente',
      risk: 'Físico',
      riskLevel: 'Médio',
      gravity: 'Médio',
      priority: 'Média',
      location: 'Canteiro Central - Andaime 2',
      employee: 'Carlos Silva',
      description: 'Ferramenta manual caiu de altura de 2m, ninguém foi atingido.',
      immediateAction: 'Isolamento imediato do perímetro inferior.',
      correctiveAction: 'Instalação de rodapé e amarrações de ferramentas.',
      photo: null,
      status: 'Pendente'
    }
  ];
}

function getDefaultEmployees() {
  return [
    { id: '1', name: 'Carlos Silva', role: 'Operador de Montagem', badge: 'FUNC-102' },
    { id: '2', name: 'Mariana Santos', role: 'Técnica de SST', badge: 'SST-04' },
    { id: '3', name: 'Roberto Lima', role: 'Encarregado Geral', badge: 'ENC-22' }
  ];
}

function populateEmployeeSelect() {
  const select = document.getElementById('inputEmployee');
  if (!select) return;
  select.innerHTML = '<option value="">Selecione o Colaborador / Relator</option>';
  employees.forEach(emp => {
    const opt = document.createElement('option');
    opt.value = emp.name;
    opt.textContent = `${emp.name} (${emp.role})`;
    select.appendChild(opt);
  });
}

// Navigation between views
function setupNavigation() {
  const navButtons = document.querySelectorAll('.nav-item');
  const panels = document.querySelectorAll('.view-panel');

  navButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const target = btn.getAttribute('data-target');
      navButtons.forEach(b => b.classList.remove('active'));
      panels.forEach(p => p.classList.remove('active'));

      btn.classList.add('active');
      const targetPanel = document.getElementById(target);
      if (targetPanel) targetPanel.classList.add('active');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  });
}

// Form Handlers
function setupFormHandlers() {
  const form = document.getElementById('occurrenceForm');
  const photoInput = document.getElementById('photoInput');
  const photoPreview = document.getElementById('photoPreview');
  const clearPhotoBtn = document.getElementById('clearPhotoBtn');

  // Photo change handler
  if (photoInput) {
    photoInput.addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = (event) => {
          currentPhotoBase64 = event.target.result;
          if (photoPreview) {
            photoPreview.src = currentPhotoBase64;
            photoPreview.style.display = 'block';
          }
          if (clearPhotoBtn) clearPhotoBtn.style.display = 'inline-flex';
        };
        reader.readAsDataURL(file);
      }
    });
  }

  if (clearPhotoBtn) {
    clearPhotoBtn.addEventListener('click', () => {
      currentPhotoBase64 = null;
      if (photoInput) photoInput.value = '';
      if (photoPreview) photoPreview.style.display = 'none';
      clearPhotoBtn.style.display = 'none';
    });
  }

  // Form Submit
  if (form) {
    form.addEventListener('submit', (e) => {
      e.preventDefault();

      const riskLevelVal = document.getElementById('inputRiskLevel') ? document.getElementById('inputRiskLevel').value : (document.getElementById('inputGravity') ? document.getElementById('inputGravity').value : 'Médio');
      const priorityVal = document.getElementById('inputPriority') ? document.getElementById('inputPriority').value : 'Média';
      const statusVal = document.getElementById('inputStatus') ? document.getElementById('inputStatus').value : 'Pendente';

      const newOccurrence = {
        id: 'SST-' + Date.now().toString().slice(-6),
        date: document.getElementById('inputDate').value,
        time: document.getElementById('inputTime').value,
        type: document.getElementById('inputType').value,
        risk: document.getElementById('inputRisk') ? document.getElementById('inputRisk').value : 'Físico',
        riskLevel: riskLevelVal,
        gravity: riskLevelVal,
        priority: priorityVal,
        status: statusVal,
        location: document.getElementById('inputLocation').value,
        employee: document.getElementById('inputEmployee').value || 'Não identificado',
        description: document.getElementById('inputDescription').value,
        immediateAction: document.getElementById('inputImmediateAction').value,
        correctiveAction: document.getElementById('inputCorrectiveAction').value,
        photo: currentPhotoBase64
      };

      occurrences.unshift(newOccurrence);
      saveData();
      renderDashboard();
      renderOccurrences();

      alert(`✅ Ocorrência ${newOccurrence.id} (${newOccurrence.status}) registrada com sucesso!`);

      // Reset form fields
      form.reset();
      currentPhotoBase64 = null;
      if (photoPreview) photoPreview.style.display = 'none';
      if (clearPhotoBtn) clearPhotoBtn.style.display = 'none';

      // Reset date/time and priority logic
      const now = new Date();
      document.getElementById('inputDate').value = now.toISOString().split('T')[0];
      document.getElementById('inputTime').value = now.toTimeString().slice(0, 5);
      if (window.syncRiskPriority) window.syncRiskPriority();

      // Switch to history tab
      document.querySelector('[data-target="panelHistory"]').click();
    });
  }

  // Employee Add Form
  const employeeForm = document.getElementById('employeeForm');
  if (employeeForm) {
    employeeForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const name = document.getElementById('empName').value.trim();
      const role = document.getElementById('empRole').value.trim();
      const badge = document.getElementById('empBadge').value.trim();

      if (!name || !role) return;

      employees.push({ id: Date.now().toString(), name, role, badge });
      saveData();
      populateEmployeeSelect();
      renderEmployees();
      employeeForm.reset();
      alert(`Colaborador ${name} cadastrado com sucesso!`);
    });
  }
}

// Voice Recognition Handler (Web Speech API)
function setupSpeechRecognition() {
  const micBtn = document.getElementById('micBtn');
  const descInput = document.getElementById('inputDescription');
  if (!micBtn || !descInput) return;

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    micBtn.title = "Reconhecimento de voz não suportado neste navegador";
    micBtn.style.opacity = '0.5';
    return;
  }

  const recognition = new SpeechRecognition();
  recognition.lang = 'pt-BR';
  recognition.continuous = false;
  recognition.interimResults = false;

  let isListening = false;

  micBtn.addEventListener('click', () => {
    if (isListening) {
      recognition.stop();
      return;
    }

    try {
      recognition.start();
      isListening = true;
      micBtn.classList.add('recording');
      micBtn.textContent = '🛑 Ouvindo...';
    } catch (e) {
      console.error(e);
    }
  });

  recognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    descInput.value = descInput.value ? `${descInput.value} ${transcript}` : transcript;
  };

  recognition.onend = () => {
    isListening = false;
    micBtn.classList.remove('recording');
    micBtn.textContent = '🎤 Gravar Voz';
  };

  recognition.onerror = (e) => {
    console.warn("Erro no reconhecimento de voz:", e.error);
    isListening = false;
    micBtn.classList.remove('recording');
    micBtn.textContent = '🎤 Gravar Voz';
  };
}

// Risk to Priority Automatic Mapping Logic
function setupRiskPriorityLogic() {
  const riskLevelInput = document.getElementById('inputRiskLevel');
  const priorityInput = document.getElementById('inputPriority');
  if (!riskLevelInput || !priorityInput) return;

  function syncPriority() {
    const val = riskLevelInput.value;
    if (val === 'Baixo') {
      priorityInput.value = 'Baixa';
    } else if (val === 'Médio') {
      priorityInput.value = 'Média';
    } else if (val === 'Alto') {
      priorityInput.value = 'Alta';
    }
  }

  window.syncRiskPriority = syncPriority;
  riskLevelInput.addEventListener('change', syncPriority);
  riskLevelInput.addEventListener('input', syncPriority);
  syncPriority();
}

// Render Dashboard
function renderDashboard() {
  const totalElem = document.getElementById('statTotal');
  const pendingElem = document.getElementById('statPending');
  const inProgressElem = document.getElementById('statInProgress');
  const resolvedElem = document.getElementById('statResolved');

  if (totalElem) totalElem.textContent = occurrences.length;
  if (pendingElem) pendingElem.textContent = occurrences.filter(o => !o.status || o.status === 'Pendente').length;
  if (inProgressElem) inProgressElem.textContent = occurrences.filter(o => o.status === 'Em Andamento').length;
  if (resolvedElem) resolvedElem.textContent = occurrences.filter(o => o.status === 'Resolvida' || o.status === 'Resolvido').length;
}

// Render Occurrence History
function renderOccurrences() {
  const container = document.getElementById('occurrenceList');
  if (!container) return;

  if (occurrences.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; padding: 40px 20px; color: #64748B;">
        <p style="font-size: 2rem;">📋</p>
        <p>Nenhuma ocorrência registrada até o momento.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = occurrences.map(occ => {
    const currentStatus = occ.status || 'Pendente';
    const currentPriority = occ.priority || (occ.gravity === 'Baixa' ? 'Baixa' : (occ.gravity === 'Alta' || occ.gravity === 'Crítica' ? 'Alta' : 'Média'));
    const currentRiskLevel = occ.riskLevel || occ.gravity || 'Médio';
    const statusClass = currentStatus === 'Resolvida' || currentStatus === 'Resolvido' ? 'resolvida' : (currentStatus === 'Em Andamento' ? 'em-andamento' : 'pendente');
    const priorityClass = currentPriority.toLowerCase().split(' ')[0];

    return `
    <div class="occurrence-item">
      <div class="occurrence-header" style="flex-wrap: wrap; gap: 8px;">
        <div>
          <span style="font-weight: 700; color: #006C4C;">#${occ.id}</span>
          <span style="font-size: 0.85rem; color: #64748B; margin-left: 8px;">${occ.date} às ${occ.time}</span>
        </div>
        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
          <span class="badge badge-status-${statusClass}">
            ${currentStatus === 'Em Andamento' ? '🔄' : (currentStatus === 'Resolvida' || currentStatus === 'Resolvido' ? '✅' : '⏳')} ${currentStatus}
          </span>
          <span class="badge badge-priority-${priorityClass}">Prioridade: ${currentPriority}</span>
          <span class="badge badge-${currentRiskLevel.toLowerCase()}">${currentRiskLevel}</span>
        </div>
      </div>
      <div><strong>Tipo:</strong> ${occ.type} | <strong>Risco:</strong> <span class="badge" style="background: #E0E7FF; color: #3730A3;">${occ.risk || 'Físico'}</span> | <strong>Local:</strong> ${occ.location}</div>
      <div><strong>Relator/Colaborador:</strong> ${occ.employee}</div>
      <div style="background: #F8FAFC; padding: 10px; border-radius: 8px; font-size: 0.95rem; margin-top: 4px;">
        ${occ.description}
      </div>
      ${occ.photo ? `<img src="${occ.photo}" style="max-height: 140px; border-radius: 8px; object-fit: cover; margin-top: 4px;" alt="Foto da ocorrência">` : ''}

      <!-- Barra de Controle de Status e Ações -->
      <div style="display: flex; align-items: center; justify-content: space-between; margin-top: 8px; padding-top: 8px; border-top: 1px dashed #E2E8F0; flex-wrap: wrap; gap: 8px;">
        <div style="display: flex; align-items: center; gap: 8px;">
          <label style="font-size: 0.8rem; font-weight: 600; color: #475569;">Status:</label>
          <select onchange="updateOccurrenceStatus('${occ.id}', this.value)" style="font-size: 0.8rem; padding: 4px 8px; border-radius: 8px; border: 1.5px solid #CBD5E1; min-height: 36px; background: #FFFFFF; font-weight: 600;">
            <option value="Pendente" ${currentStatus === 'Pendente' ? 'selected' : ''}>⏳ Pendente</option>
            <option value="Em Andamento" ${currentStatus === 'Em Andamento' ? 'selected' : ''}>🔄 Em Andamento</option>
            <option value="Resolvida" ${(currentStatus === 'Resolvida' || currentStatus === 'Resolvido') ? 'selected' : ''}>✅ Resolvida</option>
          </select>
        </div>

        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
          <button class="btn btn-secondary" style="padding: 6px 10px; font-size: 0.8rem; min-height: 36px; display: inline-flex; align-items: center; gap: 4px;" onclick="exportOccurrencePdf('${occ.id}')" title="Gerar Laudo Técnico em PDF desta ocorrência">
            📄 Laudo PDF
          </button>
          <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 0.85rem; min-height: 36px;" onclick="shareOccurrenceWhatsApp('${occ.id}')">
            📲 WhatsApp
          </button>
          <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 0.85rem; min-height: 36px;" onclick="shareOccurrenceEmail('${occ.id}')">
            ✉️ E-mail
          </button>
          <button class="btn btn-danger" style="padding: 6px 12px; font-size: 0.85rem; min-height: 36px;" onclick="deleteOccurrence('${occ.id}')">
            🗑️ Excluir
          </button>
        </div>
      </div>
    </div>
    `;
  }).join('');
}

// Render Employee List
function renderEmployees() {
  const list = document.getElementById('employeeList');
  if (!list) return;

  if (employees.length === 0) {
    list.innerHTML = '<p style="color: #64748B;">Nenhum colaborador cadastrado.</p>';
    return;
  }

  list.innerHTML = employees.map(emp => `
    <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px; background: #FFFFFF; border-radius: 8px; margin-bottom: 8px; border: 1px solid #E2E8F0;">
      <div>
        <strong>${emp.name}</strong>
        <div style="font-size: 0.85rem; color: #64748B;">${emp.role} ${emp.badge ? `• Matrícula: ${emp.badge}` : ''}</div>
      </div>
      <button class="btn btn-danger" style="padding: 4px 10px; font-size: 0.8rem; min-height: 36px;" onclick="deleteEmployee('${emp.id}')">Excluir</button>
    </div>
  `).join('');
}

// Global actions
window.deleteOccurrence = function(id) {
  if (confirm(`Deseja realmente excluir a ocorrência ${id}?`)) {
    occurrences = occurrences.filter(o => o.id !== id);
    saveData();
    renderDashboard();
    renderOccurrences();
  }
};

window.updateOccurrenceStatus = function(id, newStatus) {
  const occ = occurrences.find(o => o.id === id);
  if (occ) {
    occ.status = newStatus;
    saveData();
    renderDashboard();
    renderOccurrences();
  }
};

window.toggleStatus = function(id) {
  const occ = occurrences.find(o => o.id === id);
  if (occ) {
    occ.status = occ.status === 'Resolvida' || occ.status === 'Resolvido' ? 'Pendente' : 'Resolvida';
    saveData();
    renderDashboard();
    renderOccurrences();
  }
};

window.deleteEmployee = function(id) {
  employees = employees.filter(e => e.id !== id);
  saveData();
  populateEmployeeSelect();
  renderEmployees();
};

window.shareOccurrenceWhatsApp = function(id) {
  const occ = occurrences.find(o => o.id === id);
  if (!occ) return;
  const text = `*RELATÓRIO DE SEGURANÇA DO TRABALHO (SST)*\n` +
    `*Código:* ${occ.id}\n` +
    `*Status:* ${occ.status || 'Pendente'}\n` +
    `*Prioridade:* ${occ.priority || 'Média'}\n` +
    `*Nível de Risco:* ${occ.riskLevel || occ.gravity || 'Médio'}\n` +
    `*Risco:* ${occ.risk || 'Físico'} | *Tipo:* ${occ.type}\n` +
    `*Data/Hora:* ${occ.date} às ${occ.time}\n` +
    `*Local:* ${occ.location}\n` +
    `*Relator:* ${occ.employee}\n` +
    `*Descrição:* ${occ.description}\n` +
    `*Medidas Imediatas:* ${occ.immediateAction || 'N/A'}\n` +
    `*Ações Corretivas:* ${occ.correctiveAction || 'N/A'}`;
  window.open(`https://api.whatsapp.com/send?text=${encodeURIComponent(text)}`, '_blank');
};

window.shareOccurrenceEmail = function(id) {
  const occ = occurrences.find(o => o.id === id);
  if (!occ) return;
  const subject = `[SST] Ocorrência ${occ.id} - ${occ.type} [${occ.status || 'Pendente'}]`;
  const body = `RELATÓRIO DE SEGURANÇA DO TRABALHO (SST)\n\n` +
    `Código: ${occ.id}\n` +
    `Status: ${occ.status || 'Pendente'}\n` +
    `Prioridade: ${occ.priority || 'Média'}\n` +
    `Nível de Risco: ${occ.riskLevel || occ.gravity || 'Médio'}\n` +
    `Risco: ${occ.risk || 'Físico'}\n` +
    `Classificação: ${occ.type}\n` +
    `Data e Hora: ${occ.date} às ${occ.time}\n` +
    `Local / Setor: ${occ.location}\n` +
    `Responsável / Relator: ${occ.employee}\n\n` +
    `Descrição dos Fatos:\n${occ.description}\n\n` +
    `Medidas Imediatas Adotadas:\n${occ.immediateAction || 'N/A'}\n\n` +
    `Ações Corretivas Propostas:\n${occ.correctiveAction || 'N/A'}\n`;
  window.location.href = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
};

window.printReport = function() {
  exportToPdf();
};

// ==========================================
// EXCEL & CSV EXPORT SUITE
// ==========================================
window.exportToExcel = function() {
  if (occurrences.length === 0) {
    alert('Nenhuma ocorrência cadastrada para exportar.');
    return;
  }

  const exportData = occurrences.map(occ => ({
    'Código': occ.id,
    'Data': occ.date,
    'Hora': occ.time,
    'Colaborador': occ.employee,
    'Local / Setor': occ.location,
    'Risco Ambiental': occ.risk || 'Físico',
    'Grau de Risco': occ.riskLevel || occ.gravity || 'Médio',
    'Prioridade': occ.priority || 'Média',
    'Status': occ.status || 'Pendente',
    'Tipo de Ocorrência': occ.type,
    'Descrição Detalhada': occ.description,
    'Medidas Imediatas': occ.immediateAction || 'N/A',
    'Ações Corretivas': occ.correctiveAction || 'N/A',
    'Foto Registrada': occ.photo ? 'Sim' : 'Não'
  }));

  // If SheetJS is available, build .xlsx
  if (window.XLSX) {
    try {
      const ws = XLSX.utils.json_to_sheet(exportData);
      ws['!cols'] = [
        { wch: 14 }, // Código
        { wch: 12 }, // Data
        { wch: 10 }, // Hora
        { wch: 22 }, // Colaborador
        { wch: 25 }, // Local / Setor
        { wch: 16 }, // Risco Ambiental
        { wch: 14 }, // Grau de Risco
        { wch: 14 }, // Prioridade
        { wch: 16 }, // Status
        { wch: 24 }, // Tipo de Ocorrência
        { wch: 45 }, // Descrição Detalhada
        { wch: 35 }, // Medidas Imediatas
        { wch: 35 }, // Ações Corretivas
        { wch: 14 }  // Foto Registrada
      ];
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Ocorrencias_SST");
      const dateStr = new Date().toISOString().split('T')[0];
      XLSX.writeFile(wb, `Relatorio_Ocorrencias_SST_${dateStr}.xlsx`);
      return;
    } catch (err) {
      console.warn("Falha ao gerar .xlsx com SheetJS, usando CSV como fallback:", err);
    }
  }

  // Fallback to formatted CSV
  exportToCsv();
};

window.exportToCsv = function() {
  if (occurrences.length === 0) {
    alert('Nenhuma ocorrência cadastrada para exportar.');
    return;
  }

  const header = "ID;Data;Hora;Colaborador;Local/Setor;Risco Ambiental;Grau de Risco;Prioridade;Status;Tipo de Ocorrência;Descrição;Medidas Imediatas;Ações Corretivas;Foto Anexa\n";
  
  const rows = occurrences.map(occ => {
    const sanitize = (text) => `"${(text || '').toString().replace(/"/g, '""').replace(/\n/g, ' ')}"`;
    return [
      occ.id,
      sanitize(occ.date),
      sanitize(occ.time),
      sanitize(occ.employee),
      sanitize(occ.location),
      sanitize(occ.risk || 'Físico'),
      sanitize(occ.riskLevel || occ.gravity || 'Médio'),
      sanitize(occ.priority || 'Média'),
      sanitize(occ.status || 'Pendente'),
      sanitize(occ.type),
      sanitize(occ.description),
      sanitize(occ.immediateAction || 'N/A'),
      sanitize(occ.correctiveAction || 'N/A'),
      occ.photo ? 'Sim' : 'Não'
    ].join(';');
  }).join('\n');

  // Prepend \uFEFF for UTF-8 BOM so Excel opens accented Portuguese characters perfectly
  const blob = new Blob(["\uFEFF" + header + rows], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `Planilha_Ocorrencias_SST_${new Date().toISOString().split('T')[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

// ==========================================
// EXECUTIVE PDF REPORT GENERATOR (NR-01 / PGR)
// ==========================================
window.exportToPdf = function() {
  if (occurrences.length === 0) {
    alert('Nenhuma ocorrência cadastrada para gerar o relatório PDF.');
    return;
  }

  const now = new Date();
  const dateStr = now.toLocaleDateString('pt-BR');
  const timeStr = now.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

  const total = occurrences.length;
  const pendentes = occurrences.filter(o => !o.status || o.status === 'Pendente').length;
  const emAndamento = occurrences.filter(o => o.status === 'Em Andamento').length;
  const resolvidas = occurrences.filter(o => o.status === 'Resolvida' || o.status === 'Resolvido').length;

  const printHtml = `
    <!DOCTYPE html>
    <html lang="pt-BR">
    <head>
      <meta charset="UTF-8">
      <title>Relatório Técnico Oficial SST - ${dateStr}</title>
      <style>
        @page { size: A4 portrait; margin: 12mm 10mm; }
        body {
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
          color: #0F172A;
          background: #FFFFFF;
          margin: 0;
          padding: 0;
          font-size: 10pt;
          line-height: 1.4;
        }
        .header {
          border-bottom: 2.5px solid #006C4C;
          padding-bottom: 12px;
          margin-bottom: 16px;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .logo-group {
          display: flex;
          align-items: center;
          gap: 12px;
        }
        .logo-icon {
          font-size: 2.4rem;
        }
        .company-name {
          font-size: 1.25rem;
          font-weight: 800;
          color: #006C4C;
          text-transform: uppercase;
          margin: 0;
        }
        .report-subtitle {
          font-size: 0.85rem;
          color: #475569;
          margin: 2px 0 0 0;
          font-weight: 600;
        }
        .meta-box {
          text-align: right;
          font-size: 0.8rem;
          color: #64748B;
        }
        .kpi-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 8px;
          margin-bottom: 16px;
        }
        .kpi-card {
          padding: 10px;
          border-radius: 8px;
          text-align: center;
          border: 1px solid #E2E8F0;
        }
        .kpi-num {
          font-size: 1.4rem;
          font-weight: 800;
        }
        .kpi-label {
          font-size: 0.75rem;
          font-weight: 700;
          text-transform: uppercase;
        }
        table {
          width: 100%;
          border-collapse: collapse;
          margin-bottom: 20px;
          font-size: 8.5pt;
        }
        th, td {
          border: 1px solid #CBD5E1;
          padding: 6px 8px;
          text-align: left;
        }
        th {
          background: #F1F5F9;
          font-weight: 700;
          color: #1E293B;
        }
        .badge {
          display: inline-block;
          padding: 2px 6px;
          border-radius: 4px;
          font-size: 7.5pt;
          font-weight: 700;
        }
        .badge-alta { background: #FEE2E2; color: #991B1B; }
        .badge-media { background: #FEF3C7; color: #92400E; }
        .badge-baixa { background: #E0E7FF; color: #3730A3; }
        .badge-status-pendente { background: #FEF3C7; color: #92400E; }
        .badge-status-em-andamento { background: #DBEAFE; color: #1E40AF; }
        .badge-status-resolvida { background: #D1FAE5; color: #065F46; }
        .card-detail {
          border: 1.5px solid #CBD5E1;
          border-radius: 8px;
          padding: 12px;
          margin-bottom: 14px;
          page-break-inside: avoid;
          background: #FFFFFF;
        }
        .card-header {
          display: flex;
          justify-content: space-between;
          border-bottom: 1px solid #E2E8F0;
          padding-bottom: 6px;
          margin-bottom: 8px;
        }
        .photo-img {
          max-height: 120px;
          border-radius: 6px;
          margin-top: 6px;
          border: 1px solid #CBD5E1;
        }
        .signatures {
          margin-top: 30px;
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 20px;
          text-align: center;
          page-break-inside: avoid;
        }
        .sig-line {
          border-top: 1.5px solid #475569;
          padding-top: 6px;
          font-size: 8pt;
          font-weight: 600;
          color: #334155;
        }
        @media print {
          .no-print-bar { display: none !important; }
        }
      </style>
    </head>
    <body>
      <div class="no-print-bar" style="background: #006C4C; color: #FFF; padding: 10px 16px; text-align: center; margin-bottom: 16px; border-radius: 6px;">
        <button onclick="window.print()" style="background: #FFFFFF; color: #006C4C; border: none; padding: 8px 16px; font-weight: 700; border-radius: 4px; cursor: pointer; font-size: 14px;">
          🖨️ Salvar como PDF / Imprimir Laudo
        </button>
      </div>

      <div class="header">
        <div class="logo-group">
          <span class="logo-icon">🛡️</span>
          <div>
            <h1 class="company-name">SISTEMA INTEGRADO DE SEGURANÇA DO TRABALHO</h1>
            <p class="report-subtitle">PROGRAMA DE GERENCIAMENTO DE RISCOS (PGR / GRO — NR-01)</p>
            <p style="margin: 2px 0 0 0; font-size: 0.8rem; color: #006C4C; font-weight: bold;">Relatório Técnico Executivo de Ocorrências e Desvios</p>
          </div>
        </div>
        <div class="meta-box">
          <div><strong>Emissão:</strong> ${dateStr} às ${timeStr}</div>
          <div><strong>Normas:</strong> NR-01 / NR-12 / NR-18</div>
          <div><strong>Documento Oficial:</strong> SST-${now.getTime().toString().slice(-6)}</div>
        </div>
      </div>

      <div class="kpi-grid">
        <div class="kpi-card" style="background: #E8F5E9;">
          <div class="kpi-num" style="color: #006C4C;">${total}</div>
          <div class="kpi-label" style="color: #1E3A2F;">Total Ocorrências</div>
        </div>
        <div class="kpi-card" style="background: #FEF3C7;">
          <div class="kpi-num" style="color: #B45309;">${pendentes}</div>
          <div class="kpi-label" style="color: #78350F;">Pendentes</div>
        </div>
        <div class="kpi-card" style="background: #DBEAFE;">
          <div class="kpi-num" style="color: #1E40AF;">${emAndamento}</div>
          <div class="kpi-label" style="color: #1E3A8A;">Em Andamento</div>
        </div>
        <div class="kpi-card" style="background: #D1FAE5;">
          <div class="kpi-num" style="color: #065F46;">${resolvidas}</div>
          <div class="kpi-label" style="color: #064E3B;">Resolvidas</div>
        </div>
      </div>

      <h3 style="font-size: 0.95rem; color: #1E293B; margin-bottom: 8px; border-bottom: 1.5px solid #CBD5E1; padding-bottom: 4px;">
        1. Quadro Consolidado de Ocorrências
      </h3>
      <table>
        <thead>
          <tr>
            <th>Protocolo</th>
            <th>Data/Hora</th>
            <th>Colaborador</th>
            <th>Local / Setor</th>
            <th>Risco</th>
            <th>Nível</th>
            <th>Prioridade</th>
            <th>Status</th>
            <th>Tipo</th>
          </tr>
        </thead>
        <tbody>
          ${occurrences.map(occ => {
            const status = occ.status || 'Pendente';
            const riskLevel = occ.riskLevel || occ.gravity || 'Médio';
            const priority = occ.priority || 'Média';
            return `
              <tr>
                <td><strong>#${occ.id}</strong></td>
                <td>${occ.date} ${occ.time}</td>
                <td>${occ.employee}</td>
                <td>${occ.location}</td>
                <td><span class="badge" style="background: #E0E7FF; color: #3730A3;">${occ.risk || 'Físico'}</span></td>
                <td><span class="badge badge-${riskLevel.toLowerCase()}">${riskLevel}</span></td>
                <td><strong>${priority}</strong></td>
                <td><span class="badge badge-status-${status === 'Em Andamento' ? 'em-andamento' : (status === 'Resolvida' || status === 'Resolvido' ? 'resolvida' : 'pendente')}">${status}</span></td>
                <td>${occ.type}</td>
              </tr>
            `;
          }).join('')}
        </tbody>
      </table>

      <h3 style="font-size: 0.95rem; color: #1E293B; margin-top: 24px; margin-bottom: 10px; border-bottom: 1.5px solid #CBD5E1; padding-bottom: 4px;">
        2. Fichas Detalhadas e Planos de Ação
      </h3>
      ${occurrences.map(occ => {
        const status = occ.status || 'Pendente';
        const riskLevel = occ.riskLevel || occ.gravity || 'Médio';
        const priority = occ.priority || 'Média';
        return `
          <div class="card-detail">
            <div class="card-header">
              <div>
                <strong style="color: #006C4C; font-size: 1rem;">Ocorrência #${occ.id}</strong>
                <span style="color: #64748B; font-size: 0.8rem; margin-left: 8px;">${occ.date} às ${occ.time}</span>
              </div>
              <div>
                <span class="badge" style="background: #E0E7FF; color: #3730A3;">Risco: ${occ.risk || 'Físico'}</span>
                <span class="badge badge-${riskLevel.toLowerCase()}">Grau: ${riskLevel}</span>
                <span class="badge">Prioridade: ${priority}</span>
                <span class="badge badge-status-${status === 'Em Andamento' ? 'em-andamento' : (status === 'Resolvida' || status === 'Resolvido' ? 'resolvida' : 'pendente')}">Status: ${status}</span>
              </div>
            </div>
            <div style="font-size: 0.85rem; margin-bottom: 6px;">
              <strong>Tipo:</strong> ${occ.type} | <strong>Local:</strong> ${occ.location} | <strong>Colaborador / Relator:</strong> ${occ.employee}
            </div>
            <div style="background: #F8FAFC; padding: 8px; border-radius: 6px; font-size: 0.85rem; margin-bottom: 6px; border: 1px solid #E2E8F0;">
              <strong>Descrição dos Fatos:</strong><br>
              ${occ.description}
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; font-size: 0.8rem; margin-bottom: 6px;">
              <div style="background: #FFFBEB; padding: 6px 8px; border-radius: 6px; border: 1px solid #FDE68A;">
                <strong>Medidas Imediatas Adotadas:</strong><br>
                ${occ.immediateAction || 'Nenhuma medida imediata informada.'}
              </div>
              <div style="background: #F0FDF4; padding: 6px 8px; border-radius: 6px; border: 1px solid #BBF7D0;">
                <strong>Ações Corretivas / Preventivas Propostas:</strong><br>
                ${occ.correctiveAction || 'Nenhuma ação corretiva informada.'}
              </div>
            </div>
            ${occ.photo ? `<div style="margin-top: 6px;"><strong>Evidência Fotográfica:</strong><br><img class="photo-img" src="${occ.photo}"></div>` : ''}
          </div>
        `;
      }).join('')}

      <div class="signatures">
        <div>
          <div class="sig-line">
            Responsável Técnico SST<br>
            Engenharia / Técnico de Segurança (Reg. MTE)
          </div>
        </div>
        <div>
          <div class="sig-line">
            Comissão Interna de Prevenção (CIPA)<br>
            Representante dos Empregados
          </div>
        </div>
        <div>
          <div class="sig-line">
            Gestão Operacional / Unidade<br>
            Aprovação e Validação de Medidas
          </div>
        </div>
      </div>

      <script>
        window.onload = function() {
          setTimeout(() => { window.print(); }, 400);
        };
      </script>
    </body>
    </html>
  `;

  const printWindow = window.open('', '_blank');
  if (printWindow) {
    printWindow.document.open();
    printWindow.document.write(printHtml);
    printWindow.document.close();
  } else {
    // If pop-up is blocked on mobile Safari, open via blob or direct print
    const blob = new Blob([printHtml], { type: 'text/html;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    window.location.href = url;
  }
};

// ==========================================
// INDIVIDUAL OCCURRENCE PDF DOSSIER
// ==========================================
window.exportOccurrencePdf = function(id) {
  const occ = occurrences.find(o => o.id === id);
  if (!occ) return;

  const now = new Date();
  const dateStr = now.toLocaleDateString('pt-BR');
  const status = occ.status || 'Pendente';
  const riskLevel = occ.riskLevel || occ.gravity || 'Médio';
  const priority = occ.priority || 'Média';

  const singleHtml = `
    <!DOCTYPE html>
    <html lang="pt-BR">
    <head>
      <meta charset="UTF-8">
      <title>Laudo Individual SST - ${occ.id}</title>
      <style>
        @page { size: A4 portrait; margin: 15mm 12mm; }
        body {
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
          color: #0F172A;
          background: #FFFFFF;
          margin: 0;
          padding: 0;
          font-size: 10.5pt;
          line-height: 1.5;
        }
        .header {
          border-bottom: 2.5px solid #006C4C;
          padding-bottom: 12px;
          margin-bottom: 20px;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .box {
          border: 1.5px solid #CBD5E1;
          border-radius: 8px;
          padding: 14px;
          margin-bottom: 16px;
        }
        .box-title {
          font-weight: 700;
          color: #006C4C;
          margin-bottom: 8px;
          font-size: 1rem;
          border-bottom: 1px solid #E2E8F0;
          padding-bottom: 4px;
        }
        .grid-2 {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;
        }
        .badge {
          display: inline-block;
          padding: 3px 8px;
          border-radius: 4px;
          font-size: 8pt;
          font-weight: 700;
        }
        .signatures {
          margin-top: 40px;
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 30px;
          text-align: center;
        }
        .sig-line {
          border-top: 1.5px solid #334155;
          padding-top: 6px;
          font-size: 8.5pt;
          font-weight: 600;
        }
        @media print { .no-print-bar { display: none !important; } }
      </style>
    </head>
    <body>
      <div class="no-print-bar" style="background: #006C4C; color: #FFF; padding: 10px 16px; text-align: center; margin-bottom: 16px; border-radius: 6px;">
        <button onclick="window.print()" style="background: #FFFFFF; color: #006C4C; border: none; padding: 8px 16px; font-weight: 700; border-radius: 4px; cursor: pointer; font-size: 14px;">
          🖨️ Salvar como PDF / Imprimir Ficha
        </button>
      </div>

      <div class="header">
        <div>
          <h2 style="margin: 0; color: #006C4C; font-size: 1.3rem;">FICHA TÉCNICA INDIVIDUAL DE OCORRÊNCIA</h2>
          <p style="margin: 2px 0 0 0; color: #475569; font-weight: 600; font-size: 0.9rem;">Investigação e Registro de Desvios / Incidentes SST</p>
        </div>
        <div style="text-align: right; font-size: 0.85rem; color: #64748B;">
          <div><strong>Protocolo:</strong> #${occ.id}</div>
          <div><strong>Data de Emissão:</strong> ${dateStr}</div>
        </div>
      </div>

      <div class="box">
        <div class="box-title">1. Identificação e Classificação do Evento</div>
        <div class="grid-2">
          <div><strong>Data da Ocorrência:</strong> ${occ.date} às ${occ.time}</div>
          <div><strong>Status Atual:</strong> <span class="badge" style="background: #E2E8F0; color: #0F172A;">${status}</span></div>
          <div><strong>Tipo de Ocorrência:</strong> ${occ.type}</div>
          <div><strong>Risco Ambiental:</strong> <span class="badge" style="background: #E0E7FF; color: #3730A3;">${occ.risk || 'Físico'}</span></div>
          <div><strong>Grau de Risco:</strong> ${riskLevel}</div>
          <div><strong>Prioridade de Tratativa:</strong> ${priority}</div>
          <div><strong>Local / Frente de Trabalho:</strong> ${occ.location}</div>
          <div><strong>Colaborador / Relator:</strong> ${occ.employee}</div>
        </div>
      </div>

      <div class="box">
        <div class="box-title">2. Descrição Detalhada dos Fatos</div>
        <div style="background: #F8FAFC; padding: 12px; border-radius: 6px; border: 1px solid #E2E8F0;">
          ${occ.description}
        </div>
      </div>

      <div class="box">
        <div class="box-title">3. Medidas Imediatas e Plano de Ação Preventivo</div>
        <div class="grid-2">
          <div style="background: #FFFBEB; padding: 10px; border-radius: 6px; border: 1px solid #FDE68A;">
            <strong>Medidas Imediatas Adotadas:</strong><br>
            ${occ.immediateAction || 'Nenhuma medida imediata registrada.'}
          </div>
          <div style="background: #F0FDF4; padding: 10px; border-radius: 6px; border: 1px solid #BBF7D0;">
            <strong>Ações Corretivas / Preventivas:</strong><br>
            ${occ.correctiveAction || 'Nenhuma ação preventiva registrada.'}
          </div>
        </div>
      </div>

      ${occ.photo ? `
        <div class="box">
          <div class="box-title">4. Registro Fotográfico</div>
          <div style="text-align: center;">
            <img src="${occ.photo}" style="max-height: 240px; border-radius: 8px; border: 1px solid #CBD5E1;" alt="Foto do evento">
          </div>
        </div>
      ` : ''}

      <div class="signatures">
        <div>
          <div class="sig-line">
            Responsável pela Investigação (SST)<br>
            Técnico / Engenheiro de Segurança
          </div>
        </div>
        <div>
          <div class="sig-line">
            Gestor Responsável pela Área<br>
            Ciência e Validação das Ações
          </div>
        </div>
      </div>

      <script>
        window.onload = function() {
          setTimeout(() => { window.print(); }, 400);
        };
      </script>
    </body>
    </html>
  `;

  const printWindow = window.open('', '_blank');
  if (printWindow) {
    printWindow.document.open();
    printWindow.document.write(singleHtml);
    printWindow.document.close();
  }
};

// ==========================================
// DATA SYNCHRONIZATION & BACKUP
// ==========================================
window.syncAllReports = function() {
  saveData();
  renderDashboard();
  renderOccurrences();
  renderEmployees();
  alert(`✅ Sincronização concluída com sucesso!\n\n• ${occurrences.length} ocorrências e ${employees.length} colaboradores atualizados.\n• Dados prontos para emissão de relatórios em Excel e PDF.`);
};

window.exportBackupJson = function() {
  const data = {
    version: '1.0',
    exportDate: new Date().toISOString(),
    occurrences: occurrences,
    employees: employees
  };

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `Backup_SST_Completo_${new Date().toISOString().split('T')[0]}.json`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

window.handleBackupFile = function(input) {
  if (!input.files || input.files.length === 0) return;
  const file = input.files[0];
  const reader = new FileReader();

  reader.onload = function(e) {
    try {
      const imported = JSON.parse(e.target.result);
      if (imported.occurrences && Array.isArray(imported.occurrences)) {
        occurrences = imported.occurrences;
        if (imported.employees && Array.isArray(imported.employees)) {
          employees = imported.employees;
        }
        saveData();
        renderDashboard();
        renderOccurrences();
        renderEmployees();
        populateEmployeeSelect();
        alert(`✅ Backup restaurado com sucesso! ${occurrences.length} ocorrências sincronizadas.`);
      } else {
        alert('Formato de backup inválido.');
      }
    } catch (err) {
      alert('Erro ao processar o arquivo de backup.');
      console.error(err);
    }
  };

  reader.readAsText(file);
};

// PWA Setup (Service Worker & Install Banner)
function setupPwaInstallation() {
  // Register Service Worker
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('./sw.js')
        .then(reg => console.log('SST PWA Service Worker registrado:', reg.scope))
        .catch(err => console.warn('Falha no registro do Service Worker:', err));
    });
  }

  // Detect iOS Safari
  const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone;

  const iosBanner = document.getElementById('iosInstallBanner');
  if (isIos && !isStandalone && iosBanner) {
    iosBanner.style.display = 'flex';
  }

  // Android / Desktop Chrome PWA Install Prompt
  const installBanner = document.getElementById('pwaInstallBanner');
  const installBtn = document.getElementById('pwaInstallBtn');

  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredInstallPrompt = e;
    if (installBanner) installBanner.style.display = 'flex';
  });

  if (installBtn) {
    installBtn.addEventListener('click', async () => {
      if (deferredInstallPrompt) {
        deferredInstallPrompt.prompt();
        const { outcome } = await deferredInstallPrompt.userChoice;
        console.log(`Instalação PWA: ${outcome}`);
        deferredInstallPrompt = null;
        if (installBanner) installBanner.style.display = 'none';
      }
    });
  }
}
