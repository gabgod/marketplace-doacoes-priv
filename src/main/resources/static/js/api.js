const API = '/api';

// ---------- Sessão (token guardado no navegador) ----------

function getToken() {
    return localStorage.getItem('token');
}

function getRole() {
    return localStorage.getItem('role');
}

function estaLogado() {
    return !!getToken();
}

function salvarSessao(dados) {
    localStorage.setItem('token', dados.token);
    localStorage.setItem('role', dados.role);
}

function limparSessao() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
}

function sair() {
    limparSessao();
    window.location.href = 'index.html';
}

// Páginas que exigem login chamam isso no começo
function exigirLogin() {
    if (!estaLogado()) {
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

// ---------- Chamadas à API ----------

async function api(caminho, opcoes = {}) {
    const headers = { 'Content-Type': 'application/json', ...(opcoes.headers || {}) };
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    const resposta = await fetch(API + caminho, { ...opcoes, headers });

    // Token expirado ou inválido: volta pro login
    if (resposta.status === 401 && token) {
        limparSessao();
        window.location.href = 'login.html';
        throw new Error('Sessão expirada. Faça login novamente.');
    }

    if (resposta.status === 204) {
        return null;
    }

    const corpo = await resposta.json().catch(() => null);

    if (!resposta.ok) {
        throw new Error(montarMensagem(corpo, resposta.status));
    }
    return corpo;
}

// Usa o formato de erro do GlobalExceptionHandler (mensagem e campos)
function montarMensagem(corpo, status) {
    if (corpo && corpo.campos) {
        return Object.values(corpo.campos).join(' | ');
    }
    if (corpo && corpo.mensagem) {
        return corpo.mensagem;
    }
    return 'Erro ' + status;
}

// ---------- Utilidades de tela ----------

// Escapa texto vindo do usuário antes de colocar em innerHTML (evita XSS)
function esc(texto) {
    const div = document.createElement('div');
    div.textContent = texto ?? '';
    return div.innerHTML;
}

function mostrarAlerta(mensagem, tipo = 'danger') {
    const area = document.getElementById('alerta');
    if (!area) return;
    area.innerHTML = mensagem
        ? `<div class="alert alert-${tipo}">${esc(mensagem)}</div>`
        : '';
}

function formatarDinheiro(valor) {
    return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatarData(iso) {
    return new Date(iso).toLocaleString('pt-BR');
}

function formatarCnpj(cnpj) {
    return String(cnpj ?? '').replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
}

function formatarTelefone(telefone) {
    const d = String(telefone ?? '').replace(/\D/g, '');
    if (d.length === 11) return d.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3');
    if (d.length === 10) return d.replace(/^(\d{2})(\d{4})(\d{4})$/, '($1) $2-$3');
    return d;
}

// ---------- Datas (sem hora, no formato AAAA-MM-DD) ----------
// Lê a data como data local, para o fuso horário não "andar um dia"

function parseData(iso) {
    const [a, m, d] = iso.split('-').map(Number);
    return new Date(a, m - 1, d);
}

function paraISO(data) {
    return data.getFullYear() + '-'
        + String(data.getMonth() + 1).padStart(2, '0') + '-'
        + String(data.getDate()).padStart(2, '0');
}

function hojeISO() {
    return paraISO(new Date());
}

function somarDias(iso, dias) {
    const d = parseData(iso);
    d.setDate(d.getDate() + dias);
    return paraISO(d);
}

function formatarDataCurta(iso) {
    return iso ? parseData(iso).toLocaleDateString('pt-BR') : '';
}

function diasAte(iso) {
    return Math.round((parseData(iso) - parseData(hojeISO())) / 86400000);
}

function textoPrazo(iso) {
    const n = diasAte(iso);
    if (n < 0) return 'expirada';
    if (n === 0) return 'termina hoje';
    if (n === 1) return 'falta 1 dia';
    return 'faltam ' + n + ' dias';
}

// ---------- Categorias das instituições ----------
// Os códigos precisam ser idênticos aos enums AreaAtuacao e TipoItem do back-end

const AREAS_ATUACAO = [
    { codigo: 'CRIANCAS_E_ADOLESCENTES', rotulo: 'Crianças e adolescentes' },
    { codigo: 'IDOSOS', rotulo: 'Idosos' },
    { codigo: 'PESSOAS_COM_DEFICIENCIA', rotulo: 'Pessoas com deficiência' },
    { codigo: 'MULHERES_E_FAMILIAS', rotulo: 'Mulheres e famílias' },
    { codigo: 'SITUACAO_DE_RUA', rotulo: 'Pessoas em situação de rua' },
    { codigo: 'ALIMENTACAO', rotulo: 'Alimentação e combate à fome' },
    { codigo: 'SAUDE', rotulo: 'Saúde' },
    { codigo: 'EDUCACAO', rotulo: 'Educação e cultura' },
    { codigo: 'ANIMAIS', rotulo: 'Proteção animal' },
    { codigo: 'MEIO_AMBIENTE', rotulo: 'Meio ambiente' }
];

const TIPOS_ITEM = [
    { codigo: 'ROUPAS_E_CALCADOS', rotulo: 'Roupas e calçados' },
    { codigo: 'COBERTORES_E_ROUPA_DE_CAMA', rotulo: 'Cobertores e roupa de cama e banho' },
    { codigo: 'ALIMENTOS', rotulo: 'Alimentos não perecíveis' },
    { codigo: 'HIGIENE_E_LIMPEZA', rotulo: 'Higiene e limpeza' },
    { codigo: 'FRALDAS', rotulo: 'Fraldas' },
    { codigo: 'BRINQUEDOS_E_LIVROS', rotulo: 'Brinquedos e livros' },
    { codigo: 'MATERIAL_ESCOLAR', rotulo: 'Material escolar' },
    { codigo: 'MOVEIS_E_ELETRODOMESTICOS', rotulo: 'Móveis e eletrodomésticos' },
    { codigo: 'RACAO_E_PRODUTOS_PET', rotulo: 'Ração e produtos para animais' }
];

function rotulo(lista, codigo) {
    const categoria = lista.find(c => c.codigo === codigo);
    return categoria ? categoria.rotulo : codigo;
}

// Minúsculas e sem acento, para a busca achar "criancas" e "CRIANÇAS"
function normalizarBusca(texto) {
    return String(texto ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

// Desenha uma lista de checkboxes dentro de um elemento (que deve ter a classe "row")
function montarCheckboxes(container, lista, prefixo, selecionados) {
    const marcados = selecionados || [];
    container.innerHTML = lista.map(c => `
        <div class="col-sm-6">
            <div class="form-check">
                <input class="form-check-input" type="checkbox" id="${prefixo}-${c.codigo}" value="${c.codigo}"
                       ${marcados.includes(c.codigo) ? 'checked' : ''}>
                <label class="form-check-label" for="${prefixo}-${c.codigo}">${esc(c.rotulo)}</label>
            </div>
        </div>`).join('');
}

function lerCheckboxes(container) {
    return Array.from(container.querySelectorAll('input[type="checkbox"]:checked')).map(i => i.value);
}

// Etiquetas (badges) das categorias; o texto livre ("Outros") entra no fim.
// destacarOutro pinta o texto livre de amarelo (usado na tela do admin, que confere esse texto).
function etiquetas(codigos, lista, outro, classe, destacarOutro = false) {
    const html = (codigos || []).map(c =>
        `<span class="badge ${classe} fw-normal">${esc(rotulo(lista, c))}</span>`);
    if (outro) {
        const classeOutro = destacarOutro ? 'text-bg-warning' : classe;
        html.push(`<span class="badge ${classeOutro} fw-normal">${esc(outro)}</span>`);
    }
    return html.join(' ');
}

// ---------- Pix ----------
// Gera o "Pix Copia e Cola" (padrão BR Code do Banco Central). O QR Code é esse mesmo texto desenhado.
// Não há integração com banco: o dinheiro vai direto para a chave da instituição.

// Nome e cidade do Pix: maiúsculas, sem acento e sem símbolos
function semAcentos(texto) {
    return String(texto ?? '')
        .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
        .replace(/[^A-Za-z0-9 ]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim()
        .toUpperCase();
}

// Cada campo do Pix é: id (2 dígitos) + tamanho (2 dígitos) + valor
function campoPix(id, valor) {
    return id + String(valor.length).padStart(2, '0') + valor;
}

// CRC16-CCITT (polinômio 0x1021, valor inicial 0xFFFF) exigido no final do código
function crc16(texto) {
    let crc = 0xFFFF;
    for (let i = 0; i < texto.length; i++) {
        crc ^= texto.charCodeAt(i) << 8;
        for (let j = 0; j < 8; j++) {
            crc = (crc & 0x8000) ? ((crc << 1) ^ 0x1021) : (crc << 1);
            crc &= 0xFFFF;
        }
    }
    return crc.toString(16).toUpperCase().padStart(4, '0');
}

function gerarPixCopiaECola({ chave, nome, cidade, valor }) {
    const conta = campoPix('00', 'br.gov.bcb.pix') + campoPix('01', chave);
    const payload =
        campoPix('00', '01') +
        campoPix('26', conta) +
        campoPix('52', '0000') +
        campoPix('53', '986') +
        (valor ? campoPix('54', Number(valor).toFixed(2)) : '') +
        campoPix('58', 'BR') +
        campoPix('59', semAcentos(nome).substring(0, 25) || 'INSTITUICAO') +
        campoPix('60', semAcentos(cidade).substring(0, 15) || 'BRASIL') +
        campoPix('62', campoPix('05', '***')) +
        '6304';
    return payload + crc16(payload);
}

async function copiarCodigo(textarea, rotulo) {
    try {
        await navigator.clipboard.writeText(textarea.value);
    } catch (e) {
        // Contextos sem permissão de área de transferência: seleciona e copia
        textarea.select();
        document.execCommand('copy');
    }
    rotulo.textContent = 'Copiado!';
    setTimeout(() => { rotulo.textContent = ''; }, 2500);
}

// Preenche um elemento com o QR Code e o Copia e Cola
// dados: { chave, nome, cidade, valor (opcional) }
function montarPix(container, dados) {
    const codigo = gerarPixCopiaECola(dados);

    container.innerHTML = `
        <div class="text-center">
            <div class="pix-qr d-flex justify-content-center my-3"></div>
            <label class="form-label small text-muted">Pix Copia e Cola</label>
            <textarea class="form-control form-control-sm pix-codigo" rows="4" readonly></textarea>
            <div class="mt-2">
                <button type="button" class="btn btn-primary btn-sm pix-copiar">Copiar código</button>
                <span class="small text-success ms-2 pix-copiado"></span>
            </div>
        </div>`;

    const area = container.querySelector('.pix-codigo');
    const rotuloCopiado = container.querySelector('.pix-copiado');
    const alvoQr = container.querySelector('.pix-qr');
    area.value = codigo;

    if (typeof QRCode === 'undefined') {
        alvoQr.innerHTML = '<div class="alert alert-warning small mb-0">Não foi possível carregar o gerador de QR Code. Use o código Copia e Cola abaixo.</div>';
    } else {
        new QRCode(alvoQr, {
            text: codigo,
            width: 220,
            height: 220,
            correctLevel: QRCode.CorrectLevel.M
        });
    }

    container.querySelector('.pix-copiar').addEventListener('click', () => copiarCodigo(area, rotuloCopiado));
}

// Resumo (instituição, chave e valor) mostrado acima do QR Code
function resumoPix(instituicao, valor) {
    return `
        <div class="fw-semibold">${esc(instituicao.nome)}</div>
        <div class="small text-muted">Chave Pix: <code>${esc(instituicao.chavePix)}</code></div>
        ${valor ? `<div class="fs-4 mt-1">${formatarDinheiro(valor)}</div>` : ''}`;
}

// Abre uma janela (modal) com o QR Code. O modal é criado na primeira chamada.
function abrirModalPix(titulo, resumoHtml, dados) {
    let modal = document.getElementById('modal-pix');
    if (!modal) {
        document.body.insertAdjacentHTML('beforeend', `
            <div class="modal fade" id="modal-pix" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="modal-pix-titulo"></h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
                        </div>
                        <div class="modal-body">
                            <div class="text-center" id="modal-pix-resumo"></div>
                            <div id="modal-pix-conteudo"></div>
                        </div>
                    </div>
                </div>
            </div>`);
        modal = document.getElementById('modal-pix');
    }

    document.getElementById('modal-pix-titulo').textContent = titulo;
    document.getElementById('modal-pix-resumo').innerHTML = resumoHtml;
    montarPix(document.getElementById('modal-pix-conteudo'), dados);
    bootstrap.Modal.getOrCreateInstance(modal).show();
}

// Pix sem cadastro: só mostra o QR Code (nada é gravado). O valor é opcional.
function abrirPixRapido(instituicao) {
    const dados = { chave: instituicao.chavePix, nome: instituicao.nome, cidade: instituicao.cidade };

    const resumo = resumoPix(instituicao) + `
        <div class="my-3 text-start">
            <label class="form-label small" for="pix-rapido-valor">Valor (opcional)</label>
            <input type="number" class="form-control" id="pix-rapido-valor" min="0.01" step="0.01"
                   placeholder="Ou digite o valor no app do banco">
        </div>
        <div class="alert alert-secondary small text-start">
            Doação sem cadastro: ela não aparece em &quot;Minhas doações&quot; e a instituição não recebe aviso pelo site.
            O comprovante fica no seu banco. Para acompanhar o status, entre na sua conta e doe por lá.
        </div>`;

    abrirModalPix('Doar via Pix', resumo, dados);

    const campo = document.getElementById('pix-rapido-valor');
    campo.addEventListener('input', () => {
        const valor = Number(campo.value);
        montarPix(document.getElementById('modal-pix-conteudo'), { ...dados, valor: valor > 0 ? valor : undefined });
    });
}

// ---------- Menu ----------

// Monta o menu do topo conforme o perfil do usuário
function renderNavbar() {
    const itens = ['<li class="nav-item"><a class="nav-link" href="index.html">Instituições</a></li>'];

    if (estaLogado()) {
        if (getRole() === 'INSTITUICAO') {
            itens.push('<li class="nav-item"><a class="nav-link" href="painel-instituicao.html">Painel da instituição</a></li>');
        } else {
            itens.push('<li class="nav-item"><a class="nav-link" href="doar.html">Doar</a></li>');
            itens.push('<li class="nav-item"><a class="nav-link" href="minhas-doacoes.html">Minhas doações</a></li>');
            if (getRole() === 'ADMIN') {
                itens.push('<li class="nav-item"><a class="nav-link" href="admin.html">Admin</a></li>');
            }
        }
        itens.push('<li class="nav-item"><button class="btn btn-outline-light btn-sm ms-lg-2" onclick="sair()">Sair</button></li>');
    } else {
        itens.push('<li class="nav-item"><a class="nav-link" href="login.html">Entrar / Cadastrar</a></li>');
        itens.push('<li class="nav-item"><a class="nav-link" href="cadastro-instituicao.html">Sou uma instituição</a></li>');
    }

    document.getElementById('navbar').innerHTML = `
        <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
            <div class="container">
                <a class="navbar-brand" href="index.html">Marketplace de Doações</a>
                <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#menu">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="menu">
                    <ul class="navbar-nav ms-auto align-items-lg-center">
                        ${itens.join('')}
                    </ul>
                </div>
            </div>
        </nav>`;
}