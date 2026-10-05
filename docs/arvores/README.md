# Árvores de Talentos — Índice e Regras Globais

Documentos de design das árvores do **Vanilla+ Talents**. Nenhum código foi implementado a partir daqui; os arquivos servem de base para os JSON em `data/vanillatalents/skills/`.

| Arquivo | Árvore | `treeCategory` | Prefixo de ID | Nós | PT total | Níveis de XP |
|---|---|---|---|---|---|---|
| [00_comum.md](00_comum.md) | Comum (Sobrevivência) | `common` | `common_` | 12 | 37 | 185 |
| [01_minerador.md](01_minerador.md) | Minerador | `miner` | `miner_` | 12 | 36 | 180 |
| [02_produtor.md](02_produtor.md) | Produtor/Fazendeiro | `farmer` | `farmer_` | 12 | 35 | 175 |
| [03_desbravador.md](03_desbravador.md) | Desbravador | `explorer` | `explorer_` | 12 | 34 | 170 |
| [04_guerreiro.md](04_guerreiro.md) | Guerreiro | `warrior` | `warrior_` | 12 | 35 | 175 |
| [05_arqueiro.md](05_arqueiro.md) | Arqueiro | `archer` | `archer_` | 12 | 37 | 185 |

> O prefixo `common_` é obrigatório na Árvore Comum: o `PlayerSkillData.resetClass` preserva apenas nós que começam com `common_`.

## Convenções usadas em todos os arquivos

- **Estrutura:** 1 nó raiz → 3 ramos temáticos → 1 capstone que exige os três ramos. Tudo é 100% completável (GDD §2); os ramos definem ordem de progressão, não exclusão.
- **Regra dos 50%:** o requisito mínimo é `ceil(maxLevel / 2)` do nó anterior → max 1→1, max 2→1, max 3→2, max 4→2, max 5→3. Alguns nós exigem mais que o mínimo de propósito (ex.: `miner_fortune` exige `miner_haste` 5, herdado do plano de MVP).
- **Pré-requisitos múltiplos:** capstones listam vários nós; **todos** precisam ser atendidos (E lógico).
- **Valores:** todos os números são propostas iniciais para playtest, não valores finais.
- **Capstones:** sempre passivos ou condicionais (sem tecla extra, sem habilidades ativas).
- **Empilhamento:** salvo indicação contrária, bônus percentuais do mod são aplicados **depois** dos encantamentos vanilla e de forma **multiplicativa** com eles, para evitar que somas cheguem a 100%.

## Custo global

- Comum completa + 1 classe completa ≈ **71–74 PT ≈ 355–370 níveis de XP**.
- Respec (GDD §4): devolve 25% dos PT da classe. Trocar uma classe completa (~36 PT) devolve ~9 PT, ou seja, perde ~135 níveis.

## ⚠️ Risco transversal: custo de "5 níveis" não é constante

A curva de XP vanilla é crescente. Custo em **pontos de XP** de 5 níveis:

| Faixa | Pontos de XP |
|---|---|
| 0 → 5 | 55 |
| 10 → 15 | 155 |
| 25 → 30 | 485 |
| 30 → 35 | 650 |

Consequências:
1. O jogador ótimo converte **toda vez** que chega ao nível 5. Quem acumula até o nível 30 paga ~9–12× mais por PT — isso pune o comportamento natural e vira "imposto sobre quem não leu a wiki".
2. Quem guarda níveis para encantamento/bigorna compete diretamente com o mod, o que é intencional, mas a diferença de custo deixa a escolha opaca.

**Alternativas para decidir (o GDD não foi alterado):**
- **A — Custo fixo em pontos de XP** (ex.: 1 PT = 100 pontos de XP, convertidos de baixo para cima na barra). Previsível e justo; a UI mostra "Custo: 100 XP".
- **B — Manter 5 níveis, mas sempre dos níveis mais baixos** (equivale a cobrar 55 pontos). Mantém a linguagem do GDD, mas fica barato demais: 74 PT ≈ 4.070 pontos, aproximadamente o mesmo que subir do nível 0 ao 45 uma única vez.
- **C — Manter como está** e aceitar a otimização "converter a cada 5 níveis" como parte do jogo.

Recomendação: **A**, com o valor calibrado no playtest.
