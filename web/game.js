/* ══════════════════════════════════════════════════════════════
   REINO OLVIDADO — game.js
   Gestiona toda la lógica del frontend: pantallas, API, mapa, combate
══════════════════════════════════════════════════════════════ */

'use strict';

const API = '/api';

// ── Estado global del cliente ──────────────────────────────────
let gameId         = null;
let currentScreen  = 'menu';
let selectedClass  = null;
let inCombat       = false;
let lastState      = null;
let bannerTimeout  = null;

// ══════════════════════════════════════════════════════════════
// NAVEGACIÓN DE PANTALLAS
// ══════════════════════════════════════════════════════════════

function showScreen(id) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  document.getElementById('screen-' + id).classList.add('active');
  currentScreen = id;
}

function showMenu()              { showScreen('menu'); }
function showCharacterCreation() { resetCreation(); showScreen('create'); }
function showRanking()           { loadRanking(); showScreen('ranking'); }
function showGame()              { showScreen('game'); attachKeyListeners(); }

// ══════════════════════════════════════════════════════════════
// PARTÍCULAS DECORATIVAS DEL MENÚ
// ══════════════════════════════════════════════════════════════

function spawnParticles() {
  const container = document.getElementById('particles');
  if (!container) return;
  for (let i = 0; i < 30; i++) {
    const p = document.createElement('div');
    p.className = 'particle';
    p.style.left     = Math.random() * 100 + 'vw';
    p.style.animationDuration = (8 + Math.random() * 12) + 's';
    p.style.animationDelay   = (Math.random() * 10) + 's';
    p.style.opacity  = '0';
    container.appendChild(p);
  }
}

// ══════════════════════════════════════════════════════════════
// CREACIÓN DE PERSONAJE
// ══════════════════════════════════════════════════════════════

function resetCreation() {
  selectedClass = null;
  document.querySelectorAll('.class-card').forEach(c => c.classList.remove('selected'));
  document.getElementById('btn-start').disabled = true;
  document.getElementById('hero-name').value = '';
}

function selectClass(cls) {
  selectedClass = cls;
  document.querySelectorAll('.class-card').forEach(c => c.classList.remove('selected'));
  document.getElementById('card-' + cls).classList.add('selected');
  document.getElementById('btn-start').disabled = false;
}

async function startGame() {
  const name = document.getElementById('hero-name').value.trim() || 'Héroe';
  if (!selectedClass) { showBanner('⚠️ Elige una clase primero'); return; }

  try {
    const res  = await fetch(`${API}/game/new`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerClass: selectedClass, playerName: name })
    });
    const data = await res.json();
    gameId    = data.gameId;
    lastState = data.state;
    showGame();
    renderState(data.state);
  } catch (e) {
    showBanner('❌ Error al conectar con el servidor');
    console.error(e);
  }
}

// ══════════════════════════════════════════════════════════════
// ENVÍO DE ACCIONES
// ══════════════════════════════════════════════════════════════

async function sendAction(action) {
  if (!gameId) return;
  try {
    const res   = await fetch(`${API}/game/${gameId}/action`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action })
    });
    const state = await res.json();
    lastState   = state;
    renderState(state);
  } catch (e) {
    showBanner('❌ Error de conexión');
    console.error(e);
  }
}

async function sendCombatAction(action) {
  closeCombatButtons();
  await sendAction(action);
}

// ══════════════════════════════════════════════════════════════
// TECLADO
// ══════════════════════════════════════════════════════════════

let keyListenersAttached = false;

function attachKeyListeners() {
  if (keyListenersAttached) return;
  keyListenersAttached = true;

  document.addEventListener('keydown', (e) => {
    if (currentScreen !== 'game') return;
    if (e.target.tagName === 'INPUT') return;

    // No procesar si hay un modal de inventario abierto
    if (!document.getElementById('modal-inventory').classList.contains('hidden')) return;

    const map = {
      'w': 'move_w', 'a': 'move_a', 's': 'move_s', 'd': 'move_d',
      'W': 'move_w', 'A': 'move_a', 'S': 'move_s', 'D': 'move_d',
      'ArrowUp':    'move_w', 'ArrowDown':  'move_s',
      'ArrowLeft':  'move_a', 'ArrowRight': 'move_d',
      'i': 'inventory', 'I': 'inventory',
      'x': 'save',       'X': 'save'
    };

    const action = map[e.key];
    if (!action) return;
    e.preventDefault();

    if (action === 'inventory') {
      openInventoryModal(false);
      return;
    }

    // Bloquear movimiento en combate
    if (inCombat && action.startsWith('move_')) {
      showBanner('⚔️ ¡Estás en combate!');
      return;
    }

    sendAction(action);
  });
}

// ══════════════════════════════════════════════════════════════
// RENDERIZADO DEL ESTADO
// ══════════════════════════════════════════════════════════════

function renderState(state) {
  if (!state) return;

  updateHUD(state);
  renderMap(state.mapGrid);
  renderInventorySidebar(state.inventoryItems);
  renderEquipment(state.weapon, state.armor);
  updateEventLog(state.recentLog, state.lastEvent);

  // ── Manejar estado especial ────────────────────────────────
  if (state.status === 'IN_COMBAT') {
    inCombat = true;
    showCombatModal(state);
  } else {
    inCombat = false;
    hideCombatModal();
    if (state.status === 'DEAD' || state.status === 'SAVED') {
      setTimeout(() => showGameOver(state), 800);
    }
  }

  // Banner de evento importante
  if (state.lastEvent && state.lastEvent !== (lastState && lastState._prevEvent)) {
    if (state.lastEvent.includes('Piso') || state.lastEvent.includes('VICTORIA') ||
        state.lastEvent.includes('Cofre') || state.lastEvent.includes('Victoria') ||
        state.lastEvent.includes('nivel') || state.lastEvent.includes('NIVEL')) {
      showBanner(state.lastEvent);
    }
  }
  if (lastState) lastState._prevEvent = state.lastEvent;
}

// ── HUD ──────────────────────────────────────────────────────────

function updateHUD(s) {
  setText('hud-name',  s.playerName);
  setText('hud-class', s.playerClass);
  setText('hud-level', s.level);
  setText('hud-floor', s.floor);
  setText('hud-turn',  s.turn);
  setText('hud-score', s.score.toLocaleString());

  setBar('bar-hp',   'text-hp',   s.hp,   s.maxHp);
  setBar('bar-mana', 'text-mana', s.mana, s.maxMana);
  setBar('bar-exp',  'text-exp',  s.exp,  100);

  // HP bajo: tremor visual
  const hpPct = s.maxHp > 0 ? s.hp / s.maxHp : 1;
  document.getElementById('game-hud').classList.toggle('low-hp', hpPct < 0.25);
}

function setBar(barId, textId, cur, max) {
  const pct = max > 0 ? Math.min(100, (cur / max) * 100) : 0;
  document.getElementById(barId).style.width = pct + '%';
  if (textId) setText(textId, `${cur}/${max}`);
}

function setText(id, val) {
  const el = document.getElementById(id);
  if (el) el.textContent = val;
}

// ── Mapa ─────────────────────────────────────────────────────────

// Símbolo visual para cada tipo de celda
const CELL_SYMBOLS = {
  PLAYER:     '@',
  ENEMY:      'g',
  BOSS:       'B',
  CHEST:      'C',
  STAIRS:     '>',
  FLOOR:      '.',
  GRASS:      ',',
  TREE_BIG:   'T',
  TREE_SMALL: 't',
  MOUNTAIN:   '^',
  WATER:      '~',
  HIDDEN:     ' '
};

function renderMap(grid) {
  if (!grid) return;
  const container = document.getElementById('game-map');
  const size = grid.length;

  // Calcular tamaño de celda dinámicamente
  const mapSection  = document.querySelector('.map-section');
  const available   = Math.min(
    mapSection.clientWidth  - 16,
    mapSection.clientHeight - 70
  );
  const cellSize    = Math.floor(available / size);
  container.style.gridTemplateColumns = `repeat(${size}, ${cellSize}px)`;
  container.style.fontSize = Math.max(5, cellSize - 3) + 'px';

  // Reconstruir celdas solo si el número cambió
  if (container.children.length !== size * size) {
    container.innerHTML = '';
    for (let i = 0; i < size * size; i++) {
      const cell = document.createElement('div');
      cell.className = 'map-cell';
      cell.style.width = cell.style.height = cellSize + 'px';
      container.appendChild(cell);
    }
  }

  const cells = container.children;
  let idx = 0;
  for (let i = 0; i < size; i++) {
    for (let j = 0; j < size; j++) {
      const raw        = grid[i][j] || 'HIDDEN:HIDDEN';
      const [type, vis] = raw.split(':');
      const cell       = cells[idx++];
      const sym        = CELL_SYMBOLS[type] || ' ';

      cell.textContent = sym;
      cell.className   = `map-cell cell-${type}${vis === 'VISIBLE' ? ' vis' : ''}${vis === 'DIM' ? ' cell-dim' : ''}`;
      cell.title       = getCellTooltip(type);
    }
  }
}

function getCellTooltip(type) {
  const tips = {
    PLAYER: 'Tú',  ENEMY: 'Enemigo', BOSS: '¡Jefe!', CHEST: 'Cofre',
    STAIRS: 'Escaleras al siguiente piso',
    WATER: 'Agua — no transitable', MOUNTAIN: 'Montaña — no transitable',
    TREE_BIG: 'Árbol grande', TREE_SMALL: 'Árbol pequeño',
    FLOOR: 'Suelo', GRASS: 'Hierba', HIDDEN: ''
  };
  return tips[type] || '';
}

// ── Inventario sidebar ────────────────────────────────────────────

function renderInventorySidebar(items) {
  const container = document.getElementById('inventory-list');
  if (!items || items.length === 0) {
    container.innerHTML = '<div class="inv-empty">— vacío —</div>';
    return;
  }
  container.innerHTML = items.map((item, i) =>
    `<div class="inv-item" onclick="quickUseItem(${i})" title="Clic para usar">${item}</div>`
  ).join('');
}

function quickUseItem(index) {
  if (inCombat) {
    sendAction(`use_item:${index}`);
  } else {
    sendAction(`use_item:${index}`);
  }
}

// ── Equipo ────────────────────────────────────────────────────────

function renderEquipment(weapon, armor) {
  const wEl = document.getElementById('equip-weapon');
  const aEl = document.getElementById('equip-armor');
  if (weapon) { wEl.textContent = '🗡️ ' + weapon; wEl.classList.add('equipped'); }
  else        { wEl.textContent = '🗡️ Sin arma'; wEl.classList.remove('equipped'); }
  if (armor)  { aEl.textContent = '🛡️ ' + armor;  aEl.classList.add('equipped'); }
  else        { aEl.textContent = '🛡️ Sin armadura'; aEl.classList.remove('equipped'); }
}

// ── Log de eventos ────────────────────────────────────────────────

function updateEventLog(log, lastEvent) {
  const container = document.getElementById('log-entries');
  if (!log) return;
  container.innerHTML = log.slice().reverse().map(e =>
    `<div class="log-entry">${escapeHtml(e)}</div>`
  ).join('');
}

// ══════════════════════════════════════════════════════════════
// MODAL DE COMBATE
// ══════════════════════════════════════════════════════════════

function showCombatModal(state) {
  const modal = document.getElementById('modal-combat');
  modal.classList.remove('hidden');

  // Icono del enemigo según nombre
  const enemyIcons = {
    'Goblin':    '👺', 'Orco': '👹', 'Dragón': '🐉', 'Rey Demonio': '👿'
  };
  const icon = enemyIcons[state.enemyName] || '👹';
  document.getElementById('combat-enemy-icon').textContent = icon;

  // Icono del jugador según clase
  const classIcons = { Warrior: '⚔️', Mage: '🔮', Archer: '🏹' };
  document.getElementById('combat-player-icon').textContent = classIcons[state.playerClass] || '🧙';

  setText('combat-title',     '⚔️ ¡COMBATE!');
  setText('combat-enemy-name', state.enemyName);
  setText('combat-enemy-hp',   `${state.enemyHp}/${state.enemyMaxHp}`);
  setText('combat-player-name', state.playerName);
  setText('combat-player-hp', `${state.hp}/${state.maxHp}`);
  setText('combat-mana-text',  `${state.mana}/${state.maxMana}`);
  setText('combat-message',    state.lastEvent);

  // Barras
  setBarDirect('combat-enemy-bar',  state.enemyHp,   state.enemyMaxHp);
  setBarDirect('combat-player-bar', state.hp,        state.maxHp);
  const manaEl = document.getElementById('combat-mana-bar');
  if (manaEl && state.maxMana > 0) {
    manaEl.style.width = `${(state.mana / state.maxMana) * 100}%`;
  }

  // Habilitar botones
  enableCombatButtons();
}

function hideCombatModal() {
  document.getElementById('modal-combat').classList.add('hidden');
}

function closeCombatButtons() {
  ['btn-attack','btn-special','btn-use-item'].forEach(id => {
    document.getElementById(id).disabled = true;
  });
}

function enableCombatButtons() {
  ['btn-attack','btn-special','btn-use-item'].forEach(id => {
    document.getElementById(id).disabled = false;
  });
}

function setBarDirect(id, cur, max) {
  const el = document.getElementById(id);
  if (!el || max <= 0) return;
  el.style.width = Math.max(0, Math.min(100, (cur / max) * 100)) + '%';
}

// ══════════════════════════════════════════════════════════════
// MODAL DE INVENTARIO
// ══════════════════════════════════════════════════════════════

function openInventoryModal(fromCombat) {
  const state = lastState;
  const modal = document.getElementById('modal-inventory');
  const list  = document.getElementById('modal-inv-list');

  if (!state || !state.inventoryItems || state.inventoryItems.length === 0) {
    list.innerHTML = '<div class="inv-empty-modal">📭 El inventario está vacío</div>';
  } else {
    list.innerHTML = state.inventoryItems.map((item, i) =>
      `<div class="modal-inv-item" onclick="useItemFromModal(${i})">${item}</div>`
    ).join('');
  }

  modal.classList.remove('hidden');
}

function openCombatInventory() { openInventoryModal(true); }

function closeInventoryModal() {
  document.getElementById('modal-inventory').classList.add('hidden');
}

function useItemFromModal(index) {
  closeInventoryModal();
  sendAction(`use_item:${index}`);
}

// ══════════════════════════════════════════════════════════════
// PANTALLA GAME OVER
// ══════════════════════════════════════════════════════════════

function showGameOver(state) {
  const isDead  = state.status === 'DEAD';
  const icon    = isDead ? '💀' : '💾';
  const title   = isDead ? 'Fin de la Aventura' : 'Partida Guardada';
  const subtitle = isDead
    ? `${state.playerName} cae en las sombras del abismo...`
    : `Tu leyenda aguarda. Hasta la próxima aventura.`;

  document.getElementById('gameover-icon').textContent   = icon;
  document.getElementById('gameover-title').textContent  = title;
  document.getElementById('gameover-subtitle').textContent = subtitle;

  const statsEl = document.getElementById('gameover-stats');
  statsEl.innerHTML = `
    <div class="gameover-stat-row"><span class="label">Héroe</span><span class="value">${escapeHtml(state.playerName)}</span></div>
    <div class="gameover-stat-row"><span class="label">Clase</span><span class="value">${escapeHtml(state.playerClass)}</span></div>
    <div class="gameover-stat-row"><span class="label">Nivel</span><span class="value">${state.level}</span></div>
    <div class="gameover-stat-row"><span class="label">Piso alcanzado</span><span class="value">${state.floor}</span></div>
    <div class="gameover-stat-row"><span class="label">Turnos sobrevividos</span><span class="value">${state.turn}</span></div>
    <div class="gameover-stat-row"><span class="label">⭐ Puntuación</span><span class="value">${(state.score).toLocaleString()}</span></div>
  `;

  gameId = null;
  showScreen('gameover');
}

// ══════════════════════════════════════════════════════════════
// RANKING
// ══════════════════════════════════════════════════════════════

async function loadRanking() {
  const tbody = document.getElementById('ranking-body');
  const empty = document.getElementById('ranking-empty');
  tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--text-dim);padding:1rem;">Cargando...</td></tr>';

  try {
    const res  = await fetch(`${API}/ranking`);
    const data = await res.json();

    if (!data || data.length === 0) {
      tbody.innerHTML = '';
      empty.classList.remove('hidden');
      return;
    }
    empty.classList.add('hidden');

    const medals = ['🥇','🥈','🥉'];
    tbody.innerHTML = data.map((entry, i) => `
      <tr>
        <td><span class="rank-medal">${medals[i] || (i+1)}</span></td>
        <td><strong>${escapeHtml(entry.playerName)}</strong></td>
        <td>${escapeHtml(entry.playerClass)}</td>
        <td>${entry.floor}</td>
        <td>${entry.level}</td>
        <td>${entry.turn}</td>
        <td class="score-cell">${entry.score.toLocaleString()}</td>
        <td style="color:var(--text-dim);font-size:0.75rem">${escapeHtml(entry.date || '')}</td>
      </tr>
    `).join('');
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--blood-red)">❌ Error al cargar el ranking</td></tr>';
  }
}

// ══════════════════════════════════════════════════════════════
// BANNER DE EVENTOS
// ══════════════════════════════════════════════════════════════

function showBanner(msg) {
  const el = document.getElementById('event-banner');
  el.textContent = msg;
  el.classList.remove('hidden');
  el.style.opacity = '1';

  if (bannerTimeout) clearTimeout(bannerTimeout);
  bannerTimeout = setTimeout(() => {
    el.style.opacity = '0';
    setTimeout(() => el.classList.add('hidden'), 500);
  }, 2500);
}

// ══════════════════════════════════════════════════════════════
// UTILIDADES
// ══════════════════════════════════════════════════════════════

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

// ══════════════════════════════════════════════════════════════
// INICIALIZACIÓN
// ══════════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', () => {
  spawnParticles();

  // Permitir seleccionar clase con Enter/Space en tarjetas
  document.querySelectorAll('.class-card').forEach(card => {
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        card.click(); e.preventDefault();
      }
    });
  });

  // Enter en nombre → seleccionar botón de inicio
  document.getElementById('hero-name').addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && selectedClass) startGame();
  });

  // Cerrar modal combate con ESC no está permitido (combate forzado)
  // Cerrar modal inventario con ESC sí
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeInventoryModal();
  });

  // Ajustar mapa al resize
  let resizeTimer;
  window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      if (lastState && lastState.mapGrid) renderMap(lastState.mapGrid);
    }, 150);
  });

  showMenu();
});
