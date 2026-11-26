-- AUTO RENT - esquema do banco de dados (SQLite)
-- As tabelas são criadas automaticamente pela classe Conexao na primeira execução;
-- este arquivo existe para documentação e para consultas diretas (ex.: DB Browser for SQLite).

CREATE TABLE veiculo (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    modelo    TEXT    NOT NULL,
    placa     TEXT    NOT NULL UNIQUE,
    ano       INTEGER NOT NULL,
    categoria TEXT    NOT NULL,            -- Hatch, Sedan, SUV, Picape, Utilitário
    diaria    REAL    NOT NULL,
    status    TEXT    NOT NULL             -- Disponível, Alugado, Manutenção
);

CREATE TABLE cliente (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    nome          TEXT NOT NULL,
    cpf           TEXT NOT NULL UNIQUE,
    telefone      TEXT NOT NULL,
    email         TEXT,
    data_cadastro TEXT NOT NULL            -- aaaa-mm-dd
);

CREATE TABLE aluguel (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id     INTEGER NOT NULL REFERENCES cliente(id),
    veiculo_id     INTEGER NOT NULL REFERENCES veiculo(id),
    data_retirada  TEXT NOT NULL,          -- aaaa-mm-dd
    data_prevista  TEXT NOT NULL,
    data_devolucao TEXT,                   -- nulo enquanto em andamento
    diaria         REAL NOT NULL,          -- diária congelada na abertura do contrato
    multa          REAL NOT NULL DEFAULT 0,
    total          REAL NOT NULL,          -- previsto (em andamento) ou final (concluído)
    status         TEXT NOT NULL           -- Em andamento, Concluído, Cancelado
);

CREATE TABLE movimentacao (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    data       TEXT NOT NULL,
    descricao  TEXT NOT NULL,
    categoria  TEXT NOT NULL,              -- Locação, Manutenção, Combustível, Seguro, Impostos, Outros
    valor      REAL NOT NULL,
    tipo       TEXT NOT NULL,              -- Entrada, Saída
    aluguel_id INTEGER REFERENCES aluguel(id)   -- preenchido nas receitas geradas pela devolução
);
