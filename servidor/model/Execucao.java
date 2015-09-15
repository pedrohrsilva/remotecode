/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package servidor.model;

import java.util.HashMap;

/**
 *
 * @author psilva
 */
public class Execucao {
    private HashMap<String,String> historico;
    private String md5;

    public Execucao(String md5) {
        this.md5 = md5;
        historico = new HashMap<String,String>();
    }
    
    public void addExecucao(String parametros, String resultado)
    {
        historico.put(parametros, resultado);
    }
    
    public String getCache(String parametros)
    {
       return historico.get(parametros);
    }
    
    public boolean temNoCache(String parametros)
    {
        return historico.containsKey(parametros);
    }
    
    
    
}
