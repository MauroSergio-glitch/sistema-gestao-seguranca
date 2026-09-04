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
      gravity: 'Média',
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

      const newOccurrence = {
        id: 'SST-' + Date.now().toString().slice(-6),
        date: document.getElementById('inputDate').value,
        time: document.getElementById('inputTime').value,
        type: document.getElementById('inputType').value,
        gravity: document.getElementById('inputGravity').value,
        location: document.getElementById('inputLocation').value,
        employee: document.getElementById('inputEmployee').value || 'Não identificado',
        description: document.getElementById('inputDescription').value,
        immediateAction: document.getElementById('inputImmediateAction').value,
        correctiveAction: document.getElementById('inputCorrectiveAction').value,
        photo: currentPhotoBase64,
        status: 'Pendente'
      };

      occurrences.unshift(newOccurrence);
      saveData();
      renderDashboard();
      renderOccurrences();

      alert(`✅ Ocorrência ${newOccurrence.id} registrada com sucesso!`);

      // Reset form fields
      form.reset();
      currentPhotoBase64 = null;
      if (photoPreview) photoPreview.style.display = 'none';
      if (clearPhotoBtn) clearPhotoBtn.style.display = 'none';

      // Reset date/time
      const now = new Date();
      document.getElementById('inputDate').value = now.toISOString().split('T')[0];
      document.getElementById('inputTime').value = now.toTimeString().slice(0, 5);

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

// Render Dashboard
function renderDashboard() {
  const totalElem = document.getElementById('statTotal');
  const highElem = document.getElementById('statHigh');
  const pendingElem = document.getElementById('statPending');

  if (totalElem) totalElem.textContent = occurrences.length;
  if (highElem) highElem.textContent = occurrences.filter(o => o.gravity === 'Alta' || o.gravity === 'Crítica').length;
  if (pendingElem) pendingElem.textContent = occurrences.filter(o => o.status === 'Pendente').length;
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

  container.innerHTML = occurrences.map(occ => `
    <div class="occurrence-item">
      <div class="occurrence-header">
        <div>
          <span style="font-weight: 700; color: #006C4C;">#${occ.id}</span>
          <span style="font-size: 0.85rem; color: #64748B; margin-left: 8px;">${occ.date} às ${occ.time}</span>
        </div>
        <span class="badge badge-${occ.gravity.toLowerCase()}">${occ.gravity}</span>
      </div>
      <div><strong>Tipo:</strong> ${occ.type} | <strong>Local:</strong> ${occ.location}</div>
      <div><strong>Relator/Colaborador:</strong> ${occ.employee}</div>
      <div style="background: #F8FAFC; padding: 10px; border-radius: 8px; font-size: 0.95rem; margin-top: 4px;">
        ${occ.description}
      </div>
      ${occ.photo ? `<img src="${occ.photo}" style="max-height: 140px; border-radius: 8px; object-fit: cover; margin-top: 4px;" alt="Foto da ocorrência">` : ''}
      <div style="display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap;">
        <button class="btn btn-secondary" style="padding: 6px 14px; font-size: 0.85rem;" onclick="shareOccurrenceWhatsApp('${occ.id}')">
          📲 WhatsApp
        </button>
        <button class="btn btn-secondary" style="padding: 6px 14px; font-size: 0.85rem;" onclick="shareOccurrenceEmail('${occ.id}')">
          ✉️ E-mail
        </button>
        <button class="btn btn-secondary" style="padding: 6px 14px; font-size: 0.85rem;" onclick="toggleStatus('${occ.id}')">
          ${occ.status === 'Resolvido' ? '↩️ Reabrir' : '✅ Marcar Resolvido'}
        </button>
        <button class="btn btn-danger" style="padding: 6px 14px; font-size: 0.85rem;" onclick="deleteOccurrence('${occ.id}')">
          🗑️ Excluir
        </button>
      </div>
    </div>
  `).join('');
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

window.toggleStatus = function(id) {
  const occ = occurrences.find(o => o.id === id);
  if (occ) {
    occ.status = occ.status === 'Resolvido' ? 'Pendente' : 'Resolvido';
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
    `*Data/Hora:* ${occ.date} às ${occ.time}\n` +
    `*Gravidade:* ${occ.gravity} | *Tipo:* ${occ.type}\n` +
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
  const subject = `[SST] Ocorrência ${occ.id} - ${occ.type} (${occ.gravity})`;
  const body = `RELATÓRIO DE SEGURANÇA DO TRABALHO (SST)\n\n` +
    `Código: ${occ.id}\n` +
    `Data e Hora: ${occ.date} às ${occ.time}\n` +
    `Classificação: ${occ.type}\n` +
    `Gravidade: ${occ.gravity}\n` +
    `Local / Setor: ${occ.location}\n` +
    `Responsável / Relator: ${occ.employee}\n\n` +
    `Descrição dos Fatos:\n${occ.description}\n\n` +
    `Medidas Imediatas Adotadas:\n${occ.immediateAction || 'N/A'}\n\n` +
    `Ações Corretivas Propostas:\n${occ.correctiveAction || 'N/A'}\n`;
  window.location.href = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
};

window.printReport = function() {
  window.print();
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
