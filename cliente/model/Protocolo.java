/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cliente.model;

/**
 *
 * @author Desktop
 */
public class Protocolo{
    
    public static final int PedirConexao = 1; // Envia para o Servidor para avisar que o cliente quer conexão
    public static final int ConexaoAceita = 1; // Envia para o Cliente para avisar que a conexão foi aceita
    public static final int Ping = 2; // Só para verificar se o cliente ainda está conectado
    public static final int Pong = 2; // Só para avisar que o servidor tambem está conectado
    public static final int Desconectar = 3; // Mensagem enviada para finalizar conexão -- Nenhuma resposta é enviada.
    public static final int CodigoInesperado = 0; //Se espera alguma coisa e recebe outra envia codigoInsesperado.
    public static final int PortaServidor = 22014; // Escolhida aleatoriamente (02/2014)
    public static final int PedirEndereco=4;
    public static final int EnviarLog = 9; //Mensagem de Log
    public static final int EnviandoEndereco = 4;
    public static final int TrocandoCodigos = 5;
    public static final int PedirCodigos = 5;
    public static final int FimCodigos = 99;
    public static final int EnviandoCodigo = 55;
    public static final int IntervaloCodigo = 10; // A cada IntervaloCodigo é enviado a lista dos codigos instalados no servidor
    public static final int UPLOAD = 11;
    public static final int DOWNLOAD = 12;
    public static final int EXECUTAR = 13;
    
}
