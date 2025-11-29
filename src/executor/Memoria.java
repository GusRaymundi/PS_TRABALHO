package executor;

public class Memoria {

    private final byte[] dados;
    private final int numPalavras;

    public Memoria(int numPalavras) {
        if (numPalavras <= 0) {
            throw new IllegalArgumentException("Número de palavras deve ser positivo.");
        }
        this.numPalavras = numPalavras;
        this.dados = new byte[numPalavras * 3]; // 1 palavra = 3 bytes
    }

    public int getNumPalavras() {
        return numPalavras;
    }

    public int getTamanhoEmBytes() {
        return dados.length;
    }

    public int lerPalavra(int enderecoPalavra) {
        validarEnderecoPalavra(enderecoPalavra);
        int base = enderecoPalavra * 3;

        int b1 = Byte.toUnsignedInt(dados[base]);
        int b2 = Byte.toUnsignedInt(dados[base + 1]);
        int b3 = Byte.toUnsignedInt(dados[base + 2]);

        return (b1 << 16) | (b2 << 8) | b3;
    }

    public void escreverPalavra(int enderecoPalavra, int valor) {
        validarEnderecoPalavra(enderecoPalavra);
        int base = enderecoPalavra * 3;
        valor = valor & 0xFFFFFF;

        dados[base]     = (byte) ((valor >> 16) & 0xFF);
        dados[base + 1] = (byte) ((valor >> 8)  & 0xFF);
        dados[base + 2] = (byte) (valor & 0xFF);
    }

    public int lerByte(int enderecoByte) {
        validarEnderecoByte(enderecoByte);
        return Byte.toUnsignedInt(dados[enderecoByte]);
    }

    public void escreverByte(int enderecoByte, int valor) {
        validarEnderecoByte(enderecoByte);
        dados[enderecoByte] = (byte) (valor & 0xFF);
    }

    public int lerPalavraPorByte(int enderecoByte) {
        validarEnderecoByte(enderecoByte);
        validarEnderecoByte(enderecoByte + 2);

        int b1 = Byte.toUnsignedInt(dados[enderecoByte]);
        int b2 = Byte.toUnsignedInt(dados[enderecoByte + 1]);
        int b3 = Byte.toUnsignedInt(dados[enderecoByte + 2]);

        return (b1 << 16) | (b2 << 8) | b3;
    }

    public void escreverPalavraPorByte(int enderecoByte, int valor) {
        validarEnderecoByte(enderecoByte);
        validarEnderecoByte(enderecoByte + 2);

        valor = valor & 0xFFFFFF;

        dados[enderecoByte]     = (byte) ((valor >> 16) & 0xFF);
        dados[enderecoByte + 1] = (byte) ((valor >> 8)  & 0xFF);
        dados[enderecoByte + 2] = (byte) (valor & 0xFF);
    }

    private void validarEnderecoPalavra(int enderecoPalavra) {
        if (enderecoPalavra < 0 || enderecoPalavra >= numPalavras) {
            throw new IndexOutOfBoundsException(
                "Endereço de palavra inválido: " + enderecoPalavra
            );
        }
    }

    private void validarEnderecoByte(int enderecoByte) {
        if (enderecoByte < 0 || enderecoByte >= dados.length) {
            throw new IndexOutOfBoundsException(
                "Endereço de byte inválido: " + enderecoByte
            );
        }
    }
}
