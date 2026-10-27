# Aula 13 — Login e proteção da API com JWT

## 1. Visão geral

Nesta aula vamos acrescentar autenticação à API do projeto `suporteos2025`.
Ao final, uma pessoa somente poderá acessar os recursos de grupos e produtos
depois de informar usuário e senha e receber um JSON Web Token (JWT).

O resultado esperado é este:

1. `POST /api/auth/login` recebe as credenciais;
2. o Spring Security consulta o usuário no PostgreSQL;
3. o BCrypt compara a senha informada com o hash armazenado;
4. a aplicação emite um JWT assinado e com prazo de validade;
5. o cliente envia esse token no cabeçalho das próximas requisições;
6. o Spring Security valida o token antes de permitir o acesso à API.

> **Referência do projeto:** os códigos desta aula estão implementados no
> repositório `suporteos2025`. O estudante deve reproduzir a solução no tema
> escolhido para seu próprio projeto, adaptando os nomes de domínio sem mudar
> os princípios de segurança.

## 2. Objetivos de aprendizagem

Ao concluir a aula, o estudante deverá ser capaz de:

- distinguir autenticação de autorização;
- explicar a diferença entre senha, hash e token;
- descrever a estrutura e o ciclo de vida de um JWT;
- configurar uma cadeia de filtros do Spring Security;
- autenticar usuários armazenados no banco de dados;
- armazenar hashes BCrypt, nunca senhas em texto puro;
- emitir e validar tokens assinados com HMAC SHA-256;
- proteger endpoints de uma API REST sem criar sessão no servidor;
- identificar o significado dos códigos HTTP `401` e `403`;
- testar o fluxo completo no Postman e com testes automatizados.

## 3. Pré-requisitos

Antes de iniciar, confirme que as aulas anteriores estão funcionando:

- Java 17 ou superior;
- PostgreSQL em execução;
- Liquibase aplicando os changelogs;
- arquivo `.env` fora do Git;
- operações CRUD de grupos e produtos funcionando;
- Docker e Docker Compose disponíveis, caso a execução seja feita em
  containers.

Execute os testes de referência:

```bash
./mvnw test
```

## 4. Fundamentação teórica

### 4.1 Autenticação e autorização

Os conceitos são relacionados, mas respondem a perguntas diferentes:

| Conceito | Pergunta | Exemplo |
|---|---|---|
| Autenticação | Quem é você? | Conferir usuário e senha |
| Autorização | O que você pode fazer? | Somente `ADMIN` pode excluir um registro |

Nesta aula implementaremos a autenticação e já transportaremos o papel do
usuário no token. Todas as rotas de negócio exigirão um usuário autenticado.
As rotas de leitura e alteração aceitarão qualquer usuário autenticado, mas as
operações `DELETE` exigirão o papel `ADMIN`.

### 4.2 Por que não armazenar a senha original?

Uma senha não deve ser criptografada de forma reversível nem armazenada em
texto puro. O banco guarda o resultado de uma função de derivação de senha.
Nesta aplicação usaremos BCrypt.

O BCrypt é propositalmente custoso. Isso reduz a quantidade de tentativas que
um atacante consegue realizar caso obtenha os hashes do banco. Cada execução
também utiliza um `salt`, por isso duas senhas iguais podem produzir hashes
diferentes.

Exemplo conceitual:

```text
senha informada  -> BCrypt -> hash armazenado
senha no login   -> BCrypt compara com o hash -> verdadeiro ou falso
```

O sistema não precisa — e não deve — descobrir a senha original a partir do
hash.

### 4.3 O que é um JWT?

JWT é um formato compacto para transportar declarações, chamadas de *claims*.
Um token normalmente possui três partes separadas por ponto:

```text
HEADER.PAYLOAD.SIGNATURE
```

- **header:** informa o tipo do token e o algoritmo de assinatura;
- **payload:** contém claims como usuário, emissor, permissões e expiração;
- **signature:** permite verificar se o conteúdo foi alterado.

Um payload didático desta aula será semelhante a:

```json
{
  "iss": "suporteos2025",
  "sub": "admin",
  "iat": 1793131200,
  "exp": 1793134800,
  "authorities": ["ROLE_ADMIN"]
}
```

`sub` identifica o usuário, `iat` indica o instante de emissão, `exp` informa
quando o token expira e `iss` identifica quem o emitiu.

> **Atenção:** codificação Base64 não é criptografia. Header e payload podem
> ser lidos pelo cliente. Nunca coloque senha, chave secreta, CPF ou outro dado
> sensível no JWT.

### 4.4 Assinatura simétrica com HS256

Usaremos o algoritmo HS256, no qual a mesma chave secreta assina e valida o
token. Essa escolha mantém a aula simples e é adequada para uma única
aplicação que emite e valida seus próprios tokens.

Consequência importante: quem possui a chave consegue gerar tokens válidos.
Portanto, `JWT_SECRET` deve:

- ser aleatória e ter pelo menos 32 bytes;
- existir apenas no ambiente de execução;
- nunca ser enviada ao Git;
- ser diferente em desenvolvimento, teste e produção;
- ser trocada imediatamente caso seja exposta.

Em arquiteturas com vários serviços, assinaturas assimétricas como RS256 ou
ES256 permitem separar a chave privada de emissão da chave pública de
validação. Esse cenário ficará fora do escopo desta aula.

### 4.5 API stateless

Configuraremos `SessionCreationPolicy.STATELESS`. Isso significa que o servidor
não manterá uma sessão HTTP para lembrar quem fez login. Cada requisição deve
trazer sua própria prova de autenticação:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Essa estratégia facilita a escalabilidade horizontal, mas também traz uma
limitação: um token já emitido continua válido até sua expiração. Logout,
revogação, refresh token e rotação de chaves exigem decisões adicionais e não
serão implementados nesta introdução.

### 4.6 Fluxo completo

```text
Cliente                 API                 Banco
  |                      |                    |
  | POST /auth/login     |                    |
  | usuario + senha      |                    |
  |--------------------->| consulta usuario   |
  |                      |------------------->|
  |                      |<-------------------|
  |                      | compara BCrypt     |
  |<---------------------| emite JWT assinado |
  |                      |                    |
  | GET /api/produto     |                    |
  | Authorization:Bearer|                    |
  |--------------------->| valida JWT         |
  |<---------------------| resposta protegida |
```

## 5. Ponto de parada 1 — Dependências de segurança

Acrescente ao `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Responsabilidades:

- `starter-security`: autenticação, autorização, filtros e BCrypt;
- `starter-oauth2-resource-server`: validação de Bearer Tokens JWT;
- `spring-security-test`: usuários simulados e ferramentas para testes.

O Spring Security oferece `JwtEncoder` e `JwtDecoder`. Por isso não precisamos
adicionar outra biblioteca para criar tokens.

Valide a compilação:

```bash
./mvnw clean compile
```

**Sugestão de commit:**

```bash
git add pom.xml
git commit -m "build: adiciona dependencias do Spring Security"
```

## 6. Ponto de parada 2 — Usuário e versionamento do banco

### 6.1 Enum de papel

Crie `src/main/java/com/curso/domains/enums/Papel.java`:

```java
package com.curso.domains.enums;

public enum Papel {
    ADMIN,
    USER
}
```

### 6.2 Entidade de usuário

Crie `src/main/java/com/curso/domains/Usuario.java`:

```java
@Entity
@Table(name = "usuario")
@SequenceGenerator(
        name = "seq_usuario",
        sequenceName = "seq_usuario",
        allocationSize = 1
)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_usuario")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 100)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Column(nullable = false)
    private Boolean ativo;

    // construtores, getters e setters
}
```

O campo `senha` armazenará o hash BCrypt. O limite de 100 caracteres deixa
espaço para o hash atual e futuras mudanças de estratégia.

### 6.3 Diagrama de classes do domínio

O diagrama a seguir representa as classes persistentes do sistema depois da
implementação desta aula. `Usuario` e `Papel` são os novos elementos. As classes
`GrupoProduto`, `Produto` e `Status` já pertenciam ao domínio do projeto.

```mermaid
classDiagram
    direction LR

    class GrupoProduto {
        <<Entity>>
        -Integer id
        -String descricao
        -Status status
        -List~Produto~ produtos
        +addProduto(Produto produto) void
        +removeProduto(Produto produto) void
    }

    class Produto {
        <<Entity>>
        -Long idProduto
        -String codigoBarra
        -String descricao
        -BigDecimal saldoEstoque
        -BigDecimal valorUnitario
        -BigDecimal valorEstoque
        -LocalDate dataCadastro
        -GrupoProduto grupoProduto
        -Status status
        -recalcValorEstoque() void
    }

    class Usuario {
        <<Entity>>
        -Long id
        -String username
        -String senha
        -Papel papel
        -Boolean ativo
    }

    class Status {
        <<enumeration>>
        INATIVO
        ATIVO
        -Integer id
        -String descricao
        +toEnum(Integer id) Status
    }

    class Papel {
        <<enumeration>>
        ADMIN
        USER
    }

    GrupoProduto "1" o-- "0..*" Produto : agrupa
    GrupoProduto --> Status : possui
    Produto --> Status : possui
    Usuario --> Papel : recebe
```

#### Como interpretar o diagrama

- `GrupoProduto` possui uma associação bidirecional de um para muitos com
  `Produto`. Um grupo pode não possuir produtos ou pode agrupar vários deles;
  cada produto pertence obrigatoriamente a um grupo.
- `GrupoProduto` e `Produto` utilizam `Status`. No banco, esse enum é convertido
  para um número pelo `StatusConverter`.
- `Usuario` utiliza o enum `Papel`, persistido como texto por meio de
  `EnumType.STRING`. Assim, o banco armazena `ADMIN` ou `USER`.
- `Usuario` não possui associação com `Produto` ou `GrupoProduto`. Sua
  responsabilidade é representar a identidade autenticável, enquanto as
  demais entidades representam o catálogo da aplicação.
- O atributo `senha` contém o **hash BCrypt**, apesar de manter o nome simples
  para fins didáticos. A senha original nunca deve ser persistida.
- O método `recalcValorEstoque()` é executado antes de inserir ou atualizar um
  produto e mantém `valorEstoque` coerente com quantidade e valor unitário.

O diagrama representa o modelo de domínio, não as tabelas físicas. As
sequências, constraints, nomes de colunas e tipos SQL são definidos pelo
Liquibase e podem ser consultados nos changelogs.

### 6.4 Changelog incremental

Não altere o changelog já aplicado da Aula 11. Crie
`db/changelog/changes/002-criar-usuario.xml` com dois changesets:

```xml
<changeSet id="004-criar-sequencia-usuario" author="curso">
    <createSequence sequenceName="seq_usuario" startValue="1" incrementBy="1"/>

    <rollback>
        <dropSequence sequenceName="seq_usuario"/>
    </rollback>
</changeSet>

<changeSet id="005-criar-tabela-usuario" author="curso">
    <createTable tableName="usuario">
        <column name="id" type="BIGINT">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="username" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="senha" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="papel" type="VARCHAR(20)">
            <constraints nullable="false"/>
        </column>
        <column name="ativo" type="BOOLEAN" defaultValueBoolean="true">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <addUniqueConstraint
            tableName="usuario"
            columnNames="username"
            constraintName="uk_usuario_username"/>

    <rollback>
        <dropTable tableName="usuario"/>
    </rollback>
</changeSet>
```

Inclua o novo arquivo no final de `db.changelog-master.xml`:

```xml
<include file="changes/002-criar-usuario.xml"
         relativeToChangelogFile="true"/>
```

Essa ordem expressa a evolução temporal do banco. O Liquibase executará apenas
os changesets que ainda não aparecem em `DATABASECHANGELOG`.

### 6.5 Repositório

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
}
```

**Sugestão de commit:**

```bash
git add src/main/java/com/curso/domains \
        src/main/java/com/curso/repositories/UsuarioRepository.java \
        src/main/resources/db/changelog
git commit -m "feat: versiona persistencia de usuarios"
```

## 7. Ponto de parada 3 — Variáveis de ambiente

### 7.1 Remover perfil fixo

Remova esta linha de `application.properties`:

```properties
spring.profiles.active=dev
```

O perfil deve ser uma decisão do ambiente, não uma decisão gravada no código.
No IntelliJ, configure `SPRING_PROFILES_ACTIVE=dev` nas variáveis da execução.

### 7.2 Configurações do perfil dev

Acrescente a `application-dev.properties`:

```properties
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration=${JWT_EXPIRATION:60m}
app.jwt.issuer=${JWT_ISSUER:suporteos2025}
app.bootstrap.admin.username=${APP_ADMIN_USERNAME:admin}
app.bootstrap.admin.password=${APP_ADMIN_PASSWORD}
```

### 7.3 Arquivo `.env`

Gere uma chave para uso local:

```bash
openssl rand -base64 32
```

Copie `.env.example` para `.env` e preencha:

```dotenv
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://localhost:5432/cursodb
DB_USERNAME=postgres
DB_PASSWORD=senha-do-postgres

JWT_SECRET=cole-aqui-a-chave-aleatoria-gerada
JWT_EXPIRATION=60m
JWT_ISSUER=suporteos2025

APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=defina-uma-senha-forte
```

O `.env` não é lido automaticamente pelo Spring Boot quando a aplicação é
executada diretamente no IntelliJ. Na configuração de execução:

1. abra **Run > Edit Configurations**;
2. selecione a aplicação `Suporteos2025Application`;
3. localize **Environment variables**;
4. informe as variáveis do bloco acima;
5. confirme com **Apply** e **OK**.

O plugin opcional **EnvFile** pode carregar o arquivo, mas a aula não depende
dele. No Docker Compose, o `.env` é lido para interpolar as variáveis do
`compose.yaml`.

> O usuário administrador é criado somente se ainda não existir. Alterar
> `APP_ADMIN_PASSWORD` depois da primeira inicialização não muda o hash que já
> está salvo no banco.

**Sugestão de commit:**

```bash
git add .env.example compose.yaml src/main/resources
git commit -m "config: adiciona variaveis de ambiente do JWT"
```

## 8. Ponto de parada 4 — Integração com o Spring Security

### 8.1 Carregar o usuário do banco

Implemente `UsuarioDetailsService`:

```java
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario nao encontrado"));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getSenha())
                .roles(usuario.getPapel().name())
                .disabled(!usuario.getAtivo())
                .build();
    }
}
```

O `DaoAuthenticationProvider`, configurado automaticamente pelo Spring,
combina esse `UserDetailsService` com o `PasswordEncoder` da aplicação.

### 8.2 Propriedades tipadas

Crie o record `JwtProperties` anotado com:

```java
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {
    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "app.jwt.secret deve possuir pelo menos 32 bytes");
        }
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException(
                    "app.jwt.expiration deve ser maior que zero");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException(
                    "app.jwt.issuer deve ser informado");
        }
    }
}
```

Essa validação faz a aplicação falhar cedo caso uma configuração de segurança
esteja ausente ou inválida.

### 8.3 Cadeia de filtros

Os trechos centrais de `SecurityConfig` são:

```java
http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
            .anyRequest().authenticated())
    .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)));
```

O login precisa ser público porque o cliente ainda não possui token. Exclusões
exigem `ROLE_ADMIN`; todas as demais rotas exigem autenticação.

O CSRF é desabilitado aqui porque a API é stateless e recebe o token no
cabeçalho `Authorization`, não em cookie de sessão enviado automaticamente
pelo navegador. Essa decisão deve ser reavaliada se a forma de transportar a
credencial mudar.

### 8.4 Encoder, decoder e BCrypt

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@Bean
public SecretKey jwtSecretKey(JwtProperties properties) {
    return new SecretKeySpec(
            properties.secret().getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
    );
}

@Bean
public JwtEncoder jwtEncoder(SecretKey secretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
}

@Bean
public JwtDecoder jwtDecoder(SecretKey secretKey, JwtProperties properties) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(properties.issuer()));
    return decoder;
}
```

O encoder cria a assinatura. O decoder verifica a assinatura e as claims
temporais, incluindo a expiração. O validador também rejeita tokens emitidos
por outro emissor.

**Sugestão de commit:**

```bash
git add src/main/java/com/curso/config \
        src/main/java/com/curso/services/UsuarioDetailsService.java
git commit -m "feat: configura autenticacao e validacao JWT"
```

## 9. Ponto de parada 5 — Login e emissão do token

### 9.1 DTOs

Use DTOs para definir o contrato da API:

```java
public record LoginRequest(
        @NotBlank(message = "O usuario deve ser informado") String username,
        @NotBlank(message = "A senha deve ser informada") String password
) { }

public record TokenResponse(
        String token,
        String tipo,
        long expiresIn
) { }
```

Não retorne a entidade `Usuario`: ela possui o hash da senha e é um modelo de
persistência, não o contrato público do endpoint.

### 9.2 Serviço de autenticação

O fluxo principal de `AuthService` é:

```java
Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
                request.username(), request.password())
);

Instant issuedAt = Instant.now();
Instant expiresAt = issuedAt.plus(jwtProperties.expiration());

List<String> authorities = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(jwtProperties.issuer())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .subject(authentication.getName())
        .claim("authorities", authorities)
        .build();

JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
        .type("JWT")
        .build();

String token = jwtEncoder
        .encode(JwtEncoderParameters.from(header, claims))
        .getTokenValue();
```

Observe a ordem: a aplicação só emite o token depois que o
`AuthenticationManager` valida as credenciais.

### 9.3 Controller

```java
@RestController
@RequestMapping("/api/auth")
public class AuthResource {

    private final AuthService authService;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
```

Também foi criado `GET /api/auth/me`, que devolve o nome e as permissões do
usuário representado pelo token. Esse endpoint ajuda a visualizar que, depois
da validação, o JWT é convertido em um objeto `Authentication`.

### 9.4 Respostas 401 e 403

- `401 Unauthorized`: a autenticação está ausente ou é inválida;
- `403 Forbidden`: o usuário está autenticado, mas não possui a permissão
  necessária.

`RestAuthenticationEntryPoint` e `RestAccessDeniedHandler` mantêm o formato de
erro JSON já adotado pelo projeto.

**Sugestão de commit:**

```bash
git add src/main/java/com/curso/domains/dtos \
        src/main/java/com/curso/resources/AuthResource.java \
        src/main/java/com/curso/resources/exceptions \
        src/main/java/com/curso/security \
        src/main/java/com/curso/services/AuthService.java
git commit -m "feat: implementa login e emissao de token JWT"
```

## 10. Ponto de parada 6 — Administrador inicial

Para possibilitar o primeiro login sem gravar uma senha no repositório,
`AdminInitializer` é ativado somente no perfil `dev`.

```java
@Configuration
@Profile("dev")
public class AdminInitializer {

    // dependencias e configuracoes recebidas pelo construtor

    @PostConstruct
    public void criarAdministradorInicial() {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            return;
        }

        Usuario admin = new Usuario(
                null,
                username,
                passwordEncoder.encode(password),
                Papel.ADMIN,
                true
        );
        usuarioRepository.save(admin);
    }
}
```

A senha existe em texto legível somente durante a inicialização. Antes de ser
persistida, ela passa pelo `PasswordEncoder`.

Nunca registre a senha ou o JWT completo nos logs.

**Sugestão de commit:**

```bash
git add src/main/java/com/curso/config/AdminInitializer.java
git commit -m "feat: cria administrador inicial com senha do ambiente"
```

## 11. Executar a aplicação

### 11.1 Pelo IntelliJ

Com as variáveis da seção 7 configuradas, execute
`Suporteos2025Application`. No primeiro início, observe:

- o Liquibase aplicando os changesets `004` e `005`;
- a tabela `usuario` sendo criada;
- o administrador inicial sendo inserido com hash BCrypt;
- a aplicação iniciando na porta 8080.

### 11.2 Pelo Docker Compose

```bash
docker compose up --build
```

O serviço `aplicacao` recebe as variáveis de JWT e de administrador definidas
no `.env`. Para acompanhar:

```bash
docker compose logs -f aplicacao
```

## 12. Testar manualmente com o Postman

### 12.1 Criar um ambiente

No Postman, crie um Environment chamado `Suporte OS - Local` com:

| Variável | Valor inicial |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `token` | deixe vazio |

### 12.2 Requisição de login

Crie `POST {{baseUrl}}/api/auth/login`.

Em **Body > raw > JSON**, informe:

```json
{
  "username": "admin",
  "password": "a-mesma-senha-de-APP_ADMIN_PASSWORD"
}
```

Resposta esperada:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiresIn": 3600
}
```

Na aba **Scripts > Post-response**, salve o token automaticamente:

```javascript
const resposta = pm.response.json();
pm.environment.set("token", resposta.token);
```

### 12.3 Requisição autenticada

Crie `GET {{baseUrl}}/api/produto`.

Em **Authorization**:

- Type: `Bearer Token`;
- Token: `{{token}}`.

O Postman enviará:

```http
Authorization: Bearer {{token}}
```

Repita o teste com `GET {{baseUrl}}/api/auth/me`.

### 12.4 Experimentos obrigatórios

Registre o resultado de cada experimento:

1. acesse `/api/produto` sem o cabeçalho: deve retornar `401`;
2. faça login com senha incorreta: deve retornar `401`;
3. altere um caractere do token: deve retornar `401`;
4. use o token correto: deve retornar `200`;
5. autentique um usuário com papel `USER` e tente executar um `DELETE`: deve
   retornar `403`;
6. defina temporariamente `JWT_EXPIRATION=10s`, gere um token, aguarde e tente
   usá-lo: deve retornar `401` após a expiração.

Esses testes demonstram que possuir um payload bem-formado não é suficiente:
a assinatura e a validade temporal também precisam ser aceitas.

## 13. Testes automatizados

Os testes de integração existentes agora usam:

```java
@ActiveProfiles("test")
@WithMockUser(username = "professor", roles = "ADMIN")
```

`@ActiveProfiles("test")` garante H2 e configurações isoladas. `@WithMockUser`
testa os controllers já autenticados sem exigir que cada teste de CRUD repita
o processo de login.

`AuthResourceIntegrationTest` cobre o fluxo real:

- emissão do JWT com credenciais válidas;
- rejeição da senha inválida;
- rejeição de acesso sem token;
- acesso a recurso protegido com o token emitido;
- bloqueio de uma exclusão quando o papel não é `ADMIN`;
- leitura do usuário autenticado em `/api/auth/me`.

Execute:

```bash
./mvnw test
```

Resultado de referência desta etapa:

```text
Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Sugestão de commit:**

```bash
git add src/test
git commit -m "test: cobre autenticacao e acesso com JWT"
```

## 14. Checklist de segurança

Antes de considerar a aula concluída, confirme:

- [ ] `.env` continua ignorado pelo Git;
- [ ] nenhuma senha está escrita em Java ou em arquivo versionado;
- [ ] `JWT_SECRET` possui pelo menos 32 bytes e não é um texto previsível;
- [ ] a coluna `senha` contém um hash BCrypt, não a senha original;
- [ ] o login é a única rota pública da aplicação;
- [ ] requisições sem token recebem `401`;
- [ ] tokens adulterados ou expirados recebem `401`;
- [ ] a aplicação não imprime senhas nem tokens completos em logs;
- [ ] produção utiliza HTTPS;
- [ ] as chaves de desenvolvimento, teste e produção são diferentes.

## 15. Atividade avaliativa

No projeto temático do estudante:

1. implemente usuário persistido e versionado pelo Liquibase;
2. implemente login JWT seguindo o fluxo desta aula;
3. proteja os endpoints de negócio;
4. crie pelo menos quatro testes de integração de segurança;
5. entregue uma coleção do Postman ou documentação equivalente;
6. escreva um parágrafo justificando por que JWT não deve conter dados
   sigilosos;
7. explique a diferença observada entre `401` e `403`.

## 16. Limites desta implementação

Esta é uma base didática, não um sistema completo de identidade. Uma evolução
para produção deverá avaliar:

- cadastro, alteração e recuperação de senha;
- política de complexidade e bloqueio por tentativas;
- refresh tokens e rotação;
- revogação de tokens;
- autenticação multifator;
- auditoria de eventos de segurança;
- CORS restrito ao frontend conhecido;
- rate limiting no endpoint de login;
- gerenciamento de chaves por serviço de segredos;
- provedor externo com OpenID Connect, quando apropriado.

## 17. Encerramento e tag

Depois de revisar todos os pontos e executar os testes:

```bash
git status
git add . ':!graphify-out'
git commit -m "feat: conclui aula 13 de login com JWT"
git tag -a aula-13-login-jwt -m "Aula 13 - Login e protecao da API com JWT"
git push origin main
git push origin aula-13-login-jwt
```

O diretório `graphify-out/`, se estiver presente apenas como ferramenta local
de análise, não deve ser incluído nesse commit.

## 18. Referências

- [Spring Security — Architecture](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html)
- [Spring Security — DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html)
- [Spring Security — Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [Spring Security — OAuth 2.0 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/)
- [Spring Security 6.5 — JWT](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html)
- [RFC 7519 — JSON Web Token](https://datatracker.ietf.org/doc/html/rfc7519)
