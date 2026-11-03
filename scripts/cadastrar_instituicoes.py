#!/usr/bin/env python3
"""
Cadastra 5 instituições de exemplo, cada uma com áreas de atuação diferentes,
e (opcionalmente) aprova todas usando a conta de administrador.

Uso:
    python3 scripts/cadastrar_instituicoes.py              # cadastra e aprova
    python3 scripts/cadastrar_instituicoes.py --sem-aprovar

Variáveis de ambiente (opcionais):
    API_URL      (padrão: http://localhost:8080)
    ADMIN_EMAIL  (padrão: admin@marketplace.com)
    ADMIN_SENHA  (padrão: admin123)

Usa só a biblioteca padrão do Python, não precisa instalar nada.
"""

import json
import os
import sys
import urllib.error
import urllib.request

API_URL = os.environ.get("API_URL", "http://localhost:8080")
ADMIN_EMAIL = os.environ.get("ADMIN_EMAIL", "admin@marketplace.com")
ADMIN_SENHA = os.environ.get("ADMIN_SENHA", "admin123")

SENHA_PADRAO = "senha123"

INSTITUICOES = [
    {
        "nome": "Lar Esperança Infantil",
        "cnpj": "11.222.333/0001-81",
        "endereco": "Rua das Flores, 120 - Centro",
        "cidade": "Porto Alegre",
        "descricao": "Acolhimento e apoio escolar para crianças e adolescentes em situação de vulnerabilidade.",
        "chavePix": "pix@laresperanca.org.br",
        "areasAtuacao": ["CRIANCAS_E_ADOLESCENTES", "EDUCACAO"],
        "tiposAceitos": ["BRINQUEDOS_E_LIVROS", "MATERIAL_ESCOLAR", "ROUPAS_E_CALCADOS"],
        "email": "contato@laresperanca.org.br",
        "senha": SENHA_PADRAO,
    },
    {
        "nome": "Casa de Repouso Vovó Feliz",
        "cnpj": "22.333.444/0001-05",
        "endereco": "Av. Ipiranga, 2500 - Partenon",
        "cidade": "Porto Alegre",
        "descricao": "Cuidado, convivência e atendimento de saúde para idosos.",
        "chavePix": "pix@vovofeliz.org.br",
        "areasAtuacao": ["IDOSOS", "SAUDE"],
        "tiposAceitos": ["FRALDAS", "HIGIENE_E_LIMPEZA", "COBERTORES_E_ROUPA_DE_CAMA"],
        "email": "contato@vovofeliz.org.br",
        "senha": SENHA_PADRAO,
    },
    {
        "nome": "Patas Amigas",
        "cnpj": "33.444.555/0001-30",
        "endereco": "Estrada do Lami, 800",
        "cidade": "Canoas",
        "descricao": "Resgate, tratamento e adoção responsável de cães e gatos abandonados.",
        "chavePix": "pix@patasamigas.org.br",
        "areasAtuacao": ["ANIMAIS", "MEIO_AMBIENTE"],
        "tiposAceitos": ["RACAO_E_PRODUTOS_PET", "COBERTORES_E_ROUPA_DE_CAMA"],
        "email": "contato@patasamigas.org.br",
        "senha": SENHA_PADRAO,
    },
    {
        "nome": "Mesa Solidária",
        "cnpj": "44.555.666/0001-48",
        "endereco": "Rua Voluntários da Pátria, 45 - Floresta",
        "cidade": "Novo Hamburgo",
        "descricao": "Distribuição de refeições e cestas básicas para pessoas em situação de rua.",
        "chavePix": "pix@mesasolidaria.org.br",
        "areasAtuacao": ["SITUACAO_DE_RUA", "ALIMENTACAO"],
        "tiposAceitos": ["ALIMENTOS", "ROUPAS_E_CALCADOS", "HIGIENE_E_LIMPEZA"],
        "email": "contato@mesasolidaria.org.br",
        "senha": SENHA_PADRAO,
    },
    {
        "nome": "Instituto Mulheres em Rede",
        "cnpj": "55.666.777/0001-60",
        "endereco": "Rua Marechal Floriano, 300",
        "cidade": "Caxias do Sul",
        "descricao": "Apoio a mulheres, mães solo e famílias, incluindo pessoas com deficiência.",
        "chavePix": "pix@mulheresemrede.org.br",
        "areasAtuacao": ["MULHERES_E_FAMILIAS", "PESSOAS_COM_DEFICIENCIA"],
        "tiposAceitos": ["FRALDAS", "MOVEIS_E_ELETRODOMESTICOS", "ALIMENTOS"],
        "email": "contato@mulheresemrede.org.br",
        "senha": SENHA_PADRAO,
    },
]


def requisicao(metodo, caminho, corpo=None, token=None):
    """Faz a chamada HTTP e devolve (status, json_da_resposta)."""
    dados = json.dumps(corpo).encode("utf-8") if corpo is not None else None
    req = urllib.request.Request(API_URL + caminho, data=dados, method=metodo)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as resp:
            texto = resp.read().decode("utf-8")
            return resp.status, json.loads(texto) if texto else None
    except urllib.error.HTTPError as e:
        texto = e.read().decode("utf-8")
        try:
            return e.code, json.loads(texto)
        except json.JSONDecodeError:
            return e.code, texto


def login_admin():
    status, resp = requisicao("POST", "/api/auth/login",
                              {"email": ADMIN_EMAIL, "senha": ADMIN_SENHA})
    if status != 200:
        print(f"Falha no login do admin ({status}): {resp}")
        return None
    return resp["token"]


def main():
    aprovar = "--sem-aprovar" not in sys.argv

    try:
        urllib.request.urlopen(API_URL + "/api/instituicoes")
    except urllib.error.URLError:
        print(f"Não foi possível conectar em {API_URL}. A aplicação está rodando?")
        sys.exit(1)

    ids = []
    for inst in INSTITUICOES:
        status, resp = requisicao("POST", "/api/instituicoes/cadastro", inst)
        if status == 201:
            ids.append(resp["id"])
            print(f"[OK]   {inst['nome']} (id {resp['id']}) - {', '.join(inst['areasAtuacao'])}")
        else:
            print(f"[ERRO] {inst['nome']} ({status}): {resp}")

    if aprovar and ids:
        token = login_admin()
        if token:
            for id_ in ids:
                status, resp = requisicao("PATCH", f"/api/instituicoes/{id_}/status?status=APROVADA",
                                          token=token)
                situacao = "aprovada" if status == 200 else f"erro {status}: {resp}"
                print(f"       instituição {id_}: {situacao}")

    print(f"\n{len(ids)} de {len(INSTITUICOES)} instituições cadastradas.")
    print(f"Login das instituições: email do cadastro + senha '{SENHA_PADRAO}'.")


if __name__ == "__main__":
    main()
