package executor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Carregador {
    private Maquina maquina; 

    public Carregador(Maquina maquina) {
        this.maquina = maquina;
    }

    public int carregar(String caminhoArquivo) throws IOException {
        List<String> linhas = Files.readAllLines(Paths.get(caminhoArquivo));
        if (linhas.isEmpty()) throw new IOException("Arquivo executável está vazio!");

        String primeiraLinha = linhas.get(0);
        boolean modoSimples = primeiraLinha.contains("SIMPLES");
        int deslocamentoRelocacao = 0;

        if (modoSimples) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("MODO SIMPLES: Digite o endereço de carga (Hex): ");
            String entrada = scanner.nextLine().trim();
            deslocamentoRelocacao = Integer.parseInt(entrada, 16);
        }

        System.out.println("Carregando programa na memória...");
        int enderecoInicialParaPC = -1;

        for (int i = 1; i < linhas.size(); i++) {
            String linha = linhas.get(i).trim();
            if (linha.isEmpty()) continue;

            String[] partes = linha.split("\\s+");
            int enderecoOriginal = Integer.parseInt(partes[0], 16);
            String codigoHex = partes[1];
            
            int enderecoReal = enderecoOriginal + deslocamentoRelocacao;
            
            if (enderecoInicialParaPC == -1) enderecoInicialParaPC = enderecoReal;
            maquina.carregarProgramaHex(Collections.singletonList(codigoHex), enderecoReal);
        }
        
        return enderecoInicialParaPC;
    }
}