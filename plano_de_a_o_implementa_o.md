# Plano de Ação para Implementação: Mod de Talentos Vanilla+

Este documento detalha os próximos passos práticos para tirar o mod do papel, baseando-se nas diretrizes do GDD e nas definições técnicas iniciais.

---

## 1. Versões e Identidade do Mod

*   **Versão Alvo:** Minecraft 1.26.3 com Forge 66 (Conforme especificado, focaremos na arquitetura mais moderna do Forge, utilizando sistemas atualizados de registro e serialização).
*   **Sugestões de Mod ID e Nome:** 
    O `MOD_ID` precisa ser único, curto e apenas com letras minúsculas. Sugiro as seguintes opções para você escolher:
    *   **Opção 1:** Nome: *Vanilla+ Talents* | Mod ID: `vanillatalents` (Direto e claro)
    *   **Opção 2:** Nome: *XP Ascendancy* | Mod ID: `xpascendancy` (Foca na mecânica de gastar XP)
    *   **Opção 3:** Nome: *Paths & Talents* | Mod ID: `pathstalents` (Foca nas escolhas de classe)
    *   *(Recomendação: Vamos adotar `vanillatalents` como padrão para os próximos exemplos de código, mas você pode alterar depois).*

---

## 2. Configuração do Ambiente Inicial

Como você ainda não tem nada configurado, o primeiro passo será preparar o seu ambiente de desenvolvimento. O mod base não precisará de dependências externas para funcionar (apenas o próprio Forge), o que facilita a manutenção.

**Passo a passo que você precisará realizar:**
1.  **Baixar o MDK (Mod Development Kit):** Vá ao site oficial do Forge, procure a versão exata que vamos usar e baixe o arquivo MDK.
2.  **Extração:** Extraia o conteúdo para uma pasta dedicada ao projeto.
3.  **Configurar o `build.gradle`:** Você precisará editar este arquivo para definir o `modid`, `group` (ex: `com.seunome.vanillatalents`) e a versão do seu mod.
4.  **Gerar os arquivos da IDE:** Pelo terminal da pasta, rodar o comando (se usar IntelliJ): `gradlew genIntellijRuns`.
5.  **Importar:** Abrir a pasta no IntelliJ IDEA ou Eclipse como um projeto Gradle.

*Nota: Se no futuro quisermos integração com outros mods (como o JEI para ver itens ou o Curios para adicionar slots de equipamento), nós adicionaremos as dependências no `build.gradle`, mas para o MVP não é necessário.*

---

## 3. Fluxo de Experiência do Usuário (UX) - Compra de Pontos

Como você definiu que a conversão de XP será feita na própria UI, o fluxo técnico acontecerá da seguinte maneira:

1.  **A Interface (Screen):** O jogador aperta uma tecla (ex: `K`) e abre a GUI da Árvore de Talentos.
2.  **O Botão de Conversão:** No canto da tela, haverá um contador de "Níveis de XP Atuais" e "Pontos de Talento Disponíveis". Abaixo, um botão visualmente chamativo: `[ Converter 5 Níveis de XP em 1 PT ]`.
3.  **Lógica de Rede (Crucial):**
    *   O jogador clica no botão (Ação do **Client**).
    *   O Client não pode deduzir o XP sozinho, senão ocorreria *desync*. Ele envia um pacote customizado `PacketConvertXP` para o servidor.
    *   O **Server** recebe o pacote, verifica se o jogador tem $\ge 5$ níveis de XP.
    *   Se sim, o Server deduz 5 níveis de XP do jogador e adiciona +1 PT na `PlayerSkillData` (Capability).
    *   O Server envia um pacote de resposta (`PacketSyncTalents`) de volta ao Client, atualizando a tela instantaneamente.

---

## 4. Escopo do Produto Mínimo Viável (MVP)

Para garantir que o sistema de carregamento de JSONs, a lógica de rede e o salvamento de dados funcionem antes de criarmos 50 habilidades, vamos focar em **1 Árvore Comum** e **1 Classe (Minerador)**. Usaremos valores e atributos reais para já testar os impactos no jogo.

### 4.1. Nós Iniciais da Árvore Comum (Sobrevivência)
*   `common_health` (Vitalidade)
    *   **Efeito:** +2 de Vida Máxima (1 Coração) por nível.
    *   **Níveis:** Max 5.
    *   **Implementação:** Atributo de modificador no `Attributes.MAX_HEALTH`.
*   `common_saturation` (Estômago de Ferro)
    *   **Efeito:** Reduz a velocidade com que a barra de fome desce ao curar vida.
    *   **Níveis:** Max 3.
    *   **Requisito:** `common_health` precisa ter nível 3 (50% de 5 = 2.5 arredondado para 3).

### 4.2. Nós Iniciais da Classe Minerador
*   `miner_haste` (Braços Incansáveis)
    *   **Efeito:** Aumenta a velocidade de quebra de blocos em 10% por nível.
    *   **Níveis:** Max 5.
    *   **Implementação:** Interceptar `PlayerEvent.BreakSpeed`.
*   `miner_darkvision` (Olhos das Profundezas)
    *   **Efeito:** Concede efeito de Visão Noturna perpétuo sempre que o jogador estiver na camada Y < 0.
    *   **Níveis:** Max 1.
    *   **Requisito:** `miner_haste` nível 3.
    *   **Implementação:** Checagem no `PlayerTickEvent` (aplicando o efeito de poção curto que se renova).
*   `miner_fortune` (Toque de Midas)
    *   **Efeito:** 5% de chance por nível de dropar minérios extras (acumula com o encantamento Fortuna).
    *   **Níveis:** Max 4.
    *   **Requisito:** `miner_haste` nível 5.