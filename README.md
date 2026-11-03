# Marketplace de Doações

Plataforma web que conecta doadores a instituições de caridade de Porto Alegre e região metropolitana. O doador **busca e filtra** as instituições pela causa que quer ajudar e doa **dinheiro via Pix** ou **itens**. As instituições se cadastram sozinhas, passam por aprovação da administração, divulgam suas necessidades com data limite e confirmam o recebimento das doações.

Projeto da Atividade Extensionista II, UNINTER (CST em Análise e Desenvolvimento de Sistemas).

## Funcionalidades

**Doador**
- Cadastro e login.
- Lista de instituições aprovadas, com as necessidades abertas de cada uma ("Precisam agora").
- **Busca e filtros** na página inicial: texto livre (nome, cidade, descrição, o que a instituição precisa), causa ("Quero ajudar"), tipo de item ("Tenho para doar") e "só instituições que estão precisando de algo agora". A busca ignora acentos e maiúsculas.
- Doação em dinheiro: o site gera o **QR Code Pix e o Pix Copia e Cola** com o valor já preenchido.
- Doação de item: **quantidade de unidades**, telefone, forma de entrega (levar ou pedir que busquem), endereço de retirada, data prevista e, opcionalmente, a necessidade da instituição que está sendo atendida. Quando o item não é de uma necessidade, o site mostra o que a instituição costuma receber, só como orientação (nada é bloqueado).
- Histórico em "Minhas doações", com acompanhamento do status e botão para pagar o Pix de novo.
- **Pix rápido**: doação em dinheiro sem cadastro, direto na página inicial (nada é gravado, só mostra o QR Code).

**Instituição**
- Cadastro próprio (CNPJ, endereço, descrição e chave Pix) com conta de acesso. Uma conta administra uma instituição.
- **Áreas de atuação** (crianças, idosos, saúde, proteção animal e outras) e **itens que costuma receber** (roupas, alimentos, fraldas e outros), ambos com campo "Outros" de texto livre. É isso que o doador usa para encontrar a instituição.
- Só aparece no site depois de **aprovada** pela administração.
- Painel para editar dados, ver o QR Code Pix como o doador vê, publicar **necessidades com data limite** e acompanhar as doações recebidas.
- Confirma ou cancela cada doação (quem confirma é a instituição, porque o Pix vai direto para a conta dela).
- Em doações de item, vê quantidade, telefone do doador (com link de WhatsApp), forma de entrega, endereço e data.

**Administrador**
- Aprova, rejeita ou suspende instituições.
- Botão **Ver QR Pix** para conferir a chave antes de aprovar (escaneando no app do banco).
- Vê as categorias de cada instituição, com os textos livres ("Outra área" e "Outros itens") destacados em amarelo, porque eles aparecem publicamente e precisam de conferência.
- Visualiza e gerencia todas as doações.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem / build | Java 17, Maven |
| Back-end | Spring Boot 3.3.4 (Web, Data JPA, Validation, Security) |
| Autenticação | JWT (jjwt 0.12.6), senhas com BCrypt |
| Banco de dados | H2 em memória (padrão); driver do PostgreSQL já incluído |
| Front-end | HTML, JavaScript puro e Bootstrap 5.3.3 (via CDN), servidos pelo próprio Spring Boot |
| QR Code | qrcodejs 1.0.0 (via CDN) |
| Testes | JUnit 5, Mockito e AssertJ (back-end); `node:test` (site) |

O site precisa de internet no navegador para carregar o Bootstrap e a biblioteca de QR Code.

## Como rodar

**Requisitos:** JDK 17 ou superior e Maven (ou uma IDE com suporte a Maven, como IntelliJ, Eclipse ou VS Code).

```bash
mvn spring-boot:run
```

Ou, pela IDE, execute a classe `MarketplaceDoacoesApplication`.

Depois acesse **http://localhost:8080**.

> O banco H2 é **em memória**: todos os dados são apagados quando a aplicação é reiniciada, e o administrador é recriado a cada execução.

### Acesso de administrador

O administrador inicial é criado automaticamente:

| Email | Senha |
|---|---|
| `admin@marketplace.com` | `admin123` |

Essa senha padrão serve **somente para desenvolvimento**. Em qualquer ambiente real, defina a variável de ambiente `ADMIN_SENHA`.

### Configurações

| Variável de ambiente | Propriedade | Padrão | Descrição |
|---|---|---|---|
| `ADMIN_SENHA` | `app.admin.senha` | `admin123` | Senha do administrador inicial |
| (nenhuma) | `app.admin.email` | `admin@marketplace.com` | Email do administrador inicial |
| `JWT_SECRET` | `app.jwt.secret` | vazio | Segredo do JWT (mínimo 32 caracteres). Se vazio, uma chave aleatória é gerada a cada execução e os tokens deixam de valer ao reiniciar |
| (nenhuma) | `app.jwt.expiracao-minutos` | `60` | Validade do token |

### Console do banco (somente desenvolvimento)

http://localhost:8080/h2-console, com JDBC URL `jdbc:h2:mem:marketplacedb`, usuário `sa` e senha vazia.

Para usar PostgreSQL, comente o bloco do H2 no `application.properties` e descomente o bloco do PostgreSQL (ajustando URL, usuário e senha). Em produção, remova também o console do H2 e a sua liberação em `SecurityConfig`.

## Testes

```bash
mvn test                              # testes do back-end (Java)
node --test src/test/js/api.test.js   # testes do site (opcional, precisa do Node 18+)
```

**Back-end (JUnit 5 + Mockito + AssertJ).** Os testes dos controllers não sobem o Spring: criam o controller com repositórios simulados (Mockito), então rodam em segundos. Cobrem as regras de negócio:
- `TelefonesTest`: formatos aceitos e recusados de telefone brasileiro, incluindo o DDD 55 que não pode ser confundido com o código do país.
- `DoacaoControllerTest`: doação em dinheiro e de item (quantidade, telefone, forma de entrega, datas e prazos), necessidade aberta, vencida, atendida ou de outra instituição, quem enxerga cada doação e quem pode confirmar.
- `InstituicaoControllerTest`: cadastro (CNPJ, chave Pix, categorias obrigatórias e opcionais), regra de voltar para análise ao trocar nome ou chave Pix, aprovação, rejeição e consultas públicas.
- `NecessidadeControllerTest`: publicação, limites de data, prorrogação, remoção bloqueada com doações ligadas e indicador de expirada.

**Site (`node:test`, sem instalar nada).** `src/test/js/api.test.js` carrega o `js/api.js` e confere o Pix Copia e Cola (campos e CRC16), a busca sem acento, as etiquetas de categoria, as datas e os formatos. Ele também lê os enums `AreaAtuacao` e `TipoItem` do Java e garante que os códigos do site são idênticos aos do back-end.

O que **não** é coberto por teste automático: login e JWT, o filtro de segurança, o tratamento global de erros, a consulta de necessidades públicas no banco e o fluxo completo entre as telas. Isso é verificado manualmente, passando pelos três perfis (doador, instituição e administrador).

## Perfis de acesso

| Perfil | Como é criado | O que pode fazer |
|---|---|---|
| `USER` (doador) | Cadastro público na tela de login | Doar, ver as próprias doações, editar o próprio perfil |
| `INSTITUICAO` | Cadastro de instituição (precisa de aprovação) | Gerenciar a própria instituição e necessidades, confirmar doações recebidas |
| `ADMIN` | Criado na inicialização | Aprovar instituições, ver e gerenciar todas as doações e usuários |

Nenhum cadastro público aceita escolher o perfil: quem se cadastra como doador sempre nasce `USER`, e quem cadastra instituição sempre nasce `INSTITUICAO`.

## Como funciona o Pix

O sistema **não processa pagamentos** e não se integra a bancos ou gateways. Ele usa a chave Pix informada pela instituição para montar o **Pix Copia e Cola** (padrão BR Code do Banco Central, com CRC16) e desenhar o QR Code no navegador. O dinheiro vai direto para a conta da instituição.

Consequências:
- A doação em dinheiro é registrada como **PENDENTE** e só vira **CONFIRMADA** quando a instituição clica em Confirmar.
- O valor no QR Code é uma conveniência: o doador pode alterá-lo no app do banco, por isso a instituição confere o valor recebido antes de confirmar.
- O **Pix rápido** (sem cadastro) não grava nada. A instituição só o vê no extrato do banco.

## Como funciona a busca

A página inicial carrega as instituições aprovadas e as necessidades abertas e filtra tudo **no navegador**, sem nova chamada ao servidor a cada tecla. A busca por texto olha nome, cidade, endereço, descrição, rótulos das áreas e dos itens aceitos, os textos livres ("Outros") e o item e os detalhes das necessidades abertas. Todos os termos digitados precisam aparecer (por exemplo, `criancas roupas`). Os filtros de causa, tipo de item e "só com necessidades" se combinam entre si e com o texto.

As categorias são listas fixas (enums `AreaAtuacao` e `TipoItem`), compartilhadas entre o back-end e o site. Os códigos do back-end e as constantes do `js/api.js` precisam ser idênticos, e o teste do site avisa se alguém mudar só um dos lados.

## Regras de negócio

- Só instituições **aprovadas** aparecem no site e recebem doações.
- Se uma instituição aprovada altera o **nome** ou a **chave Pix**, ela volta para análise. Isso evita a troca de chave por uma de golpista depois da aprovação. Alterar áreas de atuação e itens aceitos **não** exige nova análise.
- A chave Pix é validada no cadastro: CPF/CNPJ (só números), telefone com +55, email ou chave aleatória.
- A instituição precisa informar **pelo menos uma área de atuação** (ou preencher "Outra área"), porque é isso que alimenta o filtro do doador. Os itens aceitos são opcionais (há instituições que só recebem dinheiro). Os textos livres têm até 100 caracteres.
- Os itens aceitos são **orientação**, não restrição: o doador pode oferecer qualquer item, e a instituição decide se aceita.
- Doação em dinheiro exige valor maior que zero.
- Doação de item exige descrição, **quantidade de 1 a 10.000 unidades**, telefone brasileiro válido (DDD + número), forma de entrega e data prevista. Se o doador pedir que busquem, o endereço de retirada é obrigatório.
- A quantidade doada não é limitada pela quantidade da necessidade: a instituição pediu 20, o doador pode dar 3.
- A data prevista não pode estar no passado e vai, no máximo, até 90 dias à frente. Se a doação atender a uma necessidade, não pode passar do prazo dela.
- Necessidades têm data limite de até 365 dias. Depois do prazo elas saem da lista pública, e a instituição pode prorrogar editando a data. Uma necessidade que já recebeu doações não pode ser removida, só marcada como atendida.
- Cada usuário vê apenas as próprias doações; a instituição vê as doações destinadas a ela; o administrador vê todas.

## Privacidade

Telefone e endereço de retirada são coletados **apenas em doações de item**, para combinar a entrega. Eles só aparecem para o próprio doador, para a instituição que recebe a doação e para o administrador, nunca em listas públicas. Senhas são guardadas com hash BCrypt, e o Pix é sempre feito diretamente entre doador e instituição.

## API

Todas as rotas começam com `/api`. Rotas protegidas exigem o cabeçalho `Authorization: Bearer <token>`.

```bash
# 1) Login: devolve o token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@marketplace.com","senha":"admin123"}'

# 2) Usar o token
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/doacoes
```

### Autenticação e usuários

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/auth/login` | Público | Login; devolve `token`, `tipo`, `expiraEmSegundos` e `role` |
| POST | `/usuarios` | Público | Cadastro de doador |
| GET | `/usuarios/me` | Logado | Perfil do usuário logado |
| PUT | `/usuarios/me` | Logado | Atualiza o nome |
| PUT | `/usuarios/me/senha` | Logado | Troca a senha (exige a senha atual) |
| GET, PUT, DELETE | `/usuarios`, `/usuarios/{id}` | ADMIN | Administração de usuários |

### Instituições

Além de nome, CNPJ, endereço, cidade, descrição e chave Pix, a instituição carrega `areasAtuacao` (lista de códigos), `outraArea`, `tiposAceitos` (lista de códigos) e `outroTipoAceito`.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/instituicoes` | Público | Lista as instituições aprovadas, com as categorias |
| GET | `/instituicoes/{id}` | Público | Detalhe de uma instituição aprovada |
| POST | `/instituicoes/cadastro` | Público | Cadastra instituição e a conta de acesso (fica pendente) |
| GET | `/instituicoes/minha` | INSTITUICAO | Dados da própria instituição |
| PUT | `/instituicoes/minha` | INSTITUICAO | Atualiza os dados (nome ou Pix alterados voltam para análise; categorias não) |
| GET | `/instituicoes/admin` | ADMIN | Lista todas, com status e responsável |
| PATCH | `/instituicoes/{id}/status?status=APROVADA` | ADMIN | Aprova, rejeita (`REJEITADA`) ou devolve para análise (`PENDENTE`) |

Códigos de `areasAtuacao`: `CRIANCAS_E_ADOLESCENTES`, `IDOSOS`, `PESSOAS_COM_DEFICIENCIA`, `MULHERES_E_FAMILIAS`, `SITUACAO_DE_RUA`, `ALIMENTACAO`, `SAUDE`, `EDUCACAO`, `ANIMAIS`, `MEIO_AMBIENTE`.

Códigos de `tiposAceitos`: `ROUPAS_E_CALCADOS`, `COBERTORES_E_ROUPA_DE_CAMA`, `ALIMENTOS`, `HIGIENE_E_LIMPEZA`, `FRALDAS`, `BRINQUEDOS_E_LIVROS`, `MATERIAL_ESCOLAR`, `MOVEIS_E_ELETRODOMESTICOS`, `RACAO_E_PRODUTOS_PET`.

### Necessidades

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/necessidades` | Público | Necessidades abertas e no prazo de instituições aprovadas (filtro opcional `?instituicaoId=`) |
| GET | `/necessidades/minhas` | INSTITUICAO | Todas as da própria instituição, com indicador de expirada |
| POST | `/necessidades` | INSTITUICAO | Publica (só instituição aprovada) |
| PUT | `/necessidades/{id}` | INSTITUICAO | Edita, inclusive para prorrogar a data |
| PATCH | `/necessidades/{id}/status?status=ATENDIDA` | INSTITUICAO | Marca como atendida (`ABERTA` reabre) |
| DELETE | `/necessidades/{id}` | INSTITUICAO | Remove (bloqueado se já houver doações ligadas) |

### Doações

Em doações de item, o corpo inclui `quantidade`, `telefoneDoador`, `formaEntrega`, `enderecoRetirada`, `dataPrevista` e, opcionalmente, `necessidadeId`.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/doacoes` | Logado | Doador: as próprias. Instituição: as recebidas. Admin: todas (filtros `usuarioId`, `instituicaoId`) |
| GET | `/doacoes/{id}` | Doador, instituição destinatária ou ADMIN | Detalhe |
| POST | `/doacoes` | USER ou ADMIN | Registra uma doação (o doador é sempre o usuário logado) |
| PATCH | `/doacoes/{id}/status?status=CONFIRMADA` | Instituição destinatária ou ADMIN | Confirma ou cancela (`CANCELADA`) |
| DELETE | `/doacoes/{id}` | ADMIN | Remove |

### Formato de erro

Todos os erros seguem o mesmo formato JSON:

```json
{
  "timestamp": "2026-10-27T20:41:10.123",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos",
  "campos": { "email": "Email inválido" }
}
```

O campo `campos` só aparece em erros de validação. Códigos usados: 400 (dados inválidos ou regra de negócio), 401 (sem login, token inválido ou credenciais erradas), 403 (sem permissão), 404 (não encontrado), 409 (conflito, como CNPJ ou email já cadastrado).

## Estrutura do projeto

```
src/main/java/br/com/marketplacedoacoes/marketplacedoacoes/
├── MarketplaceDoacoesApplication.java
├── controllers/   Auth, Usuario, Instituicao, Necessidade e Doacao
├── dto/           Objetos de entrada e saída da API (records)
├── exception/     Exceções de negócio e GlobalExceptionHandler
├── model/         Entidades JPA (Usuario, Instituicao, Necessidade, Doacao, Role)
│                  e enums de categoria (AreaAtuacao, TipoItem)
├── repository/    Repositórios Spring Data JPA
├── security/      SecurityConfig, JwtService, JwtAuthFilter, UserDetailsServiceImpl, AdminInitializer
└── util/          Telefones (validação de telefone brasileiro)

src/main/resources/
├── application.properties
└── static/        Site: index (busca e filtros), login, cadastro-instituicao, doar,
                   minhas-doacoes, painel-instituicao, admin e js/api.js
                   (chamadas à API, sessão, categorias, busca e geração do Pix)

src/test/
├── java/.../controllers/   Testes das regras de Doacao, Instituicao e Necessidade
├── java/.../util/          Teste de Telefones
└── js/api.test.js          Testes do Pix, da busca e das categorias do site
```

## Decisões de projeto

- **Sem camada de Service:** os controllers falam direto com os repositórios, para caber no prazo. As regras de negócio estão nos controllers e são candidatas a ir para uma camada própria. Isso não atrapalha os testes, que criam o controller com repositórios simulados.
- **DTOs em vez de entidades na API:** a entidade nunca é exposta; senha e relacionamentos não vazam e não há recursão no JSON.
- **JWT stateless:** o token guarda só o email. O perfil é consultado no banco a cada requisição, então remover um usuário ou mudar o perfil vale na hora.
- **Aprovação de instituições:** é a defesa contra chaves Pix falsas, já que a plataforma exibe a chave para os doadores.
- **Categorias como orientação:** áreas e itens aceitos ajudam o doador a escolher, mas nunca bloqueiam uma doação. O campo "Outros" garante que nenhuma instituição fique sem categoria, e o administrador confere esse texto livre antes de aprovar.
- **Busca no navegador:** para o volume esperado (dezenas de instituições), filtrar no cliente é mais simples e mais rápido que uma rota de busca.

## Limitações conhecidas

- Banco em memória: os dados não persistem entre execuções.
- Não há automação do Pix: a confirmação é manual, feita pela instituição.
- Não há contagem automática do que já foi doado de uma necessidade (por exemplo, "faltam 30 de 50 cobertores"); a instituição marca como atendida.
- A busca carrega todas as instituições aprovadas de uma vez; com milhares de instituições seria preciso paginar e buscar no servidor.
- Alterações no texto livre ("Outros") de uma instituição já aprovada não passam por nova análise.
- Não há recuperação de senha nem envio de email.
- Tokens JWT não podem ser revogados antes de expirar (mitigado pela consulta ao banco a cada requisição).
- Os testes automáticos cobrem as regras de negócio e a lógica do site, mas não há testes de integração (API com banco e segurança) nem testes de interface.

## Próximos passos

- Mostrar nas necessidades quanto já foi doado e quanto falta, somando as doações confirmadas.
- Testes de integração: login e JWT, filtro de segurança, tratamento de erros e consultas ao banco.
- Camada de Service.
- Migração para PostgreSQL com versionamento de banco (Flyway).
- Refresh token e recuperação de senha.