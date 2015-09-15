package cliente.threads;

import cliente.interfaces.InterfaceInfo;
import java.awt.Component;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import cliente.model.Codigo;
import cliente.model.Contato;
import cliente.model.Protocolo;
import cliente.model.Tarefa;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Desktop Aqui fica o Cliente TCP que se conecta com o servidor
 */
public class ConexaoServidor implements Runnable {

    private String ipServidor;
    private int portaServidor;
    private String nomeServidor;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    InterfaceInfo trataInfo;
    private int codigo;
    public ArrayList<Contato> contatos;
    public ArrayList<Codigo> codigos;
    private Tarefa tarefaAtual;

    public ConexaoServidor(InterfaceInfo t) throws IOException {
        trataInfo = t;
        socket = new Socket(trataInfo.getIpServidor(), Protocolo.PortaServidor);

        dos = new DataOutputStream(socket.getOutputStream());
        dis = new DataInputStream(socket.getInputStream());

    }

    // Esse metodo recebe um a um os contatos que o servidor envia
    // Note que existe uma ordem para receber
    private ArrayList<Codigo> codigosOnline(int qt) throws IOException {
        System.out.println("Recebendo contatos: ");
        codigos = new ArrayList<Codigo>();
        for (int i = 0; i < qt; i++) {
            String md5 = dis.readUTF(); //Primeiro é recebido o nome
            String nome = dis.readUTF(); // Depois o IP
            System.out.println("Codigo recebido" + nome);
            codigos.add(new Codigo(nome, md5));
        }
        return codigos;
    }

    public void desconecta() {
        codigo = Protocolo.Desconectar;
    }

    @Override
    public void run() {
        int resposta;
        boolean continua = true;
        codigo = Protocolo.PedirEndereco; // Se a resposta demorar mais de 3 segundos então o servidor morreu eu paro de comunicar com ele.

        while (continua) {
            try {
                if(trataInfo.temTarefa())
                {
                            tarefaAtual = trataInfo.getProxTarefa();
                            codigo = tarefaAtual.getProtocolo();
                            System.out.println("realizando tarefa: "+codigo);
                }
                dos.writeInt(codigo);  // Envia o codigo com a requisição para o servidor
                if (codigo == Protocolo.PedirConexao) { // Se o código enviado foi PedirConexao
                    resposta = dis.readInt(); // recebe uma resposta do servidor
                    if (resposta == Protocolo.ConexaoAceita) { // se a respsota foi ConexaoAceita está tudo certo;
                        Contato c = trataInfo.getContato();
                        dos.writeUTF(c.getNome()); // Envia para o servidor o nome deste cliente
                        //   dos.writeInt(trataInfo.preparaConexaoCliente()); // envia para o servidor a porta UDP deste cliente
                        //   c.setPorta(trataInfo.preparaConexaoCliente());
                        c.setIp(socket.getLocalAddress().getHostAddress()); // Anota neste cliente o IP de conexão dele mesmo
                        System.out.println("Nome: " + c.getNome());
                        System.out.println("IP:" + c.getIp());
                        //          System.out.println("Porta: " + c.getPorta());
                        int qt = dis.readInt(); // Informa quantos contatos estão online 
                        codigos = codigosOnline(qt); //Recebe os contatos
                        trataInfo.atualizaCodigos(codigos);
                        codigo = Protocolo.Ping; // Proximo passo é PING
                    }
                } else if (codigo == Protocolo.PedirEndereco) {
                    resposta = dis.readInt();
                    if (resposta == Protocolo.EnviandoEndereco) {
                        nomeServidor = dis.readUTF();
                        ipServidor = dis.readUTF();
                        portaServidor = dis.readInt();
                        dos.close();
                        dis.close();
                        socket.close();
                        socket = new Socket(ipServidor, portaServidor);
                        dos = new DataOutputStream(socket.getOutputStream());
                        dis = new DataInputStream(socket.getInputStream());
                        System.out.println("Desconectou do MIDDLEWARE, se conectou ao servidor:" + nomeServidor);
                        System.out.println("IP:" + ipServidor);
                        System.out.println("Porta:" + portaServidor);
                        trataInfo.setNomeServidor(nomeServidor);
                        codigo = Protocolo.PedirConexao;
                    } else {
                        continua = false;
                        throw new Exception("Não existe servidor");
                    }
                } 
                else if (codigo == Protocolo.Ping) // Se a requisição enviada para o Servidor for PING
                {
                    resposta = dis.readInt(); // recebe uma respsota
                    if (resposta == Protocolo.Pong) // a resposta tem que ser PONG
                    {
                        int qt = dis.readInt(); // Informa quantos contatos estão online 
                        codigos = codigosOnline(qt); //Recebe os contatos
                        trataInfo.atualizaCodigos(codigos);
                        System.out.println("Ping!");
                        Thread.sleep(1000); // IMPORTANTE!! Põe essa conexão para dormir 1 segundo para não sobrecarregar a rede.
                    }
                } 
                else if(codigo == Protocolo.DOWNLOAD)
                {
                    System.out.println("Entrou no Download");
                    resposta = dis.readInt(); // recebe uma respsota
                    
                    if(resposta==Protocolo.UPLOAD)
                    {
                        dos.writeUTF(tarefaAtual.getCodigo().getMd5());
                        resposta = dis.readInt();
                        if(resposta==1)
                        {
                            String saida = recebeArquivo();
                            trataInfo.updateTerminal(saida);
                        }
                        else{
                            System.out.println("Arquivo fail!");
                        }
                            tarefaAtual = null;
                    }
                    codigo = Protocolo.Ping;
                }
                else if(codigo == Protocolo.UPLOAD)
                {
                    resposta = dis.readInt(); // recebe uma respsota
                    if(resposta==Protocolo.DOWNLOAD)
                    {
                        dos.writeUTF(tarefaAtual.getCodigo().getMd5());
                        resposta = dis.readInt();
                        if(resposta==0)
                        {
                            enviaArquivo(tarefaAtual.getCodigo());
                            JOptionPane.showMessageDialog((Component) trataInfo, "Script carregado com sucesso!");
                        }
                        else{
                            System.out.println("Arquivo ja existe!");
                        }
                        tarefaAtual = null;
                    }
                    codigo = Protocolo.Ping;
                }
                else if(codigo==Protocolo.EXECUTAR)
                {
                    resposta = dis.readInt(); // recebe uma respsota
                    if(resposta==Protocolo.EXECUTAR)
                    {
                        dos.writeUTF(tarefaAtual.getCodigo().getMd5());
                        resposta = dis.readInt();
                        if(resposta==1)
                        {
                            dos.writeUTF(tarefaAtual.getParametros());
                            String answer = dis.readUTF();
                            trataInfo.updateTerminal(answer);
                        }
                        else{
                            trataInfo.updateTerminal("O arquivo nao existe no servidor");
                        }
                        tarefaAtual = null;
                    }
                    codigo = Protocolo.Ping;
                }
                else if (codigo == Protocolo.Desconectar) {
                    System.out.println("Bye!");
                    continua = false;
                }

            } catch (InterruptedIOException iioe) {
                try {
                    codigo = Protocolo.PedirEndereco; 
                    socket = new Socket(trataInfo.getIpServidor(), Protocolo.PortaServidor);
                    dos = new DataOutputStream(socket.getOutputStream());
                    dis = new DataInputStream(socket.getInputStream());
                } catch (IOException ex) {
                    System.out.println("Nao foi possivel reconectar!");
                }

                System.out.println("Timeout! pedindo novo servidor...");
            } catch (IOException ex) {
                System.out.println("Erro IO");
                    
                try {
                    codigo = Protocolo.PedirEndereco; 
                    socket = new Socket(trataInfo.getIpServidor(), Protocolo.PortaServidor);
                    dos = new DataOutputStream(socket.getOutputStream());
                    dis = new DataInputStream(socket.getInputStream());
                } catch (IOException ex1) {
                    Logger.getLogger(ConexaoServidor.class.getName()).log(Level.SEVERE, null, ex1);
                }
                    

            } catch (InterruptedException ex) {
                Logger.getLogger(ConexaoServidor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(ConexaoServidor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            dos.close();
            dis.close();
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ConexaoServidor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    
    private void enviaArquivo(Codigo value) {
        try {
            File file = new File(value.getFullPath());
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
    
    private String recebeArquivo() {
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
            }
            DataOutputStream fileOut = new DataOutputStream(new FileOutputStream(file));

            int tamanho = dis.readInt();
            byte[] mybytearray = new byte[tamanho]; // Vetor de bytes
            dis.read(mybytearray, 0, tamanho);
            fileOut.write(mybytearray);
            fileOut.close();
            return new String(mybytearray);
        } catch (FileNotFoundException ex) {
           System.out.println("Erro ao tentar abrir arquivo(" + nome + ")");
        } catch (IOException ex) {
            System.out.println("Erro ao tentar ler mensagem do Servidor | codigo(" + nome + ").");
        }
        return ("ERRO AO RECEBER ARQUIVO");
    }

}
