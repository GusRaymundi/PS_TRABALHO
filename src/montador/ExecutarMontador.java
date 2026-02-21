package montador;

import java.nio.file.*;
import java.util.*;

public class ExecutarMontador {
    public static void main(String[] args) {
        try {
            String nomeArquivo;
            if (args.length > 0) {
                nomeArquivo = args[0];
            } else {
                nomeArquivo = "programa.asm"; 
            }
            
            System.out.println("📄 Montando arquivo: " + nomeArquivo);
            
            Path caminhoAsm = Paths.get(nomeArquivo);
            List<String> linhasFonte = Files.readAllLines(caminhoAsm);
            Montador montador = new Montador(linhasFonte);
            
            System.out.println("Iniciando Primeira Passagem...");
            montador.primeiraPassagem();

            System.out.println("Iniciando Segunda Passagem...");
            List<String> codigoHex = montador.segundaPassagem();

            Files.write(Paths.get("programa.txt"), codigoHex);
            String nomeBase = nomeArquivo.replace(".asm", "");
            String nomeObj = nomeBase + ".obj";
            montador.salvarArquivoObjeto(nomeObj, codigoHex);
            
            System.out.println("✅ programa.txt gerado (formato simulador)");
            System.out.println("✅ objetos/programa.obj gerado (formato ligador)");

        } catch (Exception e) {
            System.err.println("Erro durante a montagem: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
