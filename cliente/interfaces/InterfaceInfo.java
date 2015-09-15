/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cliente.interfaces;

import java.util.ArrayList;
import cliente.model.Codigo;
import cliente.model.Contato;
import cliente.model.Tarefa;

/**
 *
 * @author Desktop
 * A utlização de Interface é padronizar a troca de informações entre a parte do programa que recebe e envia mensagens pela rede
 * Com a parte que mostra para o usuario.
 * 
 * Para exibir as informacoes basta criar uma classe que implemente essa Interface e associar ela com o cliente desejado.
 * 
 */
public interface InterfaceInfo {
    
    public Contato getContato();
    
    public void addTarefa(Tarefa t);
    
    public boolean temTarefa();
    
    public Tarefa getProxTarefa();
    
    public void atualizaContatos(ArrayList<Contato> c);
    
    public void atualizaCodigos(ArrayList<Codigo> c);
    
    public String getIpServidor();
    
    public void setNomeServidor(String nome);
    
    public void setContato(Contato c);
    
    public void updateTerminal(String texto);
    
    public void limpaTarefas();
    
    public int preparaConexaoCliente(); // Todo cliente deve tambem ser um servidor UDP que vai esperar uma conexao em alguma porta
    // Não posso definir qual porta é essa pois cada computador tem portas livres diferentes
    // O que faço é quando crio uma conexao UDP (DatagramSocket) passo o parametro 0 e ai o Java procura uma porta livre e me entrega
    // O objetivo dessa funcao é deixar o servidor sabendo qual é essa porta livre para entregar a todos os clientes que quiserem se comunicar
            
    
    
}
