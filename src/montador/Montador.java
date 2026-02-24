package montador;

import executor.Opcode;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class Montador {
    private int valorRegistradorB = 0;
    private int locctr;
    private int enderecoInicial = 0;
    private final TabelaSimbolos tabelaSimbolos;
    private final List<String> linhasCodigoFonte;
    private List<String> registrosDeReferencia = new ArrayList<>();
    private Set<String> simbolosExternos = new HashSet<>();
    private Set<String> simbolosDefinidos = new HashSet<>();

    private static final Set<String> FORMATO2 = new HashSet<>();
    static {
        FORMATO2.add("ADDR"); FORMATO2.add("SUBR"); FORMATO2.add("MULR");
        FORMATO2.add("DIVR"); FORMATO2.add("COMPR"); FORMATO2.add("SHIFTL");
        FORMATO2.add("SHIFTR"); FORMATO2.add("RMO"); FORMATO2.add("CLEAR");
        FORMATO2.add("TIXR");
    }

    private static final Set<String> DIRETIVAS = new HashSet<>();
    static {
        DIRETIVAS.add("START"); DIRETIVAS.add("END"); DIRETIVAS.add("BASE");
        DIRETIVAS.add("RESW"); DIRETIVAS.add("RESB"); DIRETIVAS.add("WORD");
        DIRETIVAS.add("BYTE"); DIRETIVAS.add("EXTDEF"); DIRETIVAS.add("EXTREF");
    }

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
            if (linha.trim().isEmpty() || linha.trim().startsWith("."))
                continue;

            String[] partes = organizarPartes(linha);
            String rotulo = partes[0];
            String instrucao = partes[1];
            String operando = partes[2];

            if (instrucao.equals("START")) {
                if (!operando.isEmpty()) {
                    locctr = Integer.parseInt(operando, 16);
                    enderecoInicial = locctr;
                }
                if (!rotulo.isEmpty()) {
                    tabelaSimbolos.adicionar(rotulo, locctr);
                }
                continue;
            }

            if (instrucao.equals("END")) {
                continue;
            }

            if (instrucao.equals("EXTDEF")) {
                String[] syms = operando.split(",");
                for (String s : syms) {
                    simbolosDefinidos.add(s.trim());
                }
                continue;
            }

            if (instrucao.equals("EXTREF")) {
                String[] syms = operando.split(",");
                for (String s : syms) {
                    simbolosExternos.add(s.trim());
                }
                continue;
            }

            if (!rotulo.isEmpty()) {
                tabelaSimbolos.adicionar(rotulo, locctr);
            }

            if (instrucao.equals("BASE") || instrucao.equals("NOBASE")) {
                continue;
            }

            locctr += calcularTamanhoInstrucao(instrucao, operando);
        }
    }

    public List<String> segundaPassagem() {
        List<String> codigoObjeto = new ArrayList<>();
        locctr = enderecoInicial;
        registrosDeReferencia.clear();

        for (String linha : linhasCodigoFonte) {
            if (linha.trim().isEmpty() || linha.trim().startsWith("."))
                continue;

            String[] partes = organizarPartes(linha);
            String instrucao = partes[1];
            String operando = partes[2];

            if (instrucao.equals("START") || instrucao.equals("END") ||
                instrucao.equals("EXTDEF") || instrucao.equals("EXTREF")) {
                continue;
            }

            if (instrucao.equals("BASE")) {
                Integer addr = tabelaSimbolos.obterEndereco(operando);
                if (addr != null)
                    valorRegistradorB = addr;
                continue;
            }

            if (instrucao.equals("NOBASE")) {
                continue;
            }

            if (instrucao.equals("RESW") || instrucao.equals("RESB")) {
                locctr += calcularTamanhoInstrucao(instrucao, operando);
                continue;
            }

            if (instrucao.equals("WORD")) {
                int valorConstante = Integer.parseInt(operando);
                codigoObjeto.add(String.format("%06X", valorConstante & 0xFFFFFF));
                locctr += 3;
                continue;
            }

            if (instrucao.equals("BYTE")) {
                String hexByte = processarByte(operando);
                codigoObjeto.add(hexByte);
                locctr += hexByte.length() / 2;
                continue;
            }

            String mnemonicoPuro = instrucao.startsWith("+") ? instrucao.substring(1) : instrucao;
            if (FORMATO2.contains(mnemonicoPuro)) {
                int hex = gerarCodigoFormato2(mnemonicoPuro, operando);
                codigoObjeto.add(String.format("%04X", hex & 0xFFFF));
                locctr += 2;
            } else {
                int hex = gerarCodigoHex(instrucao, operando);
                if (instrucao.startsWith("+")) {
                    codigoObjeto.add(String.format("%08X", hex & 0xFFFFFFFFL));
                    locctr += 4;
                } else {
                    codigoObjeto.add(String.format("%06X", hex & 0xFFFFFF));
                    locctr += 3;
                }
            }
        }
        return codigoObjeto;
    }

    private int calcularTamanhoInstrucao(String instrucao, String operando) {
        if (instrucao.equals("RESW")) {
            return Integer.parseInt(operando) * 3;
        }
        if (instrucao.equals("RESB")) {
            return Integer.parseInt(operando);
        }
        if (instrucao.equals("WORD")) {
            return 3;
        }
        if (instrucao.equals("BYTE")) {
            return processarByte(operando).length() / 2;
        }
        if (instrucao.startsWith("+")) {
            return 4;
        }
        String puro = instrucao;
        if (FORMATO2.contains(puro)) {
            return 2;
        }
        return 3;
    }

    private String processarByte(String operando) {
        if (operando.startsWith("C'") && operando.endsWith("'")) {
            String texto = operando.substring(2, operando.length() - 1);
            StringBuilder sb = new StringBuilder();
            for (char c : texto.toCharArray()) {
                sb.append(String.format("%02X", (int) c));
            }
            return sb.toString();
        } else if (operando.startsWith("X'") && operando.endsWith("'")) {
            return operando.substring(2, operando.length() - 1);
        }
        return "00";
    }

    private int gerarCodigoFormato2(String mnemonico, String operando) {
        int opcodeBase = buscarOpcode(mnemonico);
        int r1 = 0, r2 = 0;

        if (operando != null && !operando.isEmpty()) {
            String[] regs = operando.split(",");
            r1 = codigoRegistrador(regs[0].trim());
            if (regs.length > 1) {
                r2 = codigoRegistrador(regs[1].trim());
            }
        }

        return (opcodeBase << 8) | (r1 << 4) | r2;
    }

    private int codigoRegistrador(String nome) {
        switch (nome.toUpperCase()) {
            case "A": return 0;
            case "X": return 1;
            case "L": return 2;
            case "B": return 3;
            case "S": return 4;
            case "T": return 5;
            case "F": return 6;
            case "PC": return 8;
            case "SW": return 9;
            default:
                try {
                    return Integer.parseInt(nome);
                } catch (NumberFormatException e) {
                    return 0;
                }
        }
    }

    private int gerarCodigoHex(String mnnemonico, String operando) {
        if (mnnemonico.equals("RSUB")) {
            int op = buscarOpcode("RSUB");
            return ((op | 0x03) << 16);
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
                        registrosDeReferencia.add(nomeSimbolo + ":" + String.format("%04X", locctr));
                    }
                    enderecoDestino = 0;
                }
            } else {
                enderecoDestino = 0;
            }
        }

        int tamanhoInst = (f.e == 1) ? 4 : 3;
        int proximoPC = locctr + tamanhoInst;

        if (f.e == 0) {
            if (f.i == 1 && f.n == 0 && enderecoDestino >= 0 && enderecoDestino <= 4095) {
                f.p = 0;
                f.b = 0;
                f.disp = enderecoDestino;
            } else {
                calcularDeslocamento(enderecoDestino, proximoPC, f);
            }
        }

        if (f.e == 0) {
            int resultado = ((opcodeBase & 0xFC) << 16);
            resultado |= (f.n << 17) | (f.i << 16);
            resultado |= (f.x << 15) | (f.b << 14) | (f.p << 13) | (f.e << 12);
            resultado |= (f.disp & 0xFFF);
            return resultado;
        } else {
            long resultadoL = ((long) (opcodeBase & 0xFC) << 24);
            resultadoL |= ((long) f.n << 25) | ((long) f.i << 24);
            resultadoL |= ((long) f.x << 23) | ((long) f.b << 22) | ((long) f.p << 21) | ((long) f.e << 20);
            resultadoL |= (enderecoDestino & 0xFFFFF);
            return (int) resultadoL;
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
            int dispBase = enderecoDestino - valorRegistradorB;
            if (dispBase >= 0 && dispBase <= 4095) {
                flags.p = 0; flags.b = 1;
                flags.disp = dispBase;
            } else {
                flags.p = 0; flags.b = 0;
                flags.disp = enderecoDestino & 0xFFF;
            }
        }
    }

    private String[] organizarPartes(String linha) {
        // tira comentarios inline (. e ;)
        String semComentario = linha;
        int idxComentPonto = linha.indexOf(" .");
        int idxComentPontoVirgula = linha.indexOf(";");
        int idxComent = -1;
        if (idxComentPonto >= 0 && idxComentPontoVirgula >= 0) {
            idxComent = Math.min(idxComentPonto, idxComentPontoVirgula);
        } else if (idxComentPonto >= 0) {
            idxComent = idxComentPonto;
        } else if (idxComentPontoVirgula >= 0) {
            idxComent = idxComentPontoVirgula;
        }
        if (idxComent >= 0) {
            semComentario = linha.substring(0, idxComent);
        }

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
            case "LDA":    return Opcode.LDA;
            case "LDX":    return Opcode.LDX;
            case "LDL":    return Opcode.LDL;
            case "LDB":    return Opcode.LDB;
            case "LDS":    return Opcode.LDS;
            case "LDT":    return Opcode.LDT;
            case "LDCH":   return Opcode.LDCH;
            case "STA":    return Opcode.STA;
            case "STX":    return Opcode.STX;
            case "STL":    return Opcode.STL;
            case "STB":    return Opcode.STB;
            case "STS":    return Opcode.STS;
            case "STT":    return Opcode.STT;
            case "STCH":   return Opcode.STCH;
            case "ADD":    return Opcode.ADD;
            case "SUB":    return Opcode.SUB;
            case "MUL":    return Opcode.MUL;
            case "DIV":    return Opcode.DIV;
            case "COMP":   return Opcode.COMP;
            case "AND":    return Opcode.AND;
            case "OR":     return Opcode.OR;
            case "J":      return Opcode.J;
            case "JEQ":    return Opcode.JEQ;
            case "JGT":    return Opcode.JGT;
            case "JLT":    return Opcode.JLT;
            case "JSUB":   return Opcode.JSUB;
            case "RSUB":   return Opcode.RSUB;
            case "TIX":    return Opcode.TIX;
            case "ADDR":   return Opcode.ADDR;
            case "SUBR":   return Opcode.SUBR;
            case "MULR":   return Opcode.MULR;
            case "DIVR":   return Opcode.DIVR;
            case "COMPR":  return Opcode.COMPR;
            case "SHIFTL": return Opcode.SHIFTL;
            case "SHIFTR": return Opcode.SHIFTR;
            case "RMO":    return Opcode.RMO;
            case "CLEAR":  return Opcode.CLEAR;
            case "TIXR":   return Opcode.TIXR;
            default:
                throw new RuntimeException("Opcode desconhecido: " + busca);
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
                    String nome = entry.getKey();
                    if (simbolosDefinidos.isEmpty() || simbolosDefinidos.contains(nome)) {
                        writer.println(nome + ":" + String.format("%04X", entry.getValue()));
                    }
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
            System.out.println("Arquivo objeto salvo: " + arquivo.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Erro ao salvar objeto: " + e.getMessage());
        }
    }

    public TabelaSimbolos getTabelaSimbolos() { return this.tabelaSimbolos; }
    public int getTamanhoPrograma() { return this.locctr; }
    public int getEnderecoInicial() { return this.enderecoInicial; }
}
