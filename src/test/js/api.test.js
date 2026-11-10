
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const RAIZ = path.join(__dirname, '..', '..', 'main');
const CODIGO = fs.readFileSync(path.join(RAIZ, 'resources', 'static', 'js', 'api.js'), 'utf8');
const PASTA_MODEL = path.join(RAIZ, 'java', 'br', 'com', 'marketplacedoacoes', 'marketplacedoacoes', 'model');

const contexto = vm.createContext({
    console,
    localStorage: { getItem: () => null, setItem() {}, removeItem() {} },
    document: {
        createElement() {
            return {
                set textContent(valor) {
                    this.html = String(valor).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                },
                get innerHTML() { return this.html; }
            };
        }
    },
    window: { location: {} }
});
vm.runInContext(CODIGO, contexto);

function js(expressao) {
    return vm.runInContext(expressao, contexto);
}

function jsLista(expressao) {
    return JSON.parse(js(`JSON.stringify(${expressao})`));
}

function lerCampos(texto) {
    const campos = {};
    let i = 0;
    while (i < texto.length) {
        const id = texto.substr(i, 2);
        const tamanho = Number(texto.substr(i + 2, 2));
        campos[id] = texto.substr(i + 4, tamanho);
        i += 4 + tamanho;
    }
    return campos;
}

test('CRC16 bate com o valor de referência do padrão CCITT-FALSE', () => {
    assert.equal(js('crc16("123456789")'), '29B1');
});

test('Pix Copia e Cola: estrutura, chave e CRC corretos', () => {
    const codigo = js('gerarPixCopiaECola({ chave: "ong@teste.com", nome: "Casa Esperança", cidade: "Porto Alegre", valor: 25.5 })');

    const corpo = codigo.slice(0, -4);
    const crc = codigo.slice(-4);
    assert.equal(crc, js(`crc16(${JSON.stringify(corpo)})`));
    assert.ok(corpo.endsWith('6304'));

    const campos = lerCampos(corpo.slice(0, -4));
    assert.equal(campos['00'], '01');
    assert.equal(campos['52'], '0000');
    assert.equal(campos['53'], '986');
    assert.equal(campos['54'], '25.50');
    assert.equal(campos['58'], 'BR');
    assert.equal(campos['59'], 'CASA ESPERANCA');
    assert.equal(campos['60'], 'PORTO ALEGRE');

    const conta = lerCampos(campos['26']);
    assert.equal(conta['00'], 'br.gov.bcb.pix');
    assert.equal(conta['01'], 'ong@teste.com');
});

test('Pix sem valor não tem o campo de valor', () => {
    const codigo = js('gerarPixCopiaECola({ chave: "12345678909", nome: "Casa", cidade: "Porto Alegre" })');
    const campos = lerCampos(codigo.slice(0, -8));
    assert.equal(campos['54'], undefined);
});

test('Pix limita nome a 25 e cidade a 15 caracteres, sem acento e em maiúsculas', () => {
    const codigo = js('gerarPixCopiaECola({ chave: "12345678909", nome: "Associação Coração São João Batista de Porto Alegre", cidade: "São Leopoldo do Sul Grande" })');
    const campos = lerCampos(codigo.slice(0, -8));
    assert.equal(campos['59'], 'ASSOCIACAO CORACAO SAO JO');
    assert.equal(campos['59'].length, 25);
    assert.ok(campos['60'].length <= 15);
    assert.equal(campos['60'], 'SAO LEOPOLDO DO');
});

test('semAcentos remove acentos e símbolos', () => {
    assert.equal(js('semAcentos("São Paulo - Zona/Sul!")'), 'SAO PAULO ZONA SUL');
});

test('normalizarBusca ignora acento e maiúsculas', () => {
    assert.equal(js('normalizarBusca("CRIANÇAS Educação")'), 'criancas educacao');
    assert.equal(js('normalizarBusca(null)'), '');
});

function codigosDoEnum(arquivo) {
    const fonte = fs.readFileSync(path.join(PASTA_MODEL, arquivo), 'utf8');
    const miolo = fonte.slice(fonte.indexOf('{') + 1, fonte.lastIndexOf('}'));
    return miolo.split(',').map(s => s.trim()).filter(Boolean);
}

test('códigos de AREAS_ATUACAO são idênticos ao enum AreaAtuacao do back-end', () => {
    assert.deepEqual(jsLista('AREAS_ATUACAO.map(c => c.codigo)'), codigosDoEnum('AreaAtuacao.java'));
});

test('códigos de TIPOS_ITEM são idênticos ao enum TipoItem do back-end', () => {
    assert.deepEqual(jsLista('TIPOS_ITEM.map(c => c.codigo)'), codigosDoEnum('TipoItem.java'));
});

test('todo código tem rótulo legível', () => {
    assert.equal(js('rotulo(AREAS_ATUACAO, "IDOSOS")'), 'Idosos');
    assert.equal(js('rotulo(TIPOS_ITEM, "FRALDAS")'), 'Fraldas');
    assert.equal(js('rotulo(AREAS_ATUACAO, "NAO_EXISTE")'), 'NAO_EXISTE');
});

test('etiquetas mostra as categorias e o texto livre, escapando HTML', () => {
    const html = js('etiquetas(["IDOSOS"], AREAS_ATUACAO, "<b>Outra</b>", "text-bg-secondary")');
    assert.match(html, /Idosos/);
    assert.match(html, /&lt;b&gt;Outra&lt;\/b&gt;/);
    assert.doesNotMatch(html, /<b>/);
    assert.doesNotMatch(html, /text-bg-warning/);
});

test('etiquetas destaca o texto livre em amarelo só quando pedido (tela do admin)', () => {
    const html = js('etiquetas(["IDOSOS"], AREAS_ATUACAO, "Cuidados paliativos", "text-bg-secondary", true)');
    assert.equal((html.match(/text-bg-warning/g) || []).length, 1);
});

test('etiquetas sem nada devolve texto vazio', () => {
    assert.equal(js('etiquetas([], AREAS_ATUACAO, null, "x")'), '');
});

test('somarDias atravessa mês e ano', () => {
    assert.equal(js('somarDias("2026-10-31", 1)'), '2026-11-01');
    assert.equal(js('somarDias("2026-12-31", 1)'), '2027-01-01');
    assert.equal(js('somarDias("2026-03-01", -1)'), '2026-02-28');
});

test('textoPrazo descreve o prazo em dias', () => {
    assert.equal(js('textoPrazo(hojeISO())'), 'termina hoje');
    assert.equal(js('textoPrazo(somarDias(hojeISO(), 1))'), 'falta 1 dia');
    assert.equal(js('textoPrazo(somarDias(hojeISO(), 5))'), 'faltam 5 dias');
    assert.equal(js('textoPrazo(somarDias(hojeISO(), -1))'), 'expirada');
});

test('formatarCnpj e formatarTelefone', () => {
    assert.equal(js('formatarCnpj("11222333000181")'), '11.222.333/0001-81');
    assert.equal(js('formatarTelefone("51999998888")'), '(51) 99999-8888');
    assert.equal(js('formatarTelefone("5133334444")'), '(51) 3333-4444');
});

test('formatarDinheiro usa o padrão brasileiro', () => {
    const texto = js('formatarDinheiro(1234.5)').replace(/\s/g, ' ');
    assert.equal(texto, 'R$ 1.234,50');
});

test('montarMensagem usa os campos do erro da API', () => {
    assert.equal(js('montarMensagem({ campos: { a: "Erro A", b: "Erro B" } }, 400)'), 'Erro A | Erro B');
    assert.equal(js('montarMensagem({ mensagem: "Não encontrado" }, 404)'), 'Não encontrado');
    assert.equal(js('montarMensagem(null, 500)'), 'Erro 500');
});