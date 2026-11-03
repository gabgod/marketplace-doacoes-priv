# Marketplace de Doações

Projeto da Atividade Extensionista (UNINTER, CST ADS): marketplace de doações a ONGs de Porto Alegre.

## Stack
- Java 17, Spring Boot 3.3.4, Maven
- Spring Web, Spring Data JPA, Validation, Lombok, DevTools, Spring Security
- Banco: H2 em memória por enquanto (console em /h2-console); PostgreSQL depois (driver já está no pom)

## Estrutura (pacote base: br.com.marketplacedoacoes.marketplacedoacoes)
- model: Usuario, Instituicao, Doacao (enums TipoDoacao e StatusDoacao aninhados), Role (USER, ADMIN)
- repository: UsuarioRepository, InstituicaoRepository, DoacaoRepository (Spring Data JPA)
- dto: records (InstituicaoDTO, UsuarioDTO, UsuarioCadastroDTO, DoacaoDTO, ErroResposta)
- controllers: um controller REST por entidade, em /api/usuarios, /api/instituicoes, /api/doacoes
- exception: exceções próprias + GlobalExceptionHandler (@RestControllerAdvice) com resposta JSON padronizada
- security: SecurityConfig, UserDetailsServiceImpl, AdminInitializer

## Decisões já tomadas
- Controllers chamam os repositories direto; ainda não existe camada Service
- Entidade nunca é exposta na API, só DTO; conversão manual com toDTO()
- Autenticação: HTTP Basic stateless (login = email), senha com BCrypt
- Perfis: USER (cadastro público) e ADMIN (criado pelo AdminInitializer; nenhum DTO aceita role)
- Doação: o doador é sempre o usuário logado (usuarioId do corpo é ignorado); só ADMIN muda status e remove
- Tabela da instituição se chama "instituicao" (a entidade já se chamou Ong)
- Erros: lançar exceção própria (RecursoNaoEncontrada, Conflito, RegraDeNegocio), nunca devolver só o status vazio
- AccessDeniedException tem handler próprio no GlobalExceptionHandler (senão o catch-all devolve 500)
- Regras de segurança usam antMatcher(...), não strings soltas (o console do H2 quebra requestMatchers(String) no Spring Security 6)

## Próximos passos possíveis
- JWT com endpoint de login, ou camada Service
- Endpoint /api/usuarios/me para o usuário ver e editar o próprio perfil
- Perfil de representante de instituição para confirmar doações
- Trocar H2 por PostgreSQL e remover a liberação do H2 console da SecurityConfig

## Como trabalhar comigo
- Prefiro digitar o código eu mesmo: mostre o código e explique, sem alterar arquivos a menos que eu peça
- Respostas em português