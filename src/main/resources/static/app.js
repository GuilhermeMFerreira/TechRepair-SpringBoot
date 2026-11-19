// Servido pelo Spring (static/), deixe vazio. Fora do Spring, use "http://localhost:8080" e @CrossOrigin.
const API_BASE = "";

const LABEL = {
  ABERTA: "Aberta", EM_DIAGNOSTICO: "Em diagnóstico", AGUARDANDO_APROVACAO: "Aguardando aprovação",
  EM_REPARO: "Em reparo", CONCLUIDA: "Concluída", ENTREGUE: "Entregue", CANCELADA: "Cancelada",
  PENDENTE: "Pendente", CONFIRMADO: "Confirmado", ESTORNADO: "Estornado"
};
// Espelha o fluxo do backend, que é quem realmente valida cada mudança.
const NEXT = {
  ABERTA: ["EM_DIAGNOSTICO", "CANCELADA"], EM_DIAGNOSTICO: ["AGUARDANDO_APROVACAO", "CANCELADA"],
  AGUARDANDO_APROVACAO: ["EM_REPARO", "CANCELADA"], EM_REPARO: ["CONCLUIDA", "CANCELADA"],
  CONCLUIDA: ["ENTREGUE"], ENTREGUE: [], CANCELADA: []
};
const FORMAS = ["Pix", "Cartão de crédito", "Cartão de débito", "Dinheiro"].map((x) => [x, x]);

const $ = (id) => document.getElementById(id);
const state = { entity: "clientes", rows: [], pagamentos: [], dlg: null };
const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const dash = (v) => (v === null || v === undefined || v === "" ? "—" : esc(v));
const data = (v) => (v ? esc(String(v).split("-").reverse().join("/")) : "—");
const badge = (s) => `<span class="badge b-${esc(String(s).toLowerCase())}">${esc(LABEL[s] || s)}</span>`;
const sub = (t) => (t ? `<span class="sub">${esc(t)}</span>` : "");

// Como cada registro aparece dentro de uma lista de escolha ou de uma célula.
const nome = {
  clientes: (r) => r.nome,
  tecnicos: (r) => r.nome,
  equipamentos: (r) => [r.tipo, r.marca, r.modelo].filter(Boolean).join(" ") + (r.cliente ? " (" + r.cliente.nome + ")" : ""),
  ordensservicos: (r) => "OS #" + r.id + " · " + nome.equipamentos(r.equipamento)
};
const pagoDa = (os) => state.pagamentos.filter((p) => p.ordem && p.ordem.id === os.id && p.status === "CONFIRMADO").reduce((s, p) => s + p.valor, 0);
const fechada = (os) => os.status === "ENTREGUE" || os.status === "CANCELADA";

const ENTITIES = {
  clientes: {
    title: "Clientes", path: "/api/clientes", desc: "Quem deixa os aparelhos para conserto.",
    cols: [["Nome", (r) => dash(r.nome)], ["E-mail", (r) => dash(r.email)], ["Telefone", (r) => dash(r.telefone)]],
    fields: [{ k: "nome", label: "Nome", required: true }, { k: "email", label: "E-mail", type: "email" }, { k: "telefone", label: "Telefone", type: "tel" }],
    actions: (r) => [A.edit(r), A.del(r)]
  },
  equipamentos: {
    title: "Equipamentos", path: "/api/equipamentos", desc: "Cada aparelho pertence a um cliente.",
    cols: [["Equipamento", (r) => dash([r.tipo, r.marca, r.modelo].filter(Boolean).join(" "))], ["Nº de série", (r) => dash(r.numero_serie)], ["Cliente", (r) => dash(r.cliente && r.cliente.nome)]],
    fields: [
      { k: "cliente", label: "Cliente", ref: "clientes", required: true, empty: "Cadastre um cliente primeiro" },
      { k: "tipo", label: "Tipo (notebook, celular...)", required: true },
      { k: "marca", label: "Marca" }, { k: "modelo", label: "Modelo" }, { k: "numero_serie", label: "Número de série" }
    ],
    actions: (r) => [A.edit(r), A.del(r)]
  },
  tecnicos: {
    title: "Técnicos", path: "/api/tecnicos", desc: "Equipe que faz os reparos.",
    cols: [["Nome", (r) => dash(r.nome)], ["Especialidades", (r) => dash(r.especialidades)], ["E-mail", (r) => dash(r.email)]],
    fields: [{ k: "nome", label: "Nome", required: true }, { k: "especialidades", label: "Especialidades" }, { k: "email", label: "E-mail", type: "email" }],
    actions: (r) => [A.edit(r), A.del(r)]
  },
  ordensservicos: {
    title: "Ordens de serviço", path: "/api/ordensservicos",
    desc: "Liga equipamento e técnico. O status segue um fluxo e só avança pelo botão Status.",
    cols: [
      ["Ordem", (r) => "#" + r.id + sub(r.descricaoProblema)],
      ["Equipamento", (r) => dash(nome.equipamentos(r.equipamento))],
      ["Técnico", (r) => dash(r.tecnico && r.tecnico.nome)],
      ["Status", (r) => badge(r.status)],
      ["Valor / pago", (r) => (r.valor > 0 ? esc(brl.format(r.valor)) + sub("pago " + brl.format(pagoDa(r))) : "Sem orçamento")],
      ["Aberta em", (r) => data(r.dataAbertura)]
    ],
    fields: [
      { k: "equipamento", label: "Equipamento", ref: "equipamentos", required: true, only: "create", empty: "Cadastre um equipamento primeiro" },
      { k: "descricaoProblema", label: "Problema relatado", type: "textarea", required: true },
      { k: "tecnico", label: "Técnico responsável", ref: "tecnicos", only: "edit", empty: "Nenhum técnico cadastrado" },
      { k: "diagnostico", label: "Diagnóstico", type: "textarea", only: "edit" },
      { k: "valor", label: "Valor do orçamento (R$)", type: "number", step: "0.01", only: "edit" }
    ],
    actions: (r) => [
      NEXT[r.status].length ? { label: "Status", run: () => mudarStatus(r) } : null,
      fechada(r) ? null : A.edit(r), A.del(r)
    ]
  },
  pagamentos: {
    title: "Pagamentos", path: "/api/pagamentos",
    desc: "Todo pagamento nasce Pendente. Só vira Confirmado com o comprovante de quem recebeu.",
    cols: [
      ["Ordem", (r) => dash(r.ordem && nome.ordensservicos(r.ordem))],
      ["Valor", (r) => esc(brl.format(r.valor))],
      ["Forma", (r) => dash(r.metodoPagamento)],
      ["Status", (r) => badge(r.status)],
      ["Comprovante", (r) => (r.referencia ? esc(r.referencia) + sub(new Date(r.confirmadoEm).toLocaleString("pt-BR")) : "—")]
    ],
    fields: [
      { k: "ordem", label: "Ordem de serviço", ref: "ordensservicos", required: true, only: "create", empty: "Nenhuma ordem com orçamento", filter: (o) => o.valor > 0 && o.status !== "CANCELADA" },
      { k: "valor", label: "Valor (R$)", type: "number", step: "0.01", required: true },
      { k: "metodoPagamento", label: "Forma de pagamento", type: "select", options: FORMAS, required: true }
    ],
    actions: (r) => r.status === "PENDENTE"
      ? [{ label: "Confirmar", run: () => confirmar(r) }, A.edit(r), A.del(r)]
      : r.status === "CONFIRMADO" ? [{ label: "Estornar", cls: "del", run: () => estornar(r) }] : []
  }
};

const A = {
  edit: (r) => ({ label: "Editar", run: () => openForm(r) }),
  del: (r) => ({ label: "Excluir", cls: "del", run: () => remove(r) })
};

function toast(msg) {
  const t = $("toast");
  t.textContent = msg;
  t.classList.add("show");
  clearTimeout(toast.t);
  toast.t = setTimeout(() => t.classList.remove("show"), 2800);
}
const setStatus = (m) => { $("statusMessage").textContent = m || ""; };

// O backend devolve {"erro": "..."} com o motivo; mostramos exatamente essa mensagem.
async function request(path, method = "GET", body) {
  const res = await fetch(API_BASE + path, {
    method,
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined
  });
  if (!res.ok) {
    let msg = "Erro " + res.status + " em " + method + " " + path;
    try { const j = await res.json(); msg = j.erro || j.message || msg; } catch (e) { /* sem corpo */ }
    throw new Error(msg);
  }
  return method === "GET" ? res.json() : null;
}

function render() {
  const e = ENTITIES[state.entity];
  const q = $("searchInput").value.trim().toLowerCase();
  const rows = state.rows.filter((r) => !q || JSON.stringify(r).toLowerCase().includes(q));
  $("tableHead").innerHTML = "<tr><th>ID</th>" + e.cols.map((c) => `<th>${esc(c[0])}</th>`).join("") + "<th>Ações</th></tr>";
  $("tableBody").innerHTML = rows.length
    ? rows.map((r) => `<tr><td>${esc(r.id)}</td>` + e.cols.map((c) => `<td>${c[1](r)}</td>`).join("") +
        `<td><div class="row-actions">` +
        e.actions(r).map((a, i) => a ? `<button class="${a.cls || ""}" data-id="${r.id}" data-a="${i}">${esc(a.label)}</button>` : "").join("") +
        `</div></td></tr>`).join("")
    : `<tr><td class="empty" colspan="${e.cols.length + 2}">Nenhum registro encontrado.</td></tr>`;
  $("recordCount").textContent = rows.length + " de " + state.rows.length + " registro(s)";
}

async function load() {
  const e = ENTITIES[state.entity];
  setStatus("");
  $("tableBody").innerHTML = `<tr><td class="empty" colspan="${e.cols.length + 2}">Carregando...</td></tr>`;
  try {
    state.rows = await request(e.path);
    state.pagamentos = state.entity === "ordensservicos" ? await request("/api/pagamentos") : [];
  } catch (err) {
    state.rows = [];
    setStatus("Não foi possível carregar: " + err.message);
  }
  render();
  updateHero();
}

async function updateHero() {
  try {
    const [c, o] = await Promise.all([request("/api/clientes"), request("/api/ordensservicos")]);
    $("cntClientes").textContent = c.length;
    $("cntOrdens").textContent = o.filter((x) => x.status === "ABERTA").length;
    $("cntAndamento").textContent = o.filter((x) => ["EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "EM_REPARO"].includes(x.status)).length;
  } catch (err) { /* mantém os traços */ }
}

function selectEntity(key) {
  state.entity = key;
  const e = ENTITIES[key];
  document.querySelectorAll(".nav-item").forEach((b) => b.classList.toggle("active", b.dataset.entity === key));
  $("entityTitle").textContent = e.title;
  $("entityDescription").textContent = e.desc;
  $("searchInput").value = "";
  load();
}

async function fieldHtml(f, values) {
  const id = "f_" + f.k, req = f.required ? " required" : "", v = values[f.k] ?? "";
  const label = (inner) => `<label>${esc(f.label)}${inner}${f.hint ? `<small>${esc(f.hint)}</small>` : ""}</label>`;
  if (f.ref) {
    const list = (await request(ENTITIES[f.ref].path)).filter(f.filter || (() => true));
    if (!list.length) return label(`<select id="${id}" disabled><option>${esc(f.empty)}</option></select>`);
    const cur = v && v.id;
    return label(`<select id="${id}"${req}><option value="">Selecione</option>` +
      list.map((r) => `<option value="${r.id}"${r.id === cur ? " selected" : ""}>${esc(nome[f.ref](r))}</option>`).join("") + "</select>");
  }
  if (f.type === "select") {
    return label(`<select id="${id}"${req}><option value="">Selecione</option>` +
      f.options.map((o) => `<option value="${esc(o[0])}"${o[0] === v ? " selected" : ""}>${esc(o[1])}</option>`).join("") + "</select>");
  }
  if (f.type === "textarea") return label(`<textarea id="${id}" rows="3"${req}>${esc(v)}</textarea>`);
  return label(`<input id="${id}" type="${f.type || "text"}" value="${esc(v)}"${f.step ? ` step="${f.step}"` : ""}${f.type === "number" ? ' min="0"' : ""}${req}>`);
}

async function openDialog(title, fields, values, submit) {
  $("dialogTitle").textContent = title;
  $("formError").textContent = "";
  state.dlg = { fields, submit };
  try {
    $("formFields").innerHTML = (await Promise.all(fields.map((f) => fieldHtml(f, values)))).join("");
  } catch (err) {
    toast("Não foi possível carregar as opções: " + err.message);
    return;
  }
  $("recordDialog").showModal();
}

function openForm(row) {
  const e = ENTITIES[state.entity];
  const mode = row ? "edit" : "create";
  openDialog((row ? "Editar " : "Novo cadastro em ") + e.title.toLowerCase(),
    e.fields.filter((f) => !f.only || f.only === mode), row || {},
    (body) => (row ? request(`${e.path}/${row.id}`, "PUT", body) : request(e.path, "POST", body)));
}

function mudarStatus(r) {
  openDialog("Mudar status da OS #" + r.id, [{ k: "status", label: "Novo status (atual: " + LABEL[r.status] + ")", type: "select", required: true,
    options: NEXT[r.status].map((s) => [s, LABEL[s]]), hint: "Entregar só é permitido com o valor total confirmado em pagamentos." }], {},
    (body) => request(`/api/ordensservicos/${r.id}/status`, "PUT", body));
}

function confirmar(r) {
  openDialog("Confirmar pagamento de " + brl.format(r.valor), [{ k: "referencia", label: "Comprovante (ID da transação ou nº do recibo)", required: true,
    hint: "Confira o valor no extrato, na maquininha ou no caixa antes de confirmar." }], {},
    (body) => request(`/api/pagamentos/${r.id}/confirmar`, "PUT", body));
}

async function estornar(r) {
  if (!confirm("Estornar o pagamento de " + brl.format(r.valor) + "?")) return;
  try { await request(`/api/pagamentos/${r.id}/estornar`, "PUT"); toast("Pagamento estornado."); load(); }
  catch (err) { setStatus(err.message); }
}

async function remove(r) {
  if (!confirm("Excluir o registro " + r.id + "? Essa ação não pode ser desfeita.")) return;
  try { await request(`${ENTITIES[state.entity].path}/${r.id}`, "DELETE"); toast("Registro excluído."); load(); }
  catch (err) { setStatus(err.message); }
}

async function save(ev) {
  ev.preventDefault();
  const body = {};
  for (const f of state.dlg.fields) {
    const raw = $("f_" + f.k).value.trim();
    if (f.ref) { if (raw) body[f.k] = { id: Number(raw) }; }
    else if (f.type === "number") body[f.k] = raw === "" ? 0 : Number(raw);
    else body[f.k] = raw;
  }
  try {
    await state.dlg.submit(body);
    $("recordDialog").close();
    toast("Salvo.");
    load();
  } catch (err) {
    $("formError").textContent = err.message;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  $("entityNav").addEventListener("click", (ev) => { const b = ev.target.closest(".nav-item"); if (b) selectEntity(b.dataset.entity); });
  $("tableBody").addEventListener("click", (ev) => {
    const b = ev.target.closest("button[data-a]");
    if (!b) return;
    const row = state.rows.find((r) => r.id === Number(b.dataset.id));
    if (row) ENTITIES[state.entity].actions(row)[Number(b.dataset.a)].run();
  });
  $("addButton").addEventListener("click", () => openForm(null));
  $("refreshButton").addEventListener("click", load);
  $("searchInput").addEventListener("input", render);
  $("recordForm").addEventListener("submit", save);
  $("closeDialog").addEventListener("click", () => $("recordDialog").close());
  $("cancelButton").addEventListener("click", () => $("recordDialog").close());
  $("menuToggle").addEventListener("click", () => {
    const open = $("mainNav").classList.toggle("open");
    $("menuToggle").setAttribute("aria-expanded", open);
  });
  $("mainNav").addEventListener("click", () => { $("mainNav").classList.remove("open"); $("menuToggle").setAttribute("aria-expanded", "false"); });
  selectEntity("clientes");
});
