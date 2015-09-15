package middleware.main;


import middleware.main.Middleware;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import middleware.model.Codigo;
import middleware.model.Protocolo;
import middleware.model.RegistroServidor;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Desktop
 */
class EntregaServidor extends RegistroServidor implements Runnable {

    Socket socket;
    DataOutputStream dos; // É a corrente de Saida de Dados
    DataInputStream dis;  // É a correde de Entrada de Dados

    public EntregaServidor(Socket connection) {
        super();
        socket = connection;

    }

    public void enviarServidor(RegistroServidor c1) throws IOException {
        dos.writeUTF(c1.getNome());
        dos.writeUTF(c1.getIp());
        dos.writeInt(c1.getPorta());
    }

    @Override
    public void run() { // Metodo principal da thread que vai tratar da comunicação enre servidor e cliente.
        try {
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            boolean conectado = true;
            // socket.setSoTimeout(3000);  // IMPORTANTE!! Define um TIMEOUT de 3 segundos
            // Ou seja apos 1,5 segundos sem conversa o servidor termina o papo e declara que o contato desconectou
            int qtdCorrido = 0;
            while (conectado) { //Enquanto o cliente nao desconectar
                // Sempre vai haver o envio de um inteiro para o servidor
                int code = dis.readInt();  // Aqui recebe-se esse inteiro
                // Esse inteiro é um código e o que cada valor representa está dentro da classe Protocolo
                Logger.getLogger("Middleware").fine(socket.toString() + " me enviou " + code + ".");
                qtdCorrido++;
                if (code == Protocolo.PedirConexao) { // Quando um servidor envia um "PedirConexao" para estar na lista de servidores disponiveis
                    dos.writeInt(Protocolo.ConexaoAceita); //O servidor responde "ConexaoAceita"
                    Logger.getLogger("Middleware").info("Servidor Conectado\nRespondi para " + socket + " Conexao Aceita. CODIGO=" + Protocolo.ConexaoAceita);
                    nome = dis.readUTF(); // O cliente envia para o servidor seu nome
                    porta = dis.readInt(); // Sua porta TCP
                    ip = socket.getInetAddress().getHostAddress(); // E o proprio middleware descobre qual é o ip do cliente
                    //  System.out.println("Servidor Conectado");
                    //   System.out.println("Nome: " + nome);
                    //   System.out.println("Ip: " + ip);
                    //  System.out.println("Porta: " + porta);
                    // System.out.println();
                      Middleware.addServidor(this); // Adiciona numa lista de servidores conectados       
                } else if (code == Protocolo.PedirEndereco) {
                    RegistroServidor rs = Middleware.getBestServidor();
                    if (rs != null) {
                        dos.writeInt(Protocolo.EnviandoEndereco); // É respondido enviando o endereço
                        enviarServidor(rs);
                        Logger.getLogger("Middleware").info("Cliente Conectado\nRespondi para " + socket + " qual o melhor servidor disponivel.\nServidor:\nNome:" + rs.getNome() + " IP:" + rs.getIp() + " Porta: " + rs.getPorta() + " Clientes Conectados: " + rs.getClientesOnline());
                        /*    System.out.println("Servidor Conectado");
                         System.out.println("Nome: " + rs.getNome());
                         System.out.println("Ip: " + rs.getIp());
                         System.out.println("Porta: " + rs.getPorta());
                         System.out.println("Clientes Conectados: " + rs.getClientesOnline());*/
                    } else {
                        dos.writeInt(Protocolo.CodigoInesperado); // Não há servidor para ser enviado.
                        Logger.getLogger("Middleware").warning("Não há servidor para ser enviado para " + socket);
                        //System.out.println("Não há servidor para enviar.");
                    }
                    conectado = false; // Para o loop

                } else if (code == Protocolo.Ping) { // O Cliente envia PING para lembrar para o servidor que ele ainda está conectado
                    dos.writeInt(Protocolo.Pong); // É respondido PONG
                    clientesOnline = dis.readInt();
                    Logger.getLogger("Middleware").finest("Respondi Pong\nRecebi que a quantidade de clientes online neste servidor(" + this.getNome() + ") é: " + clientesOnline);

                    //System.out.println("Nome: "+nome+" PING! Clientes Conectados: "+clientesOnline);
                } else if (code == Protocolo.Desconectar) { // Se o codigo recebido foi desconectar 
                    conectado = false; // para o loop
                    Logger.getLogger("Middleware").warning(socket + " pediu para desconectar");
                } else if (code == Protocolo.EnviarLog) {
                    dos.writeInt(Protocolo.EnviarLog); // É respondido OK
                    String level = dis.readUTF(); // Recebe o nivel da mensagem
                    String log = dis.readUTF(); // O cliente envia a mensagem a ser salva
                    try {
                        Logger.getLogger("Middleware").log(Level.parse(level), "Servidor (" + nome + "," + ip + "," + porta + ") diz: " + log);
                    } catch (IllegalArgumentException illegalArgumentException) {
                        Logger.getLogger("Middleware").severe("Algo deu errado ao tentar logar: " + log);
                    }
                } else if (code == Protocolo.PedirCodigos) // Está na hora de conferir o codigo com o servidor.
                {
                    System.out.println("Recebido PedirCodigos");
                    dos.writeInt(Protocolo.PedirCodigos);
                    System.out.println("Enviado PedirCodigos");
                    dos.writeInt(Middleware.getEstadoCodigos());
                    System.out.println("Enviado estado de Codigos:" + Middleware.getEstadoCodigos());
                    int diferenca = dis.readInt();
                    System.out.println("Diferenca recebida: " + diferenca);
                    if (diferenca != 0) {
                        Map<String, Codigo> map = Middleware.getListaCodigos();
                        for (Map.Entry<String, Codigo> entry : map.entrySet()) {
                            dos.writeInt(Protocolo.TrocandoCodigos); // É respondido TrocandoCodigo
                            System.out.println("Enviado TrocandoCodigos");
                            code = dis.readInt();
                            if (code == Protocolo.TrocandoCodigos) {
                                System.out.println("Recebido TrocandoCodigos");
                                dos.writeUTF(entry.getKey());
                                int jaexiste = dis.readInt();
                                System.out.println("Ja Existe= " + jaexiste);
                                if (jaexiste == 0) {
                                    enviaArquivo(entry.getValue());
                                    System.out.println("Arquivo enviado");
                                }
                            } else {
                                Logger.getLogger("Middleware").severe("Quebra de protocolo ao Trocar códigos: " + socket);
                                throw new Exception("Quebra de protocolo ao Trocar códigos: " + socket);
                            }
                        }
                        dos.writeInt(Protocolo.FimCodigos);
                        System.out.println("Enviado FimCodigos");
                        code = dis.readInt();
                        if (code == Protocolo.FimCodigos) {
                            System.out.println("Recebido FimCodigos");
                            diferenca = dis.readInt();
                            System.out.println("Recebido nova Diferenca=" + diferenca);
                            int prot = dis.readInt();;
                            if (diferenca > 0) {

                                do {
                                    if (prot == Protocolo.EnviandoCodigo) {
                                        System.out.println("Recebido EnviandoCodigos");
                                        dos.writeInt(Protocolo.EnviandoCodigo);
                                        System.out.println("Enviado EnviandoCodigo");
                                        String md5 = dis.readUTF();
                                        System.out.println("Recebido o MD5: " + md5);
                                        recebeArquivo(md5);
                                    }
                                    prot = dis.readInt();
                                } while (prot == Protocolo.EnviandoCodigo);
                            }
                            if (prot == Protocolo.FimCodigos) {
                                System.out.println("Recebido FimCodigos");
                                dos.writeInt(Protocolo.FimCodigos);
                                dos.writeInt(Middleware.getEstadoCodigos());
                                System.out.println("Enviado FimCodigos");
                            } else {
                                Logger.getLogger("Middleware").severe("Quebra de protocolo ao finalizar recepção de códigos: " + socket);
                                throw new Exception("Quebra de protocolo ao finalizar recepção de códigos: " + socket);
                            }
                        } else {
                            Logger.getLogger("Middleware").severe("Quebra de protocolo ao finalizar Troca de códigos: " + socket);
                            throw new Exception("Quebra de protocolo ao finalizar Troca de códigos: " + socket);
                        }
                    }
                } else {
                    dos.writeInt(Protocolo.CodigoInesperado); // Caso recebe algum codigo desconhecido responde codigoInesperado
                    Logger.getLogger("Middleware").warning(socket + " enviou um codigo inesperado");
                }
            }

        } catch (InterruptedIOException iioe) {
            Logger.getLogger("Middleware").warning("Timeout! removendo esse cliente...");
            //System.out.println("Timeout! removendo cliente do servidor...");
        } catch (IOException ex) {
            Logger.getLogger("Middleware").warning("Fui desconectado do nada!");
        } catch (Exception ex) {
            Logger.getLogger("Middleware").severe(ex.getMessage());
        } finally {
            try {
                dos.close();
                dis.close();
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger("Middleware").severe("Erro ao conectar!");
                //System.out.println("Erro ao desconectar");
            }
            Middleware.removeServidor(this);
        }

    }

    private void enviaArquivo(Codigo value) {
        try {
            File file = new File(value.getPath());
            DataInputStream fileIn = new DataInputStream(new FileInputStream(file));
            byte[] mybytearray = new byte[(int) file.length()]; // Vetor de bytes
            fileIn.read(mybytearray, 0, mybytearray.length);
            fileIn.close();
            dos.writeUTF(value.getPath());
            dos.writeInt(mybytearray.length);
            dos.write(mybytearray);
            dos.flush();
        } catch (FileNotFoundException ex) {
            Logger.getLogger("Middleware").severe("O codigo(" + value.getPath() + ") que tentei enviar para " + socket + " não foi encontrado.");
        } catch (IOException ex) {
            Logger.getLogger("Middleware").severe("Não foi possivel descobrir o tamanho do codigo(" + value.getPath() + ")");
        }

    }

    private void recebeArquivo(String md5) {
        try {
            String nome = dis.readUTF();
            File file = new File(nome);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger("Middleware").severe("Erro ao tentar criar novo arquivo(" + nome + ")");
                }
            }
                        else
            {
                String nomeArquivo = nome.substring(0, nome.length()-2);
                file = new File(nomeArquivo+"1.js");
            }
            DataOutputStream fileOut = new DataOutputStream(new FileOutputStream(file));

            Codigo novo = new Codigo(nome, md5);
            int tamanho = dis.readInt();
            byte[] mybytearray = new byte[tamanho]; // Vetor de bytes
            dis.read(mybytearray, 0, tamanho);
            fileOut.write(mybytearray);
            fileOut.close();
            Middleware.addCodigo(novo);
        } catch (FileNotFoundException ex) {
            Logger.getLogger("Middleware").severe("Erro ao tentar abrir arquivo(" + nome + ")");
        } catch (IOException ex) {
            Logger.getLogger("Middleware").severe("Erro ao tentar ler mensagem do Servidor | codigo(" + nome + ").");
        }

    }

}
