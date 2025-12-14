# Planejador Financeiro Android (Compose)

Aplicativo Android em Jetpack Compose para planejar saldos e gastos diários a partir de contas atuais, créditos futuros e transações simuladas.

## Funcionalidades
- **Tema escuro moderno** com cartões e navegação inferior entre Início, Inputs, Simulação e Dashboard.
- **Resumo inicial** com saldos da conta corrente, caixinhas CDB, vales e dívida do cartão.
- **Formulários dedicados** para conta corrente, salário (com ajuste automático para dia útil anterior), cartão de crédito, caixinhas editáveis e vales com opção de mudar o dia de crédito (penúltimo dia útil por padrão).
- **Simulações** de débitos/créditos e transferências entre conta corrente, caixinhas e vales, aceitando datas únicas ou intervalos.
- **Tabela diária** (30 dias por padrão) mostrando saldos por conta e cartão, destacando positivos em verde e negativos em vermelho.
- **Dashboard analítico** com variações entre início/fim do período, visão detalhada das caixinhas e gráficos simples de faixa e linha para acompanhar evolução e variação diária.
- **Cartões de transações futuras** para salário, vales e pagamento do cartão, além das simulações cadastradas com opção de exclusão.

## Como rodar
1. Instale o JDK 17+ e Android Studio Iguana (ou mais recente).
2. Abra a pasta `Android_AppV2` no Android Studio.
3. Sincronize o projeto (`Sync Project with Gradle Files`).
4. Rode no emulador ou dispositivo físico (API 26+). O app usa apenas Compose e não requer serviços externos.

## Convenções para contribuições
- Mantenha **AGENTS.md** e este README atualizados sempre que fluxos ou pré-requisitos mudarem.
- Prefira composables pequenos e reutilizáveis, valide inputs de usuário e evite estados que possam gerar crashes em parsing numérico ou de datas.
- Utilize nomenclatura clara para telas, seções e modelos financeiros.

## Estrutura das principais telas
- `MainActivity.kt`: Navegação e telas principais (Início, Inputs, Simulação, Dashboard).
- `model/FinancialModels.kt`: Modelos e regras de simulação (datas, eventos padrão, cálculo de saldos diários).
- `ui/theme`: Tema escuro personalizado.

Mantenha as telas e documentos alinhados com a experiência descrita acima conforme novas funcionalidades forem adicionadas.
