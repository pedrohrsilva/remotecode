/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package servidor.model;

/**
 *
 * @author Desktop
 */
public class Contato {
    protected String nome;
    protected String ip;
    protected int porta; //Porta que este cliente vai ficar esperando dados.

    public Contato(String nome, String ip, int porta) {
        this.nome = nome;
        this.ip = ip;
        this.porta = porta;
    }

    public Contato() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPorta() {
        return porta;
    }

    public void setPorta(int porta) {
        this.porta = porta;
    }

    @Override
        public String toString() {
        return nome;
    }
    
    public boolean equals(Contato c)
    {
        return nome.equals(c.getNome()) && ip.equals(c.getIp()) && porta == c.getPorta();
    }
    
    
    
    
}
