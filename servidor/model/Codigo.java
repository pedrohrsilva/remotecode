/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package servidor.model;

/**
 *
 * @author psilva
 */
public class Codigo {
    private String path;
    private String md5;
    private String fullPath;

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }
    
    public Codigo(String path,String md5) {
        this.path = path;
        this.md5 = md5;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
    
    public String toString()
    {
        return path;
    }
    
    
    
    
}
