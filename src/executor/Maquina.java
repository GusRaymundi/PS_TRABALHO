package executor;

import java.util.List;

public class Maquina {

    private final CPU cpu;
    private final Memoria memoria;

    public Maquina(int numPalavrasMemoria) {
        this.cpu = new CPU();
        this.memoria = new Memoria(numPalavrasMemoria);
    }

    public CPU getCpu() {
        return cpu;
    }

    public Memoria getMemoria() {
        return memoria;
    }

    public void carregarProgramaHex(List<String> linhas, int enderecoInicialByte) {
        int addr = enderecoInicialByte;

        for (String linha : linhas) {
            String hex = linha.trim();
            if (hex.isEmpty()) continue;

            int numBytes = hex.length() / 2;
            long valor = Long.parseLong(hex, 16);

            for (int b = numBytes - 1; b >= 0; b--) {
                int byteval = (int) ((valor >> (b * 8)) & 0xFF);
                memoria.escreverByte(addr + (numBytes - 1 - b), byteval);
            }
            addr += numBytes;
        }

        cpu.PC().setValor(enderecoInicialByte);
    }

    public void passo() {
        int pc = cpu.PC().getValorUnsigned();

        // busca e decodifica
        Instrucao inst = Instrucao.decodificar(memoria, pc);
        int proximoPC = (pc + inst.tamanhoBytes) & 0xFFFFFF;

        executarInstrucao(inst, proximoPC);
    }

    /**
     * Executa um passo e retorna uma descricao verbosa do que aconteceu.
     */
    public String passoVerboso() {
        int pc = cpu.PC().getValorUnsigned();
        Instrucao inst = Instrucao.decodificar(memoria, pc);
        int proximoPC = (pc + inst.tamanhoBytes) & 0xFFFFFF;

        // Monta hex bruto da instrucao
        StringBuilder hexBruto = new StringBuilder();
        for (int i = 0; i < inst.tamanhoBytes; i++) {
            hexBruto.append(String.format("%02X", memoria.lerByte(pc + i)));
        }

        // Captura estado antes
        String nomeOp = nomeOpcode(inst.opcode);
        String modoAddr = descreverModo(inst);

        // Captura registradores antes
        int aAntes = cpu.A().getValorUnsigned();
        int xAntes = cpu.X().getValorUnsigned();
        int lAntes = cpu.L().getValorUnsigned();
        int bAntes = cpu.B().getValorUnsigned();
        int sAntes = cpu.S().getValorUnsigned();
        int tAntes = cpu.T().getValorUnsigned();
        int ccAntes = cpu.getCC();

        // Executa
        executarInstrucao(inst, proximoPC);

        int pcDepois = cpu.PC().getValorUnsigned();

        // Monta descricao
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%04X] %-8s  %-6s  fmt=%d  %s",
                pc, hexBruto.toString(), nomeOp, inst.formato, modoAddr));

        // Mostra o que mudou
        StringBuilder mudancas = new StringBuilder();
        if (cpu.A().getValorUnsigned() != aAntes)
            mudancas.append(String.format(" A:%06X->%06X", aAntes, cpu.A().getValorUnsigned()));
        if (cpu.X().getValorUnsigned() != xAntes)
            mudancas.append(String.format(" X:%06X->%06X", xAntes, cpu.X().getValorUnsigned()));
        if (cpu.L().getValorUnsigned() != lAntes)
            mudancas.append(String.format(" L:%06X->%06X", lAntes, cpu.L().getValorUnsigned()));
        if (cpu.B().getValorUnsigned() != bAntes)
            mudancas.append(String.format(" B:%06X->%06X", bAntes, cpu.B().getValorUnsigned()));
        if (cpu.S().getValorUnsigned() != sAntes)
            mudancas.append(String.format(" S:%06X->%06X", sAntes, cpu.S().getValorUnsigned()));
        if (cpu.T().getValorUnsigned() != tAntes)
            mudancas.append(String.format(" T:%06X->%06X", tAntes, cpu.T().getValorUnsigned()));
        if (cpu.getCC() != ccAntes) {
            String ccStr = cpu.getCC() < 0 ? "LT" : cpu.getCC() > 0 ? "GT" : "EQ";
            mudancas.append(" CC->" + ccStr);
        }
        if (pcDepois != proximoPC) {
            mudancas.append(String.format(" JUMP->%04X", pcDepois));
        }

        if (mudancas.length() > 0) {
            sb.append("  |").append(mudancas);
        }

        return sb.toString();
    }

    private static String nomeOpcode(int op) {
        switch (op) {
            case Opcode.LDA: return "LDA";
            case Opcode.LDX: return "LDX";
            case Opcode.LDL: return "LDL";
            case Opcode.LDB: return "LDB";
            case Opcode.LDS: return "LDS";
            case Opcode.LDT: return "LDT";
            case Opcode.STA: return "STA";
            case Opcode.STX: return "STX";
            case Opcode.STL: return "STL";
            case Opcode.STB: return "STB";
            case Opcode.STS: return "STS";
            case Opcode.STT: return "STT";
            case Opcode.ADD: return "ADD";
            case Opcode.SUB: return "SUB";
            case Opcode.MUL: return "MUL";
            case Opcode.DIV: return "DIV";
            case Opcode.COMP: return "COMP";
            case Opcode.TIX: return "TIX";
            case Opcode.JEQ: return "JEQ";
            case Opcode.JGT: return "JGT";
            case Opcode.JLT: return "JLT";
            case Opcode.J: return "J";
            case Opcode.AND: return "AND";
            case Opcode.OR: return "OR";
            case Opcode.JSUB: return "JSUB";
            case Opcode.RSUB: return "RSUB";
            case Opcode.LDCH: return "LDCH";
            case Opcode.STCH: return "STCH";
            case Opcode.ADDR: return "ADDR";
            case Opcode.SUBR: return "SUBR";
            case Opcode.MULR: return "MULR";
            case Opcode.DIVR: return "DIVR";
            case Opcode.COMPR: return "COMPR";
            case Opcode.SHIFTL: return "SHIFTL";
            case Opcode.SHIFTR: return "SHIFTR";
            case Opcode.RMO: return "RMO";
            case Opcode.CLEAR: return "CLEAR";
            case Opcode.TIXR: return "TIXR";
            default: return String.format("?%02X", op);
        }
    }

    private static String descreverModo(Instrucao inst) {
        if (inst.formato == 2) {
            return String.format("r1=%d,r2=%d", inst.r1, inst.r2);
        }
        if (inst.formato != 3 && inst.formato != 4) return "";

        StringBuilder sb = new StringBuilder();
        boolean n = inst.n == 1, i = inst.i == 1;
        if (!n && i) sb.append("imediato");
        else if (n && !i) sb.append("indireto");
        else sb.append("simples");

        if (inst.x == 1) sb.append(",indexado");
        if (inst.p == 1) sb.append(",PC-rel");
        if (inst.b == 1) sb.append(",base-rel");
        if (inst.e == 1) sb.append(",estendido");

        sb.append(String.format(" disp=%X", inst.disp & 0xFFFFF));
        return sb.toString();
    }

    private void executarInstrucao(Instrucao inst, int proximoPC) {
        int op = inst.opcode;

        switch (op) {
            case Opcode.LDA: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                cpu.A().setValor(operando);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.LDX: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                cpu.X().setValor(operando);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.LDL: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                cpu.L().setValor(operando);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.LDB: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                cpu.B().setValor(operando);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.LDS: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                cpu.S().setValor(operando);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.LDT: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                cpu.T().setValor(operando);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.STA: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                memoria.escreverPalavraPorByte(ea, cpu.A().getValorUnsigned());
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.STX: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                memoria.escreverPalavraPorByte(ea, cpu.X().getValorUnsigned());
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.STL: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                memoria.escreverPalavraPorByte(ea, cpu.L().getValorUnsigned());
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.STB: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                memoria.escreverPalavraPorByte(ea, cpu.B().getValorUnsigned());
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.STS: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                memoria.escreverPalavraPorByte(ea, cpu.S().getValorUnsigned());
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.STT: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                memoria.escreverPalavraPorByte(ea, cpu.T().getValorUnsigned());
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.ADD: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                int resultado = cpu.A().getValor() + operando;
                cpu.A().setValor(resultado);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.SUB: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                int resultado = cpu.A().getValor() - operando;
                cpu.A().setValor(resultado);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.COMP: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                int resultado = cpu.A().getValor() - operando;
                cpu.setCCFromCompare(resultado);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.J: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                cpu.PC().setValor(ea);
                break;
            }

            case Opcode.JEQ: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                if (cpu.getCC() == 0) {
                    cpu.PC().setValor(ea);
                } else {
                    cpu.PC().setValor(proximoPC);
                }
                break;
            }

            case Opcode.JSUB: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                cpu.L().setValor(proximoPC);
                cpu.PC().setValor(ea);
                break;
            }

            case Opcode.RSUB: {
                int newPC = cpu.L().getValorUnsigned();
                cpu.PC().setValor(newPC);
                break;
            }

            case Opcode.TIX: {
                int m = lerOperandoMemoria(inst, proximoPC);

                int novoX = cpu.X().getValor() + 1;
                cpu.X().setValor(novoX);

                int resultado = cpu.X().getValor() - m;
                cpu.setCCFromCompare(resultado);

                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.LDCH: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                int byteLido = memoria.lerByte(ea);

                int a = cpu.A().getValorUnsigned();
                a = (a & 0xFFFF00) | (byteLido & 0xFF); // mantém 16 bits altos
                cpu.A().setValor(a);

                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.STCH: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                int a = cpu.A().getValorUnsigned();
                int byteA = a & 0xFF;
                memoria.escreverByte(ea, byteA);

                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.MUL: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                int resultado = cpu.A().getValor() * operando;
                cpu.A().setValor(resultado);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.DIV: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                if (operando == 0) {
                    throw new ArithmeticException("Divisão por zero em DIV");
                }
                int resultado = cpu.A().getValor() / operando;
                cpu.A().setValor(resultado);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.AND: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                int resultado = cpu.A().getValorUnsigned() & (operando & 0xFFFFFF);
                cpu.A().setValor(resultado);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.OR: {
                int operando = lerOperandoMemoria(inst, proximoPC);
                int resultado = cpu.A().getValorUnsigned() | (operando & 0xFFFFFF);
                cpu.A().setValor(resultado);
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.JGT: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                if (cpu.getCC() > 0) {
                    cpu.PC().setValor(ea);
                } else {
                    cpu.PC().setValor(proximoPC);
                }
                break;
            }

            case Opcode.JLT: {
                int ea = calcularEnderecoEfetivo(inst, proximoPC);
                if (cpu.getCC() < 0) {
                    cpu.PC().setValor(ea);
                } else {
                    cpu.PC().setValor(proximoPC);
                }
                break;
            }

            case Opcode.ADDR: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                Registrador r2 = cpu.getRegistradorPorCodigo(inst.r2);
                if (r1 != null && r2 != null) {
                    int resultado = r2.getValor() + r1.getValor();
                    r2.setValor(resultado);
                }
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.SUBR: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                Registrador r2 = cpu.getRegistradorPorCodigo(inst.r2);
                if (r1 != null && r2 != null) {
                    int resultado = r2.getValor() - r1.getValor();
                    r2.setValor(resultado);
                }
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.MULR: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                Registrador r2 = cpu.getRegistradorPorCodigo(inst.r2);
                if (r1 != null && r2 != null) {
                    int resultado = r2.getValor() * r1.getValor();
                    r2.setValor(resultado);
                }
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.DIVR: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                Registrador r2 = cpu.getRegistradorPorCodigo(inst.r2);
                if (r1 != null && r2 != null) {
                    int divisor = r1.getValor();
                    if (divisor == 0) {
                        throw new ArithmeticException("Divisão por zero em DIVR");
                    }
                    int resultado = r2.getValor() / divisor;
                    r2.setValor(resultado);
                }
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.COMPR: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                Registrador r2 = cpu.getRegistradorPorCodigo(inst.r2);
                if (r1 != null && r2 != null) {
                    int diff = r1.getValor() - r2.getValor();
                    cpu.setCCFromCompare(diff);
                }
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.SHIFTL: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                int n = inst.r2 & 0x0F; 
                if (r1 != null && n > 0) {
                    int v = r1.getValorUnsigned();
                    v = (v << n) & 0xFFFFFF; // mantém 24 bits
                    r1.setValor(v);
                }
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.SHIFTR: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                int n = inst.r2 & 0x0F;
                if (r1 != null && n > 0) {
                    int v = r1.getValorUnsigned();
                    v = v >>> n;       // deslocamento lógico à direita
                    v &= 0xFFFFFF;     // garante 24 bits
                    r1.setValor(v);
                }
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.RMO: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                Registrador r2 = cpu.getRegistradorPorCodigo(inst.r2);

                if (r1 != null && r2 != null) {
                    r2.setValor(r1.getValorUnsigned()); // copia mantendo 24 bits
                }

                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.CLEAR: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                if (r1 != null) {
                    r1.limpar();
                }
                cpu.PC().setValor(proximoPC);
                break;
            }

            case Opcode.TIXR: {
                Registrador r1 = cpu.getRegistradorPorCodigo(inst.r1);
                int novoX = cpu.X().getValor() + 1;
                cpu.X().setValor(novoX);

                if (r1 != null) {
                    int resultado = cpu.X().getValor() - r1.getValor();
                    cpu.setCCFromCompare(resultado);
                }
                cpu.PC().setValor(proximoPC);
                break;
            }

            default:
                throw new UnsupportedOperationException(
                    String.format("Opcode não existente: 0x%02X", op)
                );
        }
    }

    private int calcularEnderecoEfetivo(Instrucao inst, int proximoPC) {
        if (inst.formato != 3 && inst.formato != 4) {
            throw new IllegalArgumentException("Endereço efetivo só para formatos 3/4.");
        }

        int ea;

        if (inst.formato == 3) {
            int disp = inst.disp; // já com sinal (32 bits)
            if (inst.p == 1) {
                ea = proximoPC + disp; // PC-relative
            } else if (inst.b == 1) {
                ea = cpu.B().getValorUnsigned() + disp;
            } else {
                ea = disp & 0xFFFFF; // direto
            }
        } else {
            // formato 4 – disp = endereço de 20 bits
            int addr = inst.disp & 0xFFFFF;
            if (inst.b == 1) {
                ea = cpu.B().getValorUnsigned() + addr;
            } else if (inst.p == 1) {
                ea = proximoPC + addr; // nem sempre usado, mas deixo aqui
            } else {
                ea = addr;
            }
        }

        if (inst.x == 1) {
            ea += cpu.X().getValorUnsigned();
        }

        return ea & 0xFFFFFF;
    }

    private int lerOperandoMemoria(Instrucao inst, int proximoPC) {
        int ea = calcularEnderecoEfetivo(inst, proximoPC);

        boolean n = inst.n == 1;
        boolean i = inst.i == 1;

        if (n && i) { // simples (direto)
            return memoria.lerPalavraPorByte(ea);
        } else if (!n && i) { // imediato
            return ea; // o valor é o próprio campo
        } else if (n && !i) { // indireto
            int ptr = memoria.lerPalavraPorByte(ea);
            return memoria.lerPalavraPorByte(ptr);
        } else {
            // caso 0 e 0, trata como simples
            return memoria.lerPalavraPorByte(ea);
        }
    }
}
