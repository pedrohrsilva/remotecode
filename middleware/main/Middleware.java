package middleware.main;


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
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
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
 * Classe Servidor e Método Main principal
 * Aqui ficam armazenados numa ArrayList os Contatos conectados ao servidor
 * Estes metodos são todos implementados com o modificador STATIC pois o Conjunto de contatos
 * é único, e deve ser compartilhado entre Threads que tratam a conexão com o cliente.
 * 
 */
public class Middleware {
    
    private static ArrayList<RegistroServidor> servidores;
    private static Map<String,Codigo> codigos;
    private static int estadoCodigos;
    
    public static void addCodigo(Codigo c)
    {
        if(codigos==null)
        {
            codigos = new HashMap<String,Codigo>();
            estadoCodigos = 0;
        }
        estadoCodigos++;
        codigos.put(c.getMd5(),c);
        
    }
    
    public static int getEstadoCodigos()
    {
        return estadoCodigos;
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
    
    public static boolean possuiCodigo(String md5)
    {
        if(codigos==null)
        {
             codigos = new HashMap<String,Codigo>();
             estadoCodigos = 0;
        }
        return codigos.containsKey(md5);
            
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
    
    public static void removeCodigo(String md5)
    {
        if(codigos==null)
        {
             codigos = new HashMap<String,Codigo>();
             estadoCodigos = 0;
        }
        estadoCodigos++;
        codigos.remove(md5);
    }
    
    public static void addServidor(RegistroServidor c)
    {
        if(servidores==null) // Sempre tomar cuidado para nao utilizar uma arraylist não iniciada.
        {
             servidores = new ArrayList<RegistroServidor>();
        }
        servidores.add(c);
    }
    
    
    public static ArrayList<RegistroServidor> enviaListaServidor(RegistroServidor c)
    {
        if(servidores==null)
        {
             servidores = new ArrayList<RegistroServidor>();
        }
        return servidores;
    }
    
    public static int qtdServidores()
    {
        if(servidores==null)
        {
             servidores = new ArrayList<RegistroServidor>();
        }
        return servidores.size();
    }
    
    public static RegistroServidor getServidor(int i)
    {
        if(servidores==null)
        {
             servidores = new ArrayList<RegistroServidor>();
        }
        return servidores.get(i);
    }
    
    public static void removeServidor(RegistroServidor c)
    {
        if(servidores==null)
        {
             servidores = new ArrayList<RegistroServidor>();
        }
        servidores.remove(c);
    }
    
    public static void main(String args[]) throws IOException
    {
        Logger logger = Logger.getLogger("Middleware");  
        logger.setLevel(Level.ALL);
        FileHandler fh;  
          
        try {  
              
            // This block configure the logger with handler and formatter  
            fh = new FileHandler("logs.log");  
            logger.addHandler(fh);  
            //logger.setLevel(Level.ALL);  
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
              
            // the following statement is used to log any messages   
              
        } catch (SecurityException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  

        System.out.println("Servidor do Redes Messenger");
        System.out.println("Esperando Conexão em: "+ Protocolo.PortaServidor);
        try
        {
       // Classe Java que implementa um socket de servidor 
       // Um socket de servidor espera uma conexão na porta definida pelo protocolo (Porta escolhida: 22014)
       // Quando chega uma conexão nessa porta, a própria classe se encarrega de criar um novo socket numa porta aleatoria do servidor
       // para desocupar a porta de espera de conexões.
        ServerSocket socketServ = new ServerSocket(Protocolo.PortaServidor); 
        Socket conexao;
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
                        Middleware.addCodigo(novo);
                    }
                    
                }
                
            }
        }

        while(true)
        {
            conexao = socketServ.accept();
            Logger.getLogger("Middleware").info("Conexao Aceita: "+conexao.toString());
            EntregaServidor c = new EntregaServidor(conexao); // ConexaoContato é a classe que cuida de cada conexão entre cliente e servidor
            new Thread(c).start(); //Ela é tratada com uma Thread pois existem varias conexões entre clientes e servidor e elas devem ser independentes.
        }
        }catch(IOException e)
        {
            System.out.println("Porta já utilizada. =/");
        }
    }


    /**
     *  O Melhor servidor é aquele que tem menos usuários online;
     * @return 
     */
    static RegistroServidor getBestServidor() {
        Iterator it = servidores.iterator();
        int menor = 999;
        RegistroServidor rsMenos = null;
        while(it.hasNext())
        {
           RegistroServidor atual = (RegistroServidor) it.next();
           if(atual.getClientesOnline() < menor)
           {
               menor = atual.getClientesOnline();
               rsMenos = atual;
           }
        }
        return rsMenos;
    }


    
    
    
    
    
}
