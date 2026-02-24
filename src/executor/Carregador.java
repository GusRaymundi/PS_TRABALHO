package executor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Carregador {
    private Maquina maquina;

    public Carregador(Maquina maquina) {
        this.maquina = maquina;
    }

    // checa se o executavel precisa de relocacao na carga
    public boolean isModoSimples(String caminhoArquivo) throws IOException {
        List<String> linhas = Files.readAllLines(Paths.get(caminhoArquivo));
        if (linhas.isEmpty()) throw new IOException("Arquivo executavel esta vazio!");
        return linhas.get(0).contains("SIMPLES");
    }

    // le o executavel e joga na memoria, retorna endereco inicial pro PC
    public int carregar(String caminhoArquivo, int deslocamentoRelocacao) throws IOException {
        List<String> linhas = Files.readAllLines(Paths.get(caminhoArquivo));
        if (linhas.isEmpty()) throw new IOException("Arquivo executavel esta vazio!");

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

    // sobrecarga sem deslocamento
    public int carregar(String caminhoArquivo) throws IOException {
        return carregar(caminhoArquivo, 0);
    }
}