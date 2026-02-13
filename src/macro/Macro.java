package macro;

import java.util.ArrayList;
import java.util.List;


public class Macro {

    private String nome;
    private List<String> parametros;
    private List<String> corpo;

    public Macro(String nome) {
      this.nome = nome;
      this.parametros = new ArrayList<>();
      this.corpo = new ArrayList<>();
    }

    public String getNome(){
      return nome;
    }

    public List<String> getParametros(){
      return parametros;
    }

    public void adicionarParametro(String parametro) {
      parametros.add(parametro);
   }

    public List<String> getCorpo(){
      return corpo;
    }

    public void adicionarLinha(String linha) {
      corpo.add(linha);
    }
}