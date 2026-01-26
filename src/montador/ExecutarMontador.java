package montador;

import java.nio.file.*;
import java.util.*;

public class ExecutarMontador {
    public static void main(String[] args) {
        try {
            // 1. Defina o caminho do seu arquivo Assembly
            Path caminhoAsm = Paths.get("programa.asm");

            // 2. Leia as linhas do arquivo
            List<String> linhasFonte = Files.readAllLines(caminhoAsm);

            // 3. Instancie o Montador
            Montador montador = new Montador(linhasFonte);

            // 4. Execute as duas passagens
            System.out.println("Iniciando Primeira Passagem ..");
            montador.primeiraPassagem();

            System.out.println("Iniciando Segunda Passagem ..");
            List<String> codigoHex = montador.segundaPassagem();

            // 5. Salve o resultado no arquivo que o Simuilador lê
            Files.write(Paths.get("programa.txt"), codigoHex);

            System.out.println("Montagem concluída com sucesso! Arquivo 'programa.txt gerado.");
        } catch (Exception e) {
            System.err.println("Erro durante a montagem: " + e.getMessage());
            e.printStackTrace();
        }
    }
}