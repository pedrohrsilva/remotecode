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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import servidor.model.Codigo;
import servidor.model.Protocolo;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Desktop Aqui fica o Cliente TCP que se conecta com o servidor
 */
public class ConexaoMiddleware implements Runnable {

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private int codigo;
    private String nomeServidor;
    private int portaServidor;

    public ConexaoMiddleware(String nomeServidor, int portaServidor, String ipMiddleware, int portMid) throws IOException {
        socket = new Socket(ipMiddleware, portMid);
        this.nomeServidor = nomeServidor;
        this.portaServidor = portaServidor;
        dos = new DataOutputStream(socket.getOutputStream());
        dis = new DataInputStream(socket.getInputStream());

    }

    public void desconecta() {
        codigo = Protocolo.Desconectar;
    }

    @Override
    public void run() {
        int resposta;
        boolean continua = true;

        try {
            // socket.setSoTimeout(3000); // Define um TIMEOUT para a conexao
            int qtdCorrido = Protocolo.IntervaloCodigo - 5;
            codigo = Protocolo.PedirConexao; // Se a resposta demorar mais de 3 segundos então o servidor morreu eu paro de comunicar com ele.
            while (continua) {
                qtdCorrido++;
                if (qtdCorrido == Protocolo.IntervaloCodigo) {
                    qtdCorrido = 0;
                    codigo = Protocolo.PedirCodigos;
                }
                dos.writeInt(codigo);  // Envia o codigo com a requisição para o servidor
                if (codigo == Protocolo.PedirConexao) { // Se o código enviado foi PedirConexao
                    resposta = dis.readInt(); // recebe uma resposta do servidor
                    if (resposta == Protocolo.ConexaoAceita) { // se a respsota foi ConexaoAceita está tudo certo;
                        dos.writeUTF(nomeServidor); // Envia para o middleware o nome deste servidor
                        dos.writeInt(portaServidor); // envia para o middleware a porta deste servidor
                        codigo = Protocolo.Ping; // Proximo passo é PING
                    }
                } else if (codigo == Protocolo.Ping) // Se a requisição enviada para o Servidor for PING
                {
                    resposta = dis.readInt(); // recebe uma respsota
                    if (resposta == Protocolo.Pong) // a resposta tem que ser PONG
                    {
                        dos.writeInt(Servidor.qtdContatos());
                        //System.out.println("Ping!");
                        Thread.sleep(1000); // IMPORTANTE!! Põe essa conexão para dormir 1 segundo para não sobrecarregar a rede.
                    }
                } else if (codigo == Protocolo.PedirCodigos) // Está na hora de conferir o codigo com o servidor.
                {
                    resposta = dis.readInt(); // Espera a resposta ao pedido.
                    if (resposta == Protocolo.PedirCodigos) // Se o protocolo é atendido.
                    {
                        int estado = dis.readInt(); // Le qual o estado de codigos do servidor.
                        int diferenca = estado - Servidor.getEstadoCodigos();
                        dos.writeInt(diferenca);
                        codigo = Protocolo.Ping;
                        if (diferenca != 0) {
                            HashMap<String, Codigo> copia = new HashMap<String, Codigo>(Servidor.getListaCodigos());
                            int prot;
                            do {
                                prot = dis.readInt();
                                if (prot == Protocolo.TrocandoCodigos) {
                                    dos.writeInt(Protocolo.TrocandoCodigos);
                                    String md5 = dis.readUTF();
                                    if (Servidor.possuiCodigo(md5)) {
                                        copia.remove(md5);
                                        dos.writeInt(1);
                                    } else {
                                        dos.writeInt(0);
                                        recebeArquivo(md5);
                                    }
                                }
                            } while (prot == Protocolo.TrocandoCodigos);
                            if (prot == Protocolo.FimCodigos) {
                                dos.writeInt(Protocolo.FimCodigos);
                                dos.writeInt(copia.size());
                                if (copia.size() > 0) {
                                    for (Map.Entry<String, Codigo> entry : copia.entrySet()) {
                                        dos.writeInt(Protocolo.EnviandoCodigo); // É respondido TrocandoCodigo
                                        prot = dis.readInt();
                                        if (prot == Protocolo.EnviandoCodigo) {
                                            dos.writeUTF(entry.getKey());
                                            enviaArquivo(entry.getValue());
                                        } else {
                                            Logger.getLogger("Middleware").severe("Quebra de protocolo ao Trocar códigos: " + socket);
                                            throw new Exception("Quebra de protocolo ao Trocar códigos: " + socket);
                                        }
                                    }
                                }
                                dos.writeInt(Protocolo.FimCodigos);
                                prot=dis.readInt();
                                if(prot!=Protocolo.FimCodigos)
                                {
                                    Logger.getLogger("Middleware").severe("Quebra de protocolo ao enviar códigos: " + socket);
                                    throw new Exception("Quebra de protocolo ao enviar códigos: " + socket);
                                }
                                else{
                                    prot = dis.readInt();
                                    Servidor.setEstadoCodigos(prot);
                                }
                            }
                        }
                    }
                } else if (codigo == Protocolo.Desconectar) {
                    System.out.println("Bye!");
                    continua = false;
                    Servidor.disconnect();
                }

            }
        } catch (InterruptedIOException iioe) {
            continua = false;
            System.out.println("Timexout! removendo cliente do servidor...");
        } catch (IOException ex) {
            System.out.println("Erro IO");
            continua = false;
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            continua = false;
            ex.printStackTrace();
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try {
            dos.close();
            dis.close();
            socket.close();
            Servidor.disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
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
            Servidor.addCodigo(novo);
        } catch (FileNotFoundException ex) {
            System.out.println("Falha de arquivo(" + nome + ") nao encontrado ao ser enviado.");
        } catch (IOException ex) {
            System.out.println("Falha de não conseguir ler o arquivo(" + nome + ")");
        }

    }

}
