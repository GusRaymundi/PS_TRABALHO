package macro;

import java.io.*;
import java.util.*;

public class MainMacro {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Uso: java macro.MainMacro <arquivo.asm>");
            return;
        }

        String arquivoEntrada = args[0];
        String arquivoSaida = "MASMAPRG.ASM";

        Map<String, Macro> tabelaMacros = new HashMap<>();

        try (
            BufferedReader reader = new BufferedReader(new FileReader(arquivoEntrada));
            BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoSaida))
        ) {

            String linha;
            Macro macroAtual = null;
            int nivelMacro = 0;

            while ((linha = reader.readLine()) != null) {

                linha = linha.trim();

                if (linha.isEmpty() || linha.startsWith(";")) {
                    continue;
                }

                String[] partes = linha.split("\\s+");

                //inicio da def de macro
                if (linha.contains(" MACRO")) {

                    String nomeMacro = partes[0];
                    macroAtual = new Macro(nomeMacro);

                    if (partes.length >= 3) {
                        String[] params = partes[2].split(",");
                        for (String p : params) {
                            macroAtual.adicionarParametro(p);
                        }
                    }

                    nivelMacro++;
                    continue;
                }

                //fim def macro
                if (linha.equals("MEND")) {

                    nivelMacro--;

                    if (nivelMacro == 0 && macroAtual != null) {
                        tabelaMacros.put(macroAtual.getNome(), macroAtual);
                        macroAtual = null;
                    }

                    continue;
                }

                //linhas
                if (nivelMacro > 0) {
                    macroAtual.adicionarLinha(linha);
                    continue;
                }

                //ve se é linha normal ou de chamada
                if (tabelaMacros.containsKey(partes[0])) {

                    expandirLinha(linha, tabelaMacros, writer);

                } else {

                    writer.write(linha);
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    //expansão recursiva
    private static void expandirLinha(String linha,
                                      Map<String, Macro> tabelaMacros,
                                      BufferedWriter writer) throws IOException {

        linha = linha.trim();

        String[] partes = linha.split("\\s+");
        String nomeMacro = partes[0];

        Macro macro = tabelaMacros.get(nomeMacro);

        if (macro == null) {
            writer.write(linha);
            writer.newLine();
            return;
        }

        //pega argumentos
        String[] argumentos = new String[0];

        if (partes.length > 1) {
            String resto = linha.substring(nomeMacro.length()).trim();
            argumentos = resto.split(",");
        }

        for (String linhaCorpo : macro.getCorpo()) {

            String linhaExpandida = linhaCorpo;

           //ALA
            for (int i = 0; i < macro.getParametros().size(); i++) {

                if (i < argumentos.length) {

                    String parametro = macro.getParametros().get(i);
                    String argumento = argumentos[i].trim();

                    linhaExpandida =
                        linhaExpandida.replace(parametro, argumento);
                }
            }

            //verifica se é aninhada
            String primeiraPalavra =
                linhaExpandida.trim().split("\\s+")[0];

            if (tabelaMacros.containsKey(primeiraPalavra)) {

                expandirLinha(linhaExpandida, tabelaMacros, writer);

            } else {

                writer.write(linhaExpandida);
                writer.newLine();
            }
        }
    }
}