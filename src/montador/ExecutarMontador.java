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
                nomeArquivo = "programa.asm"; // default se não passar nada
            }
            
            System.out.println("📄 Montando arquivo: " + nomeArquivo);
            
            // 1. Caminho do arquivo Assembly (AGORA USA O ARGUMENTO!)
            Path caminhoAsm = Paths.get(nomeArquivo);

            // 2. Leia as linhas do arquivo
            List<String> linhasFonte = Files.readAllLines(caminhoAsm);

            // 3. Instancie o Montador
            Montador montador = new Montador(linhasFonte);

            // 4. Execute as duas passagens
            System.out.println("Iniciando Primeira Passagem...");
            montador.primeiraPassagem();

            System.out.println("Iniciando Segunda Passagem...");
            List<String> codigoHex = montador.segundaPassagem();

            // =============================================
            // MUDANÇA AQUI: usar SEU método, não o antigo
            // =============================================
            
            // Opção 1: Manter os dois formatos (para compatibilidade)
            Files.write(Paths.get("programa.txt"), codigoHex); // para o simulador
            String nomeBase = nomeArquivo.replace(".asm", "");
            String nomeObj = nomeBase + ".obj";
            montador.salvarArquivoObjeto(nomeObj, codigoHex);
            
            System.out.println("✅ programa.txt gerado (formato simulador)");
            System.out.println("✅ objetos/programa.obj gerado (formato ligador)");
            
            // Opção 2: Se quiser só o formato do ligador (recomendado)
            // montador.salvarArquivoObjeto("programa.obj", codigoHex);
            // System.out.println("✅ objetos/programa.obj gerado");

        } catch (Exception e) {
            System.err.println("Erro durante a montagem: " + e.getMessage());
            e.printStackTrace();
        }
    }
}