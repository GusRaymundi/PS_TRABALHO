package montador;

import java.util.LinkedHashMap;
import java.util.Map;

public class TabelaSimbolos {
    private final Map<String, Integer> simbolos = new LinkedHashMap<>();

    public void adicionar(String rotulo, int endereco) {
        if (simbolos.containsKey(rotulo)) {
            throw new RuntimeException("Erro: Rótulo duplicado: " + rotulo);
        }
        simbolos.put(rotulo, endereco);
    }

    public Integer obterEndereco(String rotulo) {
        return simbolos.get(rotulo);
    }

    public Map<String, Integer> getMapaSimbolos() {
        return this.simbolos;
    }

    public void limpar() {
        simbolos.clear();
    }

}