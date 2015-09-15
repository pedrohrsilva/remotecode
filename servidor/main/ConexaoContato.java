package servidor.main;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import servidor.model.Codigo;
import servidor.model.Contato;
import servidor.model.Protocolo;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Desktop
 */
class ConexaoContato extends Contato implements Runnable {

    Socket socket;
    DataOutputStream dos; // É a corrente de Saida de Dados
    DataInputStream dis;  // É a correde de Entrada de Dados

    public ConexaoContato(Socket connection) {
        super();
        socket = connection;

    }

    public void enviarLista() throws IOException {
        int qt = Servidor.qtdContatos();
        System.out.println("Enviando QTD=" + (qt - 1));  // Informa quantos contatos vão ser enviados subtrai-se 1 porque nao envia um contato para ele mesmo
        dos.writeInt(qt - 1); // envia
        for (int i = 0; i < qt; i++) {
            Contato c1 = Servidor.getContato(i);
            if (!c1.equals(this)) {
                dos.writeUTF(c1.getNome());
                dos.writeUTF(c1.getIp());
                //        dos.writeInt(c1.getPorta());
            }

        }
    }

    public void enviarListaCodigos() throws IOException {
        Map<String, Codigo> copia = Servidor.getListaCodigos();
        int qt = copia.size();
        System.out.println("Enviando QTD=" + (qt));  // Informa quantos contatos vão ser enviados subtrai-se 1 porque nao envia um contato para ele mesmo
        dos.writeInt(qt); // envia
        for (Map.Entry<String, Codigo> entry : copia.entrySet()) {
            dos.writeUTF(entry.getValue().getMd5());
            dos.writeUTF(entry.getValue().getPath());
        }
    }

    @Override
    public void run() { // Metodo principal da thread que vai tratar da comunicação enre servidor e cliente.
        try {
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            boolean conectado = true;
           // IMPORTANTE!! Define um TIMEOUT de 3 segundos
            // Ou seja apos 1,5 segundos sem conversa o servidor termina o papo e declara que o contato desconectou

            while (conectado) { //Enquanto o cliente nao desconectar
                // Sempre vai haver o envio de um inteiro para o servidor
                int code = dis.readInt();  // Aqui recebe-se esse inteiro
                // Esse inteiro é um código e o que cada valor representa está dentro da classe Protocolo

                if (code == Protocolo.PedirConexao) { // Quando o cliente envia um "PedirConexao"
                    dos.writeInt(Protocolo.ConexaoAceita); //O servidor responde "ConexaoAceita"
                    nome = dis.readUTF(); // O cliente envia para o servidor seu nome
                    //        porta = dis.readInt(); // Sua porta UDP
                    ip = socket.getInetAddress().getHostAddress(); // E o proprio servidor descobre qual é o ip do cliente
                    System.out.println("Contato Conectado");
                    System.out.println("Nome: " + nome);
                    System.out.println("Ip: " + ip);
                    //  System.out.println("Porta: " + porta);
                    System.out.println();
                    Servidor.addContato(this); // Adiciona numa lista de clientes conectados
                    enviarListaCodigos(); // Envia a lista para o cliente

                } else if (code == Protocolo.Ping) { // O Cliente envia PING para lembrar para o servidor que ele ainda está conectado
                    dos.writeInt(Protocolo.Pong); // É respondido PONG
                    enviarListaCodigos();    // E é enviado a lista com todos os contatos conectados Não importando se houve ou não alteração
                    // Ou seja isso podia ser melhorado com uma adptação do protocolo
                    // Por exemplo enviando a lista somente se houve alteração
                    // Ou enviando somente as modificações
                    // Fica pra versão 2.0
                    System.out.println("Nome: " + nome + " PING!");
                } else if (code == Protocolo.UPLOAD) {
                    dos.writeInt(Protocolo.DOWNLOAD);
                    String md5 = dis.readUTF();
                    if (Servidor.possuiCodigo(md5)) {
                        dos.writeInt(1);
                    } else {
                        dos.writeInt(0);
                        recebeArquivo(md5);
                    }
                } else if (code == Protocolo.DOWNLOAD) {
                    dos.writeInt(Protocolo.UPLOAD);
                    String md5 = dis.readUTF();
                    if (Servidor.possuiCodigo(md5)) {
                        dos.writeInt(1);
                        enviaArquivo(Servidor.getCodigo(md5));
                    } else {
                        dos.writeInt(0);

                    }
                } else if (code == Protocolo.Desconectar) { // Se o codigo recebido foi desconectar 
                    conectado = false; // para o loop
                } else if (code == Protocolo.EXECUTAR) {
                    dos.writeInt(Protocolo.EXECUTAR); // recebe uma respsota
                    String md5 = dis.readUTF();
                    if (Servidor.possuiCodigo(md5)) {
                        dos.writeInt(1);
                        String arg = dis.readUTF();
                        String[] partir = arg.split(",");
                        String funcao = partir[0];
                        String[] args = Arrays.copyOfRange(partir, 1, partir.length);
                        String cache = Servidor.getCache(md5, arg);
                        if (cache.equals("Nao existe no cache.")) {
                            ScriptEngineManager manager = new ScriptEngineManager();
                            ScriptEngine engine = manager.getEngineByName("JavaScript");
                            String script = lerCodigo(Servidor.getCodigo(md5));
                            Invocable inv = (Invocable) engine;
                            try {
                                engine.eval(script);
                                String resultado = "" + inv.invokeFunction(funcao, args);
                                Servidor.addCache(md5, arg, resultado);
                                dos.writeUTF(resultado);
                            } catch (ScriptException ex) {
                                dos.writeUTF(ex.getMessage());
                            } catch (NoSuchMethodException ex) {
                                dos.writeUTF(ex.getMessage());
                            }
                        }
                        else{
                            String resultado = "Diretamente do Cache:\n";
                            resultado += cache;
                            dos.writeUTF(resultado);
                        }

                    } else {
                        dos.writeInt(0);

                    }
                } else {
                    dos.writeInt(Protocolo.CodigoInesperado); // Caso recebe algum codigo desconhecido responde codigoInesperado
                }
            }

        } catch (InterruptedIOException iioe) {
            System.out.println("Timeout! removendo cliente do servidor...");
        } catch (IOException ex) {
            System.out.println(ex.toString());
        } finally {
            try {
                dos.close();
                dis.close();
                socket.close();

            } catch (IOException ex) {
                System.out.println("Erro ao desconectar");
            }
            Servidor.removeContato(this);
        }

    }

    private void recebeArquivo(String md5) {
        String nome = null;
        try {
            nome = dis.readUTF();
            File file = new File(nome);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger("Middleware").severe("Erro ao tentar criar novo arquivo(" + nome + ")");
                }
            } else {
                String nomeArquivo = nome.substring(0, nome.length() - 2);
                file = new File(nomeArquivo + "1.js");
            }
            DataOutputStream fileOut = new DataOutputStream(new FileOutputStream(file));
            Codigo novo = new Codigo(nome, md5);
            int tamanho = dis.readInt();
            byte[] mybytearray = new byte[tamanho]; // Vetor de bytes
            dis.read(mybytearray, 0, tamanho);
            fileOut.write(mybytearray);
            fileOut.close();
            Servidor.addCodigo(novo);
        } catch (FileNotFoundException ex) {
            System.out.println("Falha de arquivo(" + nome + ") nao encontrado ao ser enviado.");
        } catch (IOException ex) {
            System.out.println("Falha de não conseguir ler o arquivo(" + nome + ")");
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
            System.out.println("Enviado o nome");
            dos.writeInt(mybytearray.length);
            System.out.println("Enviado o tamanho");
            dos.write(mybytearray);
            dos.flush();
        } catch (FileNotFoundException ex) {
            Logger.getLogger("Middleware").severe("O codigo(" + value.getPath() + ") que tentei enviar para " + socket + " não foi encontrado.");
        } catch (IOException ex) {
            Logger.getLogger("Middleware").severe("Não foi possivel descobrir o tamanho do codigo(" + value.getPath() + ")");
        }

    }

    private String lerCodigo(Codigo value) {
        try {
            File file = new File(value.getPath());
            DataInputStream fileIn = new DataInputStream(new FileInputStream(file));
            byte[] mybytearray = new byte[(int) file.length()]; // Vetor de bytes
            fileIn.read(mybytearray, 0, mybytearray.length);
            fileIn.close();
            return new String(mybytearray);
        } catch (FileNotFoundException ex) {
            Logger.getLogger("Middleware").severe("O codigo(" + value.getPath() + ") que tentei enviar para " + socket + " não foi encontrado.");
        } catch (IOException ex) {
            Logger.getLogger("Middleware").severe("Não foi possivel descobrir o tamanho do codigo(" + value.getPath() + ")");
        }
        return null;

    }

}
