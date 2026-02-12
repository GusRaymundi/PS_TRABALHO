package macro;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MainMacro {

  public static void main (String[] args) {
    
    if (args.length == 0){
      System.out.println("Uso: java macro.MainMacro <arquivo.asm>");
      return;
    }

    String nomeArquivo = args[0];
    System.out.println("Arquivo informado: " + nomeArquivo);
    System.out.println("Lendo arquivo...");

    try {
        BufferedReader reader = new BufferedReader( new FileReader(nomeArquivo) );
        String linha;

        Map<String, Macro> tabelaMacros = new HashMap<>();
        Macro macroAtual = null;
        boolean dentroDaMacro = false;

        while (( linha = reader.readLine()) != null ) {
            linha = linha.trim();

            if (linha.isEmpty() || linha.startsWith(";")) {
               continue;
            }

            //inicio da Macro
            if (linha.contains(" MACRO ")) {
              String[] partes = linha.split("\\s+");
              String nomeMacro = partes[0];

              macroAtual = new Macro(nomeMacro);

              //Parâmetros começam na posição 2
              for (int i = 2; i < partes.length; i++) {
                macroAtual.adicionarParametro(partes[i]);
              }

              dentroDaMacro = true;
              System.out.println("[INICIO MACRO] " + nomeMacro);
              continue;
            }

          // FIM DA MACRO

          if (linha.equals("MEND")) {
            tabelaMacros.put(macroAtual.getNome(), macroAtual);
            System.out.println("[FIM MACRO]" + macroAtual.getNome());

            macroAtual = null;
            dentroDaMacro= false;
            continue;
          }   

          //linha dentro da macro
          if (dentroDaMacro) {
            macroAtual.adicionarLinha(linha);
            System.out.println("[LINHA MACRO]" + linha);
            continue;
          }

          //linha normal 
          System.out.println("[LINHA NORMAL]" + linha);
          

       }
        reader.close();

    } catch (IOException e) {
      System.out.println("Erro ao ler o arquivo: " + e.getMessage());
    }



  }
}