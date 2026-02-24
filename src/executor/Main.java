package executor;

import java.nio.file.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {

        Path caminho = Paths.get("programa.txt");

        List<String> linhas = Files.readAllLines(caminho);
        List<String> programa = new ArrayList<>();

        for (String linha : linhas) {
            linha = linha.trim();
            if (!linha.isEmpty()) {
                programa.add(linha);
            }
        }

        Maquina maquina = new Maquina(4096);

        maquina.carregarProgramaHex(programa, 0x0000);

        System.out.println("== PROGRAMA CARREGADO ==\n");
        imprimirEstado(maquina);

        System.out.println("\n== EXECUCAO ==");

        int maxPassos = 10000;
        for (int passo = 0; passo < maxPassos; passo++) {
            int pcAntes = maquina.getCpu().PC().getValorUnsigned();
            System.out.println("\n-- Passo " + passo + " --");

            try {
                maquina.passo();
            } catch (Exception e) {
                System.out.println("Programa finalizado: " + e.getMessage());
                break;
            }

            imprimirEstado(maquina);

            int pcDepois = maquina.getCpu().PC().getValorUnsigned();
            if (pcDepois == 0 && pcAntes != 0) {
                System.out.println("\nPrograma retornou (RSUB). Finalizando.");
                break;
            }
        }

        System.out.println("\n== FIM DA EXECUCAO ==\n");
    }

    private static void imprimirEstado(Maquina m) {
        CPU c = m.getCpu();
        System.out.printf(
            "PC=%06X  A=%06X  X=%06X  L=%06X  B=%06X  S=%06X  T=%06X  CC=%d%n",
            c.PC().getValorUnsigned(),
            c.A().getValorUnsigned(),
            c.X().getValorUnsigned(),
            c.L().getValorUnsigned(),
            c.B().getValorUnsigned(),
            c.S().getValorUnsigned(),
            c.T().getValorUnsigned(),
            c.getCC()
        );
    }
}
