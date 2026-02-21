package montador;

import executor.Opcode;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Montador {
    private int valorRegistradorB = 0;
    private int locctr;
    private final TabelaSimbolos tabelaSimbolos;
    private final List<String> linhasCodigoFonte;
    private List<String> registrosDeReferencia = new ArrayList<>();

    private static class Flags {
        int n, i, x, b, p, e;
        int disp;
        String operandoLimpo;
        String operandoOriginal;
    }

    public Montador(List<String> codigoFonte) {
        this.linhasCodigoFonte = codigoFonte;
        this.tabelaSimbolos = new TabelaSimbolos();
        this.locctr = 0;
    }

    public void primeiraPassagem() {
        locctr = 0;
        for (String linha : linhasCodigoFonte) {
            if (linha.trim().isEmpty() || linha.startsWith("."))
                continue;

            String[] partes = organizarPartes(linha);
            String rotulo = partes[0];
            String instrucao = partes[1];

            if (!rotulo.isEmpty()) {
                tabelaSimbolos.adicionar(rotulo, locctr);
            }

            if (instrucao.equals("BASE"))
                continue;

            atualizarLocctr(linha, partes);
        }
    }

    public List<String> segundaPassagem() {
        List<String> codigoObjeto = new ArrayList<>();
        locctr = 0;
        registrosDeReferencia.clear(); 

        for (String linha : linhasCodigoFonte) {
            if (linha.trim().isEmpty() || linha.startsWith("."))
                continue;

            String[] partes = organizarPartes(linha);
            String instrucao = partes[1];
            String operando = partes[2];

            System.out.println("📄 LINHA[" + locctr + "]: '" + linha + "'");
            System.out.println("   instrucao: '" + instrucao + "'");
            System.out.println("   operando: '" + operando + "'");

            if (instrucao.equals("BASE")) {
                Integer addr = tabelaSimbolos.obterEndereco(operando);
                if (addr != null)
                    valorRegistradorB = addr;
                continue;
            }
            if (instrucao.equals("RESW")) {
                atualizarLocctr(linha, partes);
                continue;
            }
            if (instrucao.equals("WORD")) {
                int valorConstante = Integer.parseInt(operando);
                codigoObjeto.add(String.format("%06X", valorConstante & 0xFFFFFF));
                atualizarLocctr(linha, partes);
                continue;
            }

            int hex = gerarCodigoHex(instrucao, operando);
            String formatOutput = (instrucao.startsWith("+")) ? "%08X" : "%06X";
            codigoObjeto.add(String.format(formatOutput, hex & (instrucao.startsWith("+") ? 0xFFFFFFFFL : 0xFFFFFF)));

            atualizarLocctr(linha, partes);
        }
        return codigoObjeto;
    }

    private boolean linhaTemRotulo(String linha) {
        if (linha == null || linha.trim().isEmpty()) return false;
        char primeiroChar = linha.charAt(0);
        return primeiroChar != ' ' && primeiroChar != '\t';
    }

    private int gerarCodigoHex(String mnnemonico, String operando) {
        if (mnnemonico.equals("RSUB")) {
            return buscarOpcode("RSUB") << 16; 
        }
        
        int opcodeBase = buscarOpcode(mnnemonico);
        Flags f = new Flags();

        f.e = mnnemonico.startsWith("+") ? 1 : 0;
        processarFlagsEnderecamento(operando, f);
        Integer enderecoDestino = null;
        
        if (f.operandoLimpo != null && !f.operandoLimpo.isEmpty()) {
            enderecoDestino = tabelaSimbolos.obterEndereco(f.operandoLimpo);
        }
        if (enderecoDestino == null) {
            if (f.operandoLimpo != null && !f.operandoLimpo.isEmpty()) {
                try {
                    enderecoDestino = Integer.parseInt(f.operandoLimpo);
                } catch (NumberFormatException ex) {
                    if (f.operandoOriginal != null && !f.operandoOriginal.isEmpty()) {
                        String nomeSimbolo = f.operandoOriginal.replace("#", "")
                                                            .replace("@", "")
                                                            .replace(",X", "");
                        System.out.println("🔍 Referência externa: " + nomeSimbolo + " na posição " + String.format("%04X", locctr));
                        registrosDeReferencia.add(nomeSimbolo + ":" + String.format("%04X", locctr));
                    }
                    enderecoDestino = 0;
                }
            } else {
                enderecoDestino = 0;
            }
        }

        int proximoPC = locctr + (f.e == 1 ? 4 : 3);

        if (f.e == 0) { 
            if (f.i == 1 && f.n == 0 && enderecoDestino <= 4095) {
                f.p = 0;
                f.b = 0;
                f.disp = enderecoDestino;
            } else {
                calcularDeslocamento(enderecoDestino, proximoPC, f);
            }
        }
        if (f.e == 0) { 
            int resultado = (opcodeBase << 16);
            resultado |= (f.n << 17) | (f.i << 16);
            resultado |= (f.x << 15) | (f.b << 14) | (f.p << 13) | (f.e << 12);
            resultado |= (f.disp & 0xFFF);
            return resultado;
        } else { 
            long resultadoL = ((long) (opcodeBase & 0xFC) << 24);
            resultadoL |= ((long) f.n << 25) | ((long) f.i << 24);
            resultadoL |= (f.x << 23) | (f.b << 22) | (f.p << 21) | (f.e << 20);
            resultadoL |= (enderecoDestino & 0xFFFFF);
            return (int) resultadoL;
        }
    }

    private void atualizarLocctr(String linha, String[] partes) {
        int idx = linhaTemRotulo(linha) ? 1 : 0;
        String instrucao = partes[idx];

        if (instrucao.equals("START") || instrucao.equals("END") || instrucao.equals("BASE")) {
            locctr += 0; 
        } else if (instrucao.startsWith("+")) {
            locctr += 4;
        } else if (instrucao.equals("RESW")) {
            int quant = Integer.parseInt(partes[idx + 1]);
            locctr += (quant * 3);
        } else if (instrucao.equals("WORD")) {
            locctr += 3;
        } else {
            locctr += 3;
        }
    }

    private void processarFlagsEnderecamento(String operando, Flags flags) {
        flags.operandoOriginal = operando;  
        if (operando.startsWith("#")) {
            flags.n = 0; flags.i = 1;
            operando = operando.substring(1);
        } else if (operando.startsWith("@")) {
            flags.n = 1; flags.i = 0;
            operando = operando.substring(1);
        } else {
            flags.n = 1; flags.i = 1;
        }

        if (operando.endsWith(",X")) {
            flags.x = 1;
            operando = operando.split(",")[0];
        } else {
            flags.x = 0;
        }
        flags.operandoLimpo = operando;
    }

    private void calcularDeslocamento(int enderecoDestino, int proximoPC, Flags flags) {
        int deslocamento = enderecoDestino - proximoPC;
        if (deslocamento >= -2048 && deslocamento <= 2047) {
            flags.p = 1; flags.b = 0;
            flags.disp = deslocamento;
        } else {
            flags.p = 0; flags.b = 1;
            flags.disp = enderecoDestino - valorRegistradorB;
        }
    }

    private String[] organizarPartes(String linha) {
        String semComentario = linha.split("\\.")[0];
        String[] partesBrutas = semComentario.trim().split("\\s+");
        String rotulo = "", instrucao = "", operando = "";

        if (!linha.startsWith(" ") && !linha.startsWith("\t")) {
            rotulo = partesBrutas[0];
            instrucao = partesBrutas.length > 1 ? partesBrutas[1] : "";
            operando = partesBrutas.length > 2 ? partesBrutas[2] : "";
        } else {
            instrucao = partesBrutas.length > 0 ? partesBrutas[0] : "";
            operando = partesBrutas.length > 1 ? partesBrutas[1] : "";
        }
        return new String[] { rotulo, instrucao, operando };
    }

    private int buscarOpcode(String mnemonico) {
        String busca = mnemonico.startsWith("+") ? mnemonico.substring(1) : mnemonico;
        switch (busca.toUpperCase()) {
            case "LDA": return Opcode.LDA;
            case "LDX": return Opcode.LDX;
            case "ADD": return Opcode.ADD;
            case "STA": return Opcode.STA;
            case "RSUB": return Opcode.RSUB;
            case "J": return Opcode.J;
            default: return 0;
        }
    }

    public void salvarArquivoObjeto(String nomeArquivo, List<String> codigoObjeto) {
        try {
            File pasta = new File("objetos");
            if (!pasta.exists()) pasta.mkdir();

            File arquivo = new File(pasta, nomeArquivo);
            try (PrintWriter writer = new PrintWriter(arquivo, "UTF-8")) {
                writer.println("[DEF]");
                for (Map.Entry<String, Integer> entry : tabelaSimbolos.getMapaSimbolos().entrySet()) {
                    writer.println(entry.getKey() + ":" + String.format("%04X", entry.getValue()));
                }

                writer.println("[REF]");
                for (String ref : registrosDeReferencia) {
                    writer.println(ref);
                }

                writer.println("[CODE]");
                for (String hex : codigoObjeto) {
                    writer.println(hex);
                }
            }
            System.out.println("✅ Arquivo objeto salvo: " + arquivo.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Erro ao salvar objeto: " + e.getMessage());
        }
    }

    public TabelaSimbolos getTabelaSimbolos() { return this.tabelaSimbolos; }
    public int getTamanhoPrograma() { return this.locctr; }
}
