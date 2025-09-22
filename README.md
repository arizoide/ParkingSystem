**Sistema de Gerenciamento de Estacionamento**

Este projeto é um sistema de backend para gerenciar a entrada e saída de veículos em um estacionamento, calcular tarifas e monitorar a receita. O sistema se integra a um simulador externo para obter a configuração da garagem e processar eventos de veículos via webhook.

***Funcionalidades Atuais***

Inicialização Dinâmica: Ao iniciar a aplicação, ela se conecta ao simulador da garagem (http://localhost:8080/garage) para buscar e persistir a configuração dos setores e vagas no banco de dados.

Tratamento de Eventos Webhook: O sistema escuta eventos do simulador (ENTRY, PARKED, EXIT) e atualiza o estado do estacionamento.

**ENTRY:** Registra a entrada de um veículo em uma vaga disponível. Se a garagem estiver lotada, uma exceção é lançada.

**PARKED:** Associa um veículo a uma vaga específica com base na localização (latitude e longitude).

**EXIT:** Marca a vaga como disponível, calcula a duração da sessão e o preço total com base nas regras de negócio.

**Cálculo de Receita:**

A tarifa é baseada no tempo de permanência, com os primeiros 30 minutos gratuitos e cobrança por hora cheia a partir daí.

Um sistema de preço dinâmico é aplicado na entrada do veículo, com descontos ou acréscimos baseados na taxa de ocupação do setor.

API REST: Expõe um endpoint GET /revenue para consultar a receita total de um setor em uma data específica.



**Melhorias a Serem Aplicadas em um Segundo Momento**



O projeto atual é funcional, mas diversas melhorias podem ser implementadas para torná-lo mais robusto, escalável e resiliente.

1. Otimização do Sistema de Preço
Revisão do Cálculo de Preço: A regra de negócio do teste exige a cobrança por hora cheia após os primeiros 30 minutos. O método calculatePrice atual divide minutesParked / 60.0, o que não implementa o arredondamento para a hora mais próxima. É necessário ajustar a lógica para garantir que a cobrança seja feita por hora completa. Por exemplo, uma permanência de 31 minutos deve ser cobrada como 1 hora completa.

Armazenamento do Preço Dinâmico: A regra de preço dinâmico é calculada no momento da saída do veículo no código atual. O documento de teste, no entanto, especifica que o preço dinâmico deve ser aplicado 

na hora da entrada. Isso indica que a lógica de cálculo de preço dinâmico deve ser movida para o método 

handleEntry e o preço ajustado deve ser salvo no objeto ParkingSession no momento da entrada.

2. Resiliência e Tratamento de Erros
Tratamento de Falhas na Inicialização: Atualmente, a falha na conexão com o simulador na inicialização (fetchAndSaveGarageData) apenas exibe uma mensagem de erro. Em um ambiente de produção, a aplicação deve ter uma política de retry (tentativa de reconexão) ou uma forma de notificar a falha para evitar que o sistema inicie sem os dados de configuração essenciais.

Validação de Dados de Entrada: Embora o código use Optional, a validação dos dados JSON de entrada nos webhooks pode ser mais robusta. O uso de DTOs (Data Transfer Objects) com anotações de validação (@Valid) garantiria que a aplicação só processe eventos com formatos corretos, evitando erros em tempo de execução.

Lançamento de Exceções: A exceção SectorFullException é um bom começo, mas o tratamento de exceções pode ser centralizado com um @ControllerAdvice para fornecer respostas de erro mais padronizadas e descritivas para o cliente.

3. Refatoração e Padrões de Código
Separação de Responsabilidades: O ParkingService está se tornando um "Deus Objeto" (God Object) ao concentrar a lógica de inicialização, tratamento de eventos, cálculo de preço e acesso a múltiplos repositórios. A lógica de negócio poderia ser extraída para um ou mais serviços menores (ex: PricingService, WebhookHandler) para melhorar a modularidade e facilitar a manutenção.

Transações de Banco de Dados: As operações que envolvem a leitura e escrita de múltiplos objetos no banco de dados (por exemplo, ao salvar a sessão de estacionamento e atualizar o status da vaga) devem ser encapsuladas em uma transação (@Transactional) para garantir atomicidade. Se uma das operações falhar, ambas devem ser revertidas.

4. Melhorias na Funcionalidade
Relatórios e APIs Adicionais: O sistema de relatório atual é simples (/revenue). Novas APIs poderiam ser adicionadas para:

Consultar a ocupação atual de cada setor.

Retornar o status de uma vaga específica.

Gerar relatórios de faturamento por período (semana, mês).

Otimização de Consultas: Em vez de buscar todas as sessões e filtrá-las em memória no método getRevenue, a lógica de consulta pode ser movida para o repositório (sessionRepository). Isso permitiria que o banco de dados fizesse a filtragem de forma mais eficiente, como sessionRepository.findByExitTimeBetweenAndSpot_Sector_Name(startDate, endDate, sectorName).
