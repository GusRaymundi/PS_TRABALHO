package montador;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MainLigador {
    public static void main(String[] args) {
        String pastaMontador = "objetos"; 
        
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
        
        File[] arquivos = pasta.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".obj")
        );
        
        if (arquivos == null || arquivos.length == 0) {
            System.err.println("❌ ERRO: Nenhum arquivo .obj encontrado em: " + pastaMontador);
            System.err.println("   Certifique-se que o montador gerou os arquivos objeto.");
            return;
        }
        
        List<String> arquivosObjeto = Arrays.stream(arquivos)
                                            .map(File::getAbsolutePath)
                                            .sorted() 
                                            .collect(Collectors.toList());
        
        String arquivoSaida = "executavel.obj";
        int enderecoDeCarga = 0x2000; 

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
            
            System.out.println("🔍 PASSAGEM 1: Mapeamento...");
            ligador.primeiraPassagem(arquivosObjeto);
            
            ligador.exibirTabelaGlobal();
            
            System.out.println("\n🔧 PASSAGEM 2: Relocação...");
            List<String> codigoFinal = ligador.segundaPassagem();
            
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
