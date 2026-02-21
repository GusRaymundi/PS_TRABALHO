package montador;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Ligador {
    
    private Map<String, SimboloGlobal> tabelaGlobal = new HashMap<>();
    private List<Modulo> modulos = new ArrayList<>();
    private int enderecoCarga;
    private boolean temErro = false;

    private enum TipoEnderecamento {
        IMEDIATO, INDIRETO, DIRETO, PC_RELATIVE, BASE_RELATIVE, ABSOLUTO, DESCONHECIDO
    }

    private static class SimboloGlobal {
        String nome;
        int enderecoAbsoluto;
        String moduloDefinidor;
        List<String> modulosReferenciadores = new ArrayList<>();
        
        SimboloGlobal(String nome, int endereco, String modulo) {
            this.nome = nome;
            this.enderecoAbsoluto = endereco;
            this.moduloDefinidor = modulo;
        }
    }

    private static class Modulo {
        String nome;
        int enderecoInicial;
        int tamanho;
        List<Instrucao> instrucoes = new ArrayList<>();
        Map<String, Integer> simbolosDefinidos = new HashMap<>();
        // Mapa de Simbolo -> Lista de Offsets em Bytes onde ele é usado
        Map<String, List<Integer>> referenciasExternas = new HashMap<>();
        
        Modulo(String nome) {
            this.nome = nome;
        }
    }

    private static class Instrucao {
        String codigoHex;
        int enderecoAbsoluto; 
        int enderecoRelativo;
        int tamanho;
        String formato;
        boolean precisaRelocar;
        String simboloReferenciado;
        TipoEnderecamento tipoEnd;

        Instrucao(String codigo, int endRelativo) {
            this.codigoHex = codigo;
            this.enderecoRelativo = endRelativo;
            this.tamanho = (codigo.length() == 8) ? 4 : 3;
            this.formato = (codigo.length() == 8) ? "4" : "3";
            this.precisaRelocar = false;
            this.tipoEnd = TipoEnderecamento.DESCONHECIDO;
        }
    }

    public Ligador(int enderecoCarga) {
        this.enderecoCarga = enderecoCarga;
    }

    private void analisarInstrucao(Instrucao inst, Modulo modulo) {
        int offsetAlvo = inst.enderecoRelativo + 1;

        for (Map.Entry<String, List<Integer>> entry : modulo.referenciasExternas.entrySet()) {
            if (entry.getValue().contains(offsetAlvo)) {
                inst.simboloReferenciado = entry.getKey();
                inst.precisaRelocar = true;
                return;
            }
        }

        long valor = Long.parseLong(inst.codigoHex, 16);
        if (inst.formato.equals("3")) {
            int ni = (int)((valor >> 16) & 0x3);
            int xbpe = (int)((valor >> 12) & 0xF);
            boolean p = ((xbpe >> 1) & 0x1) == 1;
            boolean b = ((xbpe >> 2) & 0x1) == 1;
            
            if (!p && !b && (ni == 3)) inst.precisaRelocar = true;
        } else {
            inst.precisaRelocar = true; 
        }
    }

    public void primeiraPassagem(List<String> arquivosObjeto) throws IOException {
        int enderecoAtual = enderecoCarga;

        for (String caminho : arquivosObjeto) {
            Modulo modulo = new Modulo(caminho);
            List<String> linhas = Files.readAllLines(Paths.get(caminho));
            String secao = "";
            int bytesAcumulados = 0;

            for (String linha : linhas) {
                linha = linha.trim();
                if (linha.isEmpty() || linha.startsWith(".")) continue;

                if (linha.startsWith("[")) {
                    secao = linha;
                    continue;
                }

                switch (secao) {
                    case "[DEF]":
                        String[] d = linha.split(":");
                        modulo.simbolosDefinidos.put(d[0], Integer.parseInt(d[1], 16));
                        break;
                    case "[REF]":
                        String[] r = linha.split(":");
                        String sym = r[0];
                        int off = Integer.parseInt(r[1], 16);
                        modulo.referenciasExternas.computeIfAbsent(sym, k -> new ArrayList<>()).add(off);
                        break;
                    case "[CODE]":
                        Instrucao inst = new Instrucao(linha, bytesAcumulados);
                        modulo.instrucoes.add(inst);
                        bytesAcumulados += inst.tamanho;
                        break;
                }
            }

            modulo.tamanho = bytesAcumulados;
            modulo.enderecoInicial = enderecoAtual;

            for (Map.Entry<String, Integer> entry : modulo.simbolosDefinidos.entrySet()) {
                String nome = entry.getKey();
                int abs = modulo.enderecoInicial + entry.getValue();
                if (tabelaGlobal.containsKey(nome)) {
                    temErro = true;
                } else {
                    tabelaGlobal.put(nome, new SimboloGlobal(nome, abs, modulo.nome));
                }
            }

            for (Instrucao i : modulo.instrucoes) analisarInstrucao(i, modulo);

            modulos.add(modulo);
            enderecoAtual += modulo.tamanho;
        }

        if (temErro) throw new RuntimeException("Erro na ligação: Símbolos duplicados.");
    }

    public List<String> segundaPassagem() {
        List<String> codigoFinal = new ArrayList<>();

        for (Modulo modulo : modulos) {
            int pcModulo = modulo.enderecoInicial;

            for (Instrucao inst : modulo.instrucoes) {
                inst.enderecoAbsoluto = pcModulo;
                String hexRelocado = relocarInstrucao(inst, modulo);
                codigoFinal.add(hexRelocado);
                pcModulo += inst.tamanho;
            }
        }
        return codigoFinal;
    }

    private String relocarInstrucao(Instrucao inst, Modulo modulo) {
        long valor = Long.parseLong(inst.codigoHex, 16);

        if (inst.formato.equals("4")) {
            long opcodeFlags = valor & 0xFFF00000L;
            int enderecoOriginal = (int)(valor & 0xFFFFF);
            int novoEnd;

            if (inst.simboloReferenciado != null) {
                SimboloGlobal sg = tabelaGlobal.get(inst.simboloReferenciado);
                if (sg != null) {
                    novoEnd = sg.enderecoAbsoluto;
                } else {
                    System.err.println("⚠ Símbolo não encontrado: " + inst.simboloReferenciado);
                    novoEnd = enderecoOriginal + modulo.enderecoInicial;
                }
            } else if (inst.precisaRelocar) {
                novoEnd = enderecoOriginal + modulo.enderecoInicial;
            } else {
                return inst.codigoHex;
            }
            return String.format("%08X", (opcodeFlags | (novoEnd & 0xFFFFF)));
        } 
        else {
            int opcodeFlags = (int)(valor & 0xFFF000);
            int enderecoOriginal = (int)(valor & 0xFFF);
            if (inst.simboloReferenciado != null) {
                SimboloGlobal sg = tabelaGlobal.get(inst.simboloReferenciado);
                if (sg != null) {
                    return String.format("%06X", (opcodeFlags | (sg.enderecoAbsoluto & 0xFFF)));
                } else {
                    System.err.println("⚠ Símbolo não encontrado: " + inst.simboloReferenciado);
                    return String.format("%06X", (opcodeFlags | ((enderecoOriginal + modulo.enderecoInicial) & 0xFFF)));
                }
            } else if (inst.precisaRelocar) {
                return String.format("%06X", (opcodeFlags | ((enderecoOriginal + modulo.enderecoInicial) & 0xFFF)));
            } else {
                return inst.codigoHex;
            }
        }
    }

    public void salvarArquivoFinal(List<String> codigo, String nomeArquivo) throws IOException {
        try (PrintWriter writer = new PrintWriter(new File(nomeArquivo), "UTF-8")) {
            writer.println("; Arquivo executável SIC/XE");
            int endereco = enderecoCarga;
            for (String linha : codigo) {
                writer.println(String.format("%04X", endereco) + "  " + linha);
                endereco += (linha.length() == 8) ? 4 : 3;
            }
        }
        System.out.println("✅ Executável '" + nomeArquivo + "' gerado com sucesso.");
    }

    public void exibirTabelaGlobal() {
        System.out.println("\n🌐 Tabela Global de Símbolos:");
        for (Map.Entry<String, SimboloGlobal> entry : tabelaGlobal.entrySet()) {
            SimboloGlobal s = entry.getValue();
            System.out.printf("   %-10s = 0x%04X (definido em %s)\n",
                            s.nome, s.enderecoAbsoluto, s.moduloDefinidor);
        }
    }

}
