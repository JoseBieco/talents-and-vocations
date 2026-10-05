# Game Design Document (GDD) e Especificações Técnicas
**Projeto:** Mod de Talentos Vanilla+ para Minecraft (Forge)

---

## 1. Visão Geral do Projeto
O mod visa expandir o *late game* do Minecraft adicionando um uso contínuo para a Experiência (XP). Focado em manter a estética e a essência "Vanilla", ele introduz um sistema de Árvores de Talentos focado em qualidade de vida e especialização de gameplay, sem adição de magias complexas ou itens excessivamente *overpowered*.

---

## 2. Economia e Progressão
A progressão é baseada no investimento de tempo do jogador, sem um *level cap* artificial. O jogador poderá completar 100% da árvore comum e 100% da sua árvore de classe.

*   **Moeda do Mod (Pontos de Talento - PT):** O jogador "compra" 1 Ponto de Talento pagando exatos **5 Níveis de XP**.
*   **Estrutura dos Nós:** Cada nó na árvore representa uma habilidade/bônus.
    *   *Nós de Múltiplos Níveis:* Muitos nós podem receber mais de 1 ponto (ex: até 5 pontos em um nó de "Dano extra", concedendo +1 de dano a cada ponto investido).
*   **Pré-requisitos e Bloqueios:** As árvores são interligadas.
    *   Para desbloquear um Nó B (subsequente), o Nó A (anterior) precisa ter **pelo menos 50% do seu nível máximo preenchido**. Exemplo: Se o Nó A tem nível máximo 5, o jogador precisa investir pelo menos 3 pontos nele para poder começar a gastar pontos no Nó B.

---

## 3. Estrutura das Árvores
Existem duas vias de progressão paralelas. A **Árvore Comum**, sempre disponível a todos, e a **Árvore de Classe**, onde apenas uma pode estar ativa por vez.

### 3.1. Árvore Comum (Base de Sobrevivência)
Melhorias universais úteis para qualquer jogador, focadas na interação com o mundo padrão.
*   **Exemplos de Nós:** Aumento passivo de Vida Máxima, Resistência à fome (saturação dura mais), Fôlego extra debaixo d'água, Resistência a fogo/lava básica.

### 3.2. Árvores de Classe (Especializações)
O jogador escolhe **uma** das cinco especializações:

1.  **Minerador (Foco: Subterrâneo e Extração)**
    *   Quebra acelerada de pedras e blocos profundos (Deepslate).
    *   Chance percentual de drop duplo em minérios (combina com Fortuna).
    *   Visão noturna passiva sob a camada Y=0.
2.  **Produtor/Fazendeiro (Foco: Agricultura e Pecuária)**
    *   Colheita rápida em área (3x3).
    *   Aumento no drop de itens passivos de animais (couro, carne, lã).
    *   Crescimento levemente acelerado de plantações em um raio ao redor do jogador.
3.  **Desbravador (Foco: Exploração e Mobilidade)**
    *   Bônus de velocidade de movimento passivo.
    *   Redução severa do dano de queda.
    *   Redução drástica do gasto de fome ao correr e pular.
4.  **Guerreiro (Foco: Combate Corpo a Corpo)**
    *   Aumento de dano direto com espadas e machados.
    *   Repulsão (Knockback) natural sem precisar do encantamento.
    *   Resistência passiva (redução de dano físico recebido).
5.  **Arqueiro (Foco: Combate à Distância)**
    *   Aumento da velocidade de puxada do arco/besta.
    *   Dano extra em todos os projéteis.
    *   Chance percentual de salvar/não consumir a flecha ao atirar.

---

## 4. Sistema de Mudança de Classe (Respec)
Os jogadores não ficam presos a uma classe permanentemente, mas a troca possui um peso significativo na economia do jogador para evitar abusos.

*   **Ação:** O jogador paga uma taxa base em Níveis de XP para resetar sua escolha.
*   **Penalidade e Retorno:** A árvore da classe atual é completamente zerada. O sistema calcula o total de PTs que foram gastos nela e devolve **apenas 25%** desses pontos para o jogador utilizar na sua nova classe escolhida.
*   *Nota:* A Árvore Comum permanece intacta e não é afetada pela troca de classe.

---

## 5. Especificações Técnicas (Desenvolvimento Forge)

Para o desenvolvimento da versão inicial do mod (Alpha/v0.1), a arquitetura do código deve ser dividida nas seguintes camadas:

### 5.1. Persistência de Dados (Capabilities)
*   **`PlayerSkillData` (Capability customizada):** Vinculada ao `PlayerEntity` via `AttachCapabilitiesEvent`.
*   **Variáveis:** 
    *   `String currentClass` (Padrão: "none").
    *   `int availablePoints` (Pontos comprados e não gastos).
    *   `Map<String, Integer> unlockedNodes` (Armazena o ID do nó e o nível investido).
*   **Serialização:** Necessário implementar `INBTSerializable` para que os pontos e classes sejam salvos no `level.dat` / NBT do jogador.

### 5.2. Comunicação de Rede (Networking)
*   **Sincronização:** Envio de pacotes do Servidor para o Cliente (`Server -> Client`) no login ou ao mudar de dimensão, garantindo que a interface gráfica (GUI) exiba os talentos corretos.
*   **Ações In-game:** Envio de pacotes do Cliente para o Servidor (`Client -> Server`):
    *   `PacketBuyNode(nodeId)`: Valida os requisitos e o custo em XP.
    *   `PacketChangeClass(newClass)`: Aplica a lógica de retenção de 25% dos pontos e reseta os nós da classe antiga.

### 5.3. Interceptação Lógica (Event Bus)
Os bônus são aplicados interceptando eventos padrão do Forge:
*   **Combate:** `LivingDamageEvent` ou `LivingHurtEvent` (Para modificar dano causado e recebido por Guerreiros/Arqueiros/Comum).
*   **Quebra de Blocos:** `BlockEvent.BreakEvent` (Para aplicar drops extras do Minerador).
*   **Drops:** `LivingDropsEvent` (Para drops extras do Produtor).
*   **Modificadores Permanentes:** Atributos estáticos (Vida Máxima, Velocidade) devem ser geridos anexando um `AttributeModifier` via `EntityAttributeModificationEvent`, atualizando-os apenas quando o jogador gasta um ponto na árvore.

### 5.4. Interface do Usuário (GUI)
*   **Classe Screen:** Criação de uma `Screen` customizada ativada por uma *KeyBinding* (tecla de atalho).
*   **Renderização:** Lógica visual que escurece nós inacessíveis, destaca os nós disponíveis para compra (lendo a regra de 50% do nó anterior) e exibe os nós maximizados de forma diferenciada.

### 5.5. Estrutura Orientada a Dados (Data-Driven / JSON)
*   As árvores não devem ser "Hardcoded" (chumbadas no código).
*   Os nós devem ser arquivos JSON localizados em `src/main/resources/data/MOD_ID/skills/`.
*   **Esquema do JSON esperado:**
    *   `id`: Identificador único (ex: "warrior_dmg_1").
    *   `name`: Chave de tradução para o nome.
    *   `description`: Chave de tradução para o texto.
    *   `icon`: Recurso de textura (Item ou sprite).
    *   `maxLevel`: Nível máximo do nó (ex: 5).
    *   `prerequisites`: Lista de IDs de nós anteriores e o nível exigido de cada um.
    *   `treeCategory`: A qual aba/classe pertence (common, warrior, miner...).