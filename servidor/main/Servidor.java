package servidor.main;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import servidor.model.Codigo;
import servidor.model.Contato;
import servidor.model.Execucao;
import servidor.model.Protocolo;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Desktop
 * Classe Servidor e Método Main principal
 * Aqui ficam armazenados numa ArrayList os Contatos conectados ao servidor
 * Estes metodos são todos implementados com o modificador STATIC pois o Conjunto de contatos
 * é único, e deve ser compartilhado entre Threads que tratam a conexão com o cliente.
 * 
 */
public class Servidor {
    
    private static ArrayList<Contato> contatos;
    private static boolean conectado;
    private static Map<String,Codigo> codigos;
    private static int estadoCodigos;
    private static Map<String,Execucao> cache;
    
    public static void addCodigo(Codigo c)
    {
        if(codigos==null)
        {
            codigos = new HashMap<String,Codigo>();
            estadoCodigos = 0;
        }
        estadoCodigos--;
        codigos.put(c.getMd5(),c);
        if(cache==null)
        {
            cache = new HashMap<String,Execucao>();
        }
        cache.put(c.getMd5(),new Execucao(c.getMd5()));
    }
    
    public static void addCache(String md5,String parametros, String resultado)
    {
        if(cache.containsKey(md5))
        {
            Execucao exc = cache.get(md5);
            exc.addExecucao(parametros, resultado);
        }
    }
    
    public static String getCache(String md5, String parametros)
    {
        if(cache.containsKey(md5))
        {
            Execucao exc = cache.get(md5);
            if(exc.temNoCache(parametros))
            {
                return exc.getCache(parametros);
            }
            else
            {
                return "Nao existe no cache.";
            }
        }
        else
            return "Nao existe esse codigo";
    }
   
    public static int getEstadoCodigos()
    {
        return estadoCodigos;
    }
    
    
    public static void setEstadoCodigos(int x)
    {
        estadoCodigos = x;
    }
    
    public static Codigo getCodigo(String md5)
    {
        if(codigos==null)
        {
             codigos = new HashMap<String,Codigo>();
             estadoCodigos = 0;
        }
        return codigos.get(md5);
    }
    
    public static Map<String,Codigo> getListaCodigos()
    {
        if(codigos==null)
        {
             codigos = new HashMap<String,Codigo>();
             estadoCodigos = 0;
        }
        return codigos;
    }
    
    public static boolean possuiCodigo(String md5)
    {
        if(codigos==null)
        {
             codigos = new HashMap<String,Codigo>();
             estadoCodigos = 0;
        }
        return codigos.containsKey(md5);
            
    }
    
    public static void removeCodigo(String md5)
    {
        if(codigos==null)
        {
             codigos = new HashMap<String,Codigo>();
             estadoCodigos = 0;
        }
        estadoCodigos--;
        codigos.remove(md5);
    }
    
    public static void addContato(Contato c)
    {
        if(contatos==null) // Sempre tomar cuidado para nao utilizar uma arraylist não iniciada.
        {
             contatos = new ArrayList<Contato>();
        }
        contatos.add(c);
    }
    
    
    public static ArrayList<Contato> enviaListaContato(Contato c)
    {
        if(contatos==null)
        {
             contatos = new ArrayList<Contato>();
        }
        return contatos;
    }
    
    public static int qtdContatos()
    {
        if(contatos==null)
        {
             contatos = new ArrayList<Contato>();
        }
        return contatos.size();
    }
    
    public static Contato getContato(int i)
    {
        if(contatos==null)
        {
             contatos = new ArrayList<Contato>();
        }
        return contatos.get(i);
    }
    
    public static void removeContato(Contato c)
    {
        if(contatos==null)
        {
             contatos = new ArrayList<Contato>();
        }
        contatos.remove(c);
    }

    public static boolean isConectado() {
        return conectado;
    }

    public static void disconnect() {
        Servidor.conectado = false;
    }
    
    
    public static void main(String args[])
    {
        conectado = true;
        System.out.println("Servidor do Redes Messenger");
        
        Scanner teclado = new Scanner(System.in);
        String nomeServidor, ipMid;
        int portaServidor, portMid;
        try
        {
            
        System.out.println("Digite o nome deste servidor: ");
        nomeServidor = teclado.nextLine();
        System.out.println("Digite o IP do middleware: ");
        ipMid = teclado.nextLine();
        System.out.println("Digite a porta deste servidor: ");
        portaServidor = teclado.nextInt();
        
       // Classe Java que implementa um socket de servidor 
       // Um socket de servidor espera uma conexão na porta definida pelo protocolo (Porta escolhida: 22014)
       // Quando chega uma conexão nessa porta, a própria classe se encarrega de criar um novo socket numa porta aleatoria do servidor
       // para desocupar a porta de espera de conexões.
        File folder = new File(Paths.get("").toAbsolutePath().toString());
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
        if (file.isFile()) {
                if(file.getName().contains(".js"))
                {
                    System.out.println(file.getName());
                    FileInputStream fis = new FileInputStream(file);
                    String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis); // Gerar MD5
                    fis.close();
                    if(!possuiCodigo(md5))
                    {
                        Codigo novo = new Codigo(file.getName(),md5);
                        Servidor.addCodigo(novo);
                    }
                    
                }
                
            }
        }
        ServerSocket socketServ = new ServerSocket(portaServidor); 
        Socket conexao;
        ConexaoMiddleware cm = new  ConexaoMiddleware(nomeServidor, portaServidor, ipMid, Protocolo.PortaServidor);
        new Thread(cm).start();
        
        
        while(conectado)
        {
            System.out.println("Esperando Conexão em: "+ portaServidor);
            conexao = socketServ.accept();
            ConexaoContato c = new ConexaoContato(conexao); // ConexaoContato é a classe que cuida de cada conexão entre cliente e servidor
            new Thread(c).start(); //Ela é tratada com uma Thread pois existem varias conexões entre clientes e servidor e elas devem ser independentes.
        }
        }catch(IOException e)
        {
            System.out.println("Porta já utilizada. =/");
        }
    }


    
    
    
    
    
}
