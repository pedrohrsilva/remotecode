# remotecode
Trabalho desenvolvido para a disciplina de Sistemas Distribuidos

Estão neste trabalho 3 partes envolvidas num sistema de avaliação de código remoto

Cliente:
1. Escreve código em Javascript e envia este código ao Serviço RemoteCode,
2. Download de Código
3. Execução de Código em algum Servidor, permitindo a passagem de parametros

Middleware:
1. Mantem lista de servidores online
2. Faz a conexão entre cliente e servidor de menor carga
3. Sincroniza os arquivos entre os diversos servidores

Servidor:
1. Recebe código do cliente
2. Envia código para o cliente
3. Executa código a partir de requisição do cliente
