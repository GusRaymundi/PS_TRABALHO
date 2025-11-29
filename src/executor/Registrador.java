package executor;

public class Registrador {

    private final String nome;
    private final int codigo;
    private int valor24bits;

    public Registrador(String nome, int codigo) {
        this.nome = nome;
        this.codigo = codigo;
        this.valor24bits = 0;
    }

    public String getNome() {
        return nome;
    }

    public int getCodigo() {
        return codigo;
    }

    public int getValor() {
        int v = valor24bits & 0xFFFFFF;
        // se o bit de sinal (bit 23) estiver 1, estende o sinal para 32 bits
        if ((v & 0x800000) != 0) {
            v |= 0xFF000000;
        }
        return v;
    }

    public int getValorUnsigned() {
        return valor24bits & 0xFFFFFF;
    }

    public void setValor(int novoValor) {
        this.valor24bits = novoValor & 0xFFFFFF;
    }

    public void limpar() {
        this.valor24bits = 0;
    }

    @Override
    public String toString() {
        return nome + " = 0x" + String.format("%06X", getValorUnsigned());
    }
}
