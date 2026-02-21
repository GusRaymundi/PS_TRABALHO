package montador;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MainLigador {
    public static void main(String[] args) {
        
        // =========================================================
        // PASTA ONDE O MONTADOR SALVA OS ARQUIVOS .obj
        // (configurável por parâmetro ou fixa)
        // =========================================================
        String pastaMontador = "objetos"; // padrão
        
        // Se passarem a pasta como argumento, usa essa
        if (args.length > 0) {
            pastaMontador = args[0];
        }
        
        File pasta = new File(pastaMontador);
        if (!pasta.exists()) {
            System.err.println("❌ ERRO: Pasta '" + pastaMontador + "' não encontrada.");
            System.err.println("   O montador precisa gerar os arquivos .obj primeiro.");
            System.err.println("   Use: java montador.MainLigador [caminho-da-pasta]");
            return;
        }
        
        // Busca TODOS os arquivos .obj (sem filtrar nada)
        File[] arquivos = pasta.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".obj")
        );
        
        if (arquivos == null || arquivos.length == 0) {
            System.err.println("❌ ERRO: Nenhum arquivo .obj encontrado em: " + pastaMontador);
            System.err.println("   Certifique-se que o montador gerou os arquivos objeto.");
            return;
        }
        
        // Lista dos arquivos a serem ligados
        List<String> arquivosObjeto = Arrays.stream(arquivos)
                                            .map(File::getAbsolutePath)
                                            .sorted() // ordena para consistência
                                            .collect(Collectors.toList());
        
        // =========================================================
        // CONFIGURAÇÕES DO LIGADOR
        // =========================================================
        String arquivoSaida = "executavel.obj";
        int enderecoDeCarga = 0x2000; // endereço padrão de carga
        
        // =========================================================
        // EXECUTAR LIGADOR
        // =========================================================
        
        System.out.println("=== LIGADOR SIC/XE ===\n");
        System.out.println("📁 Pasta: " + pastaMontador);
        System.out.println("📦 Módulos a ligar (" + arquivosObjeto.size() + "):");
        
        for (String obj : arquivosObjeto) {
            File f = new File(obj);
            System.out.println("   └─ " + f.getName() + " (" + f.length() + " bytes)");
        }
        
        System.out.println("\n📍 Endereço de carga: 0x" + 
                         Integer.toHexString(enderecoDeCarga).toUpperCase());
        System.out.println("📄 Saída: " + arquivoSaida);
        System.out.println();

        try {
            Ligador ligador = new Ligador(enderecoDeCarga);
            
            // PASSAGEM 1: Mapeamento de símbolos
            System.out.println("🔍 PASSAGEM 1: Mapeamento...");
            ligador.primeiraPassagem(arquivosObjeto);
            
            // Mostra tabela global
            ligador.exibirTabelaGlobal();
            
            // PASSAGEM 2: Geração de código
            System.out.println("\n🔧 PASSAGEM 2: Relocação...");
            List<String> codigoFinal = ligador.segundaPassagem();
            
            // Salva resultado
            ligador.salvarArquivoFinal(codigoFinal, arquivoSaida);
            
            System.out.println("\n✅ Ligação concluída com sucesso!");
            System.out.println("   Arquivo gerado: " + new File(arquivoSaida).getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("\n❌ ERRO: " + e.getMessage());
            System.err.println("\nPossíveis causas:");
            System.err.println("  • Formato de arquivo .obj inválido");
            System.err.println("  • Símbolos externos não resolvidos");
            System.err.println("  • Erro no montador (arquivos corrompidos)");
            e.printStackTrace();
        }
    }
}