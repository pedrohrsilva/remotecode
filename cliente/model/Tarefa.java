/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cliente.model;

/**
 *
 * @author psilva
 */
public class Tarefa {
    private int protocolo;
    private Codigo codigo;
    private String parametros;

    public Tarefa(int protocolo, Codigo codigo) {
        this.protocolo = protocolo;
        this.codigo = codigo;
    }
    
    public Tarefa(Codigo codigo)
    {
        this.codigo = codigo;
    }

    public String getParametros() {
        return parametros;
    }

    public void setParametros(String parametros) {
        this.parametros = parametros;
    }
    
    

    public int getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(int protocolo) {
        this.protocolo = protocolo;
    }

    public Codigo getCodigo() {
        return codigo;
    }

    public void setCodigo(Codigo codigo) {
        this.codigo = codigo;
    }
    
    public void setDownload()
    {
        this.protocolo = Protocolo.DOWNLOAD;
    }
    
    public void setUpload()
    {
        this.protocolo = Protocolo.UPLOAD;
    }
    
    public void setExecutar()
    {
        this.protocolo = Protocolo.EXECUTAR;
    }
}
