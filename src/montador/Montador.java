package montador;

import executor.Opcode; // Reutiliza os opcodes já definidos
import java.util.ArrayList;
import java.util.List;

public class Montador {
    private int valorRegistradorB = 0; // Para endereçamento relativo à base
    private int locctr;
    private final TabelaSimbolos tabelaSimbolos;
    private final List<String> linhasCodigoFonte;

    // Classe auxiliar interna para organizar as flags
    private static class Flags {
        int n, i, x, b, p, e;
        int disp;
        String operandoLimpo;
    }

    public Montador(List<String> codigoFonte) {
        this.linhasCodigoFonte = codigoFonte;
        this.tabelaSimbolos = new TabelaSimbolos();
        this.locctr = 0;
    }

    // Primeira passagem: Descobre os endereços dos rótulos
    // Ajuste na Primeira Passagem
    public void primeiraPassagem() {
        locctr = 0;
        for (String linha : linhasCodigoFonte) {
            if (linha.trim().isEmpty() || linha.startsWith("."))
                continue;

            // CORREÇÃO: Usar organizarPartes para manter consistência entre as passagens
            String[] partes = organizarPartes(linha);
            String rotulo = partes[0];
            String instrucao = partes[1];

            if (!rotulo.isEmpty()) {
                tabelaSimbolos.adicionar(rotulo, locctr);
            }

            if (instrucao.equals("BASE"))
                continue;

            // CORREÇÃO: Passar a linha original para detectar rótulo corretamente
            atualizarLocctr(linha, partes);
        }
    }

    public List<String> segundaPassagem() {
        List<String> codigoObjeto = new ArrayList<>();
        locctr = 0;

        for (String linha : linhasCodigoFonte) {
            if (linha.trim().isEmpty() || linha.startsWith("."))
                continue;

            String[] partes = organizarPartes(linha);
            String instrucao = partes[1]; // Mnemônico (ex: LDA)
            String operando = partes[2]; // Rótulo ou valor (ex: TOTAL)

            // Trata diretivas que não geram código de máquina diretamente ou mudam o estado
            if (instrucao.equals("BASE")) {
                Integer addr = tabelaSimbolos.obterEndereco(operando);
                if (addr != null)
                    valorRegistradorB = addr;
                continue;
            }
            if (instrucao.equals("RESW")) {
                atualizarLocctr(linha, partes);
                continue; // RESW apenas pula espaço, não gera bytes
            }

            int hex = gerarCodigoHex(instrucao, operando);
            // Garante que o hexadecimal tenha 6 digitos (ou 8 para formato 4)
            String formatOutput = (instrucao.startsWith("+")) ? "%08X" : "%06X";
            codigoObjeto.add(String.format(formatOutput, hex & (instrucao.startsWith("+") ? 0xFFFFFFFFL : 0xFFFFFF)));

            atualizarLocctr(linha, partes);
        }
        return codigoObjeto;
    }

    private boolean linhaTemRotulo(String linha) {
        // Se a linha estiver vazia ou for apenas espaços, não tem rótulo
        if (linha == null || linha.trim().isEmpty()) {
            return false;
        }

        // Verifica se o primeiro caractere NÃO é um espaço ou tabulação
        char primeiroChar = linha.charAt(0);
        return primeiroChar != ' ' && primeiroChar != '\t';
    }

    private int gerarCodigoHex(String mnnemonico, String operando) {
        int opcodeBase = buscarOpcode(mnnemonico);
        Flags f = new Flags();

        // 1. Identifica o formato (3 ou 4)
        f.e = mnnemonico.startsWith("+") ? 1 : 0;

        // 2. Processa prefixos (#, @, X)
        processarFlagsEnderecamento(operando, f);

        // 3. Calcula o endereço de destino
        Integer enderecoDestino = tabelaSimbolos.obterEndereco(f.operandoLimpo);

        // Se o operando for um número direto (ex: #10) e não um rótulo
        if (enderecoDestino == null) {
            try {
                enderecoDestino = Integer.parseInt(f.operandoLimpo);
            } catch (NumberFormatException ex) {
                enderecoDestino = 0; // Ou tratar erro de rótulo inexistente
            }
        }

        // 4. Calcula deslocamento (disp) e flags p/b
        int proximoPC = locctr + (f.e == 1 ? 4 : 3);

        if (f.i == 1 && f.n == 0 && enderecoDestino <= 4095) {
            // Caso especial: Imediato com valor pequeno (direto no disp)
            f.p = 0;
            f.b = 0;
            f.disp = enderecoDestino;
        } else {
            calcularDeslocamento(enderecoDestino, proximoPC, f);
        }

        // 5. Montagem dos bits (Bitwise)
        int resultado;
        if (f.e == 0) { // Formato 3 (24 bits)
            resultado = (opcodeBase << 16);
            resultado |= (f.n << 17) | (f.i << 16);
            resultado |= (f.x << 15) | (f.b << 14) | (f.p << 13) | (f.e << 12);
            resultado |= (f.disp & 0xFFF);
            return resultado;
        } else { // Formato 4 (32 bits / 4 bytes)
            // O opcode base no SIC/XE ocupa os 6 bits superiores do primeiro byte
            // Deslocamos 24 bits para ele ocupar a posição correta em um Inteiro de 32 bits
            long resultadoL = ((long) (opcodeBase & 0xFC) << 24);

            // Adicionamos as flags n e i no primeiro byte
            resultadoL |= ((long) f.n << 25) | ((long) f.i << 24);

            // Adicionamos x, b, p, e (e = 1) no segundo byte
            resultadoL |= (f.x << 23) | (f.b << 22) | (f.p << 21) | (f.e << 20);

            // Adicionamos o endereço de 20 bits (disp)
            resultadoL |= (enderecoDestino & 0xFFFFF);

            // Para retornar como int (32 bits)
            return (int) resultadoL;
        }

    }

    private void atualizarLocctr(String linha, String[] partes) {
        int idx = linhaTemRotulo(linha) ? 1 : 0;
        String instrucao = partes[idx];

        if (instrucao.startsWith("+")) {
            locctr += 4;
        } else if (instrucao.equals("RESW")) {
            int quant = Integer.parseInt(partes[idx + 1]);
            locctr += (quant * 3);
        } else {
            locctr += 3;
        }
    }

    private void processarFlagsEnderecamento(String operando, Flags flags) {
        // 1. Determina n e i (Modos de Endereçamento)
        if (operando.startsWith("#")) {
            flags.n = 0;
            flags.i = 1; // Imediato
            operando = operando.substring(1);
        } else if (operando.startsWith("@")) {
            flags.n = 1;
            flags.i = 0; // Indireto
            operando = operando.substring(1);
        } else {
            flags.n = 1;
            flags.i = 1; // Simples/Direto
        }

        // 2. Determina x (Indexação)
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

        // Tenta PC-Relative primeiro (Intervalo de -2048 a 2047)
        if (deslocamento >= -2048 && deslocamento <= 2047) {
            flags.p = 1;
            flags.b = 0;
            flags.disp = deslocamento;
        }
        // Se não couber, usaria Base-Relative (exige que o registrador B esteja
        // carregado)
        else {
            flags.p = 0;
            flags.b = 1;
            flags.disp = enderecoDestino - valorRegistradorB;
        }
    }

    private String[] organizarPartes(String linha) {
        // Remove comentários (tudo após o ponto '.')
        String semComentario = linha.split("\\.")[0];
        // Divide por espaços em branco
        String[] partesBrutas = semComentario.trim().split("\\s+");

        String rotulo = "";
        String instrucao = "";
        String operando = "";

        // Se a linha original não começa com espaço, o primeiro item é um rótulo
        if (!linha.startsWith(" ") && !linha.startsWith("\t")) {
            rotulo = partesBrutas[0];
            instrucao = partesBrutas.length > 1 ? partesBrutas[1] : "";
            operando = partesBrutas.length > 2 ? partesBrutas[2] : "";
        } else {
            // Se comeca com espaço, não tem rótulo
            instrucao = partesBrutas.length > 0 ? partesBrutas[0] : "";
            operando = partesBrutas.length > 1 ? partesBrutas[1] : "";
        }

        return new String[] { rotulo, instrucao, operando };
    }

    private int buscarOpcode(String mnemonico) {
        // Se for formato 4 (ex: +LDA), removemos o '+' para buscar o opcode
        String busca = mnemonico.startsWith("+") ? mnemonico.substring(1) : mnemonico;

        switch (busca.toUpperCase()) {
            case "LDA":
                return Opcode.LDA; // Retorna 0x00
            case "LDX":
                return Opcode.LDX; // Retorna 0x04
            case "ADD":
                return Opcode.ADD; // Retorna 0x18
            case "STA":
                return Opcode.STA; // Retorna 0x0C
            case "RSUB":
                return Opcode.RSUB; // Retorna 0x4C
            case "J":
                return Opcode.J;
            default:
                return 0;
        }
    }
}